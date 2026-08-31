package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.media.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.model.media.MediaType;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.tag.TagCategory;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.domain.repository.UserTagRepository;
import com.fafa.infrastructure.ai.AiTaskDispatcher;
import com.fafa.infrastructure.ai.SemanticSearchGateway;
import com.fafa.infrastructure.config.FaFaProperties;
import com.fafa.infrastructure.oss.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 素材应用服务
 *
 * 负责素材管理闭环（见 phase4-implementation-plan.md 第 6 节）：
 * - 批量上传（OSS 先行 → 配额原子扣减 → 落库 → AFTER_COMMIT 分发 AI 分析）
 * - 确定性检索（不限次）与 AI 语义检索（会员月限额）
 * - 编辑 / 确认归属 / 删除（先删库后删文件四步链路）
 * - embedding 向量化回调处理（幂等，并追加全部命中的宠物名称标签）
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaApplicationService {

    /** 对象存储文件夹（单桶 + 路径前缀区分模块） */
    private static final String STORAGE_FOLDER = "media";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final MediaAssetRepository mediaAssetRepository;
    private final UserTagRepository userTagRepository;
    private final QuotaApplicationService quotaApplicationService;
    private final StorageService storageService;
    private final AiTaskDispatcher aiTaskDispatcher;
    private final SemanticSearchGateway semanticSearchGateway;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties faFaProperties;
    private final PetAccessApplicationService petAccessApplicationService;

    /**
     * 批量上传素材
     *
     * 事务内：OSS 上传 → 配额原子增量（失败整批回滚并清理已上传对象）→ 落库 → 打标联动；
     * AI 分析分发注册在事务提交后（AFTER_COMMIT），避免分发到可能回滚的数据。
     */
    @Transactional
    public List<MediaDTO> upload(List<MultipartFile> files, List<String> tags, Long petId, LocalDateTime takenAt) {
        Long operatorId = getCurrentUserId();
        Long userId = operatorId;
        FaFaProperties.Media mediaConfig = faFaProperties.getMedia();
        Pet assignedPet = null;
        List<Long> candidatePetIds = List.of();

        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        if (files.size() > mediaConfig.getBatchMaxCount()) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_COUNT_EXCEEDED);
        }

//        todo member
        if (petId != null) {
            petAccessApplicationService.require(operatorId, petId, PetPermission.CREATE_RECORD);
            assignedPet = petApplicationService.getPetDomain(petId);
            userId = assignedPet.getUserId();
        } else {
            List<Pet> userPets = petApplicationService.getUserPetDomains(userId);
            if (userPets.size() == 1) {
                assignedPet = userPets.getFirst();
            } else if (userPets.size() > 1) {
                candidatePetIds = userPets.stream().map(Pet::getId).toList();
            }
        }
        LocalDateTime effectiveTakenAt = takenAt != null ? takenAt : LocalDateTime.now();

        List<String> uploadedUrls = new ArrayList<>();
        try {
            // 1. 预校验全部文件（类型与大小，任一不合法立即终止，避免浪费已上传文件）
            int count = files.size();
            MediaType[] mediaTypes = new MediaType[count];
            for (int i = 0; i < count; i++) {
                MultipartFile file = files.get(i);
                mediaTypes[i] = resolveMediaType(file);
                validateFileSize(file, mediaTypes[i], mediaConfig);
            }

            // 2. 虚拟线程并行上传（对象存储为阻塞 IO，接口耗时由 n 次之和降为最慢一次）
            String[] urls = new String[count];
            try (ExecutorService uploadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<String>> futures = new ArrayList<>(count);
                for (MultipartFile file : files) {
                    futures.add(uploadExecutor.submit(() -> storageService.upload(file, STORAGE_FOLDER)));
                }
                for (int i = 0; i < count; i++) {
                    urls[i] = joinUpload(futures.get(i));
                    uploadedUrls.add(urls[i]);
                }
            }

            // 3. 组装领域对象（顺序与入参一致）
            List<MediaAsset> assets = new ArrayList<>(count);
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                MultipartFile file = files.get(i);
                MediaAsset asset = MediaAsset.create(userId, mediaTypes[i], urls[i], file.getOriginalFilename(),
                        file.getSize(), file.getContentType(), tags,
                        assignedPet == null ? null : assignedPet.getId(), effectiveTakenAt,
                        operatorId, LocalDateTime.now());
                if (assignedPet != null) {
                    asset.addTags(List.of(assignedPet.getName()));
                }
                assets.add(asset);
                totalSize += file.getSize();
            }

            // 4. 存储配额原子增量（失败抛 STORAGE_QUOTA_EXCEEDED → 整批回滚）
            quotaApplicationService.increaseStorage(userId, totalSize);

            // 5. 落库 + 打标联动
            List<MediaDTO> result = new ArrayList<>();
            List<AnalysisTask> mediaToAnalyze = new ArrayList<>();
            for (MediaAsset asset : assets) {
                MediaAsset saved = mediaAssetRepository.save(asset);
                mediaToAnalyze.add(new AnalysisTask(saved, candidatePetIds));
                result.add(toDTO(saved));
            }
            if (tags != null && !tags.isEmpty()) {
                userTagRepository.upsertAndTouch(userId, tags, null, LocalDateTime.now());
            }

//            如果只有一个宠物就打上一个宠物名称tag
            if (assignedPet != null) {
                userTagRepository.upsertAndTouch(userId, List.of(assignedPet.getName()),
                        TagCategory.PET, LocalDateTime.now());
            }

            // 6. AFTER_COMMIT 分发照片/视频 AI 分析（虚拟线程异步执行，不阻塞接口返回）
            if (!mediaToAnalyze.isEmpty()) {
                registerAfterCommit(() -> dispatchAnalysisAsync(mediaToAnalyze));
            }

            log.info("素材上传成功: userId={}, count={}, totalSize={}", userId, files.size(), totalSize);
            return result;

        } catch (RuntimeException ex) {
            // 配额不足/落库失败：清理已上传的 OSS 对象后抛出（事务回滚）
            cleanupUploaded(uploadedUrls);
            throw ex;
        }
    }

    /**
     * 确定性检索（不限次、无 AI）
     */
    public PageDTO<MediaDTO> search(Long petId, List<String> tags, LocalDate startDate,
                                    LocalDate endDate, int page, int size) {
        Long userId = getCurrentUserId();
        String petTagName = null;
        if (petId != null) {
            petAccessApplicationService.require(userId, petId, PetPermission.READ_RECORD);
            Pet pet = petApplicationService.getPetDomain(petId);
            userId = pet.getUserId();
            petTagName = pet.getName();
        }
        int offset = (page - 1) * size;
        List<MediaAsset> assets = mediaAssetRepository.search(userId, petTagName, tags,
                startDate, endDate, offset, size);
        long total = mediaAssetRepository.countSearch(userId, petTagName, tags, startDate, endDate);
        return PageDTO.of(assets.stream().map(this::toDTO).toList(), total, page, size);
    }

    /**
     * AI 语义检索（会员月限额）
     *
     * 配额获取 → 网关检索（失败回滚配额）→ MySQL 补全（归属/删除二次过滤）→ 统计摘要。
     */
    public SemanticSearchResultDTO semanticSearch(SemanticSearchCommand command) {
        Long userId = getCurrentUserId();
        return semanticSearchForUser(userId, command);
    }

    /**
     * Agent 内部语义检索入口。
     *
     * 与公开接口共用配额、Qdrant 网关和 MySQL 二次过滤，但用户身份由内部调用方显式传入，
     * 不依赖不存在的 Sa-Token 会话。
     */
    public SemanticSearchResultDTO semanticSearchForUser(Long userId, SemanticSearchCommand command) {
        if (command.query() == null || command.query().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "检索语句不能为空");
        }
        if (command.query().length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "检索语句不能超过100字符");
        }
        if (command.petId() != null) {
            petApplicationService.checkPetOwnership(userId, command.petId());
        }
        int limit = command.limit() == null ? 20 : Math.min(Math.max(command.limit(), 1), 100);

        // 1. 配额获取（VIP 不限；普通用户 INCR 超限抛 AI_SEARCH_QUOTA_EXCEEDED）
        quotaApplicationService.tryAcquireAiSearch(userId);

        Long resolvedPetId = command.petId();
        List<SemanticSearchGateway.PetCandidate> petCandidates = List.of();
        if (resolvedPetId == null) {
            List<Pet> userPets = petApplicationService.getUserPetDomains(userId);
            if (userPets.size() == 1) {
                // 单宠物无需调用大模型做名称匹配，直接作为检索条件。
                resolvedPetId = userPets.getFirst().getId();
            } else if (userPets.size() > 1) {
                petCandidates = userPets.stream()
                        .map(pet -> new SemanticSearchGateway.PetCandidate(pet.getId(), pet.getName()))
                        .toList();
            }
        }

        List<SemanticSearchGateway.SearchHit> hits;
        try {
            // 2. 网关检索
            hits = semanticSearchGateway.search(userId, command.query(), resolvedPetId,
                    petCandidates, command.startDate(), command.endDate(), limit);
        } catch (Exception ex) {
            // 网关失败：回滚本次计数（失败请求不占额度）
            quotaApplicationService.releaseAiSearch(userId);
            log.error("语义检索网关调用失败: userId={}", userId, ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        // 3. MySQL 补全 + 归属/删除二次过滤（免疫 Qdrant 残留 point）
        List<Long> ids = hits.stream().map(SemanticSearchGateway.SearchHit::mediaId).toList();
        Map<Long, MediaAsset> assetMap = mediaAssetRepository.findByIds(userId, ids).stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        List<MediaDTO> media = hits.stream()
                .map(hit -> assetMap.get(hit.mediaId()))
                .filter(java.util.Objects::nonNull)
                .map(asset -> toDTO(asset, userId))
                .toList();

        return new SemanticSearchResultDTO(media, buildSummary(media));
    }

    /**
     * 素材详情
     */
    public MediaDTO getById(Long id) {
        return toDTO(loadAccessibleMedia(id));
    }

    /** Agent 内部读取素材详情，权限由显式 userId 校验。 */
    public MediaDTO getByIdForUser(Long userId, Long id) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!userId.equals(asset.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toDTO(asset, userId);
    }

    /** 面向协同成员的媒体读取，权限按素材所属宠物空间判断。 */
    public MediaDTO getByIdForAccess(Long userId, Long id) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (asset.getPetId() != null) {
            petAccessApplicationService.require(userId, asset.getPetId(), PetPermission.READ_RECORD);
        } else if (!userId.equals(asset.getUserId())) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        return toDTO(asset, userId);
    }

    /** 重新投递素材 AI 分析，支持历史视频和失败素材补偿。 */
    @Transactional
    public MediaDTO reanalyze(Long id) {
        MediaAsset asset = loadOwnedMedia(id);
        asset.requestAnalysis(LocalDateTime.now());
        MediaAsset saved = mediaAssetRepository.save(asset);
        List<Long> candidatePetIds = candidatePetIdsFor(saved);
        registerAfterCommit(() -> Thread.ofVirtual().name("media-analysis-", 0)
                .start(() -> dispatchMediaAnalysis(saved, candidatePetIds)));
        return toDTO(saved);
    }

    /**
     * 识别待确认列表（疑似归属 + 相似度）
     */
    public List<MediaDTO> findUnconfirmed() {
        Long userId = getCurrentUserId();
        return mediaAssetRepository.findUnconfirmed(userId, 50).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 编辑标签与拍摄时间
     */
    @Transactional
    public MediaDTO update(Long id, UpdateMediaCommand command) {
        MediaAsset asset = loadOwnedMedia(id);
        asset.updateInfo(command.tags(), command.takenAt(), LocalDateTime.now());
        MediaAsset saved = mediaAssetRepository.save(asset);
        if (asset.getTags() != null && !asset.getTags().isEmpty()) {
            userTagRepository.upsertAndTouch(getCurrentUserId(), asset.getTags(), null, LocalDateTime.now());
        }
        return toDTO(saved);
    }

    /**
     * 确认或修正归属（petId 传 null 表示清除归属）
     */
    @Transactional
    public MediaDTO confirmPet(Long id, ConfirmPetCommand command) {
        MediaAsset asset = loadOwnedMedia(id);
        Pet pet = null;
        if (command.petId() != null) {
            pet = petApplicationService.checkPetOwnership(command.petId());
        }
        asset.confirmPet(command.petId());
        if (pet != null) {
            asset.addTags(List.of(pet.getName()));
            userTagRepository.upsertAndTouch(asset.getUserId(), List.of(pet.getName()),
                    TagCategory.PET, LocalDateTime.now());
        }
        return toDTO(mediaAssetRepository.save(asset));
    }

    /**
     * 删除素材（先删库后删文件）
     *
     * 事务内：逻辑删除 + 配额释放；AFTER_COMMIT：删 OSS 对象 + 删 Qdrant 向量（失败仅记日志）。
     */
    @Transactional
    public void delete(Long id) {
        MediaAsset asset = loadOwnedMedia(id);

        mediaAssetRepository.deleteById(id);
        quotaApplicationService.decreaseStorage(asset.getUserId(), asset.getFileSize());

        String fileUrl = asset.getFileUrl();
        Long mediaId = asset.getId();
        registerAfterCommit(() -> Thread.ofVirtual().name("media-cleanup-", 0).start(() -> {
            try {
                storageService.delete(fileUrl);
            } catch (Exception ex) {
                log.error("删除 OSS 对象失败（不阻断）: mediaId={}, url={}", mediaId, fileUrl, ex);
            }
            aiTaskDispatcher.deleteMediaVector(mediaId);
        }));

        log.info("素材删除成功: mediaId={}, userId={}", id, asset.getUserId());
    }

    /**
     * embedding 回调处理（幂等：不存在/已删 → MEDIA_NOT_FOUND；已终态跳过）。
     * Python 返回的所有达阈值宠物都转换为名称标签，MySQL media_asset.tags 是业务事实来源。
     */
    @Transactional
    public void handleAnalysisCallback(PhotoAnalysisCallback callback) {
        MediaAsset asset = mediaAssetRepository.findById(callback.mediaId())
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));

        if (asset.getStatus().isTerminal()) {
            return;
        }

        if (callback.success()) {
            Map<Long, Pet> petsById = petApplicationService.getUserPetDomains(asset.getUserId()).stream()
                    .collect(Collectors.toMap(Pet::getId, Function.identity()));
            List<Long> recognizedPetIds = callback.recognizedPetIds() == null
                    ? List.of()
                    : callback.recognizedPetIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .filter(petsById::containsKey)
                    .toList();
            List<String> petNames = recognizedPetIds.stream()
                    .map(petsById::get)
                    .map(Pet::getName)
                    .toList();

            if (!asset.markVectorizationCompleted(recognizedPetIds, callback.recognitionScore(), LocalDateTime.now())) {
                return;
            }
            asset.addTags(petNames);
            if (!petNames.isEmpty()) {
                userTagRepository.upsertAndTouch(asset.getUserId(), petNames, TagCategory.PET, LocalDateTime.now());
            }
        } else {
            asset.markAnalysisFailed(callback.errorMessage());
        }
        mediaAssetRepository.save(asset);
        log.info("素材分析回调处理完成: mediaId={}, success={}", callback.mediaId(), callback.success());
    }

    /**
     * 宠物删除级联：清空该宠物名下素材归属（素材保留）
     */
    public void clearPetReference(Long petId) {
        int affected = mediaAssetRepository.clearPetReference(petId);
        if (affected > 0) {
            log.info("宠物删除级联清空素材归属: petId={}, affected={}", petId, affected);
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 加载当前用户名下素材（不存在/非本人 → 业务异常）
     */
    private MediaAsset loadOwnedMedia(Long id) {
        Long userId = getCurrentUserId();
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!asset.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return asset;
    }

    private MediaAsset loadAccessibleMedia(Long id) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        Long userId = getCurrentUserId();
        if (asset.getPetId() != null) {
            petAccessApplicationService.require(userId, asset.getPetId(), PetPermission.READ_RECORD);
        } else if (!asset.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        return asset;
    }

    /**
     * MIME 类型 → 媒体类型（image/* → photo，video/* → video）
     */
    private MediaType resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return MediaType.PHOTO;
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        throw new BusinessException(ErrorCode.MEDIA_TYPE_INVALID);
    }

    private void validateFileSize(MultipartFile file, MediaType mediaType, FaFaProperties.Media config) {
        long limit = mediaType == MediaType.PHOTO ? config.getPhotoMaxSizeBytes() : config.getVideoMaxSizeBytes();
        if (file.getSize() > limit) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_TOO_LARGE);
        }
    }

    /** 视频使用预签名 URL，永久 OSS 地址单独传递并写入 Qdrant。 */
    private void dispatchMediaAnalysis(MediaAsset asset, List<Long> candidatePetIds) {
        try {
            String analysisUrl = asset.getMediaType() == MediaType.VIDEO
                    ? storageService.generatePresignedUrl(
                    asset.getFileUrl(), faFaProperties.getAi().getMediaUrlExpireSeconds())
                    : asset.getFileUrl();
            aiTaskDispatcher.dispatchMediaAnalysis(
                    asset.getId(), asset.getUserId(), asset.getFileUrl(), analysisUrl,
                    asset.getMediaType().getCode(), asset.getTags(), asset.getPetId(),
                    candidatePetIds, asset.getTakenAt(), asset.getCreatedAt());
        } catch (Exception ex) {
            log.error("素材 AI 分析地址准备失败: mediaId={}, mediaType={}",
                    asset.getId(), asset.getMediaType(), ex);
        }
    }

    /**
     * 等待并行上传结果，失败时还原原始异常抛给调用方
     */
    private String joinUpload(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("素材上传被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new IllegalStateException("素材上传失败", cause);
        }
    }

    /**
     * 异步分发素材 AI 分析（虚拟线程执行，避免同步 HTTP 调用阻塞上传接口返回）
     */
    private void dispatchAnalysisAsync(List<AnalysisTask> mediaToAnalyze) {
        Thread.ofVirtual().name("media-analysis-", 0).start(() ->
                mediaToAnalyze.forEach(task -> dispatchMediaAnalysis(task.asset(), task.candidatePetIds())));
    }

    /** 多宠物上传时把当前用户的全部有效宠物作为 Python 匹配候选。 */
    private List<Long> candidatePetIdsFor(MediaAsset asset) {
        if (asset.getPetId() != null) {
            return List.of();
        }
        return petApplicationService.getUserPetDomains(asset.getUserId()).stream()
                .map(Pet::getId)
                .toList();
    }

    private record AnalysisTask(MediaAsset asset, List<Long> candidatePetIds) {
    }

    /**
     * 上传失败补偿：清理已上传的 OSS 对象
     */
    private void cleanupUploaded(List<String> uploadedUrls) {
        for (String url : uploadedUrls) {
            try {
                storageService.delete(url);
            } catch (Exception ex) {
                log.error("上传回滚清理 OSS 对象失败: url={}", url, ex);
            }
        }
    }

    /**
     * 注册事务提交后回调（AI 分发/文件删除必须在提交后执行）
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * 确定性统计摘要（时间分布）
     */
    private String buildSummary(List<MediaDTO> media) {
        if (media.isEmpty()) {
            return "未找到匹配的素材";
        }
        List<LocalDateTime> times = media.stream()
                .map(MediaDTO::takenAt)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (times.isEmpty()) {
            return "共找到 " + media.size() + " 个素材";
        }
        return "共找到 " + media.size() + " 个素材，拍摄时间从 "
                + times.get(0).format(DATE_FMT) + " 到 " + times.get(times.size() - 1).format(DATE_FMT);
    }

    private MediaDTO toDTO(MediaAsset asset) {
        return toDTO(asset, getCurrentUserId());
    }

    private MediaDTO toDTO(MediaAsset asset, Long userId) {
        return new MediaDTO(
                asset.getId(),
                asset.getPetId(),
                petNameOf(userId, asset.getPetId()),
                asset.getMediaType().getCode(),
                asset.getFileUrl(),
                asset.getFileName(),
                asset.getFileSize(),
                asset.getMimeType(),
                asset.getTags(),
                asset.getStatus().getCode(),
                asset.getAiDescription(),
                asset.getAiTags(),
                asset.getRecognitionScore(),
                asset.isRecognitionConfirmed(),
                asset.getTakenAt(),
                asset.getErrorMessage(),
                asset.getCreatedAt()
        );
    }

    private String petNameOf(Long userId, Long petId) {
        if (petId == null) {
            return null;
        }
        return petApplicationService.getPetDomain(petId).getName();
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}

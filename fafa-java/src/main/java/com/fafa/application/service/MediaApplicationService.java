package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.media.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.model.media.MediaType;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 素材应用服务
 *
 * 负责素材管理闭环（见 phase4-implementation-plan.md 第 6 节）：
 * - 批量上传（OSS 先行 → 配额原子扣减 → 落库 → AFTER_COMMIT 分发 AI 分析）
 * - 确定性检索（不限次）与 AI 语义检索（会员月限额）
 * - 编辑 / 确认归属 / 删除（先删库后删文件四步链路）
 * - AI 分析回调处理（幂等）
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

    /**
     * 批量上传素材
     *
     * 事务内：OSS 上传 → 配额原子增量（失败整批回滚并清理已上传对象）→ 落库 → 打标联动；
     * AI 分析分发注册在事务提交后（AFTER_COMMIT），避免分发到可能回滚的数据。
     */
    @Transactional
    public List<MediaDTO> upload(List<MultipartFile> files, List<String> tags, Long petId, LocalDateTime takenAt) {
        Long userId = getCurrentUserId();
        FaFaProperties.Media mediaConfig = faFaProperties.getMedia();

        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        if (files.size() > mediaConfig.getBatchMaxCount()) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_COUNT_EXCEEDED);
        }
        if (petId != null) {
            petApplicationService.checkPetOwnership(petId);
        }
        LocalDateTime effectiveTakenAt = takenAt != null ? takenAt : LocalDateTime.now();

        List<String> uploadedUrls = new ArrayList<>();
        try {
            // 1. OSS 上传（事务内先行，失败直接终止）
            List<MediaAsset> assets = new ArrayList<>();
            long totalSize = 0;
            for (MultipartFile file : files) {
                MediaType mediaType = resolveMediaType(file);
                validateFileSize(file, mediaType, mediaConfig);
                String url = storageService.upload(file, STORAGE_FOLDER);
                uploadedUrls.add(url);
                totalSize += file.getSize();

                assets.add(MediaAsset.create(userId, mediaType, url, file.getOriginalFilename(),
                        file.getSize(), file.getContentType(), tags, petId, effectiveTakenAt, LocalDateTime.now()));
            }

            // 2. 存储配额原子增量（失败抛 STORAGE_QUOTA_EXCEEDED → 整批回滚）
            quotaApplicationService.increaseStorage(userId, totalSize);

            // 3. 落库 + 打标联动
            List<MediaDTO> result = new ArrayList<>();
            List<MediaAsset> photosToAnalyze = new ArrayList<>();
            for (MediaAsset asset : assets) {
                MediaAsset saved = mediaAssetRepository.save(asset);
                if (saved.getMediaType() == MediaType.PHOTO) {
                    photosToAnalyze.add(saved);
                }
                result.add(toDTO(saved));
            }
            if (tags != null && !tags.isEmpty()) {
                userTagRepository.upsertAndTouch(userId, assets.get(0).getTags(), null, LocalDateTime.now());
            }

            // 4. AFTER_COMMIT 分发 AI 分析（仅照片）
            if (!photosToAnalyze.isEmpty()) {
                registerAfterCommit(() -> photosToAnalyze.forEach(photo ->
                        aiTaskDispatcher.dispatchMediaAnalysis(photo.getId(), photo.getUserId(),
                                photo.getFileUrl(), photo.getMediaType().getCode(),
                                photo.getTags(), photo.getPetId(), photo.getTakenAt())));
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
        if (petId != null) {
            petApplicationService.checkPetOwnership(petId);
        }
        int offset = (page - 1) * size;
        List<MediaAsset> assets = mediaAssetRepository.search(userId, petId, tags, startDate, endDate, offset, size);
        long total = mediaAssetRepository.countSearch(userId, petId, tags, startDate, endDate);
        return PageDTO.of(assets.stream().map(this::toDTO).toList(), total, page, size);
    }

    /**
     * AI 语义检索（会员月限额）
     *
     * 配额获取 → 网关检索（失败回滚配额）→ MySQL 补全（归属/删除二次过滤）→ 统计摘要。
     */
    public SemanticSearchResultDTO semanticSearch(SemanticSearchCommand command) {
        Long userId = getCurrentUserId();
        if (command.query() == null || command.query().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "检索语句不能为空");
        }
        if (command.query().length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "检索语句不能超过100字符");
        }
        if (command.petId() != null) {
            petApplicationService.checkPetOwnership(command.petId());
        }
        int limit = command.limit() == null ? 20 : Math.min(Math.max(command.limit(), 1), 100);

        // 1. 配额获取（VIP 不限；普通用户 INCR 超限抛 AI_SEARCH_QUOTA_EXCEEDED）
        quotaApplicationService.tryAcquireAiSearch(userId);

        List<SemanticSearchGateway.SearchHit> hits;
        try {
            // 2. 网关检索
            hits = semanticSearchGateway.search(userId, command.query(), command.petId(),
                    command.startDate(), command.endDate(), limit);
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
                .map(this::toDTO)
                .toList();

        return new SemanticSearchResultDTO(media, buildSummary(media));
    }

    /**
     * 素材详情
     */
    public MediaDTO getById(Long id) {
        return toDTO(loadOwnedMedia(id));
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
        if (command.petId() != null) {
            petApplicationService.checkPetOwnership(command.petId());
        }
        asset.confirmPet(command.petId());
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
        registerAfterCommit(() -> {
            try {
                storageService.delete(fileUrl);
            } catch (Exception ex) {
                log.error("删除 OSS 对象失败（不阻断）: mediaId={}, url={}", mediaId, fileUrl, ex);
            }
            aiTaskDispatcher.deleteMediaVector(mediaId);
        });

        log.info("素材删除成功: mediaId={}, userId={}", id, asset.getUserId());
    }

    /**
     * AI 分析回调处理（幂等：不存在/已删 → MEDIA_NOT_FOUND；已终态跳过）
     */
    @Transactional
    public void handleAnalysisCallback(PhotoAnalysisCallback callback) {
        MediaAsset asset = mediaAssetRepository.findById(callback.mediaId())
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));

        if (callback.success()) {
            asset.markAnalysisCompleted(callback.description(), callback.aiTags(),
                    callback.recognizedPetId(), callback.recognitionScore());
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
        return new MediaDTO(
                asset.getId(),
                asset.getPetId(),
                petNameOf(asset.getPetId()),
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

    private String petNameOf(Long petId) {
        if (petId == null) {
            return null;
        }
        return petApplicationService.getUserPetDomains(getCurrentUserId()).stream()
                .filter(p -> p.getId().equals(petId))
                .map(Pet::getName)
                .findFirst()
                .orElse(null);
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}

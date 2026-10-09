package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.pet.PetInfoDTO;
import com.fafa.application.dto.pet.PetPhotoSlot;
import com.fafa.application.dto.pet.PetVectorizationCallbackCommand;
import com.fafa.application.dto.pet.RegisterPetCommand;
import com.fafa.application.dto.pet.UpdatePetProfileCommand;
import com.fafa.domain.model.pet.Gender;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.model.pet.PetVectorizationStatus;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.infrastructure.ai.AiTaskDispatcher;
import com.fafa.infrastructure.oss.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 宠物应用服务
 * <p>
 * 负责协调宠物相关业务流程：
 * - 宠物注册（三张照片并行上传，位1=头像、位2=正面照、位3=侧面照）
 * - 资料更新
 * - 三视图照片替换（合并接口，槽位支持新文件或复用现有URL）
 * - 三视图向量化重试（失败或任务丢失时的补偿入口）
 * - 三视图向量化状态回调（Python 处理完成后更新状态）
 * - 排序调整
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetApplicationService {

    private final PetRepository petRepository;
    private final ReminderRepository reminderRepository;
    private final StorageService storageService;
    private final AiTaskDispatcher aiTaskDispatcher;
    private final MediaAssetRepository mediaAssetRepository;
    private final TagApplicationService tagApplicationService;
    private final PetAccessApplicationService petAccessApplicationService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 注册新宠物（三张照片必传：位1=头像、位2=正面照、位3=侧面照）
     * <p>
     * 流程：名称预检 → 并行上传三张照片（虚拟线程，事务外）→
     * 短事务持久化（先记录 PENDING 状态）→ 事务提交后异步派发向量化（后派发）。
     * OSS 上传属于慢速网络 IO，刻意放在事务外执行，避免长时间占用数据库连接；
     * 上传成功后的任何失败路径都会异步清理已上传文件，避免产生孤儿对象。
     */
    public PetInfoDTO registerPet(RegisterPetCommand command) {
        Long userId = getCurrentUserId();
        String petName = command.name() == null ? null : command.name().trim();
        ensurePetNameAvailable(userId, petName, null);

        log.info("用户注册宠物: userId={}, name={}, type={}",
                userId, petName, command.type());

        List<MultipartFile> photoFiles = requireValidPhotoFiles(command.photos());

        // 事务外并行上传（创建时尚无 petId，目录按用户组织；对象 key 含 UUID，不会冲突）
        List<String> photoUrls = uploadPhotosInParallel(photoFiles, userId, null, List.of(0, 1, 2));

        // 上传成功后任何一步失败，都异步清理已上传的文件，避免产生孤儿对象
        try {
            // 短事务：仅覆盖数据库读写（先记录状态）
            Pet pet = transactionTemplate.execute(status -> {
                // 创建宠物领域对象（三张照片齐全，向量化状态初始为 PENDING）
                Pet created = Pet.register(
                        userId,
                        petName,
                        PetType.fromCode(command.type()),
                        command.breed(),
                        Gender.fromCode(command.gender()),
                        command.birthDate(),
                        command.neutered() != null && command.neutered(),
                        command.adoptDate(),
                        command.weight(),
                        photoUrls
                );

                // 设置排序号（放到最后）
                created.adjustSortOrder(petRepository.getMaxSortOrder(userId) + 1);

                created = savePetWithUniqueName(created);
                tagApplicationService.createPetNameTagIfAbsent(userId, created.getName());

                // 事务提交后再派发向量化（后派发），避免 Python 读到未提交状态
                dispatchVectorizationAfterCommit(created);
                return created;
            });

            log.info("宠物注册成功: petId={}", pet.getId());
            return toPetInfoDTO(pet);
        } catch (RuntimeException ex) {
            Thread.ofVirtual().name("pet-photo-orphan-", 0).start(() ->
                    photoUrls.forEach(this::deleteQuietly));
            throw ex;
        }
    }

    /**
     * 获取用户所有宠物列表
     */
    public List<PetInfoDTO> getUserPets() {
        Long userId = getCurrentUserId();

        List<Pet> pets = petRepository.findByUserId(userId);

        return pets.stream()
                .map(this::toPetInfoDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户所有宠物领域对象（供其他应用服务装配宠物名等使用）
     */
    public List<Pet> getUserPetDomains(Long userId) {
        return petRepository.findByUserId(userId);
    }

    /**
     * 返回宠物领域对象，具体访问权限由调用方先通过 PetAccessApplicationService 判断。
     */
    public Pet getPetDomain(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
    }

    /**
     * 获取宠物详情
     */
    public PetInfoDTO getPetInfo(Long petId) {
        Long userId = getCurrentUserId();
        petAccessApplicationService.require(userId, petId, PetPermission.READ_PROFILE);
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        return toPetInfoDTO(pet);
    }

    /**
     * 更新宠物资料
     */
    @Transactional
    public PetInfoDTO updatePetProfile(UpdatePetProfileCommand command) {
        Long userId = getCurrentUserId();

        log.info("更新宠物资料: userId={}, petId={}", userId, command.petId());

        Pet pet = petRepository.findById(command.petId())
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        // 验证所有权
        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        String oldName = pet.getName();
        String newName = command.name() == null ? oldName : command.name().trim();
        if (!oldName.equals(newName)) {
            ensurePetNameAvailable(userId, newName, pet.getId());
        }

        // 更新资料
        pet.updateProfile(
                newName,
                command.breed(),
                command.gender() != null ? Gender.fromCode(command.gender()) : null,
                command.birthDate(),
                command.neutered() != null && command.neutered(),
                command.adoptDate()
        );

        pet = savePetWithUniqueName(pet);

        if (!oldName.equals(newName)) {
            tagApplicationService.createPetNameTagIfAbsent(userId, newName);
            Long renamedPetId = pet.getId();
            registerAfterCommit(() -> Thread.ofVirtual().name("pet-name-tag-rename-", 0).start(() -> {
                try {
                    int affected = mediaAssetRepository.replacePetNameTag(userId, oldName, newName);
                    log.info("宠物名称标签异步迁移完成: petId={}, oldName={}, newName={}, affected={}",
                            renamedPetId, oldName, newName, affected);
                } catch (Exception ex) {
                    log.error("宠物名称标签异步迁移失败: petId={}, oldName={}, newName={}",
                            renamedPetId, oldName, newName, ex);
                }
            }));
        }

        log.info("宠物资料更新成功: petId={}", pet.getId());

        return toPetInfoDTO(pet);
    }

    /**
     * 替换宠物三张照片（合并接口：位1=头像、位2=正面照、位3=侧面照）
     * <p>
     * 每个槽位二选一：上传新文件（MultipartFile），或传入现有照片 URL（仅调位序时免重传）；
     * 三张缺一不可。新文件通过虚拟线程并行上传。
     * <p>
     * 性能与一致性设计：
     * 1. OSS 并行上传在事务外执行（慢速网络 IO 不占用数据库连接），落库使用短事务；
     * 2. 向量化状态在落库时已被领域层重置为 PENDING（先记录状态），
     *    事务提交后再派发向量化（后派发），避免 Python 拿到旧库状态；
     * 3. 按位序三元组完全不变且向量化已完成时直接跳过；纯调位序（URL 集合相同、顺序变化）
     *    仍会重新派发——Python 端按 view 分别 upsert，view 与照片的对应关系已改变；
     * 4. 被替换的旧文件在事务提交后异步清理（仅删除未在新位序中复用的 URL）；
     *    落库失败时异步清理本次新上传的文件，避免产生孤儿对象；
     * 5. 对象 key 含 UUID（{folder}/{yyyyMM}/{uuid}.{ext}），新上传不会覆盖旧文件。
     */
    public PetInfoDTO replacePetPhotos(Long petId, PetPhotoSlot avatarSlot, PetPhotoSlot frontSlot, PetPhotoSlot sideSlot) {
        Long userId = getCurrentUserId();

        log.info("替换宠物三视图: userId={}, petId={}", userId, petId);

        // 事务外完成读取、校验与上传
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        List<PetPhotoSlot> slots = List.of(avatarSlot, frontSlot, sideSlot);
        validateSlots(slots, pet);

        String[] oldUrls = pet.getPhotosInOrder().toArray(String[]::new);
        String[] newUrls = new String[Pet.REQUIRED_PHOTO_COUNT];
        List<MultipartFile> uploadFiles = new ArrayList<>();
        List<Integer> uploadIndexes = new ArrayList<>();
        for (int i = 0; i < Pet.REQUIRED_PHOTO_COUNT; i++) {
            PetPhotoSlot slot = slots.get(i);
            if (slot.hasFile()) {
                uploadFiles.add(slot.file());
                uploadIndexes.add(i);
            } else {
                newUrls[i] = slot.reuseUrl();
            }
        }

        // 三元组完全未变化且向量化已完成时，跳过重传与重新向量化
        if (uploadFiles.isEmpty() && Arrays.equals(oldUrls, newUrls)
                && pet.getVectorizationStatus() == PetVectorizationStatus.COMPLETED) {
            log.info("三视图未变化且向量化已完成，跳过替换: petId={}", petId);
            return toPetInfoDTO(pet);
        }

        // 事务外并行上传新文件槽位
        List<String> uploadedUrls = uploadFiles.isEmpty()
                ? List.of()
                : uploadPhotosInParallel(uploadFiles, userId, petId, uploadIndexes);
        for (int i = 0; i < uploadedUrls.size(); i++) {
            newUrls[uploadIndexes.get(i)] = uploadedUrls.get(i);
        }

        try {
            // 短事务：仅覆盖数据库读写（先记录状态）
            Pet savedPet = transactionTemplate.execute(status -> {
                // 领域更新：三张照片齐全校验 + 向量化状态重置为 PENDING
                pet.updatePhotos(newUrls[0], newUrls[1], newUrls[2]);
                Pet result = petRepository.save(pet);

                // 事务提交后异步清理被替换的旧文件（复用调位序的 URL 必须保留）+ 派发向量化
                Set<String> retained = new HashSet<>(List.of(newUrls));
                List<String> staleUrls = Arrays.stream(oldUrls)
                        .filter(url -> url != null && !retained.contains(url))
                        .toList();
                registerAfterCommit(() -> Thread.ofVirtual().name("pet-photo-vector-", 0).start(() -> {
                    staleUrls.forEach(this::deleteQuietly);
                    aiTaskDispatcher.dispatchPetPhotoVectorization(
                            petId, userId, newUrls[0], newUrls[1], newUrls[2]);
                }));
                return result;
            });

            log.info("宠物三视图替换成功: petId={}", petId);
            return toPetInfoDTO(savedPet);
        } catch (RuntimeException ex) {
            // 落库失败：异步清理本次新上传的文件，避免产生孤儿对象（复用的旧 URL 不受影响）
            Thread.ofVirtual().name("pet-photo-orphan-", 0).start(() ->
                    uploadedUrls.forEach(this::deleteQuietly));
            throw ex;
        }
    }

    /**
     * 处理 Python 三视图向量化回调（幂等）
     * <p>
     * 乱序回调防护：已 COMPLETED 的宠物不会被迟到的失败回调降级；
     * 重复回调直接忽略，不产生多余写入。
     */
    @Transactional
    public void handleVectorizationCallback(PetVectorizationCallbackCommand command) {
        log.info("收到向量化回调: petId={}, success={}, message={}",
                command.petId(), command.success(), command.message());

        Pet pet = petRepository.findById(command.petId())
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在: " + command.petId()));

        boolean changed = Boolean.TRUE.equals(command.success())
                ? pet.markVectorizationCompleted()
                : pet.markVectorizationFailed();
        if (!changed) {
            log.info("向量化回调为重复或过期通知，忽略: petId={}, status={}",
                    command.petId(), pet.getVectorizationStatus().getCode());
            return;
        }
        petRepository.save(pet);

        log.info("向量化状态更新成功: petId={}, status={}",
                command.petId(), pet.getVectorizationStatus().getCode());
    }

    /**
     * 重新触发宠物三视图向量化（补偿入口）
     * <p>
     * 面向向量化失败、任务丢失等异常场景：状态重置为 PENDING 落库（先记录状态），
     * 事务提交后重新派发向量化（后派发）。
     * 三张照片缺失（存量数据）或向量化已完成时拒绝执行。
     */
    @Transactional
    public PetInfoDTO revectorizePetPhotos(Long petId) {
        Long userId = getCurrentUserId();

        log.info("重新触发三视图向量化: userId={}, petId={}", userId, petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        if (!pet.hasThreePhotos()) {
            throw new IllegalArgumentException("宠物三视图照片不齐全，请先补齐照片再重新向量化");
        }
        if (!pet.resetVectorizationToPending()) {
            throw new IllegalArgumentException("当前向量化已完成，无需重新处理");
        }
        petRepository.save(pet);

        log.info("宠物向量化状态已重置为待处理: petId={}", petId);

        dispatchVectorizationAfterCommit(pet);
        return toPetInfoDTO(pet);
    }

    /**
     * 等待并行上传结果，失败时还原原始异常抛给调用方
     */
    private String joinUpload(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("照片上传被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new IllegalStateException("照片上传失败", cause);
        }
    }

    /**
     * 静默删除旧文件（仅记录日志，失败不影响主流程）
     */
    private void deleteQuietly(String fileUrl) {
        if (fileUrl == null) {
            return;
        }
        try {
            storageService.delete(fileUrl);
        } catch (Exception ex) {
            log.warn("清理旧照片文件失败（忽略）: url={}", fileUrl, ex);
        }
    }

    /**
     * 校验并收集创建宠物时的三张照片文件（位1头像、位2正面照、位3侧面照）
     */
    private List<MultipartFile> requireValidPhotoFiles(List<MultipartFile> photos) {
        if (photos == null || photos.size() != Pet.REQUIRED_PHOTO_COUNT) {
            throw new IllegalArgumentException("必须上传三张照片：位1头像、位2正面照、位3侧面照");
        }
        for (int i = 0; i < photos.size(); i++) {
            requireImageFile(photos.get(i), i + 1);
        }
        return photos;
    }

    /**
     * 校验替换槽位：每个槽位必须提供新文件或属于该宠物现有照片的 URL
     */
    private void validateSlots(List<PetPhotoSlot> slots, Pet pet) {
        Set<String> currentUrls = new HashSet<>(pet.getPhotosInOrder());
        for (int i = 0; i < slots.size(); i++) {
            PetPhotoSlot slot = slots.get(i);
            int position = i + 1;
            if (slot == null || (!slot.hasFile() && !slot.hasReuseUrl())) {
                throw new IllegalArgumentException("第" + position + "张照片缺失：需上传新文件或传入现有照片URL");
            }
            if (slot.hasFile()) {
                requireImageFile(slot.file(), position);
            } else if (!currentUrls.contains(slot.reuseUrl())) {
                throw new IllegalArgumentException("第" + position + "张照片复用的URL不属于该宠物现有照片");
            }
        }
    }

    /**
     * 虚拟线程并行上传照片（阻塞 IO 适合虚拟线程），返回按传入顺序排列的 URL 列表
     */
    private List<String> uploadPhotosInParallel(List<MultipartFile> files, Long userId, Long petId, List<Integer> indexes) {
        try (ExecutorService uploadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>(files.size());
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String folder = photoFolder(indexes.get(i), userId, petId);
                futures.add(uploadExecutor.submit(() -> storageService.upload(file, folder)));
            }
            return futures.stream()
                    .map(this::joinUpload)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 照片存储目录：位1=头像、位2=正面、位3=侧面；创建时无 petId 则按用户目录存放
     */
    private String photoFolder(int index, Long userId, Long petId) {
        String scope = petId != null ? "pet/" + petId : "user/" + userId;
        return switch (index) {
            case 0 -> "avatar/" + scope;
            case 1 -> "profile/" + scope + "/front";
            case 2 -> "profile/" + scope + "/side";
            default -> throw new IllegalStateException("意外的照片位序: " + index);
        };
    }

    /**
     * 事务提交后异步派发三视图向量化（落库时状态已是 PENDING，先记录状态、后派发）
     */
    private void dispatchVectorizationAfterCommit(Pet pet) {
        Long petId = pet.getId();
        Long userId = pet.getUserId();
        String avatarUrl = pet.getAvatarUrl();
        String frontPhotoUrl = pet.getFrontPhotoUrl();
        String sidePhotoUrl = pet.getSidePhotoUrl();
        registerAfterCommit(() -> Thread.ofVirtual().name("pet-photo-vector-", 0).start(() ->
                aiTaskDispatcher.dispatchPetPhotoVectorization(
                        petId, userId, avatarUrl, frontPhotoUrl, sidePhotoUrl)));
    }

    private void requireImageFile(MultipartFile file, int position) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("第" + position + "张照片不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("第" + position + "张照片必须是图片文件");
        }
    }

    /**
     * 更新宠物体重
     */
    @Transactional
    public PetInfoDTO updateWeight(Long petId, BigDecimal weight) {
        Long userId = getCurrentUserId();

        log.info("更新宠物体重: userId={}, petId={}, weight={}", userId, petId, weight);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        pet.updateWeight(weight);
        pet = petRepository.save(pet);

        log.info("宠物体重更新成功: petId={}", petId);

        return toPetInfoDTO(pet);
    }

    /**
     * 调整宠物排序
     * <p>
     * 业务规则：
     * 1. 将目标宠物移动到指定位置
     * 2. 其他宠物顺延，保证排序号唯一且连续
     */
    @Transactional
    public void adjustSortOrder(Long petId, Integer sortOrder) {
        Long userId = getCurrentUserId();

        log.info("调整宠物排序: userId={}, petId={}, targetSortOrder={}", userId, petId, sortOrder);

        // 获取用户所有宠物（已按 sortOrder 升序排列）
        List<Pet> pets = petRepository.findByUserId(userId);

        // 找到目标宠物并从列表移除
        Pet targetPet = pets.stream()
                .filter(p -> p.getId().equals(petId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!targetPet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        pets.remove(targetPet);

        // 限制目标位置在有效范围内
        int targetIndex = Math.max(0, Math.min(sortOrder - 1, pets.size()));
        pets.add(targetIndex, targetPet);

        // 重新分配排序号（从1开始连续递增）
        for (int i = 0; i < pets.size(); i++) {
            pets.get(i).adjustSortOrder(i + 1);
        }

        // 批量更新
        petRepository.batchUpdateSortOrder(pets);

        log.info("宠物排序调整成功: petId={}, 共更新{}只宠物", petId, pets.size());
    }

    /**
     * 删除宠物（逻辑删除）
     */
    @Transactional
    public void deletePet(Long petId) {
        Long userId = getCurrentUserId();

        log.info("删除宠物: userId={}, petId={}", userId, petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        // 逻辑删除：走 MyBatis-Plus deleteById（生成 UPDATE ... SET is_deleted=1），
        // 不能用 save()/updateById（MyBatis-Plus 会剔除 @TableLogic 字段，is_deleted 不会被更新）
        petRepository.delete(petId);

        // 素材属于用户而非宠物，删除宠物只清空当前归属并保留素材本身。
        mediaAssetRepository.clearPetReference(petId);

        // 级联逻辑删除该宠物的全部提醒（提醒是活着的待办，宠物删除后不应继续被扫描推送；
        // 完成历史与事件记录保留，快照可独立展示）
        reminderRepository.deleteByPetId(petId);

        // 清理 Qdrant 中的宠物照片向量（幂等，失败不影响删除主流程）
        aiTaskDispatcher.cleanupPetVectors(petId);

        log.info("宠物删除成功: petId={}", petId);
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 同一用户的有效宠物不允许使用重复名称，名称比较口径为 trim 后的精确匹配。 */
    private void ensurePetNameAvailable(Long userId, String name, Long excludedPetId) {
        if (name == null || name.isBlank()) {
            return;
        }
        boolean duplicated = petRepository.findByUserId(userId).stream()
                .anyMatch(item -> !item.getId().equals(excludedPetId) && name.equals(item.getName()));
        if (duplicated) {
            throw new IllegalArgumentException("同一用户下宠物名称不能重复");
        }
    }

    /** 应用层预检之外，再把并发下的数据库唯一约束冲突转换成明确的业务提示。 */
    private Pet savePetWithUniqueName(Pet pet) {
        try {
            return petRepository.save(pet);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("同一用户下宠物名称不能重复", ex);
        }
    }

    /** 事务提交后再执行标签迁移、向量分发等非关键异步动作。 */
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
     * 校验宠物归属权（供记录模块等应用服务复用）
     * <p>
     * 校验当前登录用户是否拥有该宠物，返回宠物领域对象。
     *
     * @param petId 宠物ID
     * @return 归属当前用户的宠物
     * @throws IllegalArgumentException 宠物不存在或不属于当前用户
     */
    public Pet checkPetOwnership(Long petId) {
        Long userId = getCurrentUserId();

        return checkPetOwnership(userId, petId);
    }

    /**
     * 校验指定用户对宠物的所有权，供 Python Agent 内部查询使用。
     */
    public Pet checkPetOwnership(Long userId, Long petId) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        return pet;
    }

    private PetInfoDTO toPetInfoDTO(Pet pet) {
        return new PetInfoDTO(
                pet.getId(),
                pet.getUserId(),
                pet.getName(),
                pet.getType().getCode(),
                pet.getBreed(),
                pet.getGender().getCode(),
                pet.getBirthDate(),
                pet.isNeutered(),
                pet.getAdoptDate(),
                pet.getWeight(),
                pet.getAvatarUrl(),
                pet.getFrontPhotoUrl(),
                pet.getSidePhotoUrl(),
                pet.getPhotosInOrder(),
                pet.getVectorizationStatus() != null ? pet.getVectorizationStatus().getCode() : null,
                pet.getSortOrder(),
                pet.hasThreePhotos()
        );
    }
}

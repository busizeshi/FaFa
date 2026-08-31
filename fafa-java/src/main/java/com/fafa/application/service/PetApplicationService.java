package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.pet.PetInfoDTO;
import com.fafa.application.dto.pet.RegisterPetCommand;
import com.fafa.application.dto.pet.UpdatePetProfileCommand;
import com.fafa.domain.model.pet.Gender;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetType;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 宠物应用服务
 * <p>
 * 负责协调宠物相关业务流程：
 * - 宠物注册
 * - 资料更新
 * - 照片上传
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

    /**
     * 注册新宠物
     */
    @Transactional
    public PetInfoDTO registerPet(RegisterPetCommand command) {
        Long userId = getCurrentUserId();
        String petName = command.name() == null ? null : command.name().trim();
        ensurePetNameAvailable(userId, petName, null);

        log.info("用户注册宠物: userId={}, name={}, type={}",
                userId, petName, command.type());

        // 创建宠物领域对象
        Pet pet = Pet.register(
                userId,
                petName,
                PetType.fromCode(command.type()),
                command.breed(),
                Gender.fromCode(command.gender()),
                command.birthDate(),
                command.neutered() != null && command.neutered(),
                command.adoptDate(),
                command.weight()
        );

        // 设置排序号（放到最后）
        int maxSortOrder = petRepository.getMaxSortOrder(userId);
        pet.adjustSortOrder(maxSortOrder + 1);

        // 持久化
        pet = savePetWithUniqueName(pet);
        tagApplicationService.createPetNameTagIfAbsent(userId, pet.getName());

        log.info("宠物注册成功: petId={}", pet.getId());

        return toPetInfoDTO(pet);
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
            Long ownerId = userId;
            Long renamedPetId = pet.getId();
            registerAfterCommit(() -> Thread.ofVirtual().name("pet-name-tag-rename-", 0).start(() -> {
                try {
                    int affected = mediaAssetRepository.replacePetNameTag(ownerId, oldName, newName);
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
     * 上传宠物头像
     * <p>
     * 上传后异步触发向量化（头像 + 已有的正面/侧面照一起重新嵌入，Qdrant upsert 幂等）
     */
    @Transactional
    public PetInfoDTO uploadAvatar(Long petId, MultipartFile file) {
        Long userId = getCurrentUserId();

        log.info("上传宠物头像: userId={}, petId={}", userId, petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        // 清理旧头像
        if (pet.getAvatarUrl() != null) {
            storageService.delete(pet.getAvatarUrl());
        }

        // 上传到存储服务
        String avatarUrl = storageService.upload(file, "avatar/pet/" + petId);

        pet.updateAvatar(avatarUrl);
        pet = petRepository.save(pet);

        log.info("宠物头像上传成功: petId={}, url={}", petId, avatarUrl);

        String finalAvatarUrl = pet.getAvatarUrl();
        String finalFrontPhotoUrl = pet.getFrontPhotoUrl();
        String finalSidePhotoUrl = pet.getSidePhotoUrl();
        registerAfterCommit(() -> Thread.ofVirtual().name("pet-photo-vector-", 0).start(() ->
                aiTaskDispatcher.dispatchPetPhotoVectorization(
                        petId, userId, finalAvatarUrl, finalFrontPhotoUrl, finalSidePhotoUrl)));

        return toPetInfoDTO(pet);
    }

    /**
     * 上传宠物三视图照片（正面照+侧面照）
     * <p>
     * 性能设计：
     * 1. 正面照与侧面照通过虚拟线程并行上传，接口耗时由两次上传之和降为最慢一次；
     * 2. 旧文件清理与向量化（图片下载 + embedding 推理 + 写 Qdrant，读超时 120s）
     *    均为非关键路径，放到虚拟线程异步执行，不阻塞接口响应；
     * 3. 对象 key 含 UUID（{folder}/{yyyyMM}/{uuid}.{ext}），新上传不会覆盖旧文件，
     *    因此旧文件清理可安全后置到新文件上传成功之后。
     */
    public PetInfoDTO uploadProfilePhotos(Long petId, MultipartFile frontPhoto, MultipartFile sidePhoto) {
        Long userId = getCurrentUserId();

        log.info("上传宠物三视图: userId={}, petId={}", userId, petId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        // 记录旧照片 URL，待新照片上传成功后异步清理
        String oldFrontPhotoUrl = pet.getFrontPhotoUrl();
        String oldSidePhotoUrl = pet.getSidePhotoUrl();

        // 并行上传正面照与侧面照（虚拟线程，适合对象存储这类阻塞 IO）
        String frontPhotoUrl;
        String sidePhotoUrl;
        try (ExecutorService uploadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> frontFuture = uploadExecutor.submit(() ->
                    storageService.upload(frontPhoto, "profile/pet/" + petId + "/front"));
            Future<String> sideFuture = uploadExecutor.submit(() ->
                    storageService.upload(sidePhoto, "profile/pet/" + petId + "/side"));
            frontPhotoUrl = joinUpload(frontFuture);
            sidePhotoUrl = joinUpload(sideFuture);
        }

        pet.updateProfilePhotos(frontPhotoUrl, sidePhotoUrl);
        pet = petRepository.save(pet);

        log.info("宠物三视图上传成功: petId={}", petId);

        // 异步执行旧文件清理 + 向量化分发（均为幂等/可兜底操作，失败只记日志不影响主流程）
        String avatarUrl = pet.getAvatarUrl();
        String finalFrontPhotoUrl = frontPhotoUrl;
        String finalSidePhotoUrl = sidePhotoUrl;
        Thread.ofVirtual().name("pet-photo-vector-", 0).start(() -> {
            deleteQuietly(oldFrontPhotoUrl);
            deleteQuietly(oldSidePhotoUrl);
            aiTaskDispatcher.dispatchPetPhotoVectorization(
                    petId, userId, avatarUrl, finalFrontPhotoUrl, finalSidePhotoUrl);
        });

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
                pet.getSortOrder(),
                pet.hasProfilePhotos()
        );
    }
}

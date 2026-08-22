package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.pet.PetInfoDTO;
import com.fafa.application.dto.pet.RegisterPetCommand;
import com.fafa.application.dto.pet.UpdatePetProfileCommand;
import com.fafa.domain.model.pet.Gender;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.ai.AiTaskDispatcher;
import com.fafa.infrastructure.oss.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物应用服务
 * 
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
    private final StorageService storageService;
    private final AiTaskDispatcher aiTaskDispatcher;

    /**
     * 注册新宠物
     */
    @Transactional
    public PetInfoDTO registerPet(RegisterPetCommand command) {
        Long userId = getCurrentUserId();
        
        log.info("用户注册宠物: userId={}, name={}, type={}", 
            userId, command.name(), command.type());

        // 创建宠物领域对象
        Pet pet = Pet.register(
            userId,
            command.name(),
            PetType.fromCode(command.type()),
            command.breed(),
            Gender.fromCode(command.gender()),
            command.birthDate(),
            command.neutered() != null && command.neutered(),
            command.adoptDate()
        );

        // 设置排序号（放到最后）
        int maxSortOrder = petRepository.getMaxSortOrder(userId);
        pet.adjustSortOrder(maxSortOrder + 1);

        // 持久化
        pet = petRepository.save(pet);

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
     * 获取宠物详情
     */
    public PetInfoDTO getPetInfo(Long petId) {
        Long userId = getCurrentUserId();
        
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
        
        // 验证所有权
        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问此宠物");
        }
        
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

        // 更新资料
        pet.updateProfile(
            command.name(),
            command.breed(),
            command.gender() != null ? Gender.fromCode(command.gender()) : null,
            command.birthDate(),
            command.neutered() != null && command.neutered(),
            command.adoptDate()
        );

        pet = petRepository.save(pet);

        log.info("宠物资料更新成功: petId={}", pet.getId());

        return toPetInfoDTO(pet);
    }

    /**
     * 上传宠物头像
     *
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

        return toPetInfoDTO(pet);
    }

    /**
     * 上传宠物三视图照片（正面照+侧面照）
     *
     * 上传后异步触发向量化（正面/侧面 + 已有头像一起重新嵌入，Qdrant upsert 幂等）
     */
    @Transactional
    public PetInfoDTO uploadProfilePhotos(Long petId, MultipartFile frontPhoto, MultipartFile sidePhoto) {
        Long userId = getCurrentUserId();

        log.info("上传宠物三视图: userId={}, petId={}", userId, petId);

        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("宠物不存在"));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此宠物");
        }

        // 清理旧三视图
        if (pet.getFrontPhotoUrl() != null) {
            storageService.delete(pet.getFrontPhotoUrl());
        }
        if (pet.getSidePhotoUrl() != null) {
            storageService.delete(pet.getSidePhotoUrl());
        }

        // 上传照片
        String frontPhotoUrl = storageService.upload(frontPhoto, "profile/pet/" + petId + "/front");
        String sidePhotoUrl = storageService.upload(sidePhoto, "profile/pet/" + petId + "/side");

        pet.updateProfilePhotos(frontPhotoUrl, sidePhotoUrl);
        pet = petRepository.save(pet);

        log.info("宠物三视图上传成功: petId={}", petId);

        // 触发向量化：传当前全部照片URL
        aiTaskDispatcher.dispatchPetPhotoVectorization(
            petId, userId, pet.getAvatarUrl(), frontPhotoUrl, sidePhotoUrl);

        return toPetInfoDTO(pet);
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
     *
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

        pet.delete();
        petRepository.save(pet);

        log.info("宠物删除成功: petId={}", petId);
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
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

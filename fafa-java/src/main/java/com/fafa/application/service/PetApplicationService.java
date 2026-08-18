package com.fafa.application.service;

import cn.hutool.core.util.StrUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetGender;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.pet.PetSpecies;
import com.fafa.domain.repository.PetRepository;
import com.fafa.interfaces.dto.request.UpdatePetRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 宠物应用服务
 * 
 * 职责：
 * 1. 协调领域对象完成业务用例
 * 2. 事务控制
 * 3. 不包含业务逻辑（业务逻辑在 Domain 层）
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetApplicationService {

    private final PetRepository petRepository;

    /**
     * 创建宠物
     * 
     * @param userId 用户 ID
     * @param name 宠物名称
     * @param speciesCode 种类代码
     * @return 宠物 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPet(Long userId, String name, String speciesCode) {
        log.info("创建宠物: userId={}, name={}, species={}", userId, name, speciesCode);

        // 1. 业务规则校验
        int petCount = petRepository.countByUserId(userId);
        if (petCount >= 10) {
            throw new BusinessException("每个用户最多只能创建 10 只宠物");
        }

        // 2. 创建领域对象（业务逻辑在领域对象中）
        PetSpecies species = PetSpecies.fromCode(speciesCode);
        Pet pet = Pet.create(userId, name, species);

        // 3. 持久化
        Pet savedPet = petRepository.save(pet);

        log.info("宠物创建成功: petId={}", savedPet.getId().getValue());
        return savedPet.getId().getValue();
    }

    /**
     * 查询宠物详情
     * 
     * @param petId 宠物 ID
     * @return 宠物实体
     */
    public Pet getPet(Long petId) {
        return petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
    }

    /**
     * 查询用户的宠物列表
     * 
     * @param userId 用户 ID
     * @return 宠物列表
     */
    public List<Pet> listUserPets(Long userId) {
        return petRepository.findByUserId(userId);
    }

    /**
     * 更新宠物体重
     * 
     * @param petId 宠物 ID
     * @param weight 新体重
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePetWeight(Long petId, Double weight) {
        log.info("更新宠物体重: petId={}, weight={}", petId, weight);

        // 1. 加载领域对象
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));

        // 2. 执行领域逻辑
        pet.updateWeight(weight);

        // 3. 持久化
        petRepository.save(pet);

        log.info("宠物体重更新成功: petId={}, weight={}", petId, weight);
    }

    /**
     * 更新宠物信息
     * 
     * @param petId 宠物 ID
     * @param userId 用户 ID
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePet(Long petId, Long userId, UpdatePetRequest request) {
        log.info("更新宠物信息: petId={}, userId={}", petId, userId);

        // 1. 加载领域对象
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));

        // 2. 权限校验
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该宠物");
        }

        // 3. 更新属性
        if (StrUtil.isNotBlank(request.getName())) {
            pet.setName(request.getName());
        }
        if (StrUtil.isNotBlank(request.getAvatar())) {
            pet.setAvatar(request.getAvatar());
        }
        if (StrUtil.isNotBlank(request.getBreed())) {
            pet.setBreed(request.getBreed());
        }
        if (StrUtil.isNotBlank(request.getGender())) {
            pet.setGender(PetGender.fromCode(request.getGender()));
        }
        if (request.getBirthDate() != null) {
            pet.setBirthDate(request.getBirthDate());
        }
        if (request.getAdoptDate() != null) {
            pet.setAdoptDate(request.getAdoptDate());
        }
        if (request.getWeight() != null) {
            pet.updateWeight(request.getWeight().doubleValue());
        }
        if (request.getIsNeutered() != null) {
            pet.setIsNeutered(request.getIsNeutered());
        }
        if (StrUtil.isNotBlank(request.getCoatColor())) {
            pet.setCoatColor(request.getCoatColor());
        }
        if (request.getRemarks() != null) {
            pet.setRemarks(request.getRemarks());
        }

        // 4. 持久化
        petRepository.save(pet);

        log.info("宠物信息更新成功: petId={}", petId);
    }

    /**
     * 更新宠物头像
     * 
     * @param petId 宠物 ID
     * @param userId 用户 ID
     * @param avatarUrl 头像 URL
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePetAvatar(Long petId, Long userId, String avatarUrl) {
        log.info("更新宠物头像: petId={}, userId={}, avatarUrl={}", petId, userId, avatarUrl);

        // 1. 加载领域对象
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));

        // 2. 权限校验
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该宠物");
        }

        // 3. 更新头像
        pet.setAvatar(avatarUrl);

        // 4. 持久化
        petRepository.save(pet);

        log.info("宠物头像更新成功: petId={}", petId);
    }

    /**
     * 宠物排序
     * 
     * @param userId 用户 ID
     * @param petIds 宠物 ID 列表（按顺序）
     */
    @Transactional(rollbackFor = Exception.class)
    public void sortPets(Long userId, List<Long> petIds) {
        log.info("宠物排序: userId={}, petIds={}", userId, petIds);

        for (int i = 0; i < petIds.size(); i++) {
            Long petId = petIds.get(i);
            Pet pet = petRepository.findById(new PetId(petId))
                    .orElseThrow(() -> new BusinessException("宠物不存在: " + petId));

            // 权限校验
            if (!pet.getUserId().equals(userId)) {
                throw new BusinessException("无权修改该宠物");
            }

            // 更新排序
            pet.setSortOrder(i);
            petRepository.save(pet);
        }

        log.info("宠物排序成功");
    }

    /**
     * 删除宠物
     * 
     * @param petId 宠物 ID
     * @param userId 用户 ID（用于权限校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePet(Long petId, Long userId) {
        log.info("删除宠物: petId={}, userId={}", petId, userId);

        // 1. 加载领域对象
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));

        // 2. 权限校验
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该宠物");
        }

        // 3. 软删除
        petRepository.deleteById(new PetId(petId));

        log.info("宠物删除成功: petId={}", petId);
    }
}

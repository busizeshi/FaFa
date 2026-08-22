package com.fafa.application.service.pet;

import com.fafa.application.service.pet.impl.ConfigurablePetQuotaPolicy;
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
import java.time.LocalDate;
import java.util.List;

/**
 * 宠物应用服务
 *
 * @author FaFa Team
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetApplicationService {

    private final PetRepository petRepository;
    private final PetQuotaPolicy petQuotaPolicy;
    private final StorageService storageService;
    private final AiTaskDispatcher aiTaskDispatcher;

    /**
     * 创建宠物
     */
    @Transactional
    public Pet createPet(Long userId, String name, String typeCode, String breed,
                         String genderCode, LocalDate birthDate, boolean neutered,
                         LocalDate adoptDate, BigDecimal weight) {
        int currentCount = petRepository.countByUserId(userId);
        if (!petQuotaPolicy.canCreatePet(userId, currentCount)) {
            throw new IllegalStateException(
                String.format("Pet limit reached. Current: %d, Max: %d", 
                    currentCount, petQuotaPolicy.getMaxPets(userId)));
        }

        PetType type = PetType.fromCode(typeCode);
        Gender gender = Gender.fromCode(genderCode);

        Pet pet = Pet.register(userId, name, type, breed, gender, birthDate, neutered, adoptDate);
        if (weight != null) {
            pet.updateWeight(weight);
        }
        petRepository.save(pet);

        log.info("Pet created: userId={}, petId={}, name={}", userId, pet.getId(), name);
        return pet;
    }

    /**
     * 更新宠物基本资料
     */
    @Transactional
    public Pet updatePet(Long userId, Long petId, String name, String breed,
                         String genderCode, LocalDate birthDate, 
                         boolean neutered, LocalDate adoptDate, BigDecimal weight) {
        Pet pet = getPetAndCheckOwnership(petId, userId);

        Gender gender = genderCode != null ? Gender.fromCode(genderCode) : pet.getGender();
        pet.updateProfile(name, breed, gender, birthDate, neutered, adoptDate);
        
        if (weight != null) {
            pet.updateWeight(weight);
        }

        petRepository.save(pet);
        log.info("Pet profile updated: petId={}, userId={}", petId, userId);
        return pet;
    }

    /**
     * 上传宠物头像
     */
    @Transactional
    public Pet uploadAvatar(Long userId, Long petId, MultipartFile file) {
        Pet pet = getPetAndCheckOwnership(petId, userId);

        String oldUrl = pet.getAvatarUrl();
        if (oldUrl != null) {
            storageService.delete(oldUrl);
        }

        String avatarUrl = storageService.upload(file, "avatar");
        pet.updateAvatar(avatarUrl);
        petRepository.save(pet);

        log.info("Pet avatar uploaded: petId={}, url={}", petId, avatarUrl);
        return pet;
    }

    /**
     * 上传三视图照片（正面照+侧面照）
     */
    @Transactional
    public void uploadProfilePhotos(Long userId, Long petId, 
                                   MultipartFile frontPhoto, 
                                   MultipartFile sidePhoto) {
        Pet pet = getPetAndCheckOwnership(petId, userId);

        String oldFrontUrl = pet.getFrontPhotoUrl();
        String oldSideUrl = pet.getSidePhotoUrl();

        String frontUrl = storageService.upload(frontPhoto, "profile");
        String sideUrl = storageService.upload(sidePhoto, "profile");

        try {
            pet.updateProfilePhotos(frontUrl, sideUrl);
            petRepository.save(pet);

            if (oldFrontUrl != null) {
                storageService.delete(oldFrontUrl);
            }
            if (oldSideUrl != null) {
                storageService.delete(oldSideUrl);
            }

            aiTaskDispatcher.dispatchProfilePhotoVectorization(
                petId, frontPhoto, sidePhoto, frontUrl, sideUrl);

            log.info("Pet profile photos uploaded: petId={}, front={}, side={}", 
                petId, frontUrl, sideUrl);

        } catch (Exception e) {
            storageService.delete(frontUrl);
            storageService.delete(sideUrl);
            throw e;
        }
    }

    /**
     * 调整宠物排序
     */
    @Transactional
    public void reorderPets(Long userId, List<Long> petIds) {
        for (int i = 0; i < petIds.size(); i++) {
            Long petId = petIds.get(i);
            Pet pet = getPetAndCheckOwnership(petId, userId);
            pet.adjustSortOrder(i);
            petRepository.save(pet);
        }
        log.info("Pets reordered: userId={}, count={}", userId, petIds.size());
    }

    /**
     * 删除宠物（逻辑删除）
     */
    @Transactional
    public void deletePet(Long userId, Long petId) {
        Pet pet = getPetAndCheckOwnership(petId, userId);
        pet.delete();
        petRepository.save(pet);
        log.info("Pet deleted: petId={}, userId={}", petId, userId);
    }

    /**
     * 查询用户所有宠物
     */
    public List<Pet> listPets(Long userId) {
        return petRepository.findByUserId(userId);
    }

    /**
     * 查询宠物详情
     */
    public Pet getPetDetail(Long userId, Long petId) {
        return getPetAndCheckOwnership(petId, userId);
    }

    /**
     * 获取宠物并检查所有权
     */
    private Pet getPetAndCheckOwnership(Long petId, Long userId) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("Pet not found: " + petId));

        if (!pet.getUserId().equals(userId)) {
            throw new IllegalStateException("Pet does not belong to user");
        }

        if (pet.isDeleted()) {
            throw new IllegalStateException("Pet has been deleted");
        }

        return pet;
    }
}

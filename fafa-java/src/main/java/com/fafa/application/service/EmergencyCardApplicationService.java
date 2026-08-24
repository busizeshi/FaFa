package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.emergency.EmergencyCardDTO;
import com.fafa.application.dto.emergency.EmergencyProfileDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.emergency.PetEmergencyProfile;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.PetEmergencyProfileRepository;
import com.fafa.domain.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 应急资料维护和卡片事实组装，明确区分用户确认数据与宠物档案数据。 */
@Service
@RequiredArgsConstructor
public class EmergencyCardApplicationService {
    private final PetAccessApplicationService accessService;
    private final PetRepository petRepository;
    private final PetEmergencyProfileRepository profileRepository;

    public EmergencyProfileDTO getProfile(Long petId) {
        accessService.require(StpUtil.getLoginIdAsLong(), petId, PetPermission.READ_EMERGENCY);
        return profileRepository.findByPetId(petId).map(this::toDTO).orElseGet(() -> emptyProfile(petId));
    }

    @Transactional
    public EmergencyProfileDTO update(Long petId, EmergencyProfileDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        accessService.requireOwner(userId, petId);
        PetEmergencyProfile profile = profileRepository.findForUpdateByPetId(petId).orElse(null);
        try {
            if (profile == null) {
                if (request.version() != 0) throw new BusinessException(ErrorCode.EMERGENCY_PROFILE_VERSION_CONFLICT);
                profile = PetEmergencyProfile.create(petId, userId, LocalDateTime.now());
            }
            profile.update(request.bloodType(), request.allergies(), request.chronicConditions(),
                    request.emergencyContactName(), request.emergencyContactPhone(), request.primaryHospitalName(),
                    request.primaryHospitalPhone(), request.primaryHospitalAddress(), request.notes(), request.version(),
                    userId, LocalDateTime.now());
        } catch (BusinessException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.EMERGENCY_PROFILE_VERSION_CONFLICT);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.EMERGENCY_PROFILE_INVALID, ex.getMessage());
        }
        return toDTO(profileRepository.save(profile));
    }

    public EmergencyCardDTO card(Long petId) {
        accessService.require(StpUtil.getLoginIdAsLong(), petId, PetPermission.READ_EMERGENCY);
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        PetEmergencyProfile profile = profileRepository.findByPetId(petId).orElse(null);
        EmergencyCardDTO.ProfileSummary summary = profile == null
                ? new EmergencyCardDTO.ProfileSummary(null, null, null, null)
                : new EmergencyCardDTO.ProfileSummary(profile.getBloodType(), profile.getAllergies(), profile.getChronicConditions(), profile.getNotes());
        EmergencyCardDTO.Contact contact = profile == null ? null
                : new EmergencyCardDTO.Contact(profile.getEmergencyContactName(), profile.getEmergencyContactPhone());
        EmergencyCardDTO.Hospital hospital = profile == null ? null
                : new EmergencyCardDTO.Hospital(profile.getPrimaryHospitalName(), profile.getPrimaryHospitalPhone(), profile.getPrimaryHospitalAddress());
        return new EmergencyCardDTO(new EmergencyCardDTO.PetSummary(pet.getId(), pet.getName(), pet.getType().getCode(),
                pet.getBreed(), pet.getGender().getCode(), pet.getBirthDate(), pet.getWeight()), summary, contact, hospital,
                profile == null ? "not_filled" : "user_confirmed", profile == null ? null : profile.getUpdatedAt());
    }

    private EmergencyProfileDTO emptyProfile(Long petId) {
        return new EmergencyProfileDTO(petId, null, null, null, null, null, null, null, null, null, 0, null);
    }

    private EmergencyProfileDTO toDTO(PetEmergencyProfile profile) {
        return new EmergencyProfileDTO(profile.getPetId(), profile.getBloodType(), profile.getAllergies(), profile.getChronicConditions(),
                profile.getEmergencyContactName(), profile.getEmergencyContactPhone(), profile.getPrimaryHospitalName(),
                profile.getPrimaryHospitalPhone(), profile.getPrimaryHospitalAddress(), profile.getNotes(), profile.getVersion(), profile.getUpdatedAt());
    }
}

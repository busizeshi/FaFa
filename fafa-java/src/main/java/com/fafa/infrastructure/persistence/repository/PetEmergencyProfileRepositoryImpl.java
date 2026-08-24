package com.fafa.infrastructure.persistence.repository;

import com.fafa.domain.model.emergency.PetEmergencyProfile;
import com.fafa.domain.repository.PetEmergencyProfileRepository;
import com.fafa.infrastructure.persistence.mapper.PetEmergencyProfileMapper;
import com.fafa.infrastructure.persistence.po.PetEmergencyProfilePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PetEmergencyProfileRepositoryImpl implements PetEmergencyProfileRepository {
    private final PetEmergencyProfileMapper mapper;

    @Override
    public PetEmergencyProfile save(PetEmergencyProfile profile) {
        PetEmergencyProfilePO po = toPO(profile);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<PetEmergencyProfile> findByPetId(Long petId) { return Optional.ofNullable(toDomain(mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PetEmergencyProfilePO>().eq(PetEmergencyProfilePO::getPetId, petId)))); }

    @Override
    public Optional<PetEmergencyProfile> findForUpdateByPetId(Long petId) { return Optional.ofNullable(toDomain(mapper.selectForUpdate(petId))); }

    private PetEmergencyProfile toDomain(PetEmergencyProfilePO po) {
        if (po == null) return null;
        return PetEmergencyProfile.restore(po.getId(), po.getPetId(), po.getBloodType(), po.getAllergies(), po.getChronicConditions(),
                po.getEmergencyContactName(), po.getEmergencyContactPhone(), po.getPrimaryHospitalName(), po.getPrimaryHospitalPhone(),
                po.getPrimaryHospitalAddress(), po.getNotes(), po.getVersion(), po.getUpdatedBy(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private PetEmergencyProfilePO toPO(PetEmergencyProfile profile) {
        PetEmergencyProfilePO po = new PetEmergencyProfilePO();
        po.setId(profile.getId()); po.setPetId(profile.getPetId()); po.setBloodType(profile.getBloodType()); po.setAllergies(profile.getAllergies());
        po.setChronicConditions(profile.getChronicConditions()); po.setEmergencyContactName(profile.getEmergencyContactName());
        po.setEmergencyContactPhone(profile.getEmergencyContactPhone()); po.setPrimaryHospitalName(profile.getPrimaryHospitalName());
        po.setPrimaryHospitalPhone(profile.getPrimaryHospitalPhone()); po.setPrimaryHospitalAddress(profile.getPrimaryHospitalAddress());
        po.setNotes(profile.getNotes()); po.setVersion(profile.getVersion()); po.setUpdatedBy(profile.getUpdatedBy());
        return po;
    }
}

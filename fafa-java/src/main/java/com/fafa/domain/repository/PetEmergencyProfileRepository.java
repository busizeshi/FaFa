package com.fafa.domain.repository;

import com.fafa.domain.model.emergency.PetEmergencyProfile;

import java.util.Optional;

public interface PetEmergencyProfileRepository {
    PetEmergencyProfile save(PetEmergencyProfile profile);
    Optional<PetEmergencyProfile> findByPetId(Long petId);
    Optional<PetEmergencyProfile> findForUpdateByPetId(Long petId);
}

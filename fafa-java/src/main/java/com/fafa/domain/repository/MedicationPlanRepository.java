package com.fafa.domain.repository;

import com.fafa.domain.model.medical.MedicationPlan;

import java.util.List;
import java.util.Optional;

public interface MedicationPlanRepository {

    MedicationPlan save(MedicationPlan plan);

    Optional<MedicationPlan> findById(Long id);

    List<MedicationPlan> findByPetId(Long petId, String status);

    void deleteById(Long id);
}

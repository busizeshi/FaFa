package com.fafa.domain.repository;

import com.fafa.domain.model.health.HealthAlert;

import java.util.List;
import java.util.Optional;

public interface HealthAlertRepository {

    HealthAlert save(HealthAlert alert);

    Optional<HealthAlert> findById(Long id);

    Optional<HealthAlert> findByFingerprint(Long petId, String ruleCode, String fingerprint);

    List<HealthAlert> findByPetId(Long petId, String status, int limit);

    List<HealthAlert> findActiveByPetId(Long petId);
}

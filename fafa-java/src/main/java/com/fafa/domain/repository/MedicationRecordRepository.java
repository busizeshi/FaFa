package com.fafa.domain.repository;

import com.fafa.domain.model.medical.MedicationRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicationRecordRepository {

    MedicationRecord save(MedicationRecord record);

    Optional<MedicationRecord> findByPlanIdAndScheduledAt(Long planId, LocalDateTime scheduledAt);

    Optional<MedicationRecord> findById(Long id);

    List<MedicationRecord> findByPetIdAndTimeRange(Long petId, LocalDateTime start,
                                                   LocalDateTime end);
}

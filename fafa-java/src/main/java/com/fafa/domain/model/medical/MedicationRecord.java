package com.fafa.domain.model.medical;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** 实际用药记录，唯一性由计划和计划执行时间共同保证。 */
@Getter
public final class MedicationRecord {

    private Long id;
    private final Long userId;
    private final Long petId;
    private final Long medicationPlanId;
    private final Long reminderId;
    private final LocalDateTime scheduledAt;
    private LocalDateTime takenAt;
    private BigDecimal dosageValue;
    private String dosageUnit;
    private MedicationRecordStatus status;
    private String notes;
    private final Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MedicationRecord(Long id, Long userId, Long petId, Long medicationPlanId,
                             Long reminderId, LocalDateTime scheduledAt, LocalDateTime takenAt,
                             BigDecimal dosageValue, String dosageUnit,
                             MedicationRecordStatus status, String notes, Long operatorId,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.medicationPlanId = medicationPlanId;
        this.reminderId = reminderId;
        this.scheduledAt = scheduledAt;
        this.takenAt = takenAt;
        this.dosageValue = dosageValue;
        this.dosageUnit = dosageUnit;
        this.status = status;
        this.notes = notes;
        this.operatorId = operatorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MedicationRecord create(Long userId, Long petId, Long planId, Long reminderId,
                                          LocalDateTime scheduledAt, LocalDateTime takenAt,
                                          BigDecimal dosageValue, String dosageUnit,
                                          MedicationRecordStatus status, String notes,
                                          Long operatorId, LocalDateTime now,
                                          long futureToleranceMinutes) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(planId, "medicationPlanId 不能为空");
        Objects.requireNonNull(scheduledAt, "scheduledAt 不能为空");
        validate(dosageValue, dosageUnit, status, notes, takenAt, now, futureToleranceMinutes);
        return new MedicationRecord(null, userId, petId, planId, reminderId, scheduledAt,
                takenAt, dosageValue, dosageUnit.trim(), status, trimToNull(notes), operatorId,
                null, null);
    }

    public static MedicationRecord restore(Long id, Long userId, Long petId, Long planId,
                                           Long reminderId, LocalDateTime scheduledAt,
                                           LocalDateTime takenAt, BigDecimal dosageValue,
                                           String dosageUnit, MedicationRecordStatus status,
                                           String notes, Long operatorId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new MedicationRecord(id, userId, petId, planId, reminderId, scheduledAt,
                takenAt, dosageValue, dosageUnit, status, notes, operatorId, createdAt, updatedAt);
    }

    public void updateStatus(MedicationRecordStatus newStatus, LocalDateTime newTakenAt,
                             BigDecimal newDosageValue, String newDosageUnit, String newNotes,
                             LocalDateTime now, long futureToleranceMinutes) {
        validate(newDosageValue, newDosageUnit, newStatus, newNotes, newTakenAt, now,
                futureToleranceMinutes);
        this.status = newStatus;
        this.takenAt = newTakenAt;
        this.dosageValue = newDosageValue;
        this.dosageUnit = newDosageUnit.trim();
        this.notes = trimToNull(newNotes);
    }

    private static void validate(BigDecimal dosage, String unit, MedicationRecordStatus status,
                                 String notes, LocalDateTime takenAt, LocalDateTime now,
                                 long futureToleranceMinutes) {
        if (dosage == null || dosage.signum() <= 0) {
            throw new IllegalArgumentException("实际剂量必须为正数");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("剂量单位不能为空");
        }
        Objects.requireNonNull(status, "status 不能为空");
        if (notes != null && notes.length() > 500) {
            throw new IllegalArgumentException("用药备注不能超过 500 字符");
        }
        if (status == MedicationRecordStatus.TAKEN && takenAt == null) {
            throw new IllegalArgumentException("已用药记录必须填写实际时间");
        }
        if (takenAt != null && takenAt.isAfter(now.plusMinutes(futureToleranceMinutes))) {
            throw new IllegalArgumentException("实际用药时间不能晚于当前时间");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.fafa.domain.model.medical;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 用药计划聚合根，提醒由应用服务根据执行时间创建。 */
@Getter
public final class MedicationPlan {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_SPECIFICATION_LENGTH = 100;
    private static final int MAX_PURPOSE_LENGTH = 500;
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final int MAX_ADMINISTRATION_TIMES = 6;

    private Long id;
    private final Long userId;
    private final Long petId;
    private final Long sourceVisitId;
    private String medicationName;
    private String specification;
    private String purpose;
    private BigDecimal dosageValue;
    private String dosageUnit;
    private MedicationFrequencyType frequencyType;
    private Integer frequencyValue;
    private List<LocalTime> administrationTimes;
    private MealRelation mealRelation;
    private LocalDate startDate;
    private LocalDate endDate;
    private MedicationPlanStatus status;
    private String notes;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MedicationPlan(Long id, Long userId, Long petId, Long sourceVisitId,
                           String medicationName, String specification, String purpose,
                           BigDecimal dosageValue, String dosageUnit,
                           MedicationFrequencyType frequencyType, Integer frequencyValue,
                           List<LocalTime> administrationTimes, MealRelation mealRelation,
                           LocalDate startDate, LocalDate endDate, MedicationPlanStatus status,
                           String notes, boolean deleted, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.sourceVisitId = sourceVisitId;
        this.medicationName = medicationName;
        this.specification = specification;
        this.purpose = purpose;
        this.dosageValue = dosageValue;
        this.dosageUnit = dosageUnit;
        this.frequencyType = frequencyType;
        this.frequencyValue = frequencyValue;
        this.administrationTimes = List.copyOf(administrationTimes);
        this.mealRelation = mealRelation;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.notes = notes;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MedicationPlan create(Long userId, Long petId, Long sourceVisitId,
                                        String medicationName, String specification,
                                        String purpose, BigDecimal dosageValue,
                                        String dosageUnit, MedicationFrequencyType frequencyType,
                                        Integer frequencyValue, List<LocalTime> administrationTimes,
                                        MealRelation mealRelation, LocalDate startDate,
                                        LocalDate endDate, String notes, LocalDate today) {
        validateIdentity(userId, petId, startDate);
        validateFields(medicationName, specification, purpose, dosageValue, dosageUnit,
                frequencyType, frequencyValue, administrationTimes, mealRelation,
                startDate, endDate, notes, today);
        return new MedicationPlan(null, userId, petId, sourceVisitId, medicationName.trim(),
                trimToNull(specification), trimToNull(purpose), dosageValue,
                dosageUnit.trim(), frequencyType, frequencyValue,
                normalizeTimes(administrationTimes), mealRelation, startDate, endDate,
                MedicationPlanStatus.ACTIVE, trimToNull(notes), false, null, null);
    }

    public static MedicationPlan restore(Long id, Long userId, Long petId, Long sourceVisitId,
                                         String medicationName, String specification,
                                         String purpose, BigDecimal dosageValue,
                                         String dosageUnit, MedicationFrequencyType frequencyType,
                                         Integer frequencyValue, List<LocalTime> administrationTimes,
                                         MealRelation mealRelation, LocalDate startDate,
                                         LocalDate endDate, MedicationPlanStatus status,
                                         String notes, boolean deleted, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new MedicationPlan(id, userId, petId, sourceVisitId, medicationName,
                specification, purpose, dosageValue, dosageUnit, frequencyType,
                frequencyValue, administrationTimes == null ? List.of() : administrationTimes,
                mealRelation, startDate, endDate, status, notes, deleted, createdAt, updatedAt);
    }

    public void update(String medicationName, String specification, String purpose,
                       BigDecimal dosageValue, String dosageUnit,
                       MedicationFrequencyType frequencyType, Integer frequencyValue,
                       List<LocalTime> administrationTimes, MealRelation mealRelation,
                       LocalDate startDate, LocalDate endDate, String notes, LocalDate today) {
        if (status == MedicationPlanStatus.COMPLETED || status == MedicationPlanStatus.DELETED) {
            throw new IllegalStateException("当前用药计划不可编辑");
        }
        validateFields(medicationName, specification, purpose, dosageValue, dosageUnit,
                frequencyType, frequencyValue, administrationTimes, mealRelation,
                startDate, endDate, notes, today);
        this.medicationName = medicationName.trim();
        this.specification = trimToNull(specification);
        this.purpose = trimToNull(purpose);
        this.dosageValue = dosageValue;
        this.dosageUnit = dosageUnit.trim();
        this.frequencyType = frequencyType;
        this.frequencyValue = frequencyValue;
        this.administrationTimes = normalizeTimes(administrationTimes);
        this.mealRelation = mealRelation;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = trimToNull(notes);
    }

    public void pause() {
        ensureStatus(MedicationPlanStatus.ACTIVE);
        status = MedicationPlanStatus.PAUSED;
    }

    public void resume() {
        ensureStatus(MedicationPlanStatus.PAUSED);
        status = MedicationPlanStatus.ACTIVE;
    }

    public void complete() {
        if (status != MedicationPlanStatus.ACTIVE && status != MedicationPlanStatus.PAUSED) {
            throw new IllegalStateException("当前用药计划不可结束");
        }
        status = MedicationPlanStatus.COMPLETED;
    }

    public void delete() {
        status = MedicationPlanStatus.DELETED;
        deleted = true;
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !deleted && status == MedicationPlanStatus.ACTIVE
                && !date.isBefore(startDate)
                && (endDate == null || !date.isAfter(endDate));
    }

    private void ensureStatus(MedicationPlanStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("用药计划当前状态不支持该操作");
        }
    }

    private static void validateIdentity(Long userId, Long petId, LocalDate startDate) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(startDate, "startDate 不能为空");
    }

    private static void validateFields(String name, String specification, String purpose,
                                       BigDecimal dosage, String unit,
                                       MedicationFrequencyType frequencyType,
                                       Integer frequencyValue, List<LocalTime> times,
                                       MealRelation mealRelation, LocalDate start,
                                       LocalDate end, String notes, LocalDate today) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("药品名称不能为空");
        }
        validateLength(name, MAX_NAME_LENGTH, "药品名称");
        validateLength(specification, MAX_SPECIFICATION_LENGTH, "药品规格");
        validateLength(purpose, MAX_PURPOSE_LENGTH, "用药用途");
        validateLength(notes, MAX_NOTES_LENGTH, "备注");
        if (dosage == null || dosage.signum() <= 0) {
            throw new IllegalArgumentException("每次剂量必须为正数");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("剂量单位不能为空");
        }
        Objects.requireNonNull(frequencyType, "frequencyType 不能为空");
        Objects.requireNonNull(mealRelation, "mealRelation 不能为空");
        Objects.requireNonNull(start, "startDate 不能为空");
        if (end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("疗程结束日期不能早于开始日期");
        }
        if (start.isAfter(today)) {
            throw new IllegalArgumentException("疗程开始日期不能晚于今天");
        }
        if (frequencyType == MedicationFrequencyType.AS_NEEDED) {
            if (frequencyValue != null && frequencyValue <= 0) {
                throw new IllegalArgumentException("按需用药频次值必须为正数");
            }
            if (times != null && !times.isEmpty()) {
                throw new IllegalArgumentException("按需用药不能配置固定执行时间");
            }
        } else {
            if (times == null || times.isEmpty()) {
                throw new IllegalArgumentException("固定频次必须配置执行时间");
            }
            if (frequencyValue == null || frequencyValue <= 0) {
                throw new IllegalArgumentException("频次值必须为正数");
            }
        }
        normalizeTimes(times);
    }

    private static List<LocalTime> normalizeTimes(List<LocalTime> times) {
        List<LocalTime> values = times == null ? List.of() : new ArrayList<>(times);
        if (values.size() > MAX_ADMINISTRATION_TIMES) {
            throw new IllegalArgumentException("每日执行时间不能超过 " + MAX_ADMINISTRATION_TIMES + " 个");
        }
        if (values.stream().anyMatch(Objects::isNull) || values.size() != new HashSet<>(values).size()) {
            throw new IllegalArgumentException("执行时间不能为空且不能重复");
        }
        values.sort(LocalTime::compareTo);
        return List.copyOf(values);
    }

    private static void validateLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + "不能超过 " + max + " 字符");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

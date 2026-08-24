package com.fafa.domain.model.medical;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 就诊档案聚合根，只保存用户或医生提供的医疗事实。 */
@Getter
public final class MedicalVisit {

    private static final int MAX_HOSPITAL_LENGTH = 200;
    private static final int MAX_DOCTOR_LENGTH = 100;
    private static final int MAX_COMPLAINT_LENGTH = 1000;
    private static final int MAX_DIAGNOSIS_LENGTH = 2000;
    private static final int MAX_PRESCRIPTION_LENGTH = 3000;
    private static final int MAX_TREATMENT_LENGTH = 2000;
    private static final int MAX_NOTES_LENGTH = 2000;

    private Long id;
    private final Long userId;
    private final Long petId;
    private final LocalDate visitDate;
    private String hospitalName;
    private String doctorName;
    private String chiefComplaint;
    private String diagnosis;
    private String prescription;
    private String treatment;
    private BigDecimal totalCost;
    private String notes;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MedicalVisit(Long id, Long userId, Long petId, LocalDate visitDate,
                         String hospitalName, String doctorName, String chiefComplaint,
                         String diagnosis, String prescription, String treatment,
                         BigDecimal totalCost, String notes, boolean deleted,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.visitDate = visitDate;
        this.hospitalName = hospitalName;
        this.doctorName = doctorName;
        this.chiefComplaint = chiefComplaint;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.treatment = treatment;
        this.totalCost = totalCost;
        this.notes = notes;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MedicalVisit create(Long userId, Long petId, LocalDate visitDate,
                                      String hospitalName, String doctorName,
                                      String chiefComplaint, String diagnosis,
                                      String prescription, String treatment,
                                      BigDecimal totalCost, String notes,
                                      LocalDate today) {
        validateIdentity(userId, petId, visitDate);
        if (visitDate.isAfter(today)) {
            throw new IllegalArgumentException("就诊日期不能晚于今天");
        }
        validateFields(hospitalName, doctorName, chiefComplaint, diagnosis,
                prescription, treatment, totalCost, notes);
        return new MedicalVisit(null, userId, petId, visitDate, hospitalName.trim(),
                trimToNull(doctorName), trimToNull(chiefComplaint), trimToNull(diagnosis),
                trimToNull(prescription), trimToNull(treatment), totalCost, trimToNull(notes),
                false, null, null);
    }

    public static MedicalVisit restore(Long id, Long userId, Long petId, LocalDate visitDate,
                                       String hospitalName, String doctorName,
                                       String chiefComplaint, String diagnosis,
                                       String prescription, String treatment,
                                       BigDecimal totalCost, String notes, boolean deleted,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new MedicalVisit(id, userId, petId, visitDate, hospitalName, doctorName,
                chiefComplaint, diagnosis, prescription, treatment, totalCost, notes,
                deleted, createdAt, updatedAt);
    }

    public void update(String hospitalName, String doctorName, String chiefComplaint,
                       String diagnosis, String prescription, String treatment,
                       BigDecimal totalCost, String notes) {
        validateFields(hospitalName, doctorName, chiefComplaint, diagnosis,
                prescription, treatment, totalCost, notes);
        this.hospitalName = hospitalName.trim();
        this.doctorName = trimToNull(doctorName);
        this.chiefComplaint = trimToNull(chiefComplaint);
        this.diagnosis = trimToNull(diagnosis);
        this.prescription = trimToNull(prescription);
        this.treatment = trimToNull(treatment);
        this.totalCost = totalCost;
        this.notes = trimToNull(notes);
    }

    public void delete() {
        this.deleted = true;
    }

    private static void validateIdentity(Long userId, Long petId, LocalDate visitDate) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(visitDate, "visitDate 不能为空");
    }

    private static void validateFields(String hospitalName, String doctorName,
                                       String chiefComplaint, String diagnosis,
                                       String prescription, String treatment,
                                       BigDecimal totalCost, String notes) {
        if (hospitalName == null || hospitalName.isBlank()) {
            throw new IllegalArgumentException("医院名称不能为空");
        }
        validateLength(hospitalName, MAX_HOSPITAL_LENGTH, "医院名称");
        validateLength(doctorName, MAX_DOCTOR_LENGTH, "医生姓名");
        validateLength(chiefComplaint, MAX_COMPLAINT_LENGTH, "主诉");
        validateLength(diagnosis, MAX_DIAGNOSIS_LENGTH, "诊断原文");
        validateLength(prescription, MAX_PRESCRIPTION_LENGTH, "处方原文");
        validateLength(treatment, MAX_TREATMENT_LENGTH, "治疗内容");
        validateLength(notes, MAX_NOTES_LENGTH, "备注");
        if (totalCost != null && totalCost.signum() < 0) {
            throw new IllegalArgumentException("就诊费用不能为负");
        }
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

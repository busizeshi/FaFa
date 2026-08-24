package com.fafa.domain.model.emergency;

import java.time.LocalDateTime;

/** 用户维护的宠物应急资料，不从 AI 或病历原文自动推断。 */
public final class PetEmergencyProfile {

    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_PHONE_LENGTH = 32;

    private Long id;
    private final Long petId;
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String primaryHospitalName;
    private String primaryHospitalPhone;
    private String primaryHospitalAddress;
    private String notes;
    private int version;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PetEmergencyProfile(Long id, Long petId, String bloodType, String allergies,
                                String chronicConditions, String emergencyContactName,
                                String emergencyContactPhone, String primaryHospitalName,
                                String primaryHospitalPhone, String primaryHospitalAddress,
                                String notes, int version, Long updatedBy,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.petId = petId; this.bloodType = bloodType; this.allergies = allergies;
        this.chronicConditions = chronicConditions; this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone; this.primaryHospitalName = primaryHospitalName;
        this.primaryHospitalPhone = primaryHospitalPhone; this.primaryHospitalAddress = primaryHospitalAddress;
        this.notes = notes; this.version = version; this.updatedBy = updatedBy;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static PetEmergencyProfile create(Long petId, Long updatedBy, LocalDateTime now) {
        validateIdentity(petId, updatedBy);
        return new PetEmergencyProfile(null, petId, null, null, null, null, null,
                null, null, null, null, 0, updatedBy, now, now);
    }

    public static PetEmergencyProfile restore(Long id, Long petId, String bloodType,
                                              String allergies, String chronicConditions,
                                              String emergencyContactName, String emergencyContactPhone,
                                              String primaryHospitalName, String primaryHospitalPhone,
                                              String primaryHospitalAddress, String notes, int version,
                                              Long updatedBy, LocalDateTime createdAt,
                                              LocalDateTime updatedAt) {
        return new PetEmergencyProfile(id, petId, bloodType, allergies, chronicConditions,
                emergencyContactName, emergencyContactPhone, primaryHospitalName,
                primaryHospitalPhone, primaryHospitalAddress, notes, version, updatedBy,
                createdAt, updatedAt);
    }

    public void update(String bloodType, String allergies, String chronicConditions,
                       String emergencyContactName, String emergencyContactPhone,
                       String primaryHospitalName, String primaryHospitalPhone,
                       String primaryHospitalAddress, String notes, int expectedVersion,
                       Long updatedBy, LocalDateTime now) {
        if (expectedVersion != version) throw new IllegalStateException("应急资料版本冲突");
        validateIdentity(petId, updatedBy);
        validateFields(bloodType, allergies, chronicConditions, emergencyContactName,
                emergencyContactPhone, primaryHospitalName, primaryHospitalPhone,
                primaryHospitalAddress, notes);
        this.bloodType = trimToNull(bloodType); this.allergies = trimToNull(allergies);
        this.chronicConditions = trimToNull(chronicConditions); this.emergencyContactName = trimToNull(emergencyContactName);
        this.emergencyContactPhone = trimToNull(emergencyContactPhone); this.primaryHospitalName = trimToNull(primaryHospitalName);
        this.primaryHospitalPhone = trimToNull(primaryHospitalPhone); this.primaryHospitalAddress = trimToNull(primaryHospitalAddress);
        this.notes = trimToNull(notes); this.updatedBy = updatedBy; this.updatedAt = now; this.version++;
    }

    public static void validateFields(String bloodType, String allergies, String chronicConditions,
                                      String emergencyContactName, String emergencyContactPhone,
                                      String primaryHospitalName, String primaryHospitalPhone,
                                      String primaryHospitalAddress, String notes) {
        validateLength(bloodType, MAX_TEXT_LENGTH, "血型"); validateLength(allergies, MAX_TEXT_LENGTH, "过敏史");
        validateLength(chronicConditions, MAX_TEXT_LENGTH, "慢性病"); validateLength(emergencyContactName, 64, "联系人姓名");
        validateLength(emergencyContactPhone, MAX_PHONE_LENGTH, "联系人电话"); validateLength(primaryHospitalName, 200, "医院名称");
        validateLength(primaryHospitalPhone, MAX_PHONE_LENGTH, "医院电话"); validateLength(primaryHospitalAddress, 500, "医院地址");
        validateLength(notes, MAX_TEXT_LENGTH, "备注");
        validatePhone(emergencyContactPhone, "联系人电话"); validatePhone(primaryHospitalPhone, "医院电话");
    }

    private static void validateIdentity(Long petId, Long updatedBy) {
        if (petId == null || petId <= 0 || updatedBy == null || updatedBy <= 0) throw new IllegalArgumentException("应急资料身份无效");
    }

    private static void validateLength(String value, int max, String field) {
        if (value != null && value.length() > max) throw new IllegalArgumentException(field + "长度不能超过" + max);
    }

    private static void validatePhone(String value, String field) {
        if (value != null && !value.isBlank() && !value.matches("[-0-9+() ]{6,32}")) throw new IllegalArgumentException(field + "格式无效");
    }

    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public String getBloodType() { return bloodType; }
    public String getAllergies() { return allergies; }
    public String getChronicConditions() { return chronicConditions; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getPrimaryHospitalName() { return primaryHospitalName; }
    public String getPrimaryHospitalPhone() { return primaryHospitalPhone; }
    public String getPrimaryHospitalAddress() { return primaryHospitalAddress; }
    public String getNotes() { return notes; }
    public int getVersion() { return version; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

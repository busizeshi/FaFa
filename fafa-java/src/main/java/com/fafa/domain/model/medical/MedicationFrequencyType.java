package com.fafa.domain.model.medical;

public enum MedicationFrequencyType {
    DAILY("daily"), EVERY_DAYS("every_days"), WEEKLY("weekly"), AS_NEEDED("as_needed");

    private final String code;

    MedicationFrequencyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MedicationFrequencyType fromCode(String code) {
        for (MedicationFrequencyType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("用药频次非法: " + code);
    }
}

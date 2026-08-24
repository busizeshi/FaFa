package com.fafa.domain.model.medical;

public enum MedicationRecordStatus {
    TAKEN("taken"), SKIPPED("skipped"), MISSED("missed"), CANCELLED("cancelled");

    private final String code;

    MedicationRecordStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MedicationRecordStatus fromCode(String code) {
        for (MedicationRecordStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("实际用药状态非法: " + code);
    }
}

package com.fafa.domain.model.medical;

public enum MedicationPlanStatus {
    ACTIVE("active"), PAUSED("paused"), COMPLETED("completed"), DELETED("deleted");

    private final String code;

    MedicationPlanStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MedicationPlanStatus fromCode(String code) {
        for (MedicationPlanStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("用药计划状态非法: " + code);
    }
}

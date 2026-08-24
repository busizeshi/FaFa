package com.fafa.domain.model.health;

public enum HealthAlertStatus {
    OPEN("open"), ACKNOWLEDGED("acknowledged"), RESOLVED("resolved"), EXPIRED("expired");

    private final String code;

    HealthAlertStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HealthAlertStatus fromCode(String code) {
        for (HealthAlertStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("预警状态非法: " + code);
    }
}

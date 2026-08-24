package com.fafa.domain.model.health;

public enum HealthAlertSeverity {
    RED("red"), YELLOW("yellow");

    private final String code;

    HealthAlertSeverity(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HealthAlertSeverity fromCode(String code) {
        for (HealthAlertSeverity value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("预警级别非法: " + code);
    }
}

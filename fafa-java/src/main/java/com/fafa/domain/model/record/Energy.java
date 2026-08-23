package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 精神活动维度
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum Energy {

    ACTIVE("active", "活跃"),
    NORMAL("normal", "正常"),
    LETHARGIC("lethargic", "低迷");

    private final String code;
    private final String label;

    Energy(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Energy fromCode(String code) {
        for (Energy value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid energy: " + code);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    /** 是否为明确异常档（低迷） */
    public boolean isAbnormal() {
        return this == LETHARGIC;
    }
}

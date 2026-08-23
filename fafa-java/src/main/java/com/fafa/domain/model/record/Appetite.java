package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 食欲维度
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum Appetite {

    EXCELLENT("excellent", "很好"),
    NORMAL("normal", "正常"),
    DECREASED("decreased", "下降"),
    REFUSED("refused", "拒食");

    private final String code;
    private final String label;

    Appetite(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Appetite fromCode(String code) {
        for (Appetite value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid appetite: " + code);
    }

    /** 是否为正常档 */
    public boolean isNormal() {
        return this == NORMAL;
    }

    /** 是否为明确异常档（下降/拒食） */
    public boolean isAbnormal() {
        return this == DECREASED || this == REFUSED;
    }
}

package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 呕吐维度
 *
 * none 为默认正常档；once/multiple 为异常档。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum Vomiting {

    NONE("none", "无"),
    ONCE("once", "一次"),
    MULTIPLE("multiple", "多次");

    private final String code;
    private final String label;

    Vomiting(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Vomiting fromCode(String code) {
        for (Vomiting value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid vomiting: " + code);
    }

    /** none 即正常档 */
    public boolean isNormal() {
        return this == NONE;
    }

    /** 是否为明确异常档（一次/多次） */
    public boolean isAbnormal() {
        return this == ONCE || this == MULTIPLE;
    }
}

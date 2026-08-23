package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 排尿维度（猫重点维度）
 *
 * not_seen 为中性逃生口：既非正常也非异常（漏记不惩罚）。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum Urination {

    NORMAL("normal", "正常"),
    ABNORMAL("abnormal", "异常"),
    NOT_SEEN("not_seen", "没看到");

    private final String code;
    private final String label;

    Urination(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Urination fromCode(String code) {
        for (Urination value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid urination: " + code);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    /** 是否为明确异常档 */
    public boolean isAbnormal() {
        return this == ABNORMAL;
    }
}

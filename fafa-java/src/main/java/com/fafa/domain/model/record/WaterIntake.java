package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 饮水程度维度
 *
 * not_noticed 为中性逃生口：既非正常也非异常（漏记不惩罚）。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum WaterIntake {

    NORMAL("normal", "正常"),
    MORE("more", "偏多"),
    LESS("less", "偏少"),
    NOT_NOTICED("not_noticed", "没注意");

    private final String code;
    private final String label;

    WaterIntake(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static WaterIntake fromCode(String code) {
        for (WaterIntake value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid water intake: " + code);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    /** 是否为明确异常档（偏多/偏少） */
    public boolean isAbnormal() {
        return this == MORE || this == LESS;
    }
}

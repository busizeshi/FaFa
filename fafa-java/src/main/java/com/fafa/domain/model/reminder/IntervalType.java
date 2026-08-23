package com.fafa.domain.model.reminder;

import lombok.Getter;

/**
 * 提醒周期类型
 *
 * intervalValue 语义随类型变化：
 * - once/yearly：不使用（null）
 * - days/months：N ≥ 1
 * - weekly：ISO 星期值 1（周一）~ 7（周日）
 * - monthly：每月几号 1~31
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public enum IntervalType {

    ONCE("once", "一次性"),
    DAYS("days", "每N天"),
    WEEKLY("weekly", "每周"),
    MONTHLY("monthly", "每月"),
    MONTHS("months", "每N个月"),
    YEARLY("yearly", "每年");

    private final String code;
    private final String label;

    IntervalType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static IntervalType fromCode(String code) {
        for (IntervalType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid interval type: " + code);
    }
}

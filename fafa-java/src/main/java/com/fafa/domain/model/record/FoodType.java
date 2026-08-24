package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 食物类型（喂食记录用，无正常/异常语义）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum FoodType {

    MAIN_FOOD("main_food", "主食"),
    SNACK("snack", "零食"),
    CAN("can", "罐头"),
    OTHER("other", "其他");

    private final String code;
    private final String label;

    FoodType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static FoodType fromCode(String code) {
        for (FoodType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        // Phase 7 食品卡使用 canned；旧喂食记录仍返回 can，输入两种写法都兼容。
        if ("canned".equalsIgnoreCase(code)) {
            return CAN;
        }
        throw new IllegalArgumentException("Invalid food type: " + code);
    }
}

package com.fafa.domain.model.food;

import lombok.Getter;

/** 食品管理卡类型。 */
@Getter
public enum FoodType {
    MAIN_FOOD("main_food", "主食"),
    CANNED("canned", "罐头"),
    SNACK("snack", "零食"),
    SUPPLEMENT("supplement", "补充剂"),
    HOMEMADE("homemade", "自制"),
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
        throw new IllegalArgumentException("食品类型不支持: " + code);
    }
}

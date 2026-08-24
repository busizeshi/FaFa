package com.fafa.domain.model.food;

import lombok.Getter;

/** 食品卡生命周期状态。 */
@Getter
public enum FoodStatus {
    ACTIVE("active"),
    ARCHIVED("archived"),
    DELETED("deleted");

    private final String code;

    FoodStatus(String code) {
        this.code = code;
    }

    public static FoodStatus fromCode(String code) {
        for (FoodStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("食品卡状态不支持: " + code);
    }
}

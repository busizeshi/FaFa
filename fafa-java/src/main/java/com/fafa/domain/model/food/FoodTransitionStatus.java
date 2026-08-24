package com.fafa.domain.model.food;

import lombok.Getter;

/** 换粮计划状态。 */
@Getter
public enum FoodTransitionStatus {
    DRAFT("draft"), ACTIVE("active"), COMPLETED("completed"), CANCELLED("cancelled");

    private final String code;

    FoodTransitionStatus(String code) {
        this.code = code;
    }

    public static FoodTransitionStatus fromCode(String code) {
        for (FoodTransitionStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("换粮计划状态不支持: " + code);
    }
}

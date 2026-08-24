package com.fafa.domain.model.medical;

public enum MealRelation {
    BEFORE_MEAL("before_meal"), AFTER_MEAL("after_meal"), ANY("any");

    private final String code;

    MealRelation(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MealRelation fromCode(String code) {
        for (MealRelation value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("用餐关系非法: " + code);
    }
}

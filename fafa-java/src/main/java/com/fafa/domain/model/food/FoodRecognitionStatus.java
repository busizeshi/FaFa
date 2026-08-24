package com.fafa.domain.model.food;

import lombok.Getter;

/** 食品包装识别任务状态。 */
@Getter
public enum FoodRecognitionStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    CONFIRMED("confirmed"),
    REJECTED("rejected"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String code;

    FoodRecognitionStatus(String code) {
        this.code = code;
    }

    public static FoodRecognitionStatus fromCode(String code) {
        for (FoodRecognitionStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("识别任务状态不支持: " + code);
    }
}

package com.fafa.domain.model.feeding;

import lombok.Getter;

/** 喂食计划状态。 */
@Getter
public enum FeedingPlanStatus {
    DRAFT("draft"), ACTIVE("active"), PAUSED("paused"), COMPLETED("completed"), CANCELLED("cancelled");

    private final String code;

    FeedingPlanStatus(String code) {
        this.code = code;
    }

    public static FeedingPlanStatus fromCode(String code) {
        for (FeedingPlanStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("喂食计划状态不支持: " + code);
    }
}

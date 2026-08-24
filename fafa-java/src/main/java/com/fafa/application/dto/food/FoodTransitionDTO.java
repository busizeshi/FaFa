package com.fafa.application.dto.food;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 换粮计划返回对象。 */
public record FoodTransitionDTO(
        Long id,
        Long petId,
        Long oldFoodId,
        Long newFoodId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal dailyTargetAmount,
        String unit,
        List<RatioStepDTO> ratioSteps,
        LocalDate reactionDate,
        String reactionCode,
        String reactionNote,
        String status
) {
    public record RatioStepDTO(LocalDate date, BigDecimal oldPercent, BigDecimal newPercent) {
    }
}

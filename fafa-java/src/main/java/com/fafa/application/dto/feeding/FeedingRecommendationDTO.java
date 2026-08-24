package com.fafa.application.dto.feeding;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** 喂食建议预览对象，不代表已保存计划。 */
public record FeedingRecommendationDTO(
        String recommendationId,
        String source,
        String policyVersion,
        BigDecimal targetCalories,
        BigDecimal targetAmount,
        String unit,
        boolean estimate,
        List<ItemDTO> items,
        List<String> explanations,
        List<String> warnings
) {
    public record ItemDTO(String itemKey, Long foodId, String foodName, LocalTime mealTime,
                          BigDecimal amount, String unit, BigDecimal percentage) {
    }
}

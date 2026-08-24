package com.fafa.application.dto.feeding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 喂食计划返回对象。 */
public record FeedingPlanDTO(
        Long id,
        Long petId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal dailyTargetCalories,
        BigDecimal dailyTargetAmount,
        String unit,
        String recommendationSource,
        String policyVersion,
        int version,
        String status,
        LocalDateTime acceptedAt,
        String notes,
        List<ItemDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ItemDTO(Long id, String itemKey, Long foodId, LocalTime mealTime,
                          BigDecimal amount, String unit, BigDecimal percentage,
                          String cycleType, String cycleValue, int sortOrder) {
    }
}

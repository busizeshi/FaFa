package com.fafa.application.dto.food;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 食品卡写入命令。 */
public record FoodCardCommand(
        Long petId,
        String name,
        String brand,
        String foodType,
        String specification,
        BigDecimal packageWeight,
        String packageWeightUnit,
        BigDecimal price,
        String currency,
        LocalDate openedAt,
        LocalDate expiresAt,
        String nutritionBasis,
        BigDecimal proteinPercent,
        BigDecimal fatPercent,
        BigDecimal fiberPercent,
        BigDecimal moisturePercent,
        BigDecimal ashPercent,
        BigDecimal calciumPercent,
        BigDecimal phosphorusPercent,
        BigDecimal energyKcal,
        String energyUnit,
        List<String> proteinSources,
        List<String> ingredients,
        List<String> additiveFlags,
        String lifeStageLabel,
        Long sourceMediaId,
        Long sourceRecognitionTaskId,
        String notes
) {
}

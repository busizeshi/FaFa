package com.fafa.application.dto.food;

import java.util.List;

/** 食品配方信息提示分返回对象。 */
public record FoodSafetyScoreDTO(
        Long foodId,
        Integer score,
        String label,
        String policyVersion,
        List<DimensionDTO> dimensions,
        List<String> missingFields,
        List<String> warnings
) {
    public record DimensionDTO(String key, String label, int score, int maxScore,
                               String status, List<String> evidence) {
    }
}

package com.fafa.application.dto.feeding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 动态喂养分析返回对象。 */
public record FeedingAnalysisDTO(
        Long petId,
        RangeDTO range,
        FactsDTO facts,
        List<ObservationDTO> observations,
        List<String> reviewSuggestions,
        List<String> limitations,
        String analysisSource,
        String policyVersion
) {
    public record RangeDTO(LocalDate startDate, LocalDate endDate) {
    }

    public record FactsDTO(int feedingDays, BigDecimal averageDailyAmount, String unit,
                           int plannedDays, BigDecimal planCompletionRate,
                           WeightDTO weight, DailyStatusFactsDTO dailyStatus) {
    }

    public record WeightDTO(BigDecimal firstKg, BigDecimal lastKg, BigDecimal changeKg, int sampleCount) {
    }

    public record DailyStatusFactsDTO(int appetiteNormalDays, int softStoolDays, int refusalDays) {
    }

    public record ObservationDTO(String key, String text, List<String> evidence) {
    }
}

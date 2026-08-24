package com.fafa.interfaces.dto.feeding;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 生成喂食建议预览请求。 */
@Data
public class FeedingRecommendationRequest {
    @NotNull private Long petId;
    @NotEmpty private List<Long> foodIds;
    @NotNull private LocalDate startDate;
    private BigDecimal dailyTargetAmount;
    private BigDecimal targetCalories;
    private String unit;
    private Integer mealCount;
    private List<LocalTime> mealTimes;
    private String activityLevel;
    private String userNote;
}

package com.fafa.interfaces.dto.feeding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 保存喂食计划请求。 */
@Data
public class SaveFeedingPlanRequest {
    @NotNull private Long petId;
    @Size(max = 100) private String name;
    @NotNull private LocalDate startDate;
    private LocalDate endDate;
    @DecimalMin("0.001") private BigDecimal dailyTargetCalories;
    @DecimalMin("0.001") private BigDecimal dailyTargetAmount;
    private String unit;
    private String recommendationSource;
    private String policyVersion;
    @NotEmpty @Valid private List<ItemRequest> items;
    @Size(max = 500) private String notes;

    @Data
    public static class ItemRequest {
        private String itemKey;
        @NotNull private Long foodId;
        @NotNull private LocalTime mealTime;
        @NotNull @DecimalMin("0.001") private BigDecimal amount;
        private String unit;
        private BigDecimal percentage;
        private String cycleType;
        private String cycleValue;
    }
}

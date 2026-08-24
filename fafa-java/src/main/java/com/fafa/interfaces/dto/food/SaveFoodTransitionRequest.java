package com.fafa.interfaces.dto.food;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 创建换粮计划请求。 */
@Data
public class SaveFoodTransitionRequest {
    @NotNull private Long petId;
    @NotNull private Long oldFoodId;
    @NotNull private Long newFoodId;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @DecimalMin("0") private BigDecimal dailyTargetAmount;
    private String unit;
    @NotEmpty @Valid private List<RatioStepRequest> ratioSteps;
    private String status;

    @Data
    public static class RatioStepRequest {
        @NotNull private LocalDate date;
        @NotNull @DecimalMin("0") private BigDecimal oldPercent;
        @NotNull @DecimalMin("0") private BigDecimal newPercent;
    }
}

package com.fafa.interfaces.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "创建换粮计划请求")
public class SaveFoodTransitionRequest {
    @NotNull @Schema(description = "所属宠物ID", example = "1") private Long petId;
    @NotNull @Schema(description = "旧食品卡ID", example = "101") private Long oldFoodId;
    @NotNull @Schema(description = "新食品卡ID", example = "102") private Long newFoodId;
    @NotNull @Schema(description = "换粮计划开始日期", example = "2026-09-01") private LocalDate startDate;
    @NotNull @Schema(description = "换粮计划结束日期", example = "2026-09-08") private LocalDate endDate;
    @DecimalMin("0") @Schema(description = "每日目标喂食总量", example = "180") private BigDecimal dailyTargetAmount;
    @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") private String unit;
    @NotEmpty @Valid @Schema(description = "每日新旧食品比例步骤") private List<RatioStepRequest> ratioSteps;
    @Schema(description = "计划状态：draft=草稿，active=进行中，completed=已完成，cancelled=已取消", example = "draft") private String status;

    @Data
    @Schema(description = "换粮比例步骤")
    public static class RatioStepRequest {
        @NotNull @Schema(description = "比例生效日期", example = "2026-09-01") private LocalDate date;
        @NotNull @DecimalMin("0") @Schema(description = "旧食品占比（百分比）", example = "75") private BigDecimal oldPercent;
        @NotNull @DecimalMin("0") @Schema(description = "新食品占比（百分比）", example = "25") private BigDecimal newPercent;
    }
}

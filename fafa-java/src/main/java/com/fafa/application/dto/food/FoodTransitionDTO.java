package com.fafa.application.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 换粮计划返回对象。 */
@Schema(description = "换粮计划")
public record FoodTransitionDTO(
        @Schema(description = "换粮计划ID", example = "1") Long id,
        @Schema(description = "所属宠物ID", example = "1") Long petId,
        @Schema(description = "旧食品卡ID", example = "101") Long oldFoodId,
        @Schema(description = "新食品卡ID", example = "102") Long newFoodId,
        @Schema(description = "计划开始日期", example = "2026-09-01") LocalDate startDate,
        @Schema(description = "计划结束日期", example = "2026-09-08") LocalDate endDate,
        @Schema(description = "每日目标喂食总量", example = "180") BigDecimal dailyTargetAmount,
        @Schema(description = "喂食量单位", example = "g") String unit,
        @Schema(description = "每日新旧食品比例步骤") List<RatioStepDTO> ratioSteps,
        @Schema(description = "最近一次反应日期", example = "2026-09-03") LocalDate reactionDate,
        @Schema(description = "最近一次反应描述或编码", example = "normal") String reactionCode,
        @Schema(description = "最近一次反应备注") String reactionNote,
        @Schema(description = "计划状态：draft=草稿，active=进行中，completed=已完成，cancelled=已取消", example = "active") String status
) {
    @Schema(description = "换粮比例步骤")
    public record RatioStepDTO(
            @Schema(description = "比例生效日期", example = "2026-09-01") LocalDate date,
            @Schema(description = "旧食品占比（百分比）", example = "75") BigDecimal oldPercent,
            @Schema(description = "新食品占比（百分比）", example = "25") BigDecimal newPercent) {
    }
}

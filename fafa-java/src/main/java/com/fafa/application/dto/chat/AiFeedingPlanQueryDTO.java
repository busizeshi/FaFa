package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** AI 喂食计划查询结果。 */
@Schema(description = "AI 喂食计划查询结果")
public record AiFeedingPlanQueryDTO(
        @Schema(description = "喂食计划事实列表") List<PlanFactDTO> records,
        @Schema(description = "喂食计划数量", example = "2") int count) {

    @Schema(description = "AI 使用的喂食计划事实")
    public record PlanFactDTO(
            @Schema(description = "喂食计划ID", example = "1") Long id,
            @Schema(description = "计划名称", example = "小白日常喂食计划") String name,
            @Schema(description = "计划开始日期", example = "2026-09-01") LocalDate startDate,
            @Schema(description = "计划结束日期") LocalDate endDate,
            @Schema(description = "计划状态", example = "active") String status,
            @Schema(description = "每日目标喂食总量", example = "180") BigDecimal dailyTargetAmount,
            @Schema(description = "每日目标能量（kcal）", example = "720") BigDecimal dailyTargetCalories,
            @Schema(description = "喂食计划项列表") List<ItemFactDTO> items) {
    }

    @Schema(description = "AI 使用的喂食计划项事实")
    public record ItemFactDTO(
            @Schema(description = "计划项ID", example = "101") Long id,
            @Schema(description = "计划项稳定标识", example = "breakfast") String itemKey,
            @Schema(description = "食品卡ID", example = "201") Long foodId,
            @Schema(description = "进餐时间", example = "08:00") LocalTime mealTime,
            @Schema(description = "每次目标喂食量", example = "60") BigDecimal amount,
            @Schema(description = "喂食量单位", example = "g") String unit,
            @Schema(description = "该食品占每日喂食总量的比例（百分比）", example = "33.33") BigDecimal percentage,
            @Schema(description = "周期类型", example = "daily") String cycleType,
            @Schema(description = "周期补充配置") String cycleValue,
            @Schema(description = "营养快照") String nutritionSnapshot,
            @Schema(description = "计划项展示顺序", example = "1") int sortOrder) {
    }
}

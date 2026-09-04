package com.fafa.application.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 喂食计划返回对象。 */
@Schema(description = "喂食计划")
public record FeedingPlanDTO(
        @Schema(description = "喂食计划ID", example = "1") Long id,
        @Schema(description = "所属宠物ID", example = "1") Long petId,
        @Schema(description = "计划名称", example = "小白日常喂食计划") String name,
        @Schema(description = "计划开始日期", example = "2026-09-01") LocalDate startDate,
        @Schema(description = "计划结束日期") LocalDate endDate,
        @Schema(description = "每日目标能量（kcal）", example = "720") BigDecimal dailyTargetCalories,
        @Schema(description = "每日目标喂食总量", example = "180") BigDecimal dailyTargetAmount,
        @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") String unit,
        @Schema(description = "建议来源：user_input=用户输入，rule=规则计算，ai_assisted=AI辅助", example = "ai_assisted") String recommendationSource,
        @Schema(description = "生成建议时使用的策略版本") String policyVersion,
        @Schema(description = "计划版本号", example = "1") int version,
        @Schema(description = "计划状态：draft=草稿，active=进行中，paused=暂停，completed=已完成，cancelled=已取消", example = "active") String status,
        @Schema(description = "用户确认计划的时间", example = "2026-09-01T08:00:00") LocalDateTime acceptedAt,
        @Schema(description = "计划备注") String notes,
        @Schema(description = "计划项列表") List<ItemDTO> items,
        @Schema(description = "创建时间", example = "2026-09-01T08:00:00") LocalDateTime createdAt,
        @Schema(description = "最后更新时间", example = "2026-09-01T08:00:00") LocalDateTime updatedAt
) {
    @Schema(description = "喂食计划项")
    public record ItemDTO(
                          @Schema(description = "计划项ID", example = "101") Long id,
                          @Schema(description = "计划项稳定标识", example = "breakfast") String itemKey,
                          @Schema(description = "食品卡ID", example = "201") Long foodId,
                          @Schema(description = "进餐时间", example = "08:00") LocalTime mealTime,
                          @Schema(description = "每次目标喂食量", example = "60") BigDecimal amount,
                          @Schema(description = "喂食量单位", example = "g") String unit,
                          @Schema(description = "该食品占每日喂食总量的比例（百分比）", example = "33.33") BigDecimal percentage,
                          @Schema(description = "周期类型：daily=每天，weekly=每周，custom=自定义", example = "daily") String cycleType,
                          @Schema(description = "周期补充配置") String cycleValue,
                          @Schema(description = "计划项展示顺序", example = "1") int sortOrder) {
    }
}

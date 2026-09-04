package com.fafa.interfaces.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "保存喂食计划请求")
public class SaveFeedingPlanRequest {
    @NotNull @Schema(description = "所属宠物ID", example = "1") private Long petId;
    @Size(max = 100) @Schema(description = "喂食计划名称", example = "小白日常喂食计划") private String name;
    @NotNull @Schema(description = "计划开始日期", example = "2026-09-01") private LocalDate startDate;
    @Schema(description = "计划结束日期，不传表示长期计划", example = "2026-12-01") private LocalDate endDate;
    @DecimalMin("0.001") @Schema(description = "每日目标能量（kcal）", example = "720") private BigDecimal dailyTargetCalories;
    @DecimalMin("0.001") @Schema(description = "每日目标喂食总量", example = "180") private BigDecimal dailyTargetAmount;
    @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") private String unit;
    @Schema(description = "建议来源：user_input=用户输入，rule=规则计算，ai_assisted=AI辅助", example = "ai_assisted")
    private String recommendationSource;
    @Schema(description = "生成建议时使用的策略版本") private String policyVersion;
    @NotEmpty @Valid @Schema(description = "每日喂食计划项列表") private List<ItemRequest> items;
    @Size(max = 500) @Schema(description = "计划备注") private String notes;

    @Data
    @Schema(description = "喂食计划项")
    public static class ItemRequest {
        @Schema(description = "计划项稳定标识，同一计划内唯一", example = "breakfast") private String itemKey;
        @NotNull @Schema(description = "食品卡ID", example = "101") private Long foodId;
        @NotNull @Schema(description = "进餐时间", example = "08:00") private LocalTime mealTime;
        @NotNull @DecimalMin("0.001") @Schema(description = "每次目标喂食量", example = "60") private BigDecimal amount;
        @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") private String unit;
        @Schema(description = "该食品占每日喂食总量的比例（百分比）", example = "33.33") private BigDecimal percentage;
        @Schema(description = "周期类型：daily=每天，weekly=每周，custom=自定义", example = "daily") private String cycleType;
        @Schema(description = "周期补充配置，具体格式由 cycleType 决定", example = "") private String cycleValue;
    }
}

package com.fafa.application.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** 喂食建议预览对象，不代表已保存计划。 */
@Schema(description = "喂食建议预览")
public record FeedingRecommendationDTO(
        @Schema(description = "建议结果ID", example = "rec-20260901-001") String recommendationId,
        @Schema(description = "建议来源：rule=规则计算，ai_assisted=AI辅助", example = "rule") String source,
        @Schema(description = "建议策略版本") String policyVersion,
        @Schema(description = "建议每日目标能量（kcal）", example = "720") BigDecimal targetCalories,
        @Schema(description = "建议每日目标喂食量", example = "180") BigDecimal targetAmount,
        @Schema(description = "喂食量单位", example = "g") String unit,
        @Schema(description = "是否为估算值", example = "true") boolean estimate,
        @Schema(description = "建议喂食项列表") List<ItemDTO> items,
        @Schema(description = "建议计算说明") List<String> explanations,
        @Schema(description = "建议警告信息") List<String> warnings
) {
    @Schema(description = "喂食建议项")
    public record ItemDTO(
                          @Schema(description = "建议项稳定标识", example = "breakfast") String itemKey,
                          @Schema(description = "食品卡ID", example = "201") Long foodId,
                          @Schema(description = "食品名称", example = "成猫鸡肉配方粮") String foodName,
                          @Schema(description = "进餐时间", example = "08:00") LocalTime mealTime,
                          @Schema(description = "每次建议喂食量", example = "60") BigDecimal amount,
                          @Schema(description = "喂食量单位", example = "g") String unit,
                          @Schema(description = "该食品占每日喂食总量的比例（百分比）", example = "33.33") BigDecimal percentage) {
    }
}

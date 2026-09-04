package com.fafa.application.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 食品配方信息提示分返回对象。 */
@Schema(description = "食品配方信息提示分")
public record FoodSafetyScoreDTO(
        @Schema(description = "食品卡ID", example = "1") Long foodId,
        @Schema(description = "配方信息提示分", example = "82") Integer score,
        @Schema(description = "提示分等级标签", example = "信息较完整") String label,
        @Schema(description = "评分策略版本") String policyVersion,
        @Schema(description = "各评分维度明细") List<DimensionDTO> dimensions,
        @Schema(description = "缺失的关键字段列表") List<String> missingFields,
        @Schema(description = "配方风险或注意事项") List<String> warnings
) {
    @Schema(description = "食品配方评分维度")
    public record DimensionDTO(
                               @Schema(description = "维度编码", example = "protein") String key,
                               @Schema(description = "维度名称", example = "蛋白质") String label,
                               @Schema(description = "该维度得分", example = "18") int score,
                               @Schema(description = "该维度满分", example = "20") int maxScore,
                               @Schema(description = "维度状态", example = "available") String status,
                               @Schema(description = "评分依据") List<String> evidence) {
    }
}

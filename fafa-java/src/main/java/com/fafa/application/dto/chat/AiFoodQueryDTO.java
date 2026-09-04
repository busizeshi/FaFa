package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** AI 食品卡查询结果。 */
@Schema(description = "AI 食品卡查询结果")
public record AiFoodQueryDTO(
        @Schema(description = "食品卡事实列表") List<FoodFactDTO> records,
        @Schema(description = "食品卡数量", example = "3") int count) {

    @Schema(description = "AI 使用的食品卡事实")
    public record FoodFactDTO(
            @Schema(description = "食品卡ID", example = "1") Long id,
            @Schema(description = "食品名称", example = "成猫鸡肉配方粮") String name,
            @Schema(description = "食品品牌", example = "示例品牌") String brand,
            @Schema(description = "食品类型编码", example = "main_food") String foodType,
            @Schema(description = "食品卡状态", example = "active") String status,
            @Schema(description = "蛋白质含量百分比", example = "32.5") BigDecimal proteinPercent,
            @Schema(description = "脂肪含量百分比", example = "15.0") BigDecimal fatPercent,
            @Schema(description = "能量数值", example = "380") BigDecimal energyKcal,
            @Schema(description = "能量单位", example = "kcal_per_100g") String energyUnit) {
    }
}

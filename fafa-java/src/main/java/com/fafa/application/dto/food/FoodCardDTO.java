package com.fafa.application.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 食品卡返回对象。 */
@Schema(description = "食品管理卡响应")
public record FoodCardDTO(
        @Schema(description = "食品卡ID", example = "1")
        Long id,
        @Schema(description = "所属宠物ID", example = "1")
        Long petId,
        @Schema(description = "食品名称", example = "成猫鸡肉配方粮")
        String name,
        @Schema(description = "食品品牌", example = "示例品牌")
        String brand,
        @Schema(description = "食品类型：main_food=主食，canned=罐头，snack=零食，supplement=补充剂，homemade=自制，other=其他", example = "main_food")
        String foodType,
        @Schema(description = "食品卡状态：active=使用中，archived=已归档，deleted=已删除", example = "active")
        String status,
        @Schema(description = "包装规格文字", example = "2kg")
        String specification,
        @Schema(description = "包装重量或容量", example = "2000")
        BigDecimal packageWeight,
        @Schema(description = "包装重量或容量单位：g=克，ml=毫升", example = "g")
        String packageWeightUnit,
        @Schema(description = "购买价格", example = "128.00")
        BigDecimal price,
        @Schema(description = "货币代码", example = "CNY")
        String currency,
        @Schema(description = "食品开封日期", example = "2026-08-20")
        LocalDate openedAt,
        @Schema(description = "食品保质期截止日期", example = "2027-08-20")
        LocalDate expiresAt,
        @Schema(description = "营养成分标注基准：per_100g=每100克，per_100ml=每100毫升，serving=每份，unknown=未知", example = "per_100g")
        String nutritionBasis,
        @Schema(description = "蛋白质含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "32.5")
        BigDecimal proteinPercent,
        @Schema(description = "脂肪含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "15.0")
        BigDecimal fatPercent,
        @Schema(description = "纤维含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "3.5")
        BigDecimal fiberPercent,
        @Schema(description = "水分含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "8.0")
        BigDecimal moisturePercent,
        @Schema(description = "灰分含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "7.0")
        BigDecimal ashPercent,
        @Schema(description = "钙含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "1.2")
        BigDecimal calciumPercent,
        @Schema(description = "磷含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "0.9")
        BigDecimal phosphorusPercent,
        @Schema(description = "能量数值", example = "380")
        BigDecimal energyKcal,
        @Schema(description = "能量单位：kcal_per_100g、kcal_per_100ml 或 kcal_per_serving", example = "kcal_per_100g")
        String energyUnit,
        @Schema(description = "蛋白质来源列表")
        List<String> proteinSources,
        @Schema(description = "配料表条目列表，按包装标注顺序排列")
        List<String> ingredients,
        @Schema(description = "添加物提示标记列表")
        List<String> additiveFlags,
        @Schema(description = "包装上标注的适用生命周期或阶段", example = "成猫")
        String lifeStageLabel,
        @Schema(description = "来源媒体ID，例如食品包装照片的媒体ID", example = "1001")
        Long sourceMediaId,
        @Schema(description = "来源食品包装识别任务ID", example = "2001")
        Long sourceRecognitionTaskId,
        @Schema(description = "备注", example = "每日建议喂食量请结合体重调整")
        String notes,
        @Schema(description = "营养信息完整度，范围 0-1；根据蛋白质、脂肪、能量、钙、磷五项是否存在计算")
        double nutritionCompleteness,
        @Schema(description = "创建时间", example = "2026-08-20T10:30:00")
        LocalDateTime createdAt,
        @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00")
        LocalDateTime updatedAt
) {
}

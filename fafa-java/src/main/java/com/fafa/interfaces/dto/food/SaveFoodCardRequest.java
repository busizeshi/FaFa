package com.fafa.interfaces.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 食品卡新增/修改请求。 */
@Data
@Schema(description = "食品管理卡请求")
public class SaveFoodCardRequest {

    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "食品名称", example = "成猫鸡肉配方粮")
    private String name;
    @Size(max = 100)
    @Schema(description = "食品品牌", example = "示例品牌")
    private String brand;
    @NotBlank
    @Schema(description = "食品类型：main_food=主食，canned=罐头，snack=零食，supplement=补充剂，homemade=自制，other=其他", example = "main_food")
    private String foodType;
    @Size(max = 100)
    @Schema(description = "包装规格文字", example = "2kg")
    private String specification;
    @DecimalMin(value = "0.001")
    @Schema(description = "包装重量或容量；填写后必须同时填写 packageWeightUnit", example = "2000")
    private BigDecimal packageWeight;
    @Schema(description = "包装重量或容量单位：g=克，ml=毫升", example = "g")
    private String packageWeightUnit;
    @DecimalMin(value = "0")
    @Schema(description = "购买价格，不能为负数", example = "128.00")
    private BigDecimal price;
    @Schema(description = "货币代码，未填写时默认为 CNY", example = "CNY")
    private String currency;
    @Schema(description = "食品开封日期", example = "2026-08-20")
    private LocalDate openedAt;
    @Schema(description = "食品保质期截止日期，不能早于 openedAt", example = "2027-08-20")
    private LocalDate expiresAt;
    @Schema(description = "营养成分标注基准：per_100g=每100克，per_100ml=每100毫升，serving=每份，unknown=未知；默认 unknown", example = "per_100g")
    private String nutritionBasis;
    @Schema(description = "蛋白质含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "32.5")
    private BigDecimal proteinPercent;
    @Schema(description = "脂肪含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "15.0")
    private BigDecimal fatPercent;
    @Schema(description = "纤维含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "3.5")
    private BigDecimal fiberPercent;
    @Schema(description = "水分含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "8.0")
    private BigDecimal moisturePercent;
    @Schema(description = "灰分含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "7.0")
    private BigDecimal ashPercent;
    @Schema(description = "钙含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "1.2")
    private BigDecimal calciumPercent;
    @Schema(description = "磷含量百分比，取值范围 0-100，以 nutritionBasis 为准", example = "0.9")
    private BigDecimal phosphorusPercent;
    @Schema(description = "能量数值；填写后必须同时填写 energyUnit", example = "380")
    private BigDecimal energyKcal;
    @Schema(description = "能量单位：kcal_per_100g、kcal_per_100ml 或 kcal_per_serving", example = "kcal_per_100g")
    private String energyUnit;
    @Schema(description = "蛋白质来源列表", example = "[\"鸡肉\", \"鱼肉\"]")
    private List<String> proteinSources;
    @Schema(description = "配料表条目列表，建议按包装标注顺序传入", example = "[\"鸡肉\", \"鸡肉粉\", \"糙米\"]")
    private List<String> ingredients;
    @Schema(description = "添加物提示标记列表，例如是否含诱食剂、着色剂等", example = "[\"含诱食剂\"]")
    private List<String> additiveFlags;
    @Schema(description = "包装上标注的适用生命周期或阶段", example = "成猫")
    private String lifeStageLabel;
    @Schema(description = "来源媒体ID，例如食品包装照片的媒体ID", example = "1001")
    private Long sourceMediaId;
    @Schema(description = "来源食品包装识别任务ID", example = "2001")
    private Long sourceRecognitionTaskId;
    @Size(max = 500)
    @Schema(description = "备注", example = "每日建议喂食量请结合体重调整")
    private String notes;
}

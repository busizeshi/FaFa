package com.fafa.interfaces.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保存喂食记录请求（新增与修改共用）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@Schema(description = "保存喂食记录请求")
public class SaveFeedingRequest {

    @NotNull(message = "宠物ID不能为空")
    @Schema(description = "宠物ID", example = "1")
    private Long petId;

    @NotNull(message = "喂食时间不能为空")
    @Schema(description = "喂食时间", example = "2026-08-22T08:30:00")
    private LocalDateTime feedingTime;

    @NotBlank(message = "食物类型不能为空")
    @Schema(description = "食物类型: main_food/snack/can/other", example = "main_food")
    private String foodType;

    @DecimalMin(value = "0", message = "喂食量不能为负")
    @Schema(description = "喂食量数值（选填）", example = "50")
    private BigDecimal amount;

    @Schema(description = "单位: g/ml，默认 g", example = "g")
    private String unit;

    @Schema(description = "关联食品管理卡ID（Phase 7 预留）", example = "null")
    private Long foodId;

    @Schema(description = "来源喂食计划ID", example = "601")
    private Long feedingPlanId;

    @Schema(description = "来源喂食计划项ID", example = "701")
    private Long feedingPlanItemId;

    @Schema(description = "计划时段", example = "2026-08-24T08:00:00")
    private LocalDateTime plannedAt;

    @Size(max = 100)
    @Schema(description = "食品名称快照", example = "室内成猫鸡肉配方")
    private String foodNameSnapshot;

    @Size(max = 500, message = "备注不能超过500字符")
    @Schema(description = "备注", example = "新开的罐头")
    private String notes;
}

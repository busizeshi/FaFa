package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喂食记录DTO
 *
 * @author FaFa Team
 * @since 2.0
 */
@Schema(description = "喂食记录")
public record FeedingRecordDTO(
    @Schema(description = "喂食记录ID", example = "1") Long id,
    @Schema(description = "所属宠物ID", example = "1") Long petId,
    @Schema(description = "实际喂食时间", example = "2026-08-20T08:30:00") LocalDateTime feedingTime,
    @Schema(description = "食物类型：main_food=主食，snack=零食，can=罐头，other=其他", example = "main_food") String foodType,
    @Schema(description = "喂食量") BigDecimal amount,
    @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") String unit,
    @Schema(description = "关联食品卡ID", example = "101") Long foodId,
    @Schema(description = "来源喂食计划ID", example = "201") Long feedingPlanId,
    @Schema(description = "来源喂食计划项ID", example = "301") Long feedingPlanItemId,
    @Schema(description = "计划喂食时间", example = "2026-08-20T08:00:00") LocalDateTime plannedAt,
    @Schema(description = "食品名称快照", example = "成猫鸡肉配方粮") String foodNameSnapshot,
    @Schema(description = "备注") String notes,
    @Schema(description = "创建时间", example = "2026-08-20T08:30:00") LocalDateTime createdAt,
    @Schema(description = "最后更新时间", example = "2026-08-20T08:30:00") LocalDateTime updatedAt
) {
}

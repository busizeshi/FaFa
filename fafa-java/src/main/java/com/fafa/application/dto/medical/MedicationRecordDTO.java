package com.fafa.application.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "实际用药记录")
public record MedicationRecordDTO(
                                  @Schema(description = "用药记录ID", example = "1") Long id,
                                  @Schema(description = "所属宠物ID", example = "1") Long petId,
                                  @Schema(description = "关联用药计划ID", example = "1001") Long medicationPlanId,
                                  @Schema(description = "关联提醒ID", example = "2001") Long reminderId,
                                  @Schema(description = "计划服药时间", example = "2026-08-20T08:00:00") LocalDateTime scheduledAt,
                                  @Schema(description = "实际服药时间", example = "2026-08-20T08:10:00") LocalDateTime takenAt,
                                  @Schema(description = "实际用药剂量数值", example = "1") BigDecimal dosageValue,
                                  @Schema(description = "剂量单位", example = "粒") String dosageUnit,
                                  @Schema(description = "服药状态：taken=已服用，skipped=跳过，missed=漏服", example = "taken") String status,
                                  @Schema(description = "用药记录备注") String notes,
                                  @Schema(description = "创建时间", example = "2026-08-20T08:10:00") LocalDateTime createdAt) {
}

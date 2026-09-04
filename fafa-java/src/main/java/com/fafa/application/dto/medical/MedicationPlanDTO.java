package com.fafa.application.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "用药计划")
public record MedicationPlanDTO(
                                @Schema(description = "用药计划ID", example = "1") Long id,
                                @Schema(description = "所属宠物ID", example = "1") Long petId,
                                @Schema(description = "来源就医档案ID", example = "1001") Long sourceVisitId,
                                @Schema(description = "药品名称", example = "阿莫西林") String medicationName,
                                @Schema(description = "药品规格", example = "50mg/粒") String specification,
                                @Schema(description = "用药目的", example = "治疗呼吸道感染") String purpose,
                                @Schema(description = "单次用药剂量数值", example = "1") BigDecimal dosageValue,
                                @Schema(description = "剂量单位", example = "粒") String dosageUnit,
                                @Schema(description = "用药频率类型", example = "daily") String frequencyType,
                                @Schema(description = "频率数值，具体含义由 frequencyType 决定", example = "2") Integer frequencyValue,
                                @Schema(description = "每日服药时间列表") List<LocalTime> administrationTimes,
                                @Schema(description = "用餐关系", example = "after_meal") String mealRelation,
                                @Schema(description = "用药计划开始日期", example = "2026-08-20") LocalDate startDate,
                                @Schema(description = "用药计划结束日期") LocalDate endDate,
                                @Schema(description = "计划状态", example = "active") String status,
                                @Schema(description = "关联提醒ID列表") List<Long> reminderIds,
                                @Schema(description = "创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
                                @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt) {
}

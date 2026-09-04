package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "保存实际用药记录请求")
public class CreateMedicationRecordRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotNull
    @Schema(description = "关联用药计划ID", example = "1001")
    private Long medicationPlanId;
    @NotNull
    @Schema(description = "计划服药时间", example = "2026-08-20T08:00:00")
    private LocalDateTime scheduledAt;
    @Schema(description = "实际服药时间", example = "2026-08-20T08:10:00")
    private LocalDateTime takenAt;
    @NotNull
    @Schema(description = "服药状态：taken=已服用，skipped=跳过，missed=漏服", example = "taken")
    private String status;
    @NotNull
    @Schema(description = "实际用药剂量数值", example = "1")
    private BigDecimal dosageValue;
    @NotNull
    @Schema(description = "剂量单位", example = "粒")
    private String dosageUnit;
    @Schema(description = "用药记录备注")
    private String notes;
}

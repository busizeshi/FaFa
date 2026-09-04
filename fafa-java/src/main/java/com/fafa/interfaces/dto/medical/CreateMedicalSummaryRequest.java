package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "生成就诊摘要请求")
public class CreateMedicalSummaryRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotNull
    @Schema(description = "摘要统计开始日期", example = "2026-08-01")
    private LocalDate startDate;
    @NotNull
    @Schema(description = "摘要统计结束日期", example = "2026-08-31")
    private LocalDate endDate;
    @Schema(description = "是否包含关联医疗素材", example = "true")
    private boolean includeMedia;
}

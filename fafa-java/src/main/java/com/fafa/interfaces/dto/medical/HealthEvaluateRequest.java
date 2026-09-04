package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "立即评估健康预警请求")
public class HealthEvaluateRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @Schema(description = "评估截止日期，不传时使用当前日期", example = "2026-08-31")
    private LocalDate asOfDate;
}

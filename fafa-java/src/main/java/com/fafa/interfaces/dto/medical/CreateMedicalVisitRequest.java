package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "创建就医档案请求")
public class CreateMedicalVisitRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotNull
    @Schema(description = "就诊日期", example = "2026-08-20")
    private LocalDate visitDate;
    @NotBlank
    @Size(max = 200)
    @Schema(description = "就诊医院名称", example = "爱宠动物医院")
    private String hospitalName;
    @Size(max = 100)
    @Schema(description = "接诊医生姓名", example = "李医生")
    private String doctorName;
    @Size(max = 1000)
    @Schema(description = "主诉，即本次就诊主要问题")
    private String chiefComplaint;
    @Size(max = 2000)
    @Schema(description = "诊断结果")
    private String diagnosis;
    @Size(max = 3000)
    @Schema(description = "处方内容")
    private String prescription;
    @Size(max = 2000)
    @Schema(description = "治疗或处置内容")
    private String treatment;
    @Schema(description = "本次就诊总费用", example = "320.00")
    private BigDecimal totalCost;
    @Size(max = 2000)
    @Schema(description = "就诊备注")
    private String notes;
}

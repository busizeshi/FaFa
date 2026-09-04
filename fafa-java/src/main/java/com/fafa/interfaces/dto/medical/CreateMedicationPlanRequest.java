package com.fafa.interfaces.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Schema(description = "创建用药计划请求")
public class CreateMedicationPlanRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @Schema(description = "来源就医档案ID", example = "1001")
    private Long sourceVisitId;
    @NotBlank
    @Size(max = 200)
    @Schema(description = "药品名称", example = "阿莫西林")
    private String medicationName;
    @Size(max = 100)
    @Schema(description = "药品规格", example = "50mg/粒")
    private String specification;
    @Size(max = 500)
    @Schema(description = "用药目的", example = "治疗呼吸道感染")
    private String purpose;
    @NotNull
    @Schema(description = "单次用药剂量数值", example = "1")
    private BigDecimal dosageValue;
    @NotBlank
    @Schema(description = "剂量单位", example = "粒")
    private String dosageUnit;
    @NotBlank
    @Schema(description = "用药频率类型", example = "daily")
    private String frequencyType;
    @Schema(description = "频率数值，具体含义由 frequencyType 决定", example = "2")
    private Integer frequencyValue;
    @Size(max = 6)
    @Schema(description = "每日服药时间列表，最多6个", example = "[\"08:00\", \"20:00\"]")
    private List<LocalTime> administrationTimes;
    @NotBlank
    @Schema(description = "用餐关系：before_meal=餐前，after_meal=餐后，with_meal=随餐，anytime=不限", example = "after_meal")
    private String mealRelation;
    @NotNull
    @Schema(description = "用药计划开始日期", example = "2026-08-20")
    private LocalDate startDate;
    @Schema(description = "用药计划结束日期，不传表示长期计划", example = "2026-08-27")
    private LocalDate endDate;
    @Size(max = 1000)
    @Schema(description = "用药计划备注")
    private String notes;
}

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
    private Long petId;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 100)
    private String brand;
    @NotBlank
    private String foodType;
    @Size(max = 100)
    private String specification;
    @DecimalMin(value = "0.001")
    private BigDecimal packageWeight;
    private String packageWeightUnit;
    @DecimalMin(value = "0")
    private BigDecimal price;
    private String currency;
    private LocalDate openedAt;
    private LocalDate expiresAt;
    private String nutritionBasis;
    private BigDecimal proteinPercent;
    private BigDecimal fatPercent;
    private BigDecimal fiberPercent;
    private BigDecimal moisturePercent;
    private BigDecimal ashPercent;
    private BigDecimal calciumPercent;
    private BigDecimal phosphorusPercent;
    private BigDecimal energyKcal;
    private String energyUnit;
    private List<String> proteinSources;
    private List<String> ingredients;
    private List<String> additiveFlags;
    private String lifeStageLabel;
    private Long sourceMediaId;
    private Long sourceRecognitionTaskId;
    @Size(max = 500)
    private String notes;
}

package com.fafa.interfaces.dto.food;

import com.fafa.application.dto.food.FoodCardCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 确认识别结果并创建食品卡的请求。 */
@Data
public class ConfirmFoodRecognitionRequest {
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
    @Size(max = 500)
    private String notes;

    public FoodCardCommand toCommand(Long petId, Long taskId, Long mediaId) {
        return new FoodCardCommand(petId, name, brand, foodType, specification, packageWeight,
                packageWeightUnit, price, currency, openedAt, expiresAt, nutritionBasis, proteinPercent,
                fatPercent, fiberPercent, moisturePercent, ashPercent, calciumPercent, phosphorusPercent,
                energyKcal, energyUnit, proteinSources, ingredients, additiveFlags, lifeStageLabel, mediaId,
                taskId, notes);
    }
}

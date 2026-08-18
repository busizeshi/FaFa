package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建宠物请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "创建宠物请求")
public class CreatePetRequest {

    @NotBlank(message = "宠物名称不能为空")
    @Schema(description = "宠物名称", example = "豆包", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "宠物种类不能为空")
    @Schema(description = "宠物种类", example = "cat", allowableValues = {"cat", "dog", "other"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String species;

    @Schema(description = "品种", example = "英短蓝猫")
    private String breed;

    @Schema(description = "性别", example = "male", allowableValues = {"male", "female", "unknown"})
    private String gender;

    @Schema(description = "出生日期", example = "2023-01-15")
    private LocalDate birthDate;

    @Schema(description = "到家日期", example = "2023-03-20")
    private LocalDate adoptDate;

    @Schema(description = "体重(kg)", example = "4.2")
    private Double weight;

    @Schema(description = "是否绝育", example = "false")
    private Boolean isNeutered;

    @Schema(description = "毛色", example = "蓝灰色")
    private String coatColor;

    @Schema(description = "备注")
    private String remarks;
}

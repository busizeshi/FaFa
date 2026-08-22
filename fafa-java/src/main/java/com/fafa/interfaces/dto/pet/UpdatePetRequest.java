package com.fafa.interfaces.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新宠物请求
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "更新宠物请求")
public class UpdatePetRequest {

    @NotBlank(message = "宠物名称不能为空")
    @Schema(description = "宠物名称", example = "小白")
    private String name;

    @Schema(description = "品种", example = "英短")
    private String breed;

    @Schema(description = "性别: male/female/unknown", example = "male")
    private String gender;

    @Schema(description = "出生日期（外推）", example = "2023-01-15")
    private LocalDate birthDate;

    @Schema(description = "是否绝育", example = "false")
    private Boolean isNeutered;

    @Schema(description = "到家日期", example = "2023-03-20")
    private LocalDate adoptDate;

    @Schema(description = "当前体重（kg）", example = "4.5")
    private BigDecimal weight;
}

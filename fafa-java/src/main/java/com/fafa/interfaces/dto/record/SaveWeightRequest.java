package com.fafa.interfaces.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保存体重记录请求
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@Schema(description = "保存体重记录请求")
public class SaveWeightRequest {

    @NotNull(message = "宠物ID不能为空")
    @Schema(description = "宠物ID", example = "1")
    private Long petId;

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期", example = "2026-08-22")
    private LocalDate recordDate;

    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "0.01", message = "体重必须为正数")
    @Schema(description = "体重（kg）", example = "4.5")
    private BigDecimal weight;

    @Size(max = 500, message = "备注不能超过500字符")
    @Schema(description = "备注", example = "早上空腹称重")
    private String notes;
}

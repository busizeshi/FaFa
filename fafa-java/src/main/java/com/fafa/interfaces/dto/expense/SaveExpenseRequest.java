package com.fafa.interfaces.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 手工开支写入请求。 */
@Data
@Schema(description = "手工开支写入请求")
public class SaveExpenseRequest {
    @NotNull
    @Schema(description = "所属宠物ID", example = "1")
    private Long petId;
    @NotBlank
    @Schema(description = "开支分类编码", example = "food")
    private String category;
    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "开支金额，必须大于0，最多保留2位小数", example = "128.00")
    private BigDecimal amount;
    @Schema(description = "货币代码，未填写时按系统默认货币处理", example = "CNY")
    private String currency;
    @NotNull
    @Schema(description = "开支发生日期", example = "2026-08-20")
    private LocalDate expenseDate;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "开支标题", example = "购买猫粮")
    private String title;
    @Size(max = 500)
    @Schema(description = "开支备注")
    private String notes;
    @Schema(description = "关联收据素材ID", example = "1001")
    private Long receiptMediaId;
    @Schema(description = "关联食品卡ID", example = "2001")
    private Long foodCardId;
}

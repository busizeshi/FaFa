package com.fafa.interfaces.dto.expense;

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
public class SaveExpenseRequest {
    @NotNull
    private Long petId;
    @NotBlank
    private String category;
    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    private String currency;
    @NotNull
    private LocalDate expenseDate;
    @NotBlank
    @Size(max = 100)
    private String title;
    @Size(max = 500)
    private String notes;
    private Long receiptMediaId;
    private Long foodCardId;
}

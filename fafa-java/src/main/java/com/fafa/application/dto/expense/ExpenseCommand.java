package com.fafa.application.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 创建或更新手工开支的应用层命令。 */
public record ExpenseCommand(Long petId, String category, BigDecimal amount, String currency,
                             LocalDate expenseDate, String title, String notes, Long receiptMediaId,
                             Long foodCardId) {
}

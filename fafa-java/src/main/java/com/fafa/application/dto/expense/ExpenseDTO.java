package com.fafa.application.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 开支记录响应。 */
public record ExpenseDTO(Long id, Long petId, String category, String categoryLabel,
                         BigDecimal amount, String currency, LocalDate expenseDate, String title,
                         String notes, Long receiptMediaId, Long foodCardId, String foodNameSnapshot,
                         Long medicalVisitId, String sourceType, boolean systemGenerated,
                         boolean editable, LocalDateTime createdAt, LocalDateTime updatedAt) {
}

package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重记录DTO
 *
 * change 为较时间上一条记录的变化值（首条为 null）。
 *
 * @author FaFa Team
 * @since 2.0
 */
public record WeightRecordDTO(
    Long id,
    Long petId,
    LocalDate recordDate,
    BigDecimal weight,
    String notes,
    BigDecimal change,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

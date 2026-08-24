package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喂食记录DTO
 *
 * @author FaFa Team
 * @since 2.0
 */
public record FeedingRecordDTO(
    Long id,
    Long petId,
    LocalDateTime feedingTime,
    String foodType,
    BigDecimal amount,
    String unit,
    Long foodId,
    Long feedingPlanId,
    Long feedingPlanItemId,
    LocalDateTime plannedAt,
    String foodNameSnapshot,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

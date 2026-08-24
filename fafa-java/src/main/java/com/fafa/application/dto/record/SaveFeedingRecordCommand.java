package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保存喂食记录命令（新增与更新共用）
 *
 * @author FaFa Team
 * @since 2.0
 */
public record SaveFeedingRecordCommand(
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
    String notes
) {
}

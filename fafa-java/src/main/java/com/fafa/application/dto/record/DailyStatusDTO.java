package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日状态记录DTO
 *
 * abnormalDimensions 为异常维度 code 列表，前端高亮与预警展示用。
 *
 * @author FaFa Team
 * @since 2.0
 */
public record DailyStatusDTO(
    Long id,
    Long petId,
    LocalDate recordDate,
    String appetite,
    String energy,
    String waterIntake,
    String defecation,
    String vomiting,
    String urination,
    BigDecimal appetiteAmount,
    BigDecimal waterAmount,
    String notes,
    Boolean allNormal,
    List<String> abnormalDimensions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

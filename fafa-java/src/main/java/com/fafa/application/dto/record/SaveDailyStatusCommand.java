package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保存每日状态命令（upsert 语义）
 *
 * 维度字段为 code 字符串，由应用服务转换为领域枚举。
 *
 * @author FaFa Team
 * @since 2.0
 */
public record SaveDailyStatusCommand(
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
    String notes
) {
}

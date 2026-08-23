package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保存体重记录命令
 *
 * @author FaFa Team
 * @since 2.0
 */
public record SaveWeightRecordCommand(
    Long petId,
    LocalDate recordDate,
    BigDecimal weight,
    String notes
) {
}

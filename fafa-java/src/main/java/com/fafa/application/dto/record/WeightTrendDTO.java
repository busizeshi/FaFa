package com.fafa.application.dto.record;

import java.math.BigDecimal;
import java.util.List;

/**
 * 体重趋势DTO
 *
 * records 按日期升序（含逐条 change），latest 为最近一条，
 * totalChange 为区间末条减首条的总变化。
 *
 * @author FaFa Team
 * @since 2.0
 */
public record WeightTrendDTO(
    List<WeightRecordDTO> records,
    WeightRecordDTO latest,
    BigDecimal totalChange
) {
}

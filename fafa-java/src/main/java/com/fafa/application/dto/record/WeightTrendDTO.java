package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "体重趋势")
public record WeightTrendDTO(
    @Schema(description = "按日期升序排列的体重记录列表") List<WeightRecordDTO> records,
    @Schema(description = "区间内最近一条体重记录") WeightRecordDTO latest,
    @Schema(description = "区间总变化量（kg），末条记录减首条记录", example = "0.5") BigDecimal totalChange
) {
}

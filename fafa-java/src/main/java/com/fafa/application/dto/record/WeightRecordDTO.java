package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "体重记录")
public record WeightRecordDTO(
    @Schema(description = "体重记录ID", example = "1") Long id,
    @Schema(description = "所属宠物ID", example = "1") Long petId,
    @Schema(description = "记录日期", example = "2026-08-20") LocalDate recordDate,
    @Schema(description = "体重（kg）", example = "4.5") BigDecimal weight,
    @Schema(description = "备注") String notes,
    @Schema(description = "相较上一条记录的体重变化（kg），首条记录为空", example = "0.2") BigDecimal change,
    @Schema(description = "创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
    @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt
) {
}

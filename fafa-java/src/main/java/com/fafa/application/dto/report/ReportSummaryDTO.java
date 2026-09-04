package com.fafa.application.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "周期报告摘要")
public record ReportSummaryDTO(
                               @Schema(description = "报告ID", example = "1") Long id,
                               @Schema(description = "所属宠物ID", example = "1") Long petId,
                               @Schema(description = "报告周期类型", example = "month") String periodType,
                               @Schema(description = "报告统计开始日期", example = "2026-08-01") LocalDate periodStart,
                               @Schema(description = "报告统计结束日期", example = "2026-08-31") LocalDate periodEnd,
                               @Schema(description = "事实数据状态") String dataStatus,
                               @Schema(description = "报告生成状态") String status,
                               @Schema(description = "内容模式") String contentMode,
                               @Schema(description = "生成版本号", example = "1") int generationVersion,
                               @Schema(description = "报告生成时间") LocalDateTime generatedAt) {
}

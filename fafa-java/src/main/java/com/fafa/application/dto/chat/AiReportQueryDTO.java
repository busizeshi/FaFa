package com.fafa.application.dto.chat;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** AI 周期报告查询结果。 */
@Schema(description = "AI 周期报告查询结果")
public record AiReportQueryDTO(
        @Schema(description = "周期报告事实列表") List<ReportFactDTO> records) {

    @Schema(description = "AI 使用的周期报告事实")
    public record ReportFactDTO(
            @Schema(description = "报告ID", example = "1") Long id,
            @Schema(description = "报告周期类型", example = "month") String periodType,
            @Schema(description = "周期开始日期", example = "2026-08-01") LocalDate periodStart,
            @Schema(description = "周期结束日期", example = "2026-08-31") LocalDate periodEnd,
            @Schema(description = "报告状态", example = "completed") String status,
            @Schema(description = "报告内容数据") JsonNode content) {
    }
}

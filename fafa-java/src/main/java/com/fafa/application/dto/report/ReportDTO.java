package com.fafa.application.dto.report;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "宠物周期报告")
public record ReportDTO(
                        @Schema(description = "报告ID", example = "1") Long id,
                        @Schema(description = "所属宠物ID", example = "1") Long petId,
                        @Schema(description = "报告周期类型", example = "month") String periodType,
                        @Schema(description = "报告统计开始日期", example = "2026-08-01") LocalDate periodStart,
                        @Schema(description = "报告统计结束日期", example = "2026-08-31") LocalDate periodEnd,
                        @Schema(description = "事实数据状态") String dataStatus,
                        @Schema(description = "报告生成状态") String status,
                        @Schema(description = "数据快照版本") String snapshotVersion,
                        @Schema(description = "统计计算版本") String calculationVersion,
                        @Schema(description = "生成报告时保存的事实快照") JsonNode factSnapshot,
                        @Schema(description = "报告内容") JsonNode content,
                        @Schema(description = "内容模式") String contentMode,
                        @Schema(description = "使用的模型名称") String modelName,
                        @Schema(description = "提示词版本") String promptVersion,
                        @Schema(description = "生成触发来源") String generationTrigger,
                        @Schema(description = "生成版本号", example = "1") int generationVersion,
                        @Schema(description = "最近一次失败代码") String lastErrorCode,
                        @Schema(description = "最近一次失败信息") String lastErrorMessage,
                        @Schema(description = "报告生成时间") LocalDateTime generatedAt,
                        @Schema(description = "创建时间") LocalDateTime createdAt,
                        @Schema(description = "最后更新时间") LocalDateTime updatedAt) {
}

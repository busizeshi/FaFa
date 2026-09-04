package com.fafa.application.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "医疗时间线项")
public record MedicalTimelineItemDTO(
                                     @Schema(description = "时间线项类型", example = "visit") String type,
                                     @Schema(description = "来源记录ID", example = "1") Long sourceId,
                                     @Schema(description = "事件日期", example = "2026-08-20") LocalDate eventDate,
                                     @Schema(description = "事件标题", example = "年度体检") String title,
                                     @Schema(description = "事件摘要") String summary,
                                     @Schema(description = "事件状态", example = "completed") String status) {
}

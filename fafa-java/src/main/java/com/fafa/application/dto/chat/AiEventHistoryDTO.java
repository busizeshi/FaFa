package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Agent 成长事件工具响应。 */
@Schema(description = "AI 查询的成长事件历史")
public record AiEventHistoryDTO(
                                @Schema(description = "宠物ID", example = "1") Long petId,
                                @Schema(description = "查询开始日期", example = "2026-08-01") LocalDate startDate,
                                @Schema(description = "查询结束日期", example = "2026-08-31") LocalDate endDate,
                                @Schema(description = "事件记录列表") List<AiEventDTO> records,
                                @Schema(description = "记录总数", example = "5") int count) {
    @Schema(description = "AI 成长事件记录")
    public record AiEventDTO(
                             @Schema(description = "事件ID", example = "1") Long id,
                             @Schema(description = "事件来源", example = "manual") String source,
                             @Schema(description = "事件类型", example = "checkup") String eventType,
                             @Schema(description = "事件标题", example = "年度体检") String title,
                             @Schema(description = "事件详细内容") String content,
                             @Schema(description = "事件日期", example = "2026-08-20") LocalDate eventDate,
                             @Schema(description = "事件标签列表") List<String> tags) {
    }
}

package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agent 提醒完成历史工具响应。 */
@Schema(description = "AI 查询的提醒完成历史")
public record AiReminderHistoryDTO(
                                   @Schema(description = "宠物ID", example = "1") Long petId,
                                   @Schema(description = "查询开始日期", example = "2026-08-01") LocalDate startDate,
                                   @Schema(description = "查询结束日期", example = "2026-08-31") LocalDate endDate,
                                   @Schema(description = "提醒完成记录列表") List<AiReminderCompletionDTO> records,
                                   @Schema(description = "记录总数", example = "8") int count) {
    @Schema(description = "AI 提醒完成记录")
    public record AiReminderCompletionDTO(
                                          @Schema(description = "完成记录ID", example = "1") Long id,
                                          @Schema(description = "提醒ID", example = "101") Long reminderId,
                                          @Schema(description = "提醒标题", example = "体内驱虫") String title,
                                          @Schema(description = "提醒类型", example = "deworming") String type,
                                          @Schema(description = "完成时间", example = "2026-08-20T10:30:00") LocalDateTime completionTime,
                                          @Schema(description = "完成备注") String notes) {
    }
}

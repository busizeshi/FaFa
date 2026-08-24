package com.fafa.application.dto.chat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agent 提醒完成历史工具响应。 */
public record AiReminderHistoryDTO(Long petId, LocalDate startDate, LocalDate endDate,
                                   List<AiReminderCompletionDTO> records, int count) {
    public record AiReminderCompletionDTO(Long id, Long reminderId, String title, String type,
                                          LocalDateTime completionTime, String notes) {
    }
}

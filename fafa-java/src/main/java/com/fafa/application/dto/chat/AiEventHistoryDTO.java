package com.fafa.application.dto.chat;

import java.time.LocalDate;
import java.util.List;

/** Agent 成长事件工具响应。 */
public record AiEventHistoryDTO(Long petId, LocalDate startDate, LocalDate endDate,
                                List<AiEventDTO> records, int count) {
    public record AiEventDTO(Long id, String source, String eventType, String title,
                             String content, LocalDate eventDate, List<String> tags) {
    }
}

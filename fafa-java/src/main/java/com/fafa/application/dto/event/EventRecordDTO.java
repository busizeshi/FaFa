package com.fafa.application.dto.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件记录DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
public record EventRecordDTO(
    Long id,
    Long petId,
    String source,
    String eventType,
    String title,
    String content,
    LocalDate eventDate,
    List<String> materialUrls,
    List<String> tags,
    LocalDateTime createdAt
) {
}

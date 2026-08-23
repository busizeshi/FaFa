package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 完成历史DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
public record CompletionDTO(
    Long id,
    Long reminderId,
    Long petId,
    String petName,
    String reminderTitle,
    String reminderType,
    LocalDateTime completionTime,
    String notes,
    List<String> photoUrls,
    Long eventRecordId,
    LocalDateTime createdAt
) {
}

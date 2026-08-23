package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;

/**
 * 提醒DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
public record ReminderDTO(
    Long id,
    Long petId,
    String petName,
    String type,
    String title,
    String description,
    String intervalType,
    Integer intervalValue,
    LocalDateTime nextRemindTime,
    String status,
    LocalDateTime lastCompletionTime,
    String lastCompletionNotes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

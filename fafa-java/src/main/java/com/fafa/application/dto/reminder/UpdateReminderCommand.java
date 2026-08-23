package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;

/**
 * 编辑提醒命令（petId 不可改）
 *
 * @author FaFa Team
 * @since 3.0
 */
public record UpdateReminderCommand(
    String type,
    String title,
    String description,
    String intervalType,
    Integer intervalValue,
    LocalDateTime nextRemindTime
) {
}

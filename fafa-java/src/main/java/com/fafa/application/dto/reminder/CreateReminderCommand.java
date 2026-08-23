package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;

/**
 * 创建提醒命令
 *
 * @author FaFa Team
 * @since 3.0
 */
public record CreateReminderCommand(
    Long petId,
    String type,
    String title,
    String description,
    String intervalType,
    Integer intervalValue,
    LocalDateTime nextRemindTime
) {
}

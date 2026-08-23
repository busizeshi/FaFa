package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 完成提醒命令（全部选填）
 *
 * @author FaFa Team
 * @since 3.0
 */
public record CompleteReminderCommand(
    LocalDateTime completionTime,
    String notes,
    List<String> photoUrls
) {
}

package com.fafa.application.dto.reminder;

import java.time.LocalDateTime;

/**
 * 今日提醒合并视图项（待办 + 今日已完成）
 *
 * @author FaFa Team
 * @since 3.0
 */
public record TodayReminderDTO(
    ReminderDTO reminder,
    boolean overdue,
    boolean completedToday,
    LocalDateTime completionTime
) {
}

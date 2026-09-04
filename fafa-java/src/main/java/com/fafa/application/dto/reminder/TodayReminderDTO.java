package com.fafa.application.dto.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 今日提醒合并视图项（待办 + 今日已完成）
 *
 * @author FaFa Team
 * @since 3.0
 */
@Schema(description = "今日提醒合并视图")
public record TodayReminderDTO(
    @Schema(description = "提醒详情") ReminderDTO reminder,
    @Schema(description = "是否已逾期", example = "false") boolean overdue,
    @Schema(description = "今日是否已完成", example = "true") boolean completedToday,
    @Schema(description = "今日完成时间") LocalDateTime completionTime
) {
}

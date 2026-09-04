package com.fafa.application.dto.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 提醒DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
@Schema(description = "提醒")
public record ReminderDTO(
    @Schema(description = "提醒ID", example = "1") Long id,
    @Schema(description = "所属宠物ID", example = "1") Long petId,
    @Schema(description = "宠物名称", example = "小白") String petName,
    @Schema(description = "提醒类型", example = "deworming") String type,
    @Schema(description = "提醒标题", example = "体内驱虫") String title,
    @Schema(description = "提醒描述") String description,
    @Schema(description = "周期类型：once=一次性，days=按天，weekly=按周，monthly=按月，months=按月间隔，yearly=按年", example = "months") String intervalType,
    @Schema(description = "周期数值，具体含义由 intervalType 决定", example = "3") Integer intervalValue,
    @Schema(description = "下次提醒时间", example = "2026-09-01T09:00:00") LocalDateTime nextRemindTime,
    @Schema(description = "提醒状态：pending=待处理，paused=已暂停，finished=已结束", example = "pending") String status,
    @Schema(description = "最近一次完成时间") LocalDateTime lastCompletionTime,
    @Schema(description = "最近一次完成备注") String lastCompletionNotes,
    @Schema(description = "创建时间") LocalDateTime createdAt,
    @Schema(description = "最后更新时间") LocalDateTime updatedAt
) {
}

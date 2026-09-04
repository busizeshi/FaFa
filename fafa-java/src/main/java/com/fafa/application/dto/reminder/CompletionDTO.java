package com.fafa.application.dto.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 完成历史DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
@Schema(description = "提醒完成记录")
public record CompletionDTO(
    @Schema(description = "完成记录ID", example = "1") Long id,
    @Schema(description = "提醒ID", example = "101") Long reminderId,
    @Schema(description = "所属宠物ID", example = "1") Long petId,
    @Schema(description = "宠物名称", example = "小白") String petName,
    @Schema(description = "提醒标题", example = "体内驱虫") String reminderTitle,
    @Schema(description = "提醒类型", example = "deworming") String reminderType,
    @Schema(description = "完成时间", example = "2026-08-20T10:30:00") LocalDateTime completionTime,
    @Schema(description = "完成备注") String notes,
    @Schema(description = "完成时上传的照片URL列表") List<String> photoUrls,
    @Schema(description = "完成提醒生成的事件记录ID", example = "201") Long eventRecordId,
    @Schema(description = "记录创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt
) {
}

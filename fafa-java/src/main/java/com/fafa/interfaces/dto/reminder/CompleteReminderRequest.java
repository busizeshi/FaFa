package com.fafa.interfaces.dto.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 完成提醒请求（全部选填）
 *
 * @author FaFa Team
 * @since 3.0
 */
@Data
@Schema(description = "完成提醒请求")
public class CompleteReminderRequest {

    @Schema(description = "完成时间（缺省=当前时间，不能晚于当前时间+5分钟）", example = "2026-08-22T10:30:00")
    private LocalDateTime completionTime;

    @Size(max = 500, message = "完成备注不能超过500字符")
    @Schema(description = "完成备注", example = "已接种，无不良反应")
    private String notes;

    @Size(max = 9, message = "完成照片不能超过9张")
    @Schema(description = "完成照片URL列表（如疫苗本照片）")
    private List<String> photoUrls;
}

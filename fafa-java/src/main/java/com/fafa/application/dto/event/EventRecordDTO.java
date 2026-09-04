package com.fafa.application.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件记录DTO
 *
 * @author FaFa Team
 * @since 3.0
 */
@Schema(description = "成长档案事件记录")
public record EventRecordDTO(
    @Schema(description = "事件ID", example = "1")
    Long id,
    @Schema(description = "所属宠物ID", example = "1")
    Long petId,
    @Schema(description = "事件来源：manual=手动创建，reminder=提醒完成生成", example = "manual")
    String source,
    @Schema(description = "事件类型", example = "checkup")
    String eventType,
    @Schema(description = "事件标题", example = "年度体检")
    String title,
    @Schema(description = "事件详细内容")
    String content,
    @Schema(description = "事件发生日期", example = "2026-08-20")
    LocalDate eventDate,
    @Schema(description = "关联素材URL列表")
    List<String> materialUrls,
    @Schema(description = "事件标签列表")
    List<String> tags,
    @Schema(description = "创建时间", example = "2026-08-20T10:30:00")
    LocalDateTime createdAt
) {
}

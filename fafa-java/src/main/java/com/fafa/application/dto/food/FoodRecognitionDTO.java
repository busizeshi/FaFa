package com.fafa.application.dto.food;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 食品识别任务返回对象。 */
@Schema(description = "食品包装识别任务")
public record FoodRecognitionDTO(
        @Schema(description = "识别任务ID", example = "1") Long taskId,
        @Schema(description = "所属宠物ID", example = "1") Long petId,
        @Schema(description = "待识别的食品包装图片媒体ID", example = "1001") Long mediaId,
        @Schema(description = "任务状态：pending=待处理，processing=处理中，completed=已完成，confirmed=已确认，rejected=已拒绝，failed=失败，expired=已过期", example = "completed") String status,
        @Schema(description = "结构化识别草稿，具体字段由识别结果决定") Map<String, Object> draft,
        @Schema(description = "识别结果警告列表") List<String> warnings,
        @Schema(description = "使用的模型名称", example = "gpt-4o") String model,
        @Schema(description = "使用的提示词版本") String promptVersion,
        @Schema(description = "确认后生成的食品卡ID", example = "2001") Long foodCardId,
        @Schema(description = "失败代码，成功时为空") String errorCode,
        @Schema(description = "失败信息，成功时为空") String errorMessage,
        @Schema(description = "任务创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
        @Schema(description = "任务完成时间", example = "2026-08-20T10:31:00") LocalDateTime completedAt
) {
}

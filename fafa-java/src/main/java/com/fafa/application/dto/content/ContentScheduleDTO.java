package com.fafa.application.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "内容周期配置")
public record ContentScheduleDTO(
                                 @Schema(description = "周期配置ID", example = "1") Long id,
                                 @Schema(description = "所属宠物ID", example = "1") Long petId,
                                 @Schema(description = "内容类型", example = "share_note") String contentType,
                                 @Schema(description = "主题类型", example = "monthly_summary") String topicType,
                                 @Schema(description = "执行间隔天数", example = "15") int intervalDays,
                                 @Schema(description = "下次执行时间") LocalDateTime nextRunAt,
                                 @Schema(description = "最近一次执行时间") LocalDateTime lastRunAt,
                                 @Schema(description = "是否启用周期生成", example = "true") boolean enabled,
                                 @Schema(description = "最后更新时间") LocalDateTime updatedAt) {
}

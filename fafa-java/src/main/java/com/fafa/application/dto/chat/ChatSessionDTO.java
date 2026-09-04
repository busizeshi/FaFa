package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** AI 会话展示 DTO。 */
@Schema(description = "AI 会话")
public record ChatSessionDTO(
                             @Schema(description = "会话ID", example = "1") Long sessionId,
                             @Schema(description = "关联宠物ID；通用会话时为空", example = "1") Long petId,
                             @Schema(description = "会话标题", example = "小白健康咨询") String title,
                             @Schema(description = "会话状态", example = "active") String status,
                             @Schema(description = "最近一条消息时间") LocalDateTime lastMessageAt,
                             @Schema(description = "会话创建时间") LocalDateTime createdAt) {
}

package com.fafa.application.dto.chat;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** AI 历史消息 DTO。 */
@Schema(description = "AI 会话消息")
public record ChatMessageDTO(
                             @Schema(description = "消息ID", example = "1") Long messageId,
                             @Schema(description = "所属会话ID", example = "1") Long sessionId,
                             @Schema(description = "消息角色：user=用户，assistant=助手", example = "assistant") String role,
                             @Schema(description = "消息文本内容") String content,
                             @Schema(description = "结构化消息块列表") List<JsonNode> blocks,
                             @Schema(description = "消息引用列表") List<JsonNode> citations,
                             @Schema(description = "AI 生成的记录草稿") JsonNode recordDraft,
                             @Schema(description = "助手消息处理状态") String assistantStatus,
                             @Schema(description = "消息创建时间") LocalDateTime createdAt) {
}

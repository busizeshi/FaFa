package com.fafa.application.dto.chat;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

/** AI 历史消息 DTO。 */
public record ChatMessageDTO(Long messageId, Long sessionId, String role, String content,
                             List<JsonNode> blocks, List<JsonNode> citations,
                             JsonNode recordDraft, String assistantStatus,
                             LocalDateTime createdAt) {
}

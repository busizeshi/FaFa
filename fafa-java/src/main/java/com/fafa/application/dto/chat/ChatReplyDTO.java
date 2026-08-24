package com.fafa.application.dto.chat;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** AI 对话响应 DTO。 */
public record ChatReplyDTO(Long sessionId, Long userMessageId, Long assistantMessageId,
                           Long petId, String content, List<JsonNode> blocks,
                           List<JsonNode> citations, List<String> suggestedQuestions,
                           JsonNode recordDraft, String traceId) {
}

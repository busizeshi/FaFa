package com.fafa.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Java 调用 Python Agent 的内部请求。 */
public record ChatAgentRequest(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("pet_id") Long petId,
        String message,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("message_id") String messageId,
        List<HistoryItem> history,
        @JsonProperty("trace_id") String traceId
) {
    public record HistoryItem(String role, String content) {
    }
}

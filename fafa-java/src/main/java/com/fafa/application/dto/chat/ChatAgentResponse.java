package com.fafa.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Python Agent 返回的内部响应。动态 block 由 Java 侧做白名单过滤。 */
public record ChatAgentResponse(
        String answer,
        List<JsonNode> blocks,
        List<JsonNode> citations,
        @JsonProperty("suggested_questions") List<String> suggestedQuestions,
        @JsonProperty("record_draft") JsonNode recordDraft,
        @JsonProperty("tool_calls") List<JsonNode> toolCalls,
        ChatAgentUsage usage,
        @JsonProperty("trace_id") String traceId
) {
    public record ChatAgentUsage(String model,
                                 @JsonProperty("prompt_tokens") Integer promptTokens,
                                 @JsonProperty("completion_tokens") Integer completionTokens) {
    }
}

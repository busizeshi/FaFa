package com.fafa.application.dto.chat;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 对话响应 DTO。 */
@Schema(description = "AI 对话响应")
public record ChatReplyDTO(
                           @Schema(description = "会话ID", example = "1") Long sessionId,
                           @Schema(description = "用户消息ID", example = "10") Long userMessageId,
                           @Schema(description = "助手消息ID", example = "11") Long assistantMessageId,
                           @Schema(description = "关联宠物ID", example = "1") Long petId,
                           @Schema(description = "AI 回复文本") String content,
                           @Schema(description = "结构化回复块列表") List<JsonNode> blocks,
                           @Schema(description = "回复引用列表") List<JsonNode> citations,
                           @Schema(description = "推荐的后续问题") List<String> suggestedQuestions,
                           @Schema(description = "AI 生成的记录草稿") JsonNode recordDraft,
                           @Schema(description = "链路追踪ID") String traceId) {
}

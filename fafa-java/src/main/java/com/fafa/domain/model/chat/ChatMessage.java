package com.fafa.domain.model.chat;

import java.time.LocalDateTime;
import java.util.Objects;

/** AI 会话消息实体。 */
public final class ChatMessage {

    public static final int MAX_CONTENT_LENGTH = 8000;

    private Long id;
    private final Long sessionId;
    private final Long userId;
    private final Long petId;
    private final ChatMessageRole role;
    private final String content;
    private final String clientMessageId;
    private final String blocksJson;
    private final String citationsJson;
    private final String toolCallsJson;
    private final String recordDraftJson;
    private final String model;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final String assistantStatus;
    private final String traceId;
    private final LocalDateTime createdAt;

    private ChatMessage(Long id, Long sessionId, Long userId, Long petId, ChatMessageRole role,
                        String content, String clientMessageId, String blocksJson,
                        String citationsJson, String toolCallsJson, String recordDraftJson,
                        String model, Integer promptTokens, Integer completionTokens,
                        String assistantStatus, String traceId, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.petId = petId;
        this.role = role;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.blocksJson = blocksJson;
        this.citationsJson = citationsJson;
        this.toolCallsJson = toolCallsJson;
        this.recordDraftJson = recordDraftJson;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.assistantStatus = assistantStatus;
        this.traceId = traceId;
        this.createdAt = createdAt;
    }

    public static ChatMessage user(Long sessionId, Long userId, Long petId, String content,
                                   String clientMessageId, String traceId, LocalDateTime now) {
        validateContent(content);
        return new ChatMessage(null, sessionId, userId, petId, ChatMessageRole.USER, content.trim(),
                clientMessageId, null, null, null, null, null, null, null, "answered", traceId, now);
    }

    public static ChatMessage assistant(Long sessionId, Long userId, Long petId, String content,
                                        String blocksJson, String citationsJson, String toolCallsJson,
                                        String recordDraftJson, String model, Integer promptTokens,
                                        Integer completionTokens, String traceId, LocalDateTime now) {
        validateContent(content);
        return new ChatMessage(null, sessionId, userId, petId, ChatMessageRole.ASSISTANT, content.trim(),
                null, blocksJson, citationsJson, toolCallsJson, recordDraftJson, model,
                promptTokens, completionTokens, "answered", traceId, now);
    }

    public static ChatMessage restore(Long id, Long sessionId, Long userId, Long petId,
                                      ChatMessageRole role, String content, String clientMessageId,
                                      String blocksJson, String citationsJson, String toolCallsJson,
                                      String recordDraftJson, String model, Integer promptTokens,
                                      Integer completionTokens, String assistantStatus,
                                      String traceId, LocalDateTime createdAt) {
        return new ChatMessage(id, sessionId, userId, petId, role, content, clientMessageId,
                blocksJson, citationsJson, toolCallsJson, recordDraftJson, model,
                promptTokens, completionTokens, assistantStatus, traceId, createdAt);
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("消息不能超过 " + MAX_CONTENT_LENGTH + " 字符");
        }
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public Long getPetId() { return petId; }
    public ChatMessageRole getRole() { return role; }
    public String getContent() { return content; }
    public String getClientMessageId() { return clientMessageId; }
    public String getBlocksJson() { return blocksJson; }
    public String getCitationsJson() { return citationsJson; }
    public String getToolCallsJson() { return toolCallsJson; }
    public String getRecordDraftJson() { return recordDraftJson; }
    public String getModel() { return model; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public String getAssistantStatus() { return assistantStatus; }
    public String getTraceId() { return traceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

package com.fafa.domain.repository;

import com.fafa.domain.model.chat.ChatMessage;

import java.util.List;
import java.util.Optional;

/** AI 消息仓储。 */
public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findBySessionId(Long sessionId, Long userId, Long beforeId, int limit);
    Optional<ChatMessage> findUserMessageByClientId(Long sessionId, Long userId, String clientMessageId);
    Optional<ChatMessage> findLatestAssistantAfter(Long sessionId, Long userId, Long messageId);
}

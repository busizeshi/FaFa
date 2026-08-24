package com.fafa.domain.repository;

import com.fafa.domain.model.chat.ChatSession;

import java.util.List;
import java.util.Optional;

/** AI 会话仓储。 */
public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findById(Long id);
    List<ChatSession> findByUserId(Long userId, Long petId, int offset, int limit);
    long countByUserId(Long userId, Long petId);
}

package com.fafa.application.dto.chat;

import java.time.LocalDateTime;

/** AI 会话展示 DTO。 */
public record ChatSessionDTO(Long sessionId, Long petId, String title, String status,
                             LocalDateTime lastMessageAt, LocalDateTime createdAt) {
}

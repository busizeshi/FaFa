package com.fafa.domain.model.chat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * AI 会话聚合根。
 *
 * 会话只负责自身状态和用户边界，消息持久化由应用服务协调。
 */
public final class ChatSession {

    private Long id;
    private final Long userId;
    private Long petId;
    private String title;
    private ChatSessionStatus status;
    private LocalDateTime lastMessageAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ChatSession(Long id, Long userId, Long petId, String title,
                        ChatSessionStatus status, LocalDateTime lastMessageAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.title = title;
        this.status = status;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatSession create(Long userId, Long petId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(now, "now 不能为空");
        return new ChatSession(null, userId, petId, null, ChatSessionStatus.ACTIVE,
                null, now, now);
    }

    public static ChatSession restore(Long id, Long userId, Long petId, String title,
                                      ChatSessionStatus status, LocalDateTime lastMessageAt,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ChatSession(id, userId, petId, title, status, lastMessageAt, createdAt, updatedAt);
    }

    public void ensureOwnedBy(Long currentUserId) {
        if (!Objects.equals(userId, currentUserId)) {
            throw new IllegalStateException("会话不属于当前用户");
        }
    }

    public void ensureActive() {
        if (status != ChatSessionStatus.ACTIVE) {
            throw new IllegalStateException("会话已删除");
        }
    }

    public Long resolvePetId(Long requestedPetId) {
        if (requestedPetId != null) {
            return requestedPetId;
        }
        return petId;
    }

    public void bindPet(Long petId, LocalDateTime now) {
        if (petId != null) {
            this.petId = petId;
        }
        touch(now);
    }

    public void setTitleIfBlank(String message, LocalDateTime now) {
        if ((title == null || title.isBlank()) && message != null && !message.isBlank()) {
            title = message.trim().substring(0, Math.min(message.trim().length(), 80));
            touch(now);
        }
    }

    public void touch(LocalDateTime now) {
        lastMessageAt = now;
        updatedAt = now;
    }

    public void delete(LocalDateTime now) {
        status = ChatSessionStatus.DELETED;
        touch(now);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPetId() { return petId; }
    public String getTitle() { return title; }
    public ChatSessionStatus getStatus() { return status; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

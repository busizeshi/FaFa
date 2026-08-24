package com.fafa.domain.model.content;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ContentSchedule {
    private Long id;
    private final Long userId;
    private final Long petId;
    private final String contentType;
    private final String topicType;
    private int intervalDays;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private boolean enabled;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ContentSchedule(Long id, Long userId, Long petId, String contentType, String topicType,
                            int intervalDays, LocalDateTime nextRunAt, LocalDateTime lastRunAt,
                            boolean enabled, boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.userId = userId; this.petId = petId; this.contentType = contentType;
        this.topicType = topicType; this.intervalDays = intervalDays; this.nextRunAt = nextRunAt;
        this.lastRunAt = lastRunAt; this.enabled = enabled; this.deleted = deleted;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static ContentSchedule create(Long userId, Long petId, String contentType, String topicType,
                                         int intervalDays, LocalDateTime nextRunAt, LocalDateTime now) {
        return new ContentSchedule(null, userId, petId, contentType, topicType, intervalDays,
                nextRunAt, null, true, false, now, now);
    }

    public static ContentSchedule restore(Long id, Long userId, Long petId, String contentType,
                                          String topicType, int intervalDays, LocalDateTime nextRunAt,
                                          LocalDateTime lastRunAt, boolean enabled, boolean deleted,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ContentSchedule(id, userId, petId, contentType, topicType, intervalDays, nextRunAt,
                lastRunAt, enabled, deleted, createdAt, updatedAt);
    }

    public void update(int intervalDays, LocalDateTime nextRunAt, LocalDateTime now) {
        this.intervalDays = intervalDays; this.nextRunAt = nextRunAt; this.updatedAt = now;
    }

    public void enable(LocalDateTime now) { this.enabled = true; this.updatedAt = now; }
    public void disable(LocalDateTime now) { this.enabled = false; this.updatedAt = now; }
    public void markRun(LocalDateTime runAt) {
        this.lastRunAt = runAt; this.nextRunAt = runAt.plusDays(intervalDays); this.updatedAt = runAt;
    }
    public void delete() { this.deleted = true; this.enabled = false; }
}

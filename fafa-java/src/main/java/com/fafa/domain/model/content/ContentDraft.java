package com.fafa.domain.model.content;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ContentDraft {
    private Long id;
    private final Long userId;
    private final Long petId;
    private final String contentType;
    private final String topicType;
    private final String customTopic;
    private final String periodType;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String selectedMediaJson;
    private final String factSnapshotJson;
    private String draftJson;
    private String status;
    private String contentMode;
    private String generationTrigger;
    private final String idempotencyKey;
    private int generationVersion;
    private String modelName;
    private String promptVersion;
    private String lastErrorCode;
    private String lastErrorMessage;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ContentDraft(Long id, Long userId, Long petId, String contentType, String topicType,
                         String customTopic, String periodType, LocalDate periodStart, LocalDate periodEnd,
                         String selectedMediaJson, String factSnapshotJson, String draftJson, String status,
                         String contentMode, String generationTrigger, String idempotencyKey,
                         int generationVersion, String modelName, String promptVersion,
                         String lastErrorCode, String lastErrorMessage, boolean deleted,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.userId = userId; this.petId = petId; this.contentType = contentType;
        this.topicType = topicType; this.customTopic = customTopic; this.periodType = periodType;
        this.periodStart = periodStart; this.periodEnd = periodEnd; this.selectedMediaJson = selectedMediaJson;
        this.factSnapshotJson = factSnapshotJson; this.draftJson = draftJson; this.status = status;
        this.contentMode = contentMode; this.generationTrigger = generationTrigger;
        this.idempotencyKey = idempotencyKey; this.generationVersion = generationVersion;
        this.modelName = modelName; this.promptVersion = promptVersion;
        this.lastErrorCode = lastErrorCode; this.lastErrorMessage = lastErrorMessage;
        this.deleted = deleted; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static ContentDraft create(Long userId, Long petId, String contentType, String topicType,
                                      String customTopic, String periodType, LocalDate periodStart,
                                      LocalDate periodEnd, String selectedMediaJson, String factSnapshotJson,
                                      String idempotencyKey, String trigger, LocalDateTime now) {
        return new ContentDraft(null, userId, petId, contentType, topicType, customTopic, periodType,
                periodStart, periodEnd, selectedMediaJson, factSnapshotJson, null, "generating", null,
                trigger, idempotencyKey, 1, null, null, null, null, false, now, now);
    }

    public static ContentDraft restore(Long id, Long userId, Long petId, String contentType,
                                       String topicType, String customTopic, String periodType,
                                       LocalDate periodStart, LocalDate periodEnd, String selectedMediaJson,
                                       String factSnapshotJson, String draftJson, String status,
                                       String contentMode, String generationTrigger, String idempotencyKey,
                                       int generationVersion, String modelName, String promptVersion,
                                       String lastErrorCode, String lastErrorMessage, boolean deleted,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ContentDraft(id, userId, petId, contentType, topicType, customTopic, periodType,
                periodStart, periodEnd, selectedMediaJson, factSnapshotJson, draftJson, status,
                contentMode, generationTrigger, idempotencyKey, generationVersion, modelName,
                promptVersion, lastErrorCode, lastErrorMessage, deleted, createdAt, updatedAt);
    }

    public void markCompleted(String draftJson, String contentMode, String modelName,
                              String promptVersion, LocalDateTime now) {
        this.draftJson = draftJson; this.status = "completed"; this.contentMode = contentMode;
        this.modelName = modelName; this.promptVersion = promptVersion;
        this.lastErrorCode = null; this.lastErrorMessage = null; this.updatedAt = now;
    }

    public void markDegraded(String draftJson, String errorCode, String errorMessage, LocalDateTime now) {
        this.draftJson = draftJson; this.status = "degraded"; this.contentMode = "deterministic";
        this.lastErrorCode = errorCode; this.lastErrorMessage = errorMessage; this.updatedAt = now;
    }

    public void markGenerating(String trigger, LocalDateTime now) {
        this.status = "generating"; this.generationTrigger = trigger; this.generationVersion++;
        this.lastErrorCode = null; this.lastErrorMessage = null; this.updatedAt = now;
    }

    public void updateDraft(String draftJson, LocalDateTime now) {
        this.draftJson = draftJson; this.updatedAt = now;
    }

    public void delete() { this.deleted = true; this.status = "deleted"; }
}

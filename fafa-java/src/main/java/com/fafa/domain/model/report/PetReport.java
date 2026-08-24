package com.fafa.domain.model.report;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PetReport {
    private Long id;
    private final Long userId;
    private final Long petId;
    private final ReportPeriodType periodType;
    private final java.time.LocalDate periodStart;
    private final java.time.LocalDate periodEnd;
    private String dataStatus;
    private String status;
    private String snapshotVersion;
    private String calculationVersion;
    private String factSnapshotJson;
    private String contentJson;
    private String contentMode;
    private String modelName;
    private String promptVersion;
    private String generationTrigger;
    private int generationVersion;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime generatedAt;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PetReport(Long id, Long userId, Long petId, ReportPeriodType periodType,
                      java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
                      String dataStatus, String status, String snapshotVersion,
                      String calculationVersion, String factSnapshotJson, String contentJson,
                      String contentMode, String modelName, String promptVersion,
                      String generationTrigger, int generationVersion, String lastErrorCode,
                      String lastErrorMessage, LocalDateTime generatedAt, boolean deleted,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dataStatus = dataStatus;
        this.status = status;
        this.snapshotVersion = snapshotVersion;
        this.calculationVersion = calculationVersion;
        this.factSnapshotJson = factSnapshotJson;
        this.contentJson = contentJson;
        this.contentMode = contentMode;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.generationTrigger = generationTrigger;
        this.generationVersion = generationVersion;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.generatedAt = generatedAt;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PetReport create(Long userId, Long petId, ReportPeriod period,
                                   String dataStatus, String snapshotVersion,
                                   String calculationVersion, String factSnapshotJson,
                                   String trigger, LocalDateTime now) {
        return new PetReport(null, userId, petId, period.type(), period.startDate(), period.endDate(),
                dataStatus, "facts_ready", snapshotVersion, calculationVersion, factSnapshotJson,
                null, null, null, null, trigger, 1, null, null, null, false, now, now);
    }

    public static PetReport restore(Long id, Long userId, Long petId, ReportPeriodType periodType,
                                    java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
                                    String dataStatus, String status, String snapshotVersion,
                                    String calculationVersion, String factSnapshotJson,
                                    String contentJson, String contentMode, String modelName,
                                    String promptVersion, String generationTrigger,
                                    int generationVersion, String lastErrorCode,
                                    String lastErrorMessage, LocalDateTime generatedAt,
                                    boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PetReport(id, userId, petId, periodType, periodStart, periodEnd, dataStatus, status,
                snapshotVersion, calculationVersion, factSnapshotJson, contentJson, contentMode,
                modelName, promptVersion, generationTrigger, generationVersion, lastErrorCode,
                lastErrorMessage, generatedAt, deleted, createdAt, updatedAt);
    }

    public void markGenerating(String trigger, LocalDateTime now) {
        boolean firstGeneration = "facts_ready".equals(this.status);
        this.status = "generating";
        this.generationTrigger = trigger;
        if (!firstGeneration) {
            this.generationVersion++;
        }
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.updatedAt = now;
    }

    public void markCompleted(String contentJson, String contentMode, String modelName,
                              String promptVersion, LocalDateTime now) {
        this.status = "completed";
        this.contentJson = contentJson;
        this.contentMode = contentMode;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.generatedAt = now;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.updatedAt = now;
    }

    public void markDegraded(String contentJson, String errorCode, String errorMessage, LocalDateTime now) {
        this.status = "degraded";
        this.contentJson = contentJson;
        this.contentMode = "deterministic";
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.generatedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String errorCode, String errorMessage, LocalDateTime now) {
        this.status = "failed";
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.updatedAt = now;
    }

    public void delete() {
        this.deleted = true;
        this.status = "deleted";
    }
}

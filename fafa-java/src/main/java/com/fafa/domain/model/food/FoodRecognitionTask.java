package com.fafa.domain.model.food;

import lombok.Getter;

import java.time.LocalDateTime;

/** 食品包装识别任务聚合。 */
@Getter
public class FoodRecognitionTask {

    private Long id;
    private final Long userId;
    private final Long petId;
    private final Long mediaId;
    private final String idempotencyKey;
    private FoodRecognitionStatus status;
    private String modelName;
    private String promptVersion;
    private String draftJson;
    private String rawResponseJson;
    private String errorCode;
    private String errorMessage;
    private Long foodCardId;
    private String quotaMonth;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FoodRecognitionTask(Long id, Long userId, Long petId, Long mediaId, String idempotencyKey,
                                FoodRecognitionStatus status, String modelName, String promptVersion,
                                String draftJson, String rawResponseJson, String errorCode,
                                String errorMessage, Long foodCardId, String quotaMonth,
                                LocalDateTime startedAt, LocalDateTime completedAt, LocalDateTime expiredAt,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.mediaId = mediaId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.draftJson = draftJson;
        this.rawResponseJson = rawResponseJson;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.foodCardId = foodCardId;
        this.quotaMonth = quotaMonth;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FoodRecognitionTask create(Long userId, Long petId, Long mediaId,
                                             String idempotencyKey, String quotaMonth) {
        if (userId == null || petId == null || mediaId == null || idempotencyKey == null
                || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("识别任务参数不能为空");
        }
        return new FoodRecognitionTask(null, userId, petId, mediaId, idempotencyKey.trim(),
                FoodRecognitionStatus.PENDING, null, null, null, null, null, null, null,
                quotaMonth, null, null, null, null, null);
    }

    public static FoodRecognitionTask restore(Long id, Long userId, Long petId, Long mediaId,
                                              String idempotencyKey, FoodRecognitionStatus status,
                                              String modelName, String promptVersion, String draftJson,
                                              String rawResponseJson, String errorCode, String errorMessage,
                                              Long foodCardId, String quotaMonth, LocalDateTime startedAt,
                                              LocalDateTime completedAt, LocalDateTime expiredAt,
                                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new FoodRecognitionTask(id, userId, petId, mediaId, idempotencyKey, status, modelName,
                promptVersion, draftJson, rawResponseJson, errorCode, errorMessage, foodCardId,
                quotaMonth, startedAt, completedAt, expiredAt, createdAt, updatedAt);
    }

    public void markProcessing(String promptVersion, LocalDateTime now) {
        requireStatus(FoodRecognitionStatus.PENDING);
        this.status = FoodRecognitionStatus.PROCESSING;
        this.promptVersion = promptVersion;
        this.startedAt = now;
    }

    public void complete(String modelName, String promptVersion, String draftJson,
                         String rawResponseJson, LocalDateTime now) {
        requireStatus(FoodRecognitionStatus.PROCESSING);
        if (draftJson == null || draftJson.isBlank()) {
            throw new IllegalArgumentException("识别草稿不能为空");
        }
        this.status = FoodRecognitionStatus.COMPLETED;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.draftJson = draftJson;
        this.rawResponseJson = rawResponseJson;
        this.completedAt = now;
    }

    public void fail(String errorCode, String errorMessage, LocalDateTime now) {
        if (status == FoodRecognitionStatus.CONFIRMED || status == FoodRecognitionStatus.REJECTED) {
            throw new IllegalStateException("已结束的识别任务不可失败");
        }
        this.status = FoodRecognitionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = now;
    }

    public void confirm(Long foodCardId) {
        requireStatus(FoodRecognitionStatus.COMPLETED);
        if (foodCardId == null) {
            throw new IllegalArgumentException("食品卡ID不能为空");
        }
        this.status = FoodRecognitionStatus.CONFIRMED;
        this.foodCardId = foodCardId;
    }

    public void reject() {
        requireStatus(FoodRecognitionStatus.COMPLETED);
        this.status = FoodRecognitionStatus.REJECTED;
    }

    public void reject(String reason, LocalDateTime now) {
        requireStatus(FoodRecognitionStatus.COMPLETED);
        this.status = FoodRecognitionStatus.REJECTED;
        this.errorMessage = reason == null || reason.isBlank() ? null : reason.trim();
        this.completedAt = now;
    }

    public void expire(LocalDateTime now) {
        requireStatus(FoodRecognitionStatus.COMPLETED);
        this.status = FoodRecognitionStatus.EXPIRED;
        this.expiredAt = now;
    }

    private void requireStatus(FoodRecognitionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("识别任务状态不支持当前操作: " + status.getCode());
        }
    }
}

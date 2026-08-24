package com.fafa.domain.model.reminder;

import java.time.LocalDateTime;

/** 单提醒、单接收人、单业务窗口的投递幂等事实。 */
public final class ReminderNotificationDelivery {
    private Long id;
    private final Long reminderId;
    private final Long petId;
    private final Long recipientUserId;
    private final String deliveryWindow;
    private DeliveryStatus status;
    private String providerMessageId;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime pushedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ReminderNotificationDelivery(Long id, Long reminderId, Long petId, Long recipientUserId,
                                         String deliveryWindow, DeliveryStatus status, String providerMessageId,
                                         String errorMessage, int retryCount, LocalDateTime pushedAt, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        this.id = id; this.reminderId = reminderId; this.petId = petId; this.recipientUserId = recipientUserId;
        this.deliveryWindow = deliveryWindow; this.status = status; this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage; this.retryCount = retryCount; this.pushedAt = pushedAt;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static ReminderNotificationDelivery pending(Long reminderId, Long petId, Long recipientUserId,
                                                       String deliveryWindow, LocalDateTime now) {
        if (reminderId == null || petId == null || recipientUserId == null || deliveryWindow == null || deliveryWindow.isBlank()) {
            throw new IllegalArgumentException("提醒投递幂等键不能为空");
        }
        return new ReminderNotificationDelivery(null, reminderId, petId, recipientUserId, deliveryWindow,
                DeliveryStatus.PENDING, null, null, 0, null, now, now);
    }

    public static ReminderNotificationDelivery restore(Long id, Long reminderId, Long petId, Long recipientUserId,
                                                        String deliveryWindow, DeliveryStatus status,
                                                        String providerMessageId, String errorMessage,
                                                        int retryCount, LocalDateTime pushedAt, LocalDateTime createdAt,
                                                        LocalDateTime updatedAt) {
        return new ReminderNotificationDelivery(id, reminderId, petId, recipientUserId, deliveryWindow, status,
                providerMessageId, errorMessage, retryCount, pushedAt, createdAt, updatedAt);
    }

    public void markSent(String providerMessageId, LocalDateTime now) {
        status = DeliveryStatus.SENT; this.providerMessageId = providerMessageId; this.pushedAt = now; this.updatedAt = now;
    }

    public void markFailed(String errorMessage, LocalDateTime now) {
        status = DeliveryStatus.FAILED; this.errorMessage = errorMessage; this.retryCount++; this.updatedAt = now;
    }

    public boolean canRetry(int maxRetries) {
        return status == DeliveryStatus.FAILED && retryCount < maxRetries;
    }

    public void prepareRetry(LocalDateTime now) {
        if (status != DeliveryStatus.FAILED) throw new IllegalStateException("只有失败投递可以重试");
        status = DeliveryStatus.PENDING;
        errorMessage = null;
        updatedAt = now;
    }

    public void assignId(Long id) { this.id = id; }

    public Long getId() { return id; }
    public Long getReminderId() { return reminderId; }
    public Long getPetId() { return petId; }
    public Long getRecipientUserId() { return recipientUserId; }
    public String getDeliveryWindow() { return deliveryWindow; }
    public DeliveryStatus getStatus() { return status; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getPushedAt() { return pushedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

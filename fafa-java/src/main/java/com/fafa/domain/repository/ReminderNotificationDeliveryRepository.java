package com.fafa.domain.repository;

import com.fafa.domain.model.reminder.ReminderNotificationDelivery;

import java.util.Optional;

public interface ReminderNotificationDeliveryRepository {
    boolean createPendingIfAbsent(ReminderNotificationDelivery delivery);
    Optional<ReminderNotificationDelivery> findByBusinessKey(Long reminderId, Long recipientUserId, String deliveryWindow);
    ReminderNotificationDelivery save(ReminderNotificationDelivery delivery);
}

package com.fafa.domain.model.reminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderNotificationDeliveryTest {

    @Test
    void failedDelivery_shouldBeRetryableOnlyWithinConfiguredLimit() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 10, 0);
        ReminderNotificationDelivery delivery = ReminderNotificationDelivery.pending(1L, 2L, 3L,
                "2026-08-23:due", now);

        delivery.markFailed("provider unavailable", now);
        assertThat(delivery.getRetryCount()).isEqualTo(1);
        assertThat(delivery.canRetry(3)).isTrue();
        delivery.prepareRetry(now.plusMinutes(5));
        delivery.markFailed("provider unavailable", now.plusMinutes(5));
        delivery.markFailed("provider unavailable", now.plusMinutes(10));

        assertThat(delivery.canRetry(3)).isFalse();
    }
}

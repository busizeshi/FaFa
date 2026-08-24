package com.fafa.domain.model.emergency;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetEmergencyProfileTest {

    @Test
    void update_shouldTrimValuesAndIncrementVersion() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 10, 0);
        PetEmergencyProfile profile = PetEmergencyProfile.create(1L, 2L, now);

        profile.update("  A  ", "花粉", null, "家人", "13800138000", "医院", null,
                null, "备注", 0, 2L, now.plusMinutes(1));

        assertThat(profile.getBloodType()).isEqualTo("A");
        assertThat(profile.getVersion()).isEqualTo(1);
    }

    @Test
    void update_shouldRejectStaleVersionAndInvalidPhone() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 10, 0);
        PetEmergencyProfile profile = PetEmergencyProfile.create(1L, 2L, now);

        assertThatThrownBy(() -> profile.update(null, null, null, null, "bad_phone!", null, null,
                null, null, 0, 2L, now.plusMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.update(null, null, null, null, null, null, null,
                null, null, 1, 2L, now.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}

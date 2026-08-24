package com.fafa.domain.model.collaboration;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetInvitationTest {

    @Test
    void invitation_shouldBeOneTimeAndTransitionToAccepted() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PetInvitation invitation = PetInvitation.create(1L, 3L, InvitationRole.MEMBER,
                "hash", expiresAt, null);

        invitation.accept(2L, LocalDateTime.now());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.isAcceptableAt(LocalDateTime.now())).isFalse();
        assertThatThrownBy(() -> invitation.accept(4L, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void caregiverInvitation_shouldRequireMemberExpiry() {
        assertThatThrownBy(() -> PetInvitation.create(1L, 3L, InvitationRole.CAREGIVER,
                "hash", LocalDateTime.now().plusMinutes(30), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("临时照护");
    }
}

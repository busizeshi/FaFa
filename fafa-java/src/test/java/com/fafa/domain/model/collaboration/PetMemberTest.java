package com.fafa.domain.model.collaboration;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetMemberTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 23, 10, 0);

    @Test
    void caregiver_shouldExpireAtBoundary() {
        PetMember member = PetMember.create(1L, 2L, PetMemberRole.CAREGIVER, 3L,
                NOW, NOW.plusDays(1));

        assertThat(member.isActiveAt(NOW.plusHours(23))).isTrue();
        assertThat(member.isActiveAt(NOW.plusDays(1))).isFalse();
        member.expire(NOW.plusDays(1));
        assertThat(member.getStatus()).isEqualTo(PetMemberStatus.EXPIRED);
    }

    @Test
    void member_shouldNotAcceptExpiryOrSelfGrant() {
        assertThatThrownBy(() -> PetMember.create(1L, 2L, PetMemberRole.MEMBER, 3L,
                NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PetMember.create(1L, 2L, PetMemberRole.MEMBER, 2L,
                NOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主人");
    }
}

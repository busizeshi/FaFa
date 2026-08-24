package com.fafa.application.dto.collaboration;

import java.time.LocalDateTime;

public record PetMemberDTO(Long memberId, Long userId, String nickname, String role,
                           String status, LocalDateTime joinedAt, LocalDateTime expiresAt) {
}

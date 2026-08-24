package com.fafa.application.dto.collaboration;

import java.time.LocalDateTime;

public record InvitationDTO(Long invitationId, Long petId, String role, String token,
                            LocalDateTime expiresAt, LocalDateTime memberExpiresAt,
                            String sharePath) {
}

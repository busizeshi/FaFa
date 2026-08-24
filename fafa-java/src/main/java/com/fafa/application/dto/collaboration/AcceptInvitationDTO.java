package com.fafa.application.dto.collaboration;

import java.time.LocalDateTime;

public record AcceptInvitationDTO(Long petId, String role, String status, LocalDateTime expiresAt) {
}

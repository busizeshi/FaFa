package com.fafa.application.dto.collaboration;

import java.time.LocalDateTime;
import java.util.Set;

public record PetAccessDTO(Long petId, String role, String status, LocalDateTime expiresAt,
                           Set<String> permissions, LocalDateTime serverTime) {
}

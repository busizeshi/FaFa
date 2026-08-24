package com.fafa.domain.model.collaboration;

import java.time.LocalDateTime;
import java.util.Set;

/** 一次服务端授权计算结果，不携带敏感凭证。 */
public record PetAccessDecision(Long petId, Long operatorId, String role, String status,
                                LocalDateTime expiresAt, boolean allowed,
                                String reason, Set<PetPermission> permissions) {
    public boolean has(PetPermission permission) {
        return allowed && permissions != null && permissions.contains(permission);
    }
}

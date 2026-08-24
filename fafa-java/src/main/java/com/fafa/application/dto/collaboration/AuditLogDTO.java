package com.fafa.application.dto.collaboration;

import java.time.LocalDateTime;

public record AuditLogDTO(Long userId, Long operatorId, String operation, String resourceType,
                          String resourceId, String result, String reason, LocalDateTime createdAt) {
}

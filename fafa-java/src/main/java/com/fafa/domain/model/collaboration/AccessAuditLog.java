package com.fafa.domain.model.collaboration;

import java.time.LocalDateTime;

/** 访问审计事实，只允许追加写入。 */
public record AccessAuditLog(String requestId, Long petId, Long userId, Long operatorId,
                             String operation, String resourceType, String resourceId,
                             String result, String reason, String metadataJson,
                             LocalDateTime createdAt) {
    public AccessAuditLog {
        if (operation == null || operation.isBlank()) throw new IllegalArgumentException("审计操作不能为空");
        if (resourceType == null || resourceType.isBlank()) throw new IllegalArgumentException("审计资源类型不能为空");
        if (result == null || result.isBlank()) throw new IllegalArgumentException("审计结果不能为空");
    }
}

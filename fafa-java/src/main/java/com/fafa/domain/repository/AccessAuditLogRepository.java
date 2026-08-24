package com.fafa.domain.repository;

import com.fafa.domain.model.collaboration.AccessAuditLog;

import java.util.List;

public interface AccessAuditLogRepository {
    void append(AccessAuditLog auditLog);
    List<AccessAuditLog> findByPetId(Long petId, int offset, int limit);
}

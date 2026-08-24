package com.fafa.infrastructure.persistence.repository;

import com.fafa.domain.model.collaboration.AccessAuditLog;
import com.fafa.domain.repository.AccessAuditLogRepository;
import com.fafa.infrastructure.persistence.mapper.AccessAuditLogMapper;
import com.fafa.infrastructure.persistence.po.AccessAuditLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccessAuditLogRepositoryImpl implements AccessAuditLogRepository {
    private final AccessAuditLogMapper mapper;

    @Override
    public void append(AccessAuditLog auditLog) {
        AccessAuditLogPO po = new AccessAuditLogPO();
        po.setRequestId(auditLog.requestId()); po.setPetId(auditLog.petId()); po.setUserId(auditLog.userId());
        po.setOperatorId(auditLog.operatorId()); po.setOperation(auditLog.operation()); po.setResourceType(auditLog.resourceType());
        po.setResourceId(auditLog.resourceId()); po.setResult(auditLog.result()); po.setReason(auditLog.reason());
        po.setMetadataJson(auditLog.metadataJson()); po.setCreatedAt(auditLog.createdAt() == null ? LocalDateTime.now() : auditLog.createdAt());
        mapper.insert(po);
    }

    @Override
    public List<AccessAuditLog> findByPetId(Long petId, int offset, int limit) {
        return mapper.selectByPetId(petId, offset, limit).stream().map(this::toDomain).toList();
    }

    private AccessAuditLog toDomain(AccessAuditLogPO po) {
        return new AccessAuditLog(po.getRequestId(), po.getPetId(), po.getUserId(), po.getOperatorId(), po.getOperation(),
                po.getResourceType(), po.getResourceId(), po.getResult(), po.getReason(), po.getMetadataJson(), po.getCreatedAt());
    }
}

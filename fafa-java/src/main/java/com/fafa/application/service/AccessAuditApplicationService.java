package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.collaboration.AuditLogDTO;
import com.fafa.domain.repository.AccessAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 主人专属审计查询，服务端不返回凭证、请求头或完整敏感文本。 */
@Service
@RequiredArgsConstructor
public class AccessAuditApplicationService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PetAccessApplicationService accessService;
    private final AccessAuditLogRepository auditLogRepository;

    public List<AuditLogDTO> list(Long petId, int page, int size) {
        accessService.requireOwner(StpUtil.getLoginIdAsLong(), petId);
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        return auditLogRepository.findByPetId(petId, offset, safeSize).stream()
                .map(audit -> new AuditLogDTO(audit.userId(), audit.operatorId(), audit.operation(), audit.resourceType(),
                        audit.resourceId(), audit.result(), audit.reason(), audit.createdAt()))
                .toList();
    }
}

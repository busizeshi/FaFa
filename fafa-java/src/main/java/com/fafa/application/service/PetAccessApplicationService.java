package com.fafa.application.service;

import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.collaboration.AccessAuditLog;
import com.fafa.domain.model.collaboration.PetAccessDecision;
import com.fafa.domain.model.collaboration.PetMember;
import com.fafa.domain.model.collaboration.PetMemberRole;
import com.fafa.domain.model.collaboration.PetMemberStatus;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.AccessAuditLogRepository;
import com.fafa.domain.repository.PetMemberRepository;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 宠物空间统一授权入口，所有协同接口和后续跨模块改造复用此服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetAccessApplicationService {
    private final PetRepository petRepository;
    private final PetMemberRepository memberRepository;
    private final AccessAuditLogRepository auditLogRepository;
    private final FaFaProperties properties;

    public PetAccessDecision currentAccess(Long operatorId, Long petId) {
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

//        主人拥有一切权力
        if (pet.getUserId().equals(operatorId))
            return ownerDecision(petId, operatorId);
        if (!properties.getCollaboration().isEnabled()) {
            return denied(petId, operatorId, "none", "unavailable", "协同功能未开放");
        }
        PetMember member = memberRepository.findByPetIdAndUserId(petId, operatorId).orElse(null);
        if (member == null)
            return denied(petId, operatorId, "none", "missing", "成员不存在");
        LocalDateTime now = LocalDateTime.now();
        if (!member.isActiveAt(now)) {
            boolean expired = member.getRole() == PetMemberRole.CAREGIVER
                    && member.getStatus() == PetMemberStatus.ACTIVE;
            if (expired)
                expireMember(member, now);
            String status = expired
                    ? PetMemberStatus.EXPIRED.getCode() : member.getStatus().getCode();
            String reason = expired ? "临时照护权限已过期" : "成员关系不可用";
            return denied(petId, operatorId, member.getRole().getCode(), status, reason);
        }
        return allowed(petId, operatorId, member.getRole().getCode(), member.getStatus().getCode(),
                member.getExpiresAt(), permissionsOf(member.getRole()));
    }

    public void require(Long operatorId, Long petId, PetPermission permission) {

//        非主人且非协同
        if (!properties.getCollaboration().isEnabled() && !isOwner(operatorId, petId)) {
            throw new BusinessException(ErrorCode.COLLABORATION_FEATURE_UNAVAILABLE);
        }
        PetAccessDecision decision = currentAccess(operatorId, petId);
        if (decision.has(permission))
            return;
        if ("expired".equals(decision.status()))
            throw new BusinessException(ErrorCode.COLLABORATION_CAREGIVER_EXPIRED);
        throw new BusinessException(ErrorCode.COLLABORATION_PERMISSION_DENIED);
    }

    public void requireOwner(Long operatorId, Long petId) {
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!pet.getUserId().equals(operatorId))
            throw new BusinessException(ErrorCode.COLLABORATION_MEMBER_MANAGEMENT_FORBIDDEN);
    }

    public void requireCollaborationEnabled() {
        if (!properties.getCollaboration().isEnabled())
            throw new BusinessException(ErrorCode.COLLABORATION_FEATURE_UNAVAILABLE);
    }

    private boolean isOwner(Long userId, Long petId) {
        return petRepository.findById(petId).map(pet -> pet.getUserId().equals(userId)).orElse(false);
    }

    private void expireMember(PetMember member, LocalDateTime now) {
        member.expire(now);
        memberRepository.save(member);
    }

    private PetAccessDecision ownerDecision(Long petId, Long userId) {
        return allowed(petId, userId, "owner", "active", null, EnumSet.allOf(PetPermission.class));
    }

    private PetAccessDecision allowed(Long petId, Long userId, String role, String status,
                                      LocalDateTime expiresAt, Set<PetPermission> permissions) {
        return new PetAccessDecision(petId, userId, role, status, expiresAt, true, null, Set.copyOf(permissions));
    }

    private PetAccessDecision denied(Long petId, Long userId, String role, String status, String reason) {
        PetAccessDecision decision = new PetAccessDecision(petId, userId, role, status, null, false, reason, Set.of());
        writeAudit(new AccessAuditLog(MDC.get("traceId"), petId, userId, userId, "read", "pet_access", null,
                "denied", reason, null, LocalDateTime.now()));
        return decision;
    }

    private Set<PetPermission> permissionsOf(PetMemberRole role) {
        if (role == PetMemberRole.MEMBER) {
            return Set.of(PetPermission.READ_PROFILE, PetPermission.READ_RECORD, PetPermission.CREATE_RECORD,
                    PetPermission.EDIT_OWN_RECORD, PetPermission.COMPLETE_REMINDER, PetPermission.READ_MEDICAL,
                    PetPermission.READ_EXPENSE, PetPermission.READ_FOOD, PetPermission.READ_REPORT,
                    PetPermission.READ_EMERGENCY);
        }
        return Set.of(PetPermission.READ_PROFILE, PetPermission.READ_RECORD, PetPermission.CREATE_RECORD,
                PetPermission.EDIT_OWN_RECORD, PetPermission.COMPLETE_REMINDER, PetPermission.READ_EMERGENCY);
    }

    private void writeAudit(AccessAuditLog logEntry) {
        try {
            auditLogRepository.append(logEntry);
        } catch (RuntimeException ex) {
            log.warn("写入协同审计失败: petId={}, operatorId={}", logEntry.petId(), logEntry.operatorId(), ex);
        }
    }
}

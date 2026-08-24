package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.collaboration.AcceptInvitationDTO;
import com.fafa.application.dto.collaboration.InvitationDTO;
import com.fafa.application.port.InvitationTokenGenerator;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.collaboration.AccessAuditLog;
import com.fafa.domain.model.collaboration.InvitationRole;
import com.fafa.domain.model.collaboration.InvitationStatus;
import com.fafa.domain.model.collaboration.PetInvitation;
import com.fafa.domain.model.collaboration.PetMember;
import com.fafa.domain.model.collaboration.PetMemberRole;
import com.fafa.domain.repository.AccessAuditLogRepository;
import com.fafa.domain.repository.PetInvitationRepository;
import com.fafa.domain.repository.PetMemberRepository;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import com.fafa.infrastructure.security.CollaborationRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 邀请凭证创建、接受和撤销的事务边界。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetInvitationApplicationService {
    private final PetAccessApplicationService accessService;
    private final PetRepository petRepository;
    private final PetInvitationRepository invitationRepository;
    private final PetMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AccessAuditLogRepository auditLogRepository;
    private final InvitationTokenGenerator tokenGenerator;
    private final CollaborationRateLimiter rateLimiter;
    private final FaFaProperties properties;

    @Transactional
    public InvitationDTO create(Long petId, String roleCode, Integer expiresInDays) {
        Long ownerId = currentUserId();
        accessService.requireCollaborationEnabled();
        accessService.requireOwner(ownerId, petId);
        if (!rateLimiter.tryAcquire("invite", ownerId, properties.getCollaboration().getInviteRateLimitPerHour())) {
            throw new BusinessException(ErrorCode.COLLABORATION_INVITATION_RATE_LIMITED);
        }

        InvitationRole role = parseRole(roleCode);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime memberExpiresAt = memberExpiry(role, expiresInDays, now);
        int maxMembers = maxMembers(ownerId);
        if (memberRepository.countActiveByPetId(petId, now) >= maxMembers) {
            throw new BusinessException(ErrorCode.COLLABORATION_MEMBER_LIMIT_EXCEEDED);
        }

        String rawToken = tokenGenerator.generate();
        PetInvitation invitation = PetInvitation.create(petId, ownerId, role,
                tokenGenerator.sha256(rawToken), now.plusMinutes(properties.getCollaboration().getInvitationExpireMinutes()),
                memberExpiresAt);
        invitation = invitationRepository.save(invitation);
        appendAudit(petId, ownerId, ownerId, "invite", "pet_invitation", invitation.getId().toString(), "success", null);
        log.info("创建宠物协同邀请: petId={}, ownerId={}, invitationId={}, role={}", petId, ownerId, invitation.getId(), role.getCode());
        return new InvitationDTO(invitation.getId(), petId, role.getCode(), rawToken, invitation.getExpiresAt(),
                invitation.getMemberExpiresAt(), "/pages/invitation/accept?invitationId=" + invitation.getId() + "&token=" + rawToken);
    }

    @Transactional
    public AcceptInvitationDTO accept(Long invitationId, String rawToken) {
        Long userId = currentUserId();
        if (!rateLimiter.tryAcquire("accept", userId, properties.getCollaboration().getAcceptRateLimitPerHour())) {
            throw new BusinessException(ErrorCode.COLLABORATION_INVITATION_RATE_LIMITED);
        }
        accessService.requireCollaborationEnabled();
        PetInvitation invitation = invitationRepository.findForUpdate(invitationId, tokenGenerator.sha256(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.COLLABORATION_INVITATION_TOKEN_INVALID));
        LocalDateTime now = LocalDateTime.now();
        if (!invitation.isAcceptableAt(now)) throw invitationStateError(invitation.getStatus());

        Long ownerId = petRepository.findById(invitation.getPetId()).map(p -> p.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COLLABORATION_INVITATION_INVALID));
        if (ownerId.equals(userId)) throw new BusinessException(ErrorCode.COLLABORATION_INVITATION_INVALID);

        PetMember member = memberRepository.findForUpdateByPetIdAndUserId(invitation.getPetId(), userId).orElse(null);
        if (member != null && member.isActiveAt(now)) throw new BusinessException(ErrorCode.COLLABORATION_INVITATION_USED);
        if (member == null) {
            member = PetMember.create(invitation.getPetId(), userId, invitation.getRole().toMemberRole(), ownerId,
                    now, invitation.getMemberExpiresAt());
        } else {
            member.activate(ownerId, now, invitation.getMemberExpiresAt());
        }
        memberRepository.save(member);
        invitation.accept(userId, now);
        invitationRepository.save(invitation);
        appendAudit(invitation.getPetId(), userId, userId, "accept", "pet_invitation", invitationId.toString(), "success", null);
        return new AcceptInvitationDTO(invitation.getPetId(), invitation.getRole().getCode(), member.getStatus().getCode(), member.getExpiresAt());
    }

    @Transactional
    public void revokeInvitation(Long petId, Long invitationId) {
        Long ownerId = currentUserId();
        accessService.requireCollaborationEnabled();
        accessService.requireOwner(ownerId, petId);
        PetInvitation invitation = invitationRepository.findById(invitationId)
                .filter(value -> value.getPetId().equals(petId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COLLABORATION_INVITATION_INVALID));
        invitation.revoke(LocalDateTime.now());
        invitationRepository.save(invitation);
        appendAudit(petId, ownerId, ownerId, "revoke", "pet_invitation", invitationId.toString(), "success", null);
    }

    private InvitationRole parseRole(String code) {
        try { return InvitationRole.fromCode(code); }
        catch (IllegalArgumentException ex) { throw new BusinessException(ErrorCode.PARAM_INVALID, ex.getMessage()); }
    }

    private LocalDateTime memberExpiry(InvitationRole role, Integer days, LocalDateTime now) {
        if (role == InvitationRole.MEMBER) return null;
        int value = days == null ? 0 : days;
        if (value <= 0 || value > properties.getCollaboration().getCaregiverMaxDays()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "临时照护有效天数超出配置范围");
        }
        return now.plusDays(value);
    }

    private int maxMembers(Long ownerId) {
        boolean vip = userRepository.findById(ownerId).map(user -> user.isVipValid()).orElse(false);
        return vip ? properties.getCollaboration().getVipMaxMembers() : properties.getCollaboration().getNormalMaxMembers();
    }

    private BusinessException invitationStateError(InvitationStatus status) {
        return status == InvitationStatus.ACCEPTED ? new BusinessException(ErrorCode.COLLABORATION_INVITATION_USED)
                : new BusinessException(ErrorCode.COLLABORATION_INVITATION_INVALID);
    }

    private void appendAudit(Long petId, Long userId, Long operatorId, String operation,
                             String resourceType, String resourceId, String result, String reason) {
        auditLogRepository.append(new AccessAuditLog(MDC.get("traceId"), petId, userId, operatorId,
                operation, resourceType, resourceId, result, reason, null, LocalDateTime.now()));
    }

    private Long currentUserId() { return StpUtil.getLoginIdAsLong(); }
}

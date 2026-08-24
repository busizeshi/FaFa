package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.collaboration.PetAccessDTO;
import com.fafa.application.dto.collaboration.PetMemberDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.collaboration.AccessAuditLog;
import com.fafa.domain.model.collaboration.PetAccessDecision;
import com.fafa.domain.model.collaboration.PetMember;
import com.fafa.domain.repository.AccessAuditLogRepository;
import com.fafa.domain.repository.PetMemberRepository;
import com.fafa.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 成员查询、权限展示和主人撤销成员的应用服务。 */
@Service
@RequiredArgsConstructor
public class PetMemberApplicationService {
    private final PetAccessApplicationService accessService;
    private final PetMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AccessAuditLogRepository auditLogRepository;

    public PetAccessDTO access(Long petId) {
        Long userId = currentUserId();
        PetAccessDecision decision = accessService.currentAccess(userId, petId);
        return new PetAccessDTO(decision.petId(), decision.role(), decision.status(), decision.expiresAt(),
                decision.permissions().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.toUnmodifiableSet()), LocalDateTime.now());
    }

    public List<PetMemberDTO> list(Long petId) {
        Long ownerId = currentUserId();
        accessService.requireOwner(ownerId, petId);
        memberRepository.expireDue(LocalDateTime.now());
        return memberRepository.findByPetId(petId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public void revoke(Long petId, Long memberId) {
        Long ownerId = currentUserId();
        accessService.requireCollaborationEnabled();
        accessService.requireOwner(ownerId, petId);
        PetMember member = memberRepository.findById(memberId)
                .filter(value -> value.getPetId().equals(petId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COLLABORATION_MEMBER_NOT_FOUND));
        try {
            member.revoke(LocalDateTime.now());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.COLLABORATION_MEMBER_NOT_FOUND, ex.getMessage());
        }
        memberRepository.save(member);
        auditLogRepository.append(new AccessAuditLog(MDC.get("traceId"), petId, ownerId, ownerId,
                "revoke", "pet_member", memberId.toString(), "success", null, null, LocalDateTime.now()));
    }

    private PetMemberDTO toDTO(PetMember member) {
        String nickname = userRepository.findById(member.getUserId()).map(user -> user.getDisplayName()).orElse(null);
        return new PetMemberDTO(member.getId(), member.getUserId(), nickname, member.getRole().getCode(),
                member.getStatus().getCode(), member.getJoinedAt(), member.getExpiresAt());
    }

    private Long currentUserId() { return StpUtil.getLoginIdAsLong(); }
}

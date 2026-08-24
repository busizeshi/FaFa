package com.fafa.domain.model.collaboration;

import java.time.LocalDateTime;
import java.util.Objects;

/** 一次性邀请凭证，领域对象只保存 token 摘要，不接触明文 token。 */
public final class PetInvitation {

    private Long id;
    private final Long petId;
    private final Long createdBy;
    private final InvitationRole role;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime memberExpiresAt;
    private final int maxUses;
    private int usedCount;
    private InvitationStatus status;
    private Long acceptedBy;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PetInvitation(Long id, Long petId, Long createdBy, InvitationRole role,
                          String tokenHash, LocalDateTime expiresAt, LocalDateTime memberExpiresAt, int maxUses,
                          int usedCount, InvitationStatus status, Long acceptedBy,
                          LocalDateTime acceptedAt, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id = id; this.petId = petId; this.createdBy = createdBy; this.role = role;
        this.tokenHash = tokenHash; this.expiresAt = expiresAt; this.memberExpiresAt = memberExpiresAt; this.maxUses = maxUses;
        this.usedCount = usedCount; this.status = status; this.acceptedBy = acceptedBy;
        this.acceptedAt = acceptedAt; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static PetInvitation create(Long petId, Long createdBy, InvitationRole role,
                                       String tokenHash, LocalDateTime expiresAt,
                                       LocalDateTime memberExpiresAt) {
        if (petId == null || createdBy == null || role == null || tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("邀请参数无效");
        }
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("邀请有效期必须晚于当前时间");
        }
        if (role == InvitationRole.CAREGIVER && (memberExpiresAt == null || !memberExpiresAt.isAfter(LocalDateTime.now()))) {
            throw new IllegalArgumentException("临时照护必须设置未来到期时间");
        }
        if (role == InvitationRole.MEMBER && memberExpiresAt != null) {
            throw new IllegalArgumentException("长期成员不能设置到期时间");
        }
        return new PetInvitation(null, petId, createdBy, role, tokenHash, expiresAt,
                memberExpiresAt, 1, 0, InvitationStatus.PENDING, null, null, null, null);
    }

    public static PetInvitation restore(Long id, Long petId, Long createdBy, InvitationRole role,
                                        String tokenHash, LocalDateTime expiresAt, int maxUses,
                                        LocalDateTime memberExpiresAt,
                                        int usedCount, InvitationStatus status, Long acceptedBy,
                                        LocalDateTime acceptedAt, LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new PetInvitation(id, petId, createdBy, role, tokenHash, expiresAt, memberExpiresAt,
                maxUses, usedCount, status, acceptedBy, acceptedAt, createdAt, updatedAt);
    }

    public void accept(Long userId, LocalDateTime acceptedAt) {
        if (status != InvitationStatus.PENDING || usedCount >= maxUses) {
            throw new IllegalStateException("邀请已被使用或不可用");
        }
        if (expiresAt == null || !expiresAt.isAfter(acceptedAt)) {
            status = InvitationStatus.EXPIRED;
            throw new IllegalStateException("邀请已过期");
        }
        this.usedCount++;
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedBy = Objects.requireNonNull(userId, "接受人不能为空");
        this.acceptedAt = acceptedAt;
        this.updatedAt = acceptedAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (status == InvitationStatus.PENDING) {
            status = InvitationStatus.REVOKED;
            updatedAt = revokedAt;
        }
    }

    public void expire(LocalDateTime now) {
        if (status == InvitationStatus.PENDING && !expiresAt.isAfter(now)) {
            status = InvitationStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public boolean isAcceptableAt(LocalDateTime now) {
        return status == InvitationStatus.PENDING && usedCount < maxUses && expiresAt.isAfter(now);
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getCreatedBy() { return createdBy; }
    public InvitationRole getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getMemberExpiresAt() { return memberExpiresAt; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public InvitationStatus getStatus() { return status; }
    public Long getAcceptedBy() { return acceptedBy; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

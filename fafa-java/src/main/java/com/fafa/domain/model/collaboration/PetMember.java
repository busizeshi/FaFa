package com.fafa.domain.model.collaboration;

import java.time.LocalDateTime;
import java.util.Objects;

/** 宠物空间协作者实体，封装成员生命周期和临时照护失效规则。 */
public final class PetMember {

    private Long id;
    private final Long petId;
    private final Long userId;
    private PetMemberRole role;
    private PetMemberStatus status;
    private Long grantedBy;
    private LocalDateTime joinedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PetMember(Long id, Long petId, Long userId, PetMemberRole role,
                      PetMemberStatus status, Long grantedBy, LocalDateTime joinedAt,
                      LocalDateTime expiresAt, LocalDateTime revokedAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.petId = petId; this.userId = userId; this.role = role;
        this.status = status; this.grantedBy = grantedBy; this.joinedAt = joinedAt;
        this.expiresAt = expiresAt; this.revokedAt = revokedAt;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static PetMember create(Long petId, Long userId, PetMemberRole role,
                                   Long grantedBy, LocalDateTime joinedAt,
                                   LocalDateTime expiresAt) {
        validateIdentity(petId, userId, role, grantedBy);
        validateExpiry(role, expiresAt, joinedAt);
        return new PetMember(null, petId, userId, role, PetMemberStatus.ACTIVE,
                grantedBy, joinedAt, expiresAt, null, null, null);
    }

    public static PetMember restore(Long id, Long petId, Long userId, PetMemberRole role,
                                    PetMemberStatus status, Long grantedBy,
                                    LocalDateTime joinedAt, LocalDateTime expiresAt,
                                    LocalDateTime revokedAt, LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
        return new PetMember(id, petId, userId, role, status, grantedBy, joinedAt,
                expiresAt, revokedAt, createdAt, updatedAt);
    }

    public void activate(Long grantedBy, LocalDateTime joinedAt, LocalDateTime expiresAt) {
        validateIdentity(petId, userId, role, grantedBy);
        validateExpiry(role, expiresAt, joinedAt);
        this.grantedBy = grantedBy;
        this.joinedAt = joinedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
        this.status = PetMemberStatus.ACTIVE;
        this.updatedAt = joinedAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (status != PetMemberStatus.ACTIVE) throw new IllegalStateException("当前成员不可撤销");
        this.status = PetMemberStatus.REVOKED;
        this.revokedAt = Objects.requireNonNull(revokedAt, "撤销时间不能为空");
        this.updatedAt = revokedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        if (role == PetMemberRole.CAREGIVER && status == PetMemberStatus.ACTIVE
                && expiresAt != null && !expiresAt.isAfter(expiredAt)) {
            this.status = PetMemberStatus.EXPIRED;
            this.updatedAt = expiredAt;
        }
    }

    public boolean isActiveAt(LocalDateTime now) {
        return status == PetMemberStatus.ACTIVE
                && (role == PetMemberRole.MEMBER || (expiresAt != null && expiresAt.isAfter(now)));
    }

    private static void validateIdentity(Long petId, Long userId, PetMemberRole role, Long grantedBy) {
        if (petId == null || petId <= 0 || userId == null || userId <= 0 || grantedBy == null || grantedBy <= 0) {
            throw new IllegalArgumentException("成员身份参数无效");
        }
        Objects.requireNonNull(role, "成员角色不能为空");
        if (userId.equals(grantedBy)) throw new IllegalArgumentException("主人不能接受自己的协同邀请");
    }

    private static void validateExpiry(PetMemberRole role, LocalDateTime expiresAt, LocalDateTime joinedAt) {
        Objects.requireNonNull(joinedAt, "加入时间不能为空");
        if (role == PetMemberRole.CAREGIVER && (expiresAt == null || !expiresAt.isAfter(joinedAt))) {
            throw new IllegalArgumentException("临时照护必须设置未来到期时间");
        }
        if (role == PetMemberRole.MEMBER && expiresAt != null) {
            throw new IllegalArgumentException("长期成员不能设置到期时间");
        }
    }

    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getUserId() { return userId; }
    public PetMemberRole getRole() { return role; }
    public PetMemberStatus getStatus() { return status; }
    public Long getGrantedBy() { return grantedBy; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

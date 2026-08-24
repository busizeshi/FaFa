package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.collaboration.PetMember;
import com.fafa.domain.model.collaboration.PetMemberRole;
import com.fafa.domain.model.collaboration.PetMemberStatus;
import com.fafa.domain.repository.PetMemberRepository;
import com.fafa.infrastructure.persistence.mapper.PetMemberMapper;
import com.fafa.infrastructure.persistence.po.PetMemberPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PetMemberRepositoryImpl implements PetMemberRepository {
    private final PetMemberMapper mapper;

    @Override
    public PetMember save(PetMember member) {
        PetMemberPO po = toPO(member);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<PetMember> findById(Long memberId) {
        return Optional.ofNullable(toDomain(mapper.selectById(memberId)));
    }

    @Override
    public Optional<PetMember> findByPetIdAndUserId(Long petId, Long userId) {
        LambdaQueryWrapper<PetMemberPO> query = new LambdaQueryWrapper<>();
        query.eq(PetMemberPO::getPetId, petId).eq(PetMemberPO::getUserId, userId);
        return Optional.ofNullable(toDomain(mapper.selectOne(query)));
    }

    @Override
    public Optional<PetMember> findForUpdateByPetIdAndUserId(Long petId, Long userId) {
        return Optional.ofNullable(toDomain(mapper.selectForUpdate(petId, userId)));
    }

    @Override
    public List<PetMember> findByPetId(Long petId) {
        LambdaQueryWrapper<PetMemberPO> query = new LambdaQueryWrapper<>();
        query.eq(PetMemberPO::getPetId, petId).orderByAsc(PetMemberPO::getCreatedAt);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public int countActiveByPetId(Long petId, LocalDateTime now) {
        LambdaQueryWrapper<PetMemberPO> query = new LambdaQueryWrapper<>();
        query.eq(PetMemberPO::getPetId, petId).eq(PetMemberPO::getStatus, PetMemberStatus.ACTIVE.getCode())
                .and(w -> w.eq(PetMemberPO::getRole, PetMemberRole.MEMBER.getCode())
                        .or().eq(PetMemberPO::getRole, PetMemberRole.CAREGIVER.getCode())
                        .gt(PetMemberPO::getExpiresAt, now));
        return Math.toIntExact(mapper.selectCount(query));
    }

    @Override
    public void expireDue(LocalDateTime now) { mapper.expireDue(now); }

    private PetMember toDomain(PetMemberPO po) {
        if (po == null) return null;
        return PetMember.restore(po.getId(), po.getPetId(), po.getUserId(), PetMemberRole.fromCode(po.getRole()),
                PetMemberStatus.fromCode(po.getStatus()), po.getGrantedBy(), po.getJoinedAt(), po.getExpiresAt(),
                po.getRevokedAt(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private PetMemberPO toPO(PetMember member) {
        PetMemberPO po = new PetMemberPO();
        po.setId(member.getId()); po.setPetId(member.getPetId()); po.setUserId(member.getUserId());
        po.setRole(member.getRole().getCode()); po.setStatus(member.getStatus().getCode()); po.setGrantedBy(member.getGrantedBy());
        po.setJoinedAt(member.getJoinedAt()); po.setExpiresAt(member.getExpiresAt()); po.setRevokedAt(member.getRevokedAt());
        return po;
    }
}

package com.fafa.infrastructure.persistence.repository;

import com.fafa.domain.model.collaboration.InvitationRole;
import com.fafa.domain.model.collaboration.InvitationStatus;
import com.fafa.domain.model.collaboration.PetInvitation;
import com.fafa.domain.repository.PetInvitationRepository;
import com.fafa.infrastructure.persistence.mapper.PetInvitationMapper;
import com.fafa.infrastructure.persistence.po.PetInvitationPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PetInvitationRepositoryImpl implements PetInvitationRepository {
    private final PetInvitationMapper mapper;

    @Override
    public PetInvitation save(PetInvitation invitation) {
        PetInvitationPO po = toPO(invitation);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<PetInvitation> findById(Long id) { return Optional.ofNullable(toDomain(mapper.selectById(id))); }

    @Override
    public Optional<PetInvitation> findForUpdate(Long id, String tokenHash) {
        return Optional.ofNullable(toDomain(mapper.selectForUpdate(id, tokenHash)));
    }

    private PetInvitation toDomain(PetInvitationPO po) {
        if (po == null) return null;
        return PetInvitation.restore(po.getId(), po.getPetId(), po.getCreatedBy(), InvitationRole.fromCode(po.getRole()),
                po.getTokenHash(), po.getExpiresAt(), po.getMaxUses(), po.getMemberExpiresAt(), po.getUsedCount(),
                InvitationStatus.fromCode(po.getStatus()), po.getAcceptedBy(), po.getAcceptedAt(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private PetInvitationPO toPO(PetInvitation invitation) {
        PetInvitationPO po = new PetInvitationPO();
        po.setId(invitation.getId()); po.setPetId(invitation.getPetId()); po.setCreatedBy(invitation.getCreatedBy());
        po.setRole(invitation.getRole().getCode()); po.setTokenHash(invitation.getTokenHash()); po.setExpiresAt(invitation.getExpiresAt());
        po.setMemberExpiresAt(invitation.getMemberExpiresAt()); po.setMaxUses(invitation.getMaxUses());
        po.setUsedCount(invitation.getUsedCount()); po.setStatus(invitation.getStatus().getCode());
        po.setAcceptedBy(invitation.getAcceptedBy()); po.setAcceptedAt(invitation.getAcceptedAt());
        return po;
    }
}

package com.fafa.domain.repository;

import com.fafa.domain.model.collaboration.PetMember;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PetMemberRepository {
    PetMember save(PetMember member);

    Optional<PetMember> findById(Long memberId);

    Optional<PetMember> findByPetIdAndUserId(Long petId, Long userId);

    Optional<PetMember> findForUpdateByPetIdAndUserId(Long petId, Long userId);

    List<PetMember> findByPetId(Long petId);

    int countActiveByPetId(Long petId, LocalDateTime now);

    void expireDue(LocalDateTime now);
}

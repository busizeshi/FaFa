package com.fafa.domain.repository;

import com.fafa.domain.model.collaboration.PetInvitation;

import java.util.Optional;

public interface PetInvitationRepository {
    PetInvitation save(PetInvitation invitation);
    Optional<PetInvitation> findById(Long id);
    Optional<PetInvitation> findForUpdate(Long id, String tokenHash);
}

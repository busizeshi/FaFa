package com.fafa.domain.repository;

import com.fafa.domain.model.content.ContentDraft;

import java.util.List;
import java.util.Optional;

public interface ContentDraftRepository {
    ContentDraft save(ContentDraft draft);
    Optional<ContentDraft> findByIdAndUserId(Long id, Long userId);
    Optional<ContentDraft> findByIdempotencyKey(Long userId, String idempotencyKey);
    List<ContentDraft> findByUserIdAndPetId(Long userId, Long petId, int offset, int limit);
    void delete(ContentDraft draft);
}

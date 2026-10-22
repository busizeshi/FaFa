package com.fafa.biz.pet.application;

import com.fafa.biz.pet.domain.BreedRepository;
import com.fafa.biz.pet.domain.PetModels.Species;
import com.fafa.biz.operation.Paging;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreedCatalog {
    private final BreedRepository breeds;
    private final Paging paging;
    public BreedCatalog(BreedRepository breeds, Paging paging) { this.breeds = breeds; this.paging = paging; }
    public CursorPage<Breed> list(BreedQuery query) {
        species(query.species());
        int limit = paging.limit(query.limit());
        if (query.query() != null && query.query().length() > 80) invalid();
        var rows = breeds.list(query.species(), query.query() == null ? "" : query.query().strip(),
                query.cursor() == null ? "" : query.cursor(), limit + 1, query.includeInactive());
        return new CursorPage<>(rows.stream().limit(limit).toList(), rows.size() > limit ? rows.get(limit - 1).id() : null);
    }
    public void validateReference(String id, Species species, String original) {
        if (id == null) return;
        var breed = breeds.find(id);
        if (breed == null || !breed.species().equals(species.name())
                || (!"ACTIVE".equals(breed.status()) && !id.equals(original))) invalid();
    }
    @Transactional
    public Breed save(BreedChange change) {
        species(change.species());
        if (change.species() == null || change.id() == null || !change.id().matches("[A-Z0-9_]{1,64}")
                || change.name() == null || change.name().isBlank() || change.name().length() > 80
                || !("ACTIVE".equals(change.status()) || "INACTIVE".equals(change.status()))
                || (change.version() != null && change.version() < 1)) invalid();
        return breeds.save(change.id(), change.species(), change.name().strip(), change.status(), change.version());
    }
    private void species(String species) {
        if (species == null) return;
        try { Species.valueOf(species); } catch (IllegalArgumentException exception) { invalid(); }
    }
    private void invalid() { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "品种或分页参数无效"); }
}

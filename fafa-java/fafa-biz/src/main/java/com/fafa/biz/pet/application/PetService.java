package com.fafa.biz.pet.application;

import com.fafa.biz.identity.application.AccountSessions;
import com.fafa.biz.identity.application.CurrentActor;
import com.fafa.biz.operation.IdempotentWrites;
import com.fafa.biz.operation.Paging;
import com.fafa.biz.pet.domain.PetModels.*;
import com.fafa.biz.pet.domain.PetRepository;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 宠物用例的事务边界；同一主人写入串行化，资源版本仍必须匹配。 */
@Service
public class PetService {
    public record PetView(String id, String ownerUserId, String role, Profile profile, Status status,
                          long version, Instant deleteAfter) { }
    public record Impact(String petId, Status status, int waitingDays, Map<String, Long> resourceCounts) { }
    public record Selection(List<PetView> pets, String currentPetId) { }
    private final CurrentActor actor;
    private final AccountSessions sessions;
    private final PetRepository pets;
    private final PetAccess access;
    private final BreedCatalog breeds;
    private final IdempotentWrites writes;
    private final PetPolicy policy;
    private final Clock clock;
    private final Paging paging;
    public PetService(CurrentActor actor, AccountSessions sessions, PetRepository pets, PetAccess access,
                      BreedCatalog breeds, IdempotentWrites writes, PetPolicy policy, Clock clock, Paging paging) {
        this.actor = actor; this.sessions = sessions; this.pets = pets; this.access = access;
        this.breeds = breeds; this.writes = writes; this.policy = policy; this.clock = clock; this.paging = paging;
    }
    @Transactional
    public PetView create(String key, Profile profile) {
        String owner = actor.lock();
        return writes.execute(owner, "CREATE_PET", key, profile, PetView.class, () -> {
            validate(profile, null);
            var pet = new Pet(UUID.randomUUID().toString(), owner, owner, profile, Status.ACTIVE, 1);
            pets.insert(pet);
            var selection = pets.preferences(owner);
            if (selection.currentPetId() == null) pets.savePreferences(owner,
                    new Preferences(pet.id(), selection.defaultPetId() == null ? pet.id() : selection.defaultPetId(), selection.version() + 1));
            return view(pet);
        });
    }
    public CursorPage<PetView> list(Status status, String cursor, int requestedLimit) {
        if (status == Status.PURGING || status == Status.DELETED) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        int limit = paging.limit(requestedLimit);
        var rows = pets.list(actor.id(), status == null ? Status.ACTIVE : status, cursor == null ? "" : cursor, limit + 1);
        return new CursorPage<>(rows.stream().limit(limit).map(this::view).toList(),
                rows.size() > limit ? rows.get(limit - 1).id() : null);
    }
    public PetView get(String id) { return view(access.owned(actor.id(), id)); }
    @Transactional
    public PetView update(String id, Patch patch) {
        var pet = access.versioned(actor.lock(), id, patch.version());
        requireState(pet, Status.ACTIVE);
        var profile = patch.merge(pet.profile());
        validate(profile, pet.profile().breedId());
        pets.update(new Pet(pet.id(), pet.ownerUserId(), pet.createdBy(), profile, pet.status(), pet.version()), pet.version());
        return view(pets.find(id));
    }
    @Transactional
    public PetView archive(String id, long version) {
        var pet = access.versioned(actor.lock(), id, version);
        requireState(pet, Status.ACTIVE);
        transition(pet, Status.ARCHIVED);
        pets.clearSelection(pet.ownerUserId(), id);
        return view(pets.find(id));
    }
    @Transactional
    public PetView restore(String id, long version) {
        var pet = access.versioned(actor.lock(), id, version);
        requireState(pet, Status.ARCHIVED);
        transition(pet, Status.ACTIVE);
        return view(pets.find(id));
    }
    public Impact impact(String id) {
        var pet = access.owned(actor.id(), id);
        return new Impact(id, pet.status(), policy.deletionWaitingDays(), Map.of("pet", 1L));
    }
    @Transactional
    public PetView requestDeletion(String id, String key, Delete request) {
        String owner = actor.lock();
        return writes.execute(owner, "DELETE_PET:" + id, key, request, PetView.class, () -> {
            var pet = access.versioned(owner, id, request.version());
            if (!request.acknowledgement()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
            if (pet.status() != Status.ACTIVE && pet.status() != Status.ARCHIVED) conflict();
            pets.requestDeletion(UUID.randomUUID().toString(), pet, clock.instant().plus(policy.deletionWaitingDays(), ChronoUnit.DAYS));
            transition(pet, Status.DELETION_PENDING);
            pets.clearSelection(owner, id);
            return view(pets.find(id));
        });
    }
    @Transactional
    public PetView withdraw(String id, long version) {
        var pet = access.versioned(actor.lock(), id, version);
        requireState(pet, Status.DELETION_PENDING);
        transition(pet, pets.withdrawDeletion(id, clock.instant()));
        return view(pets.find(id));
    }
    public Preferences preferences() { return pets.preferences(actor.id()); }
    @Transactional
    public Preferences preferences(PreferenceChange change) {
        String owner = actor.lock();
        var existing = pets.preferences(owner);
        if (change.version() != existing.version()) throw new FafaException(BusinessError.VERSION_CONFLICT);
        if (change.currentPetId() != null) access.active(owner, change.currentPetId());
        if (change.defaultPetId() != null) access.active(owner, change.defaultPetId());
        var updated = new Preferences(change.currentPetId(), change.defaultPetId(), existing.version() + 1);
        pets.savePreferences(owner, updated);
        return updated;
    }
    public Selection selection() {
        String owner = actor.id();
        var preferences = pets.preferences(owner);
        var first = pets.list(owner, Status.ACTIVE, "", paging.defaultPageSize());
        String current = validSelection(owner, preferences.currentPetId());
        if (current == null) current = validSelection(owner, preferences.defaultPetId());
        if (current == null && !first.isEmpty()) current = first.getFirst().id();
        var summaries = new java.util.ArrayList<>(first.stream().map(this::view).toList());
        String selected = current;
        if (selected != null && summaries.stream().noneMatch(pet -> selected.equals(pet.id()))) summaries.add(view(pets.find(selected)));
        return new Selection(List.copyOf(summaries), current);
    }
    private String validSelection(String owner, String id) {
        var pet = id == null ? null : pets.find(id);
        return pet != null && owner.equals(pet.ownerUserId()) && pet.status() == Status.ACTIVE ? id : null;
    }
    private void validate(Profile profile, String originalBreed) {
        profile.validate(LocalDate.ofInstant(clock.instant(), ZoneId.of(sessions.me().timezone())));
        breeds.validateReference(profile.breedId(), profile.species(), originalBreed);
    }
    private PetView view(Pet pet) {
        return new PetView(pet.id(), pet.ownerUserId(), "OWNER", pet.profile(), pet.status(), pet.version(),
                pet.status() == Status.DELETION_PENDING ? pets.deletionDeadline(pet.id()) : null);
    }
    private void transition(Pet pet, Status status) {
        pets.update(new Pet(pet.id(), pet.ownerUserId(), pet.createdBy(), pet.profile(), status, pet.version()), pet.version());
    }
    private void requireState(Pet pet, Status expected) { if (pet.status() != expected) conflict(); }
    private void conflict() { throw new FafaException(BusinessError.PET_STATE_CONFLICT); }
}

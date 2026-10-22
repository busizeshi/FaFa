package com.fafa.biz.pet.interfaces.http;

import com.fafa.biz.pet.application.*;
import com.fafa.biz.pet.domain.PetModels.*;
import com.fafa.common.api.*;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@SecurityRequirement(name = "bearerAuth")
public class PetController {
    private final PetService pets;
    private final BreedCatalog breeds;
    public PetController(PetService pets, BreedCatalog breeds) { this.pets = pets; this.breeds = breeds; }
    @GetMapping("/pet-species") @Operation(operationId = "listPetSpecies")
    public R<List<Species>> species() { return R.ok(List.of(Species.values())); }
    @GetMapping("/pet-breeds") @Operation(operationId = "listPetBreeds")
    public R<CursorPage<Breed>> breeds(@RequestParam(required = false) Species species,
            @RequestParam(required = false) String q, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "0") int limit) {
        return R.ok(breeds.list(new BreedQuery(species == null ? null : species.name(), q, cursor, limit, false)));
    }
    @GetMapping("/pets") @Operation(operationId = "listAccessiblePets")
    public R<CursorPage<PetService.PetView>> list(@RequestParam(required = false) Status status,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(pets.list(status, cursor, limit));
    }
    @PostMapping("/pets") @ResponseStatus(HttpStatus.CREATED) @Operation(operationId = "createPet")
    public R<PetService.PetView> create(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody Profile body) {
        return R.ok(pets.create(key, body));
    }
    @GetMapping("/pets/{id}") @Operation(operationId = "getPet")
    public R<PetService.PetView> get(@PathVariable String id) { return R.ok(pets.get(id)); }
    @PatchMapping("/pets/{id}") @Operation(operationId = "updatePet")
    public R<PetService.PetView> update(@PathVariable String id, @Valid @RequestBody Patch body) { return R.ok(pets.update(id, body)); }
    @PostMapping("/pets/{id}:archive") @Operation(operationId = "archivePet")
    public R<PetService.PetView> archive(@PathVariable String id, @Valid @RequestBody Version body) {
        return R.ok(pets.archive(id, body.version()));
    }
    @PostMapping("/pets/{id}:restore") @Operation(operationId = "restorePet")
    public R<PetService.PetView> restore(@PathVariable String id, @Valid @RequestBody Version body) {
        return R.ok(pets.restore(id, body.version()));
    }
    @GetMapping("/pets/{id}/deletion-impact") @Operation(operationId = "previewPetDeletion")
    public R<PetService.Impact> impact(@PathVariable String id) { return R.ok(pets.impact(id)); }
    @PostMapping("/pets/{id}/deletion-requests") @ResponseStatus(HttpStatus.ACCEPTED) @Operation(operationId = "requestPetDeletion")
    public R<PetService.PetView> delete(@PathVariable String id, @RequestHeader("Idempotency-Key") String key,
                                      @Valid @RequestBody Delete body) { return R.ok(pets.requestDeletion(id, key, body)); }
    @PostMapping("/pets/{id}/deletion-request:withdraw") @Operation(operationId = "withdrawPetDeletion")
    public R<PetService.PetView> withdraw(@PathVariable String id, @Valid @RequestBody Version body) {
        return R.ok(pets.withdraw(id, body.version()));
    }
    @GetMapping("/me/pet-preferences") @Operation(operationId = "getPetPreferences")
    public R<Preferences> preferences() { return R.ok(pets.preferences()); }
    @PutMapping("/me/pet-preferences") @Operation(operationId = "updatePetPreferences")
    public R<Preferences> preferences(@Valid @RequestBody PreferenceChange body) { return R.ok(pets.preferences(body)); }
}

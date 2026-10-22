package com.fafa.admin.interfaces.http;

import com.fafa.admin.audit.AdminAudit;
import com.fafa.admin.infrastructure.rpc.AdminBusinessClient;
import com.fafa.common.api.*;
import com.fafa.common.rpc.AdminBusinessRpc;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.context.annotation.Profile("!admin-init")
@RequestMapping("/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AdminBusinessController {
    public record StatusChange(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status,
                               @NotBlank @Size(max = 500) String reason, @Positive long version) { }
    public record Decision(@NotNull Boolean approved, @Pattern(regexp = "[1-9][0-9]{0,18}") String amount,
                           @NotBlank @Size(max = 500) String reason, @Positive long version) { }
    public record Grant(@NotBlank @Pattern(regexp = "STORAGE_BYTES") String bucket,
                        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") String amount,
                        @NotBlank @Pattern(regexp = "GRANT") String entryType,
                        @NotBlank @Size(max = 500) String reason) { }
    public record BreedWrite(@NotBlank @Pattern(regexp = "[A-Z0-9_]{1,64}") String id,
            @NotBlank @Pattern(regexp = "CAT|DOG|OTHER") String species,
            @NotBlank @Size(max = 80) String name, @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status,
            @Positive Long version, @NotBlank @Size(max = 500) String reason) { }
    private final AdminBusinessClient rpc;
    private final AdminAudit audit;
    public AdminBusinessController(AdminBusinessClient rpc, AdminAudit audit) { this.rpc = rpc; this.audit = audit; }

    @GetMapping("/users") @AdminPermission("USER_READ") @Operation(operationId = "listAdminUsers")
    public R<CursorPage<UserSummary>> users(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(rpc.call("users", "USER_READ", new PageQuery(cursor, limit), AdminBusinessRpc::users));
    }
    @GetMapping("/users/{id}") @AdminPermission("USER_READ") @Operation(operationId = "getAdminUser")
    public R<UserSummary> user(@PathVariable String id) { return R.ok(rpc.call("user", "USER_READ", new Id(id), AdminBusinessRpc::user)); }
    @PostMapping("/users/{id}:set-status") @AdminPermission("USER_WRITE") @Operation(operationId = "setAdminUserStatus")
    public R<UserSummary> status(@PathVariable String id, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody StatusChange body) {
        return R.ok(audit.execute("USER_STATUS", id, body.reason(), key, new UserStatusChange(null, id, body.status(), body.reason(), body.version()),
                op -> rpc.call("setUserStatus", "USER_WRITE", new UserStatusChange(op, id, body.status(), body.reason(), body.version()), AdminBusinessRpc::setUserStatus)));
    }
    @GetMapping("/quota-grant-requests") @AdminPermission("QUOTA_READ") @Operation(operationId = "listAdminQuotaRequests")
    public R<CursorPage<QuotaRequest>> requests(@RequestParam(required = false) String state,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(rpc.call("quotaRequests", "QUOTA_READ", new RequestQuery(state, cursor, limit), AdminBusinessRpc::quotaRequests));
    }
    @PostMapping("/quota-grant-requests/{id}:decide") @AdminPermission("QUOTA_WRITE") @Operation(operationId = "decideAdminQuotaRequest")
    public R<QuotaRequest> decide(@PathVariable String id, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody Decision body) {
        return R.ok(audit.execute("QUOTA_DECISION", id, body.reason(), key,
                new QuotaDecision(null, id, body.approved(), body.amount(), body.reason(), body.version()),
                op -> rpc.call("decideQuota", "QUOTA_WRITE", new QuotaDecision(op, id, body.approved(), body.amount(), body.reason(), body.version()), AdminBusinessRpc::decideQuota)));
    }
    @PostMapping("/users/{id}/quota-adjustments") @AdminPermission("QUOTA_WRITE") @Operation(operationId = "grantAdminUserQuota")
    public R<QuotaView> grant(@PathVariable String id, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody Grant body) {
        return R.ok(audit.execute("QUOTA_GRANT", id, body.reason(), key, new QuotaGrant(null, id, body.bucket(), body.amount(), body.reason()),
                op -> rpc.call("grantQuota", "QUOTA_WRITE", new QuotaGrant(op, id, body.bucket(), body.amount(), body.reason()), AdminBusinessRpc::grantQuota)));
    }
    @GetMapping("/dictionaries/pet-breeds") @AdminPermission("BREED_READ") @Operation(operationId = "listAdminPetBreeds")
    public R<CursorPage<Breed>> breeds(@RequestParam(required = false) String species, @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(rpc.call("breeds", "BREED_READ", new BreedQuery(species, q, cursor, limit, true), AdminBusinessRpc::breeds));
    }
    @PostMapping("/pet-breeds") @AdminPermission("BREED_WRITE") @ResponseStatus(HttpStatus.CREATED) @Operation(operationId = "createAdminPetBreed")
    public R<Breed> createBreed(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody BreedWrite body) {
        if (body.version() != null) throw new com.fafa.common.error.FafaException(com.fafa.common.error.CommonErrorCode.INVALID_ARGUMENT);
        return R.ok(saveBreed(key, body));
    }
    @PatchMapping("/pet-breeds/{id}") @AdminPermission("BREED_WRITE") @Operation(operationId = "updateAdminPetBreed")
    public R<Breed> updateBreed(@PathVariable String id, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody BreedWrite body) {
        if (body.version() == null || !id.equals(body.id())) throw new com.fafa.common.error.FafaException(com.fafa.common.error.CommonErrorCode.INVALID_ARGUMENT);
        return R.ok(saveBreed(key, body));
    }
    private Breed saveBreed(String key, BreedWrite body) {
        var payload = new BreedChange(null, body.id(), body.species(), body.name(), body.status(), body.version());
        return audit.execute("SAVE_BREED", body.id(), body.reason(), key, payload,
                op -> rpc.call("saveBreed", "BREED_WRITE", new BreedChange(op, body.id(), body.species(), body.name(), body.status(), body.version()), AdminBusinessRpc::saveBreed));
    }
    @GetMapping("/audit-logs") @AdminPermission("AUDIT_READ") @Operation(operationId = "listAdminAuditLogs")
    public R<CursorPage<AdminAudit.Entry>> audits(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(audit.list(cursor, limit));
    }
}

package com.fafa.biz.usage.interfaces.http;

import com.fafa.biz.identity.application.CurrentActor;
import com.fafa.biz.usage.application.*;
import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.biz.usage.domain.QuotaRequestRepository;
import com.fafa.common.api.*;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1")
@SecurityRequirement(name = "bearerAuth")
public class QuotaController {
    private final CurrentActor actor;
    private final StorageQuota quota;
    private final QuotaRequests requests;
    public QuotaController(CurrentActor actor, StorageQuota quota, QuotaRequests requests) {
        this.actor = actor; this.quota = quota; this.requests = requests;
    }
    @GetMapping("/usage") @Operation(operationId = "getMyUsage")
    public R<List<QuotaView>> usage(@RequestParam(defaultValue = "STORAGE_BYTES") String bucket) {
        QuotaAmounts.bucket(bucket);
        return R.ok(List.of(quota.view(actor.id())));
    }
    @GetMapping("/usage/ledger") @Operation(operationId = "listMyUsageLedger")
    public R<CursorPage<QuotaRequestRepository.Ledger>> ledger(@RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "0") int limit, @RequestParam(defaultValue = "STORAGE_BYTES") String bucket) {
        QuotaAmounts.bucket(bucket);
        return R.ok(requests.ledger(cursor, limit));
    }
    @PostMapping("/quota-grant-requests") @ResponseStatus(HttpStatus.CREATED) @Operation(operationId = "requestFreeQuota")
    public R<QuotaRequest> create(@RequestHeader("Idempotency-Key") String key, @jakarta.validation.Valid @RequestBody QuotaRequests.Create body) {
        return R.ok(requests.create(key, body));
    }
    @GetMapping("/quota-grant-requests") @Operation(operationId = "listMyQuotaRequests")
    public R<CursorPage<QuotaRequest>> list(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "0") int limit) {
        return R.ok(requests.mine(cursor, limit));
    }
}

package com.fafa.biz.usage.application;

import com.fafa.biz.identity.application.CurrentActor;
import com.fafa.biz.operation.IdempotentWrites;
import com.fafa.biz.operation.Paging;
import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.biz.usage.domain.QuotaRequestRepository;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.*;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;

@Service
public class QuotaRequests {
    public record Create(@NotBlank @Pattern(regexp = "STORAGE_BYTES") String bucket,
                         @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") String amount,
                         @NotBlank @Size(max = 500) String reason) { }
    private final CurrentActor actor;
    private final IdempotentWrites writes;
    private final QuotaRequestRepository store;
    private final StorageQuota quota;
    private final Paging paging;
    public QuotaRequests(CurrentActor actor, IdempotentWrites writes, QuotaRequestRepository store, StorageQuota quota, Paging paging) {
        this.actor = actor; this.writes = writes; this.store = store; this.quota = quota; this.paging = paging;
    }
    @Transactional
    public QuotaRequest create(String key, Create request) {
        String owner = actor.lock();
        QuotaAmounts.bucket(request.bucket()); QuotaAmounts.reason(request.reason());
        long amount = QuotaAmounts.positive(request.amount());
        return writes.execute(owner, "REQUEST_QUOTA", key, request, QuotaRequest.class, () -> {
            String id = UUID.randomUUID().toString();
            store.insert(id, owner, amount, request.reason().strip());
            return store.find(id, false);
        });
    }
    public CursorPage<QuotaRequest> mine(String cursor, int limit) { return list(actor.id(), null, cursor, limit); }
    public CursorPage<QuotaRequest> list(String owner, String state, String cursor, int requested) {
        if (state != null && !java.util.Set.of("PENDING", "APPROVED", "REJECTED").contains(state)) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        int limit = paging.limit(requested);
        var rows = store.list(owner, state, cursor == null ? "" : cursor, limit + 1);
        return new CursorPage<>(rows.stream().limit(limit).toList(), rows.size() > limit ? rows.get(limit - 1).id() : null);
    }
    public CursorPage<QuotaRequestRepository.Ledger> ledger(String cursor, int requested) {
        int limit = paging.limit(requested);
        var rows = store.ledger(actor.id(), cursor == null ? "" : cursor, limit + 1);
        return new CursorPage<>(rows.stream().limit(limit).toList(), rows.size() > limit ? rows.get(limit - 1).id() : null);
    }
    @Transactional
    public QuotaRequest decide(String operator, QuotaDecision decision) {
        QuotaAmounts.reason(decision.reason());
        var lookup = store.find(decision.requestId(), false);
        if (lookup == null) throw new FafaException(CommonErrorCode.NOT_FOUND);
        // 先锁账号再锁申请，与其他业务写入保持相同锁顺序。
        quota.view(lookup.userId());
        var request = store.find(decision.requestId(), true);
        if (request.version() != decision.version()) throw new FafaException(BusinessError.VERSION_CONFLICT);
        if (!"PENDING".equals(request.state())) throw new FafaException(BusinessError.OPERATION_CONFLICT);
        Long amount = decision.approved() ? QuotaAmounts.positive(decision.amount()) : null;
        if (!decision.approved() && decision.amount() != null) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        if (amount != null) quota.grant(request.userId(), "request:" + request.id(), amount, operator, decision.reason());
        store.decide(request.id(), decision.approved() ? "APPROVED" : "REJECTED", amount, operator, decision.reason().strip());
        return store.find(request.id(), false);
    }
}

package com.fafa.biz.admin.application;

import com.fafa.biz.identity.application.AccountAdministration;
import com.fafa.biz.identity.domain.UserDirectory;
import com.fafa.biz.operation.AdminOperations;
import com.fafa.biz.operation.Paging;
import com.fafa.biz.pet.application.BreedCatalog;
import com.fafa.biz.usage.application.*;
import com.fafa.biz.usage.domain.QuotaAmounts;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.*;
import com.fafa.common.rpc.AdminBusinessRpc.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台跨领域用例；操作收据与业务变动处于同一 Biz 数据库事务。 */
@Service
public class AdminBusinessService {
    private final AdminOperations operations;
    private final BreedCatalog breeds;
    private final AccountAdministration accounts;
    private final UserDirectory users;
    private final QuotaRequests requests;
    private final StorageQuota quotas;
    private final Paging paging;
    public AdminBusinessService(AdminOperations operations, BreedCatalog breeds, AccountAdministration accounts,
                                UserDirectory users, QuotaRequests requests, StorageQuota quotas, Paging paging) {
        this.operations = operations; this.breeds = breeds; this.accounts = accounts;
        this.users = users; this.requests = requests; this.quotas = quotas; this.paging = paging;
    }
    public CursorPage<UserSummary> users(PageQuery query) {
        int limit = paging.limit(query.limit());
        var rows = users.list(query.cursor() == null ? "" : query.cursor(), limit + 1);
        return new CursorPage<>(rows.stream().limit(limit).toList(), rows.size() > limit ? rows.get(limit - 1).id() : null);
    }
    public UserSummary user(String id) {
        var user = users.find(id);
        if (user == null) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return user;
    }
    @Transactional
    public Breed breed(String operator, BreedChange change) {
        return operations.execute(change.operationId(), operator, "SAVE_BREED", change, Breed.class, () -> breeds.save(change));
    }
    @Transactional
    public UserSummary status(String operator, UserStatusChange change) {
        return operations.execute(change.operationId(), operator, "USER_STATUS", change, UserSummary.class, () -> {
            QuotaAmounts.reason(change.reason());
            if (change.status() == null || !java.util.Set.of("ACTIVE", "DISABLED").contains(change.status())) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
            var account = accounts.lock(change.userId());
            if (account.version() != change.version()) throw new FafaException(BusinessError.VERSION_CONFLICT);
            users.status(change.userId(), change.status());
            return user(change.userId());
        });
    }
    @Transactional
    public QuotaRequest decide(String operator, QuotaDecision decision) {
        return operations.execute(decision.operationId(), operator, "QUOTA_DECISION", decision, QuotaRequest.class,
                () -> requests.decide(operator, decision));
    }
    @Transactional
    public QuotaView grant(String operator, QuotaGrant grant) {
        QuotaAmounts.bucket(grant.bucket());
        return operations.execute(grant.operationId(), operator, "QUOTA_GRANT", grant, QuotaView.class,
                () -> quotas.grant(grant.userId(), grant.operationId(), QuotaAmounts.positive(grant.amount()), operator, grant.reason()));
    }
}

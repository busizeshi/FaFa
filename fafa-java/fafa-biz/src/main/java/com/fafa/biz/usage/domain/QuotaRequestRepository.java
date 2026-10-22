package com.fafa.biz.usage.domain;

import com.fafa.common.rpc.AdminBusinessRpc.QuotaRequest;
import java.time.Instant;
import java.util.List;

/** 额度申请和流水查询端口；申请审批必须在账号锁之后取得申请行锁。 */
public interface QuotaRequestRepository {
    record Ledger(String id, String operationId, String entryType, String amount, String reason, Instant createdAt) { }
    QuotaRequest find(String id, boolean lock);
    List<QuotaRequest> list(String owner, String state, String cursor, int limit);
    void insert(String id, String owner, long amount, String reason);
    void decide(String id, String state, Long amount, String operator, String reason);
    List<Ledger> ledger(String owner, String cursor, int limit);
}

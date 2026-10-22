package com.fafa.biz.usage.domain;

import com.fafa.common.rpc.AdminBusinessRpc.QuotaView;

/** 存储额度仓储；调用写方法前应取得账号写锁。 */
public interface QuotaRepository {
    record Balance(long granted, long used, long reserved, long version) {
        public long available() { return granted - used - reserved; }
        public QuotaView view() { return new QuotaView(QuotaAmounts.STORAGE, Long.toString(granted), Long.toString(used),
                Long.toString(reserved), Long.toString(available()), version); }
    }
    record Reservation(String operationId, String userId, long amount, Long settledAmount, String state) { }
    Balance find(String owner);
    void create(String owner, long initial);
    void save(String owner, Balance balance);
    Reservation reservation(String operationId);
    void reserve(String operationId, String owner, long amount);
    void finish(String operationId, String state, long settledAmount);
    void ledger(String owner, String operationId, String type, long amount, String operatorId, String reason);
}

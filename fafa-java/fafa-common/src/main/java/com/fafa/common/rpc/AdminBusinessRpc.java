package com.fafa.common.rpc;

import com.fafa.common.api.CursorPage;
import java.io.Serializable;

/** 管理端可使用的明确业务能力；不提供任意数据库或脚本执行入口。 */
public interface AdminBusinessRpc {
    Reply<CursorPage<Breed>> breeds(Call<BreedQuery> call);
    Reply<Breed> saveBreed(Call<BreedChange> call);
    Reply<CursorPage<UserSummary>> users(Call<PageQuery> call);
    Reply<UserSummary> user(Call<Id> call);
    Reply<UserSummary> setUserStatus(Call<UserStatusChange> call);
    Reply<CursorPage<QuotaRequest>> quotaRequests(Call<RequestQuery> call);
    Reply<QuotaRequest> decideQuota(Call<QuotaDecision> call);
    Reply<QuotaView> grantQuota(Call<QuotaGrant> call);

    record Call<T>(String operatorId, String permission, String requestId, long issuedAt,
                   T payload, String signature) implements Serializable {
        @Override public String toString() { return "Call[operatorId=" + operatorId + "]"; }
    }
    record Reply<T>(String code, String message, int status, T data) implements Serializable { }
    record Id(String id) implements Serializable { }
    record PageQuery(String cursor, int limit) implements Serializable { }
    record BreedQuery(String species, String query, String cursor, int limit, boolean includeInactive) implements Serializable { }
    record Breed(String id, String species, String name, String status, long version) implements Serializable { }
    record BreedChange(String operationId, String id, String species, String name, String status, Long version) implements Serializable { }
    record UserSummary(String id, String maskedEmail, String nickname, String status, long version) implements Serializable { }
    record UserStatusChange(String operationId, String userId, String status, String reason, long version) implements Serializable { }
    record RequestQuery(String state, String cursor, int limit) implements Serializable { }
    record QuotaView(String bucket, String granted, String used, String reserved, String available, long version) implements Serializable { }
    record QuotaRequest(String id, String userId, String bucket, String amount, String reason, String state,
                        String grantedAmount, String decisionReason, String operatorId, long version) implements Serializable { }
    record QuotaDecision(String operationId, String requestId, boolean approved, String amount,
                         String reason, long version) implements Serializable { }
    record QuotaGrant(String operationId, String userId, String bucket, String amount, String reason) implements Serializable { }
}

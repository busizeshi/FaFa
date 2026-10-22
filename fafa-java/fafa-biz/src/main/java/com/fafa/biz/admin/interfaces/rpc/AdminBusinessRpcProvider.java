package com.fafa.biz.admin.interfaces.rpc;

import com.fafa.biz.admin.application.AdminBusinessService;
import com.fafa.biz.pet.application.BreedCatalog;
import com.fafa.biz.usage.application.QuotaRequests;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.*;
import com.fafa.common.rpc.AdminBusinessRpc;
import com.fafa.common.rpc.RpcAuthentication;
import java.util.function.Supplier;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

/** 内部业务 RPC；所有方法都先验证服务签名和固定权限。 */
@DubboService
public class AdminBusinessRpcProvider implements AdminBusinessRpc {
    private final RpcAuthentication auth;
    private final AdminBusinessService business;
    private final BreedCatalog breeds;
    private final QuotaRequests requests;
    public AdminBusinessRpcProvider(RpcAuthentication auth, AdminBusinessService business, BreedCatalog breeds, QuotaRequests requests) {
        this.auth = auth; this.business = business; this.breeds = breeds; this.requests = requests;
    }
    public Reply<CursorPage<Breed>> breeds(Call<BreedQuery> call) {
        return invoke("breeds", "BREED_READ", call, () -> breeds.list(call.payload()));
    }
    public Reply<Breed> saveBreed(Call<BreedChange> call) {
        return invoke("saveBreed", "BREED_WRITE", call, () -> business.breed(call.operatorId(), call.payload()));
    }
    public Reply<CursorPage<UserSummary>> users(Call<PageQuery> call) {
        return invoke("users", "USER_READ", call, () -> business.users(call.payload()));
    }
    public Reply<UserSummary> user(Call<Id> call) {
        return invoke("user", "USER_READ", call, () -> business.user(call.payload().id()));
    }
    public Reply<UserSummary> setUserStatus(Call<UserStatusChange> call) {
        return invoke("setUserStatus", "USER_WRITE", call, () -> business.status(call.operatorId(), call.payload()));
    }
    public Reply<CursorPage<QuotaRequest>> quotaRequests(Call<RequestQuery> call) {
        return invoke("quotaRequests", "QUOTA_READ", call, () -> requests.list(null, call.payload().state(), call.payload().cursor(), call.payload().limit()));
    }
    public Reply<QuotaRequest> decideQuota(Call<QuotaDecision> call) {
        return invoke("decideQuota", "QUOTA_WRITE", call, () -> business.decide(call.operatorId(), call.payload()));
    }
    public Reply<QuotaView> grantQuota(Call<QuotaGrant> call) {
        return invoke("grantQuota", "QUOTA_WRITE", call, () -> business.grant(call.operatorId(), call.payload()));
    }
    private <T> Reply<T> invoke(String method, String permission, Call<?> call, Supplier<T> work) {
        try {
            auth.verify(method, permission, call);
            return new Reply<>("0", "成功", 200, work.get());
        } catch (FafaException exception) {
            return new Reply<>(exception.errorCode().code(), exception.getMessage(), exception.errorCode().httpStatus().value(), null);
        } catch (DuplicateKeyException exception) {
            return new Reply<>(BusinessError.OPERATION_CONFLICT.code(), BusinessError.OPERATION_CONFLICT.message(), 409, null);
        } catch (DataAccessException exception) {
            LoggerFactory.getLogger(getClass()).error("内部业务数据访问失败，方法={}", method, exception);
            var error = CommonErrorCode.DEPENDENCY_UNAVAILABLE;
            return new Reply<>(error.code(), error.message(), 503, null);
        }
    }
}

package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.IdentityRepository;
import com.fafa.biz.identity.domain.IdentityModels.Account;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 后台与额度服务使用的账号锁入口，不暴露凭证给 RPC 调用方。 */
@Component
public class AccountAdministration {
    private final IdentityRepository accounts;
    public AccountAdministration(IdentityRepository accounts) { this.accounts = accounts; }
    public Account lock(String userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("必须在事务中锁定账号");
        var user = accounts.lockAccount(userId);
        if (user == null) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return user;
    }
}

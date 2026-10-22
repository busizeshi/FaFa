package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.IdentityRepository;
import com.fafa.biz.identity.domain.IdentityError;
import com.fafa.common.error.FafaException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 为业务提供身份边界；同一用户的写操作先锁账号，再锁业务资源。 */
@Component
public class CurrentActor {
    private final AccountSessions sessions;
    private final IdentityRepository repository;
    public CurrentActor(AccountSessions sessions, IdentityRepository repository) {
        this.sessions = sessions; this.repository = repository;
    }
    public String id() { return sessions.currentAccount().id(); }
    public String lock() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("身份写锁必须处于事务中");
        }
        var current = sessions.currentAccount();
        var locked = repository.lockAccount(current.id());
        if (!"ACTIVE".equals(locked.status()) || locked.authVersion() != current.authVersion()) {
            throw new FafaException(IdentityError.SESSION_REVOKED);
        }
        return locked.id();
    }
}

package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.*;
import com.fafa.biz.identity.domain.IdentityModels.Account;
import com.fafa.biz.identity.domain.IdentityModels.Profile;
import com.fafa.common.error.FafaException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AccountSessions {
    public record UserView(String id, String email, String nickname, String signature,
                           String avatarMediaId, String timezone, long version) { }
    public record LoginView(UserView user, SessionTokens.Grant session) { }
    private final IdentityRepository repository;
    private final PasswordEncoder encoder;
    private final SessionTokens tokens;
    private final TransactionTemplate tx;
    private final AuthRateLimiter limiter;
    private final SecurityAudit audit;
    private final Clock clock;
    private final String dummyHash;

    public AccountSessions(IdentityRepository repository, PasswordEncoder encoder, SessionTokens tokens,
                           TransactionTemplate tx, AuthRateLimiter limiter, SecurityAudit audit, Clock clock) {
        this.repository = repository; this.encoder = encoder; this.tokens = tokens; this.tx = tx;
        this.limiter = limiter; this.audit = audit; this.clock = clock;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    public LoginView login(String email, String password, SessionTokens.Device device, String address) {
        String canonical = EmailAddress.parse(email).canonical();
        limiter.login(canonical, address);
        Account account = repository.findByEmail(canonical);
        if (account == null) {
            encoder.matches(password, dummyHash);
            audit.record(null, "LOGIN", "INVALID_CREDENTIALS");
            throw new FafaException(IdentityError.INVALID_CREDENTIALS);
        }
        LoginView view = tx.execute(status -> {
            Account locked = repository.lockAccount(account.id());
            if (!encoder.matches(password, locked.passwordHash())) return null;
            return issueLocked(locked, device);
        });
        if (view == null) {
            audit.record(account.id(), "LOGIN", "INVALID_CREDENTIALS");
            throw new FafaException(IdentityError.INVALID_CREDENTIALS);
        }
        return view;
    }
    public LoginView afterRegistration(String userId, SessionTokens.Device device) {
        return tx.execute(status -> issueLocked(repository.lockAccount(userId), device));
    }
    private LoginView issueLocked(Account account, SessionTokens.Device device) {
        requireActive(account);
        repository.advanceAuthVersion(account.id(), LocalDateTime.now(clock));
        // 数据库代际校验确保回滚、重置和 Redis 部分失败时默认拒绝访问。
        SessionTokens.Grant grant = tokens.issue(account.id(), account.authVersion() + 1, device);
        audit.record(account.id(), "LOGIN", "SUCCEEDED");
        return new LoginView(view(account), grant);
    }
    public Account currentAccount() {
        Account account = repository.findById(tokens.requireUserId());
        requireActive(account);
        if (account.authVersion() != tokens.currentAuthVersion()) {
            throw new FafaException(IdentityError.SESSION_REVOKED);
        }
        return account;
    }
    public UserView me() { return view(currentAccount()); }
    public UserView user(String id) { return view(repository.findById(id)); }
    public boolean hasToken() { return tokens.hasToken(); }
    public SessionTokens.Device device() { currentAccount(); return tokens.currentDevice(); }
    public void logout() {
        Account account = currentAccount();
        tx.executeWithoutResult(status -> {
            Account locked = repository.lockAccount(account.id());
            if (locked.authVersion() != account.authVersion()) {
                throw new FafaException(IdentityError.SESSION_REPLACED);
            }
            repository.advanceAuthVersion(account.id(), LocalDateTime.now(clock));
            audit.record(account.id(), "LOGOUT", "SUCCEEDED");
        });
        tokens.logout();
    }
    private UserView view(Account account) {
        Profile profile = repository.profile(account.id());
        return new UserView(account.id(), account.emailDisplay(), profile.nickname(), profile.signature(),
                profile.avatarMediaId(), profile.timezone(), profile.version());
    }
    private void requireActive(Account account) {
        if (account == null || !"ACTIVE".equals(account.status())) {
            throw new FafaException(IdentityError.ACCOUNT_DISABLED);
        }
    }
}

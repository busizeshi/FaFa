package com.fafa.admin.security;

import com.fafa.common.error.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 管理员会话与移动端隔离；数据库只保存随机令牌的摘要。 */
@Service
public class AdminSessions {
    public record Principal(String id, String username, Set<String> permissions) { }
    public record Login(Principal admin, String token, Instant expiresAt) {
        @Override public String toString() { return "AdminLogin[REDACTED]"; }
    }
    private final AdminStore store;
    private final AdminLoginLimiter limiter;
    private final PasswordEncoder passwords;
    private final AdminPolicy policy;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;
    public AdminSessions(AdminStore store, AdminLoginLimiter limiter, PasswordEncoder passwords,
                         AdminPolicy policy, Clock clock, TransactionTemplate tx) {
        this.store = store; this.limiter = limiter; this.passwords = passwords;
        this.policy = policy; this.clock = clock; this.tx = tx;
        dummyHash = passwords.encode(Base64.getEncoder().encodeToString(random.generateSeed(32)));
    }
    public Login login(String username, String password, String address) {
        if (!limiter.acquire(username, address)) throw new FafaException(BusinessError.ADMIN_LOGIN_LIMITED);
        var account = store.username(username);
        boolean matches = passwords.matches(password, account == null ? dummyHash : account.passwordHash());
        if (account == null || !matches || !"ACTIVE".equals(account.status())) throw new FafaException(BusinessError.ADMIN_CREDENTIALS_INVALID);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expires = clock.instant().plusSeconds(policy.sessionTtlSeconds());
        store.session(AdminLoginLimiter.hash(token), account, clock.instant(), expires);
        return new Login(principal(account), token, expires);
    }
    public Principal require(String authorization) {
        var account = store.session(AdminLoginLimiter.hash(token(authorization)), clock.instant());
        if (account == null) throw new FafaException(BusinessError.ADMIN_SESSION_INVALID);
        return principal(account);
    }
    public void permission(Principal principal, String permission) {
        if (!principal.permissions().contains(permission)) throw new FafaException(CommonErrorCode.FORBIDDEN);
    }
    public void logout(String authorization) { require(authorization); store.logout(AdminLoginLimiter.hash(token(authorization))); }
    public void bootstrap(String username, String password) {
        if (username == null || !username.matches("[a-zA-Z0-9._-]{3,64}") || password == null || password.length() < 12
                || password.length() > 128 || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*[0-9].*")) {
            throw new IllegalArgumentException("初始化用户名需 3—64 位；密码需 12—128 位且包含字母和数字");
        }
        String hash = passwords.encode(password);
        tx.executeWithoutResult(status -> store.bootstrap(username, hash, clock.instant()));
    }
    private Principal principal(AdminStore.Account account) { return new Principal(account.id(), account.username(), Set.copyOf(store.permissions(account.id()))); }
    private String token(String header) {
        if (header == null || !header.matches("Bearer [A-Za-z0-9_-]{43}")) throw new FafaException(BusinessError.ADMIN_SESSION_INVALID);
        return header.substring(7);
    }
}

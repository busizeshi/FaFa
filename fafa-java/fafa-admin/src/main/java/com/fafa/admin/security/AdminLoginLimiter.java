package com.fafa.admin.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 登录失败计数使用独立事务，避免认证异常回滚计数；按账号和直连 IP 分别节流。 */
@Component
public class AdminLoginLimiter {
    private final JdbcTemplate jdbc;
    private final AdminPolicy policy;
    private final Clock clock;
    public AdminLoginLimiter(JdbcTemplate jdbc, AdminPolicy policy, Clock clock) { this.jdbc = jdbc; this.policy = policy; this.clock = clock; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean acquire(String username, String ip) {
        boolean account = increment(hash("account:" + username));
        boolean address = increment(hash("ip:" + ip));
        return account && address;
    }
    private boolean increment(String key) {
        var now = LocalDateTime.now(clock);
        try { jdbc.update("INSERT INTO admin_login_throttle(fingerprint,attempts,window_start) VALUES(?,0,?)", key, now); }
        catch (DuplicateKeyException ignored) { /* 唯一键竞争后读取已存在的计数行。 */ }
        var row = jdbc.queryForObject("SELECT attempts,window_start FROM admin_login_throttle WHERE fingerprint=? FOR UPDATE",
                (rs, n) -> new Window(rs.getInt(1), rs.getObject(2, LocalDateTime.class)), key);
        var started = row.started();
        int attempts = row.attempts();
        if (!started.plusSeconds(policy.loginWindowSeconds()).isAfter(now)) { attempts = 0; started = now; }
        if (attempts >= policy.loginAttemptLimit()) return false;
        jdbc.update("UPDATE admin_login_throttle SET attempts=?,window_start=? WHERE fingerprint=?", attempts + 1, started, key);
        return true;
    }
    public static String hash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private record Window(int attempts, LocalDateTime started) { }
}

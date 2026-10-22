package com.fafa.biz.identity.infrastructure;

import com.fafa.biz.identity.application.AuthRateLimiter;
import com.fafa.biz.identity.application.AuthRateLimitException;
import com.fafa.biz.identity.application.IdentityProperties;
import com.fafa.biz.identity.domain.ChallengePurpose;
import com.fafa.biz.identity.domain.IdentityError;
import com.fafa.common.error.FafaException;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisAuthRateLimiter implements AuthRateLimiter {
    private static final DefaultRedisScript<Long> COUNT = new DefaultRedisScript<>("""
        local n=redis.call('INCR',KEYS[1])
        if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
        return n
        """, Long.class);
    private final StringRedisTemplate redis;
    private final SecretDigests digests;
    private final IdentityProperties policy;
    public RedisAuthRateLimiter(StringRedisTemplate redis, SecretDigests digests, IdentityProperties policy) {
        this.redis = redis; this.digests = digests; this.policy = policy;
    }
    public void challenge(String email, String address, ChallengePurpose purpose) {
        count("challenge-ip", address, 3600, policy.ipHourlyLimit());
        count("challenge-email", email, 86400, policy.emailDailyLimit());
        Boolean accepted = redis.opsForValue().setIfAbsent(key("resend", email), "1",
                Duration.ofSeconds(policy.resendSeconds()));
        if (!Boolean.TRUE.equals(accepted)) throw limited(key("resend", email), policy.resendSeconds());
    }
    public void login(String email, String address) {
        count("login-ip", address, 300, policy.loginWindowLimit() * 10);
        count("login-email", email, 300, policy.loginWindowLimit());
    }
    public void verification(String address) {
        count("verify-ip", address, 300, policy.loginWindowLimit() * 10);
    }
    private void count(String namespace, String value, int seconds, int limit) {
        Long count = redis.execute(COUNT, List.of(key(namespace, value)), String.valueOf(seconds));
        if (count == null) throw new IllegalStateException("Rate limiter did not return a result");
        if (count > limit) throw limited(key(namespace, value), seconds);
    }
    private AuthRateLimitException limited(String key, long fallback) {
        Long ttl = redis.getExpire(key);
        return new AuthRateLimitException(ttl != null && ttl > 0 ? ttl : fallback);
    }
    private String key(String namespace, String value) {
        return "fafa:auth:rate:" + namespace + ":" + digests.digest(namespace, value);
    }
}

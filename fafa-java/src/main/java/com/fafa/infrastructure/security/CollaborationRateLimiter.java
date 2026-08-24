package com.fafa.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 基于Redis的用户级小时窗口限流，Redis不可用时安全拒绝敏感操作。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationRateLimiter {
    private static final String KEY_PREFIX = "fafa:collaboration:rate:";
    private static final Duration WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String action, Long userId, int limit) {
        if (limit <= 0 || userId == null) return false;
        String key = KEY_PREFIX + action + ":" + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) redisTemplate.expire(key, WINDOW);
            return count != null && count <= limit;
        } catch (RuntimeException ex) {
            log.error("协同限流依赖不可用: action={}, userId={}", action, userId, ex);
            return false;
        }
    }
}

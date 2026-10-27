package com.fafa.biz.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis 固定窗口限流器（Lua 脚本保证自增+过期原子性）。
 *
 * <p>用途（M00）：登录按 IP/邮箱维度限流、验证码发送冷却与每日上限。
 * 架构规范 §3：限流在 Redis 完成，避免滥用分布式锁；脚本失败视为放行失败（fail-close）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class RedisRateLimiter {

    /**
     * Lua：INCR 后首次设置过期；返回 {当前计数, 剩余TTL秒}。
     */
    private static final DefaultRedisScript<List> INCR_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "local ttl = redis.call('PTTL', KEYS[1]) "
                    + "return {c, ttl}",
            List.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * @param redisTemplate 字符串 Redis 模板
     */
    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 固定窗口计数放行判断。
     *
     * @param key    限流键（建议 fafa:auth:rate:{维度}:{标识}）
     * @param limit  窗口内最大次数
     * @param window 窗口长度
     * @return 结果：allowed=false 时 retryAfterMillis 为窗口剩余毫秒
     */
    public RateResult tryAcquire(String key, int limit, Duration window) {
        List<Long> result = redisTemplate.execute(INCR_WINDOW_SCRIPT,
                List.of(key), String.valueOf(window.toMillis()));
        long count = result.get(0);
        long ttlMillis = result.get(1);
        if (count <= limit) {
            return RateResult.ok();
        }
        return RateResult.rejected(Math.max(ttlMillis, 0));
    }

    /**
     * 读取剩余冷却毫秒（不计数）：用于判断"验证码重发冷却"。
     *
     * @param key 冷却键
     * @return 剩余毫秒；无冷却返回 0
     */
    public long remainingMillis(String key) {
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl < 0) {
            return 0;
        }
        return ttl * 1000L;
    }

    /**
     * 设置冷却键（验证码发送成功后调用）。
     *
     * @param key    冷却键
     * @param window 冷却时长
     */
    public void markCooldown(String key, Duration window) {
        redisTemplate.opsForValue().set(key, "1", window);
    }

    /**
     * 限流结果。
     *
     * @param allowed            是否放行
     * @param retryAfterMillis   被拒时建议的等待毫秒（响应头 Retry-After）
     */
    public record RateResult(boolean allowed, long retryAfterMillis) {

        /**
         * @return 放行结果
         */
        public static RateResult ok() {
            return new RateResult(true, 0);
        }

        /**
         * @param retryAfterMillis 建议等待毫秒
         * @return 拒绝结果
         */
        public static RateResult rejected(long retryAfterMillis) {
            return new RateResult(false, retryAfterMillis);
        }
    }
}

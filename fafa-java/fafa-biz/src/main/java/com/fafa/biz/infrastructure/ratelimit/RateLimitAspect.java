package com.fafa.biz.infrastructure.ratelimit;

import com.fafa.biz.infrastructure.cache.RedisRateLimiter;
import com.fafa.common.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@link RateLimit} 切面：方法进入前执行 Redis 固定窗口计数，任一规则拒绝即抛业务异常。
 *
 * <p>计数逻辑复用 {@link RedisRateLimiter}（Lua 脚本保证 INCR + PEXPIRE 原子性，
 * 架构规范 §3 fail-close 口径）；key 由注解上的 SpEL 表达式基于方法参数求值。</p>
 *
 * <p>阈值解析顺序：配置键（limitProperty/windowProperty）优先于注解常量；
 * 配置键缺失或非正数、SpEL 结果为空，均视为注解配置错误，直接抛异常终止请求。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;
    private final Environment environment;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * @param rateLimiter Redis 固定窗口限流器
     * @param environment Spring 环境配置（解析阈值配置键）
     */
    public RateLimitAspect(RedisRateLimiter rateLimiter, Environment environment) {
        this.rateLimiter = rateLimiter;
        this.environment = environment;
    }

    /**
     * 环绕拦截：兼容单注解与叠加注解（编译后收拢为 {@link RateLimits} 容器）。
     *
     * @param joinPoint 连接点
     * @return 业务方法返回值
     * @throws Throwable 业务异常原样透出
     */
    @Around("@annotation(com.fafa.biz.infrastructure.ratelimit.RateLimit) "
            + "|| @annotation(com.fafa.biz.infrastructure.ratelimit.RateLimits)")
    public Object rejectIfLimited(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        for (RateLimit rule : method.getAnnotationsByType(RateLimit.class)) {
            check(rule, method, joinPoint.getArgs(), joinPoint.getTarget());
        }
        return joinPoint.proceed();
    }

    /**
     * 校验单条规则：命中拒绝时抛出注解指定的错误码（文案缺省用错误码默认文案）。
     */
    private void check(RateLimit rule, Method method, Object[] args, Object target) {
        String key = buildKey(rule, method, args, target);
        RedisRateLimiter.RateResult result = rateLimiter.tryAcquire(
                key, resolveLimit(rule), Duration.ofSeconds(resolveWindowSeconds(rule)));
        if (!result.allowed()) {
            throw new BizException(rule.error(),
                    rule.message().isBlank() ? rule.error().getMessage() : rule.message());
        }
    }

    /**
     * 求值限流键：SpEL 上下文绑定方法参数（依赖编译参数 -parameters）。
     */
    private String buildKey(RateLimit rule, Method method, Object[] args, Object target) {
        EvaluationContext context =
                new MethodBasedEvaluationContext(target, method, args, parameterNameDiscoverer);
        Object value = parser.parseExpression(rule.key()).getValue(context);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalStateException("RateLimit key 表达式结果为空：" + rule.key());
        }
        return String.valueOf(value);
    }

    /**
     * @return 有效限次：配置键优先，其次注解常量
     */
    private int resolveLimit(RateLimit rule) {
        int limit = rule.limitProperty().isBlank()
                ? rule.limit()
                : environment.getProperty(rule.limitProperty(), Integer.class, -1);
        if (limit <= 0) {
            throw new IllegalStateException(
                    "RateLimit 限次无效：limitProperty=" + rule.limitProperty() + ", limit=" + limit);
        }
        return limit;
    }

    /**
     * @return 有效窗口秒数：配置键优先（按 windowUnit 换算），其次注解常量
     */
    private long resolveWindowSeconds(RateLimit rule) {
        long seconds = rule.windowProperty().isBlank()
                ? rule.windowSeconds()
                : TimeUnit.SECONDS.convert(
                        environment.getProperty(rule.windowProperty(), Long.class, -1L), rule.windowUnit());
        if (seconds <= 0) {
            throw new IllegalStateException(
                    "RateLimit 窗口无效：windowProperty=" + rule.windowProperty()
                            + ", windowSeconds=" + seconds);
        }
        return seconds;
    }
}

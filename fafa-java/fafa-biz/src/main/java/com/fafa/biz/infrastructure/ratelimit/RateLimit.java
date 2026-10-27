package com.fafa.biz.infrastructure.ratelimit;

import com.fafa.biz.domain.AuthErrorCode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法级 Redis 固定窗口限流注解（架构规范 §3：限流在 Redis 完成，脚本失败视为放行失败）。
 *
 * <p>由 {@link RateLimitAspect} 拦截执行：方法进入前按 key 自增计数，
 * 超过窗口内上限直接抛出 {@link com.fafa.common.exception.BizException}，
 * 不进入业务逻辑（与原 {@code rejectIfLimited} 语义一致）。</p>
 *
 * <p>可叠加多个注解实现多维度限流（如登录的 IP + 邮箱双维度）：
 * 按声明顺序依次校验，任一命中拒绝即整体拒绝并抛出对应错误码。</p>
 *
 * <p>key 为 SpEL 表达式，可引用方法参数（依赖编译参数 {@code -parameters}，
 * 已在父工程编译插件统一开启），例：</p>
 * <pre>{@code
 * @RateLimit(key = "'fafa:auth:rate:register:ip:' + #clientIp", limit = 10, windowSeconds = 3600)
 * }</pre>
 *
 * <p>阈值两种口径二选一：直接写死 {@link #limit()}/{@link #windowSeconds()}；
 * 或指定配置键 {@link #limitProperty()}/{@link #windowProperty()}（优先级更高，
 * 满足手册 §11"限流阈值配置化，修改只动配置不改代码"）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Documented
@Repeatable(RateLimits.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {

    /**
     * Redis 限流键（SpEL）：可引用方法参数或做简单变换，
     * 如 {@code "'fafa:auth:rate:login:email:' + #email?.trim()?.toLowerCase()"}。
     * 表达式结果为空视为配置错误，直接抛出异常（fail-fast）。
     */
    String key();

    /**
     * 窗口内最大放行次数。
     * 设置 {@link #limitProperty()} 时本属性被忽略。
     */
    int limit() default -1;

    /**
     * 限次配置键（从 Spring Environment 读取），设置后优先于 {@link #limit()}。
     * 配置缺失或非正数视为注解配置错误。
     */
    String limitProperty() default "";

    /**
     * 窗口长度（秒）。
     * 设置 {@link #windowProperty()} 时本属性被忽略。
     */
    long windowSeconds() default -1;

    /**
     * 窗口配置键（从 Spring Environment 读取），设置后优先于 {@link #windowSeconds()}；
     * 数值单位由 {@link #windowUnit()} 指定。
     * 配置缺失或非正数视为注解配置错误。
     */
    String windowProperty() default "";

    /**
     * {@link #windowProperty()} 读到的数值单位，默认秒。
     */
    TimeUnit windowUnit() default TimeUnit.SECONDS;

    /**
     * 拒绝时抛出的业务错误码（注解当前面向认证域，错误码归属各业务域枚举）。
     */
    AuthErrorCode error() default AuthErrorCode.AUTH_REQUEST_RATE_LIMITED;

    /**
     * 拒绝文案覆盖；留空则使用 {@link #error()} 的默认文案。
     */
    String message() default "";
}

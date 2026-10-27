package com.fafa.biz.infrastructure.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link RateLimit} 可重复注解的容器（Java 语言要求，勿直接使用）。
 *
 * <p>同一方法叠加多个 {@code @RateLimit} 时由编译器自动收拢到本注解，
 * 切面按 {@code getAnnotationsByType} 取回原始声明顺序。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimits {

    /**
     * @return 叠加的限流规则（按声明顺序）
     */
    RateLimit[] value();
}

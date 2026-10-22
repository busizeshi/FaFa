package com.fafa.admin.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 管理会话、认证节流和分页参数。 */
@Validated
@ConfigurationProperties("fafa.admin")
public record AdminPolicy(@Min(60) int sessionTtlSeconds, @Min(60) int loginWindowSeconds,
        @Min(1) int loginAttemptLimit, @Min(1) int defaultPageSize, @Min(1) int maxPageSize,
        @Min(16384) int passwordMemoryKib, @Min(2) int passwordIterations) { }

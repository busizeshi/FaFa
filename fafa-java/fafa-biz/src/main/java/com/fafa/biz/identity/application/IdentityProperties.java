package com.fafa.biz.identity.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("fafa.identity")
public record IdentityProperties(
        @NotBlank @Size(min = 32) String hmacSecret,
        @NotBlank String mailFrom,
        @Min(60) int challengeTtlSeconds,
        @Min(1) int resendSeconds,
        @Min(1) int maxAttempts,
        @Min(1) int emailDailyLimit,
        @Min(1) int ipHourlyLimit,
        @Min(1) int loginWindowLimit,
        @Min(60) int resetTicketTtlSeconds,
        @NotBlank String defaultTimezone,
        @Min(16384) int passwordMemoryKib,
        @Min(2) int passwordIterations) {
    @Override public String toString() { return "IdentityProperties[REDACTED]"; }
}

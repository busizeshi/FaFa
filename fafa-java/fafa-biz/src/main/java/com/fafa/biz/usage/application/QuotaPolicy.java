package com.fafa.biz.usage.application;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("fafa.quota")
public record QuotaPolicy(@Min(0) long initialStorageBytes) { }

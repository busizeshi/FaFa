package com.fafa.biz.pet.application;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 生命周期和分页策略由部署配置控制。 */
@Validated
@ConfigurationProperties("fafa.pet")
public record PetPolicy(@Min(1) int deletionWaitingDays) { }

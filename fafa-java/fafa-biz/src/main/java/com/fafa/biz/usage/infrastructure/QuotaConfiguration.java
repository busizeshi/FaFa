package com.fafa.biz.usage.infrastructure;

import com.fafa.biz.usage.application.QuotaPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QuotaPolicy.class)
public class QuotaConfiguration { }

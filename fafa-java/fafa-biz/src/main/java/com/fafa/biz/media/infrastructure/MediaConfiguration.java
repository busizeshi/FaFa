package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.application.MediaPolicy;
import com.fafa.biz.media.application.ProcessingPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 媒体配置和持久化清理扫描入口。 */
@Configuration(proxyBeanMethods=false)
@EnableScheduling
@EnableConfigurationProperties({MediaPolicy.class,ProcessingPolicy.class})
public class MediaConfiguration { }

package com.fafa.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 外部 HTTP 客户端配置（统一 RestClient，禁止散用 Hutool HTTP）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Configuration
public class RestClientConfig {

    /**
     * 调用 fafa-python 的客户端：注入内网令牌，读超时较长以覆盖 AI 推理耗时
     */
    @Bean
    public RestClient pythonRestClient(RestClient.Builder builder, FaFaProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        return builder
                .baseUrl(properties.getPython().getBaseUrl())
                .defaultHeader("X-Internal-Token", properties.getInternalToken())
                .requestFactory(factory)
                .build();
    }

    /**
     * 调用微信接口的客户端
     */
    @Bean
    public RestClient wechatRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(factory).build();
    }
}

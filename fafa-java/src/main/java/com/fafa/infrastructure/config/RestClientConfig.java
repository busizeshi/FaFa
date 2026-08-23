package com.fafa.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

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
     *
     * 微信开放接口的响应 body 是 JSON，但 Content-Type 头却返回 text/plain，
     * 默认 Jackson 转换器只认 application/json 会抛 UnknownContentTypeException，
     * 因此注册一个同时支持 text/plain 的 Jackson 转换器（须用 Spring 管理的
     * ObjectMapper，才能反序列化 record）。
     */
    @Bean
    public RestClient wechatRestClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        jsonConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));

        return builder
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, jsonConverter))
                .build();
    }
}

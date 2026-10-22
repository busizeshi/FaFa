package com.fafa.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** 仅用于 Servlet 的请求上下文配置；响应式 Gateway 使用自己的 WebFilter。 */
@Configuration(proxyBeanMethods = false)
public class CommonWebMvcConfiguration {
    /** 创建通用 Servlet 配置。 */
    public CommonWebMvcConfiguration() {
    }

    /** 在应用处理器之前注册 Servlet 请求 ID 过滤器。
     *
     * @return 已配置的过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}

package com.fafa.infrastructure.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截配置 + 内部令牌拦截配置
 *
 * 拦截规则：除登录、探活、内部回调、监控文档路径外全部鉴权。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final InternalTokenInterceptor internalTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户会话鉴权：只对真实 Controller 方法校验登录态；
        // 未匹配到 Controller 的路径（handler 为资源处理器）放行，由其抛 404，
        // 避免「路径打错」被误报为「未登录」
        registry.addInterceptor(new SaInterceptor(handler -> {
                    if (handler instanceof HandlerMethod) {
                        StpUtil.checkLogin();
                    }
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证与探活
                        "/auth/wechat/login",
                        "/api/ping",
                        // Python 服务回调与内部只读接口（由内部令牌保护）
                        "/api/callback/**",
                        "/internal/**",
                        // 监控与文档
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/favicon.ico",
                        "/error"
                );

        // Python 内部调用令牌校验：仅覆盖服务间接口
        registry.addInterceptor(internalTokenInterceptor)
                .addPathPatterns("/api/callback/**", "/internal/**");
    }
}

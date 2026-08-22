package com.fafa.infrastructure.config;

import com.fafa.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 服务间内部令牌拦截器
 *
 * fafa-python 调用 Java 的回调/内部接口时必须携带 X-Internal-Token，
 * 与配置 fafa.internal-token 一致，否则返回 401。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class InternalTokenInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final FaFaProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader(TOKEN_HEADER);
        if (Objects.equals(token, properties.getInternalToken()) && token != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                com.fafa.common.Result.fail(ErrorCode.UNAUTHORIZED)));
        return false;
    }
}

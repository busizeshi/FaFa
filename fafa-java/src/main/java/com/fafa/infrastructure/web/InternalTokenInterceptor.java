package com.fafa.infrastructure.web;

import cn.hutool.core.util.StrUtil;
import com.fafa.domain.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部服务令牌校验拦截器
 *
 * Python 服务回调 Java（/api/callback/**）时必须携带 X-Internal-Token 头，
 * 与 Java 调用 Python 时携带的令牌为同一配置值。
 * 依赖网络隔离（8000/内网不暴露公网）作为第二道防线。
 */
@Component
@RequiredArgsConstructor
public class InternalTokenInterceptor implements HandlerInterceptor {

    @Value("${fafa.internal-token}")
    private String internalToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("X-Internal-Token");
        if (StrUtil.isBlank(token) || !internalToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }

    /** 供校验失败的调用方获取错误语义 */
    public static ErrorCode denied() {
        return ErrorCode.UNAUTHORIZED;
    }
}

package com.fafa.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 为 Servlet 请求补充请求 ID，并将它写入响应。 */
public class RequestIdFilter extends OncePerRequestFilter {
    /** 创建请求 ID 过滤器。 */
    public RequestIdFilter() {
    }

    /** 为当前请求写入校验后的请求 ID，并放入日志上下文。
     *
     * @param request 当前 Servlet 请求
     * @param response 当前 Servlet 响应
     * @param filterChain 剩余的 Servlet 过滤器链和处理器
     * @throws ServletException 下游 Servlet 处理失败时抛出
     * @throws IOException 请求或响应发生 I/O 错误时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestIdContext.resolve(request.getHeader(RequestIdContext.HEADER_NAME));
        response.setHeader(RequestIdContext.HEADER_NAME, requestId);
        MDC.put(RequestIdContext.MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }
}

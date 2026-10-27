package com.fafa.biz.interfaces;

import com.fafa.common.constant.HeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求关联 ID 过滤器：把网关透传的 X-Request-Id 写入 MDC，供日志 pattern 输出。
 *
 * <p>链路约定（架构规范 §11.2）：requestId 由 Gateway 生成/沿用并强制注入下行请求头，
 * 本过滤器只读取；若直连调试（未经过网关）则缺省为 "-"，不自行生成，避免链路语义混乱。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /** MDC 中 requestId 的键名（与 logback pattern 中 %X{requestId} 对应） */
    public static final String MDC_REQUEST_ID = "requestId";

    /** {@inheritDoc} */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = request.getHeader(HeaderConstants.REQUEST_ID);
            if (requestId != null && !requestId.isBlank() && requestId.length() <= 64) {
                MDC.put(MDC_REQUEST_ID, requestId);
            }
            filterChain.doFilter(request, response);
        } finally {
            // 线程复用前清理，防止串号
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}

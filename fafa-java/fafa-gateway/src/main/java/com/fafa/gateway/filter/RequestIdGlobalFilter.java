package com.fafa.gateway.filter;

import com.fafa.common.constant.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求关联 ID（requestId）过滤器：生成/透传请求 ID 并输出访问日志。
 *
 * <p>规则（架构规范 §6.1 / §11.2）：</p>
 * <ul>
 *   <li>客户端已携带 {@code X-Request-Id} 时沿用（支持端到端串联），否则生成 UUID；</li>
 *   <li>requestId 写回响应头，客户端报障时可凭此定位日志；</li>
 *   <li>requestId 透传至下游服务（fafa-biz / fafa-admin / Python），纳入统一 JSON 日志；</li>
 *   <li>请求完成后输出访问日志：方法、路径、状态码、耗时。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);

    /** 请求开始时间在 exchange attribute 中的键 */
    private static final String ATTR_START_TIME = "fafa.request.startTime";

    /**
     * @return 伪身份头剥离（HIGHEST_PRECEDENCE）之后立即执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 注入 requestId、记录开始时间，并在响应完成后输出访问日志。
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return 异步放行信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = resolveRequestId(exchange);
        long startMillis = System.currentTimeMillis();

        // 请求头注入 requestId（透传下游），并把开始时间挂到 exchange 属性
        var request = exchange.getRequest().mutate()
                .header(HeaderConstants.REQUEST_ID, requestId)
                .build();
        exchange.getAttributes().put(ATTR_START_TIME, startMillis);

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getResponse().getHeaders().set(HeaderConstants.REQUEST_ID, requestId);

        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            long costMillis = System.currentTimeMillis() - startMillis;
            // 访问日志：生产环境 INFO 级别，配合 traceId/requestId 建立完整链路视图
            log.info("[网关访问] {} {} -> {} {}ms requestId={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    mutatedExchange.getResponse().getStatusCode(),
                    costMillis,
                    requestId);
        }));
    }

    /**
     * 取出或生成请求关联 ID。
     *
     * @param exchange 当前请求上下文
     * @return 客户端传入的 requestId（非空校验后）或新生成的 UUID
     */
    private String resolveRequestId(ServerWebExchange exchange) {
        String inbound = exchange.getRequest().getHeaders().getFirst(HeaderConstants.REQUEST_ID);
        if (inbound != null && !inbound.isBlank() && inbound.length() <= 64) {
            // 限定长度防止恶意超长头；仅作关联标识使用，不参与身份判定
            return inbound;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}

package com.fafa.gateway.filter;

import com.fafa.common.constant.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全头过滤：剥离客户端自带的伪造身份头（架构规范 §4.1 强制要求）。
 *
 * <p>背景：客户端提交的 {@code X-User-Id}、{@code X-Role} 等请求头属于不可信输入。
 * 若不在网关统一剥离，下游服务一旦误读该头作为身份依据，将造成越权漏洞。
 * 服务端身份只能来自 Sa-Token 会话校验结果。</p>
 *
 * <p>本过滤器在路由前执行（最高优先级），确保伪造头不进入任何下游链路。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class HeaderSanitizeGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(HeaderSanitizeGlobalFilter.class);

    /**
     * @return 最高优先级（数值最小），保证在其他过滤器之前执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 移除黑名单身份头并放行。
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return 异步放行信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 逐个剥离黑名单头；ServerHttpRequest 不可变，需通过 builder 重建
        var sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    for (String header : HeaderConstants.FORBIDDEN_IDENTITY_HEADERS) {
                        if (headers.containsKey(header)) {
                            // 记录剥离行为，便于排查客户端误传或攻击试探
                            log.warn("[网关安全] 已剥离客户端伪造身份头: header={}, uri={}",
                                    header, exchange.getRequest().getURI().getPath());
                            headers.remove(header);
                        }
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }
}

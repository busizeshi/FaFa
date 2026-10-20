package com.fafa.gateway.web;

import com.fafa.common.web.RequestIdContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = RequestIdContext.resolve(
                exchange.getRequest().getHeaders().getFirst(RequestIdContext.HEADER_NAME));
        ServerWebExchange forwarded = exchange.mutate()
                .request(exchange.getRequest().mutate().header(RequestIdContext.HEADER_NAME, requestId).build())
                .response(exchange.getResponse())
                .build();
        forwarded.getResponse().beforeCommit(() -> {
            forwarded.getResponse().getHeaders().set(RequestIdContext.HEADER_NAME, requestId);
            return Mono.empty();
        });
        return chain.filter(forwarded);
    }
}

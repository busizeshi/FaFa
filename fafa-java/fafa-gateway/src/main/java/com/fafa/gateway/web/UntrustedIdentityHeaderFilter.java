package com.fafa.gateway.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class UntrustedIdentityHeaderFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest().mutate().headers(headers -> {
            for (String name : headers.keySet().toArray(String[]::new)) {
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (lower.equals("x-user-id") || lower.equals("x-operator-id")
                        || lower.startsWith("x-internal-") || lower.startsWith("x-delegation-")
                        || lower.equals("x-fafa-client-ip")) {
                    headers.remove(name);
                }
            }
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                headers.set("X-Fafa-Client-IP", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}

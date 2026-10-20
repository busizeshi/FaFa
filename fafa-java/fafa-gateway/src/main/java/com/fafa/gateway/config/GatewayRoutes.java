package com.fafa.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GatewayRoutes {
    @Bean
    RouteLocator fafaRoutes(
            RouteLocatorBuilder builder,
            @Value("${fafa.gateway.admin-uri:http://localhost:8081}") String adminUri,
            @Value("${fafa.gateway.biz-uri:http://localhost:8082}") String bizUri,
            @Value("${fafa.gateway.python-uri:http://localhost:8000}") String pythonUri) {
        return builder.routes()
                .route("fafa-admin", route -> route.path("/api/admin/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(adminUri))
                .route("fafa-biz", route -> route.path("/api/biz/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(bizUri))
                .route("fafa-python", route -> route.path("/api/ai/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(pythonUri))
                .build();
    }
}

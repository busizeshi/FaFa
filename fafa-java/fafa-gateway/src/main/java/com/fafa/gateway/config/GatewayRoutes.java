package com.fafa.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class GatewayRoutes {
    @Bean
    RouteLocator fafaRoutes(
            RouteLocatorBuilder builder,
            @Value("${fafa.gateway.admin-uri:http://localhost:8081}") String adminUri,
            @Value("${fafa.gateway.biz-uri:http://localhost:8082}") String bizUri,
            @Value("${fafa.gateway.python-uri:http://localhost:8000}") String pythonUri,
            @Value("${fafa.gateway.legacy-routes-enabled:false}") boolean legacyRoutes,
            Environment environment) {
        var routes = builder.routes()
                .route("fafa-admin-v1", route -> route.path("/api/admin/v1/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(adminUri))
                .route("fafa-app-v1", route -> route.path("/api/v1/**")
                        .filters(filters -> filters.stripPrefix(1))
                        .uri(bizUri));
        if (legacyRoutes && environment.matchesProfiles("dev & !prod")) {
            routes.route("legacy-admin", route -> route.path("/api/admin/**")
                        .filters(filters -> filters.stripPrefix(2)).uri(adminUri))
                .route("legacy-biz", route -> route.path("/api/biz/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(bizUri))
                .route("legacy-python", route -> route.path("/api/ai/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri(pythonUri));
        }
        return routes.build();
    }
}

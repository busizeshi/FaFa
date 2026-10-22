package com.fafa.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:gateway-test.yml")
@AutoConfigureWebTestClient
class GatewayRoutingTest {
    private static final HttpServer DOWNSTREAM = startServer();
    @Autowired WebTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        String uri = "http://127.0.0.1:" + DOWNSTREAM.getAddress().getPort();
        properties.add("fafa.gateway.biz-uri", () -> uri);
        properties.add("fafa.gateway.admin-uri", () -> uri);
    }
    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", request -> {
                String body = request.getRequestURI().getPath() + "|user=" + request.getRequestHeaders().getFirst("X-User-ID")
                        + "|operator=" + request.getRequestHeaders().getFirst("X-Operator-ID")
                        + "|internal=" + request.getRequestHeaders().getFirst("X-Internal-Authorization")
                        + "|ip=" + request.getRequestHeaders().getFirst("X-Fafa-Client-IP");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                request.sendResponseHeaders(200, bytes.length);
                try (var output = request.getResponseBody()) { output.write(bytes); }
            });
            server.start();
            return server;
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    @AfterAll static void stop() { DOWNSTREAM.stop(0); }

    @Test void v1PrefixesReachActualDownstreamPaths() {
        client.get().uri("/api/v1/me").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> org.assertj.core.api.Assertions.assertThat(body).startsWith("/v1/me|"));
        client.get().uri("/api/admin/v1/me").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> org.assertj.core.api.Assertions.assertThat(body).startsWith("/v1/me|"));
    }
    @Test void legacyAndInternalPathsAreNotPublicEvenIfFlagIsSetWithoutLocalProfile() {
        for (String path : new String[]{"/api/biz/internal/biz/ping", "/api/ai/api/v1/health", "/internal/v1/me"}) {
            client.get().uri(path).exchange().expectStatus().isNotFound()
                    .expectBody().jsonPath("$.code").isEqualTo("FAFA-40400");
        }
    }
    @Test void forgedIdentityHeadersAreRemovedAndRequestIdIsPreserved() {
        client.get().uri("/api/v1/me").header("X-User-ID", "victim").header("X-Operator-ID", "admin")
                .header("X-Internal-Authorization", "fake").header("X-Fafa-Client-IP", "spoof")
                .header("X-Request-ID", "routing-test").exchange().expectStatus().isOk()
                .expectHeader().valueEquals("X-Request-ID", "routing-test")
                .expectBody(String.class).value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .contains("|user=null|operator=null|internal=null|").doesNotContain("spoof"));
    }
}

package com.fafa.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.common.rpc.RpcAuthentication;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminPolicy.class)
@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme = "bearer")
public class AdminConfiguration {
    @Bean public Clock adminClock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder adminPasswordEncoder(AdminPolicy policy) {
        return new Argon2PasswordEncoder(16, 32, 1, policy.passwordMemoryKib(), policy.passwordIterations());
    }
    @Bean public RpcAuthentication adminRpcAuthentication(@Value("${fafa.internal-rpc.secret}") String secret,
            @Value("${fafa.internal-rpc.max-skew-seconds:120}") long skew, Clock clock, ObjectMapper json) {
        return new RpcAuthentication(secret, clock, skew, json);
    }
}

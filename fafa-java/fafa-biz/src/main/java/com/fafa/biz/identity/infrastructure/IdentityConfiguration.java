package com.fafa.biz.identity.infrastructure;

import cn.dev33.satoken.stp.StpLogic;
import com.fafa.biz.identity.application.IdentityProperties;
import java.time.Clock;
import java.security.SecureRandom;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
@MapperScan("com.fafa.biz.identity.infrastructure.persistence")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class IdentityConfiguration {
    @Bean public Clock identityClock() { return Clock.systemUTC(); }
    @Bean public SecureRandom identityRandom() { return new SecureRandom(); }
    @Bean public StpLogic appStpLogic() { return new StpLogic("app"); }
    @Bean public PasswordEncoder passwordEncoder(IdentityProperties properties) {
        return new Argon2PasswordEncoder(16, 32, 1,
                properties.passwordMemoryKib(), properties.passwordIterations());
    }
}

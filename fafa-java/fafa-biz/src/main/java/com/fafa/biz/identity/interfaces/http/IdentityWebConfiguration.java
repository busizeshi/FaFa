package com.fafa.biz.identity.interfaces.http;

import com.fafa.biz.identity.application.AccountSessions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class IdentityWebConfiguration implements WebMvcConfigurer {
    private static final Set<String> PUBLIC = Set.of(
            "GET /v1/bootstrap", "GET /v1/legal-documents",
            "POST /v1/auth/email-challenges", "POST /v1/auth/registrations", "POST /v1/auth/sessions",
            "POST /v1/auth/password-reset-tickets", "POST /v1/auth/password-resets");
    private final AccountSessions sessions;
    public IdentityWebConfiguration(AccountSessions sessions) { this.sessions = sessions; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                if (!PUBLIC.contains(request.getMethod() + " " + request.getRequestURI())) sessions.currentAccount();
                return true;
            }
        }).addPathPatterns("/v1/**");
    }
}

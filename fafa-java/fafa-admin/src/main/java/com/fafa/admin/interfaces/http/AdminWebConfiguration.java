package com.fafa.admin.interfaces.http;

import com.fafa.admin.security.AdminSessions;
import com.fafa.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration(proxyBeanMethods = false)
@org.springframework.context.annotation.Profile("!admin-init")
public class AdminWebConfiguration implements WebMvcConfigurer {
    public static final String PRINCIPAL = AdminWebConfiguration.class.getName() + ".principal";
    private final AdminSessions sessions;
    public AdminWebConfiguration(AdminSessions sessions) { this.sessions = sessions; }
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                if ("POST".equals(request.getMethod()) && "/v1/auth/sessions".equals(request.getRequestURI())) return true;
                var principal = sessions.require(request.getHeader("Authorization"));
                request.setAttribute(PRINCIPAL, principal);
                if (handler instanceof HandlerMethod method) {
                    var required = method.getMethodAnnotation(AdminPermission.class);
                    boolean ownSession = "/v1/me".equals(request.getRequestURI())
                            || "/v1/auth/session".equals(request.getRequestURI());
                    if (required == null && !ownSession) throw new FafaException(CommonErrorCode.FORBIDDEN);
                    if (required != null) sessions.permission(principal, required.value());
                }
                return true;
            }
        }).addPathPatterns("/v1/**");
    }
    public static AdminSessions.Principal current(HttpServletRequest request) {
        var principal = request.getAttribute(PRINCIPAL);
        if (!(principal instanceof AdminSessions.Principal actor)) throw new FafaException(BusinessError.ADMIN_SESSION_INVALID);
        return actor;
    }
}

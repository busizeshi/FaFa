package com.fafa.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Servlet-only request context setup; reactive Gateway uses its own WebFilter. */
@Configuration(proxyBeanMethods = false)
public class CommonWebMvcConfiguration {
    /** Creates the common servlet configuration. */
    public CommonWebMvcConfiguration() {
    }

    /** Registers the servlet request ID filter before application handlers.
     *
     * @return the configured filter
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}

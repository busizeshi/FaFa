package com.fafa.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Adds a request ID to servlet requests and includes it in the response. */
public class RequestIdFilter extends OncePerRequestFilter {
    /** Creates a request ID filter. */
    public RequestIdFilter() {
    }

    /** Adds the validated request ID to the response and logging context for one request.
     *
     * @param request current servlet request
     * @param response current servlet response
     * @param filterChain remaining servlet filters and handler
     * @throws ServletException if downstream servlet processing fails
     * @throws IOException if request or response I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestIdContext.resolve(request.getHeader(RequestIdContext.HEADER_NAME));
        response.setHeader(RequestIdContext.HEADER_NAME, requestId);
        MDC.put(RequestIdContext.MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }
}

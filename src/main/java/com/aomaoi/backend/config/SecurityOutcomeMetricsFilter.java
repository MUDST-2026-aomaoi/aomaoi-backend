package com.aomaoi.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Records aggregate authentication/authorization rejections without request attributes. */
public class SecurityOutcomeMetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry registry;

    public SecurityOutcomeMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
                registry.counter("aomaoi.security.rejections", "kind", "authentication").increment();
            } else if (response.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                registry.counter("aomaoi.security.rejections", "kind", "authorization").increment();
            }
        }
    }
}

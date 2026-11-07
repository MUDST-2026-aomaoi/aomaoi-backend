package com.aomaoi.backend.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityOutcomeMetricsFilterTest {
    @Test
    void countsAuthenticationAndAuthorizationRejections() throws Exception {
        var registry = new SimpleMeterRegistry();
        var filter = new SecurityOutcomeMetricsFilter(registry);
        FilterChain unauthorized = (request, response) ->
                ((MockHttpServletResponse) response).setStatus(401);
        FilterChain forbidden = (request, response) ->
                ((MockHttpServletResponse) response).setStatus(403);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), unauthorized);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), forbidden);

        assertEquals(1.0, registry.get("aomaoi.security.rejections").tag("kind", "authentication").counter().count());
        assertEquals(1.0, registry.get("aomaoi.security.rejections").tag("kind", "authorization").counter().count());
    }
}

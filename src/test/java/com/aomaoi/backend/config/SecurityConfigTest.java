package com.aomaoi.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null);

    @Test
    void passwordEncoder_UsesBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
        String raw = "secretPassword";
        String encoded = encoder.encode(raw);
        assertNotEquals(raw, encoded);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    void corsConfigurationSource_PermitsWildcardOrigins() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workers");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertEquals(List.of("*"), config.getAllowedOrigins());
        assertEquals(List.of("*"), config.getAllowedMethods());
        assertEquals(List.of("*"), config.getAllowedHeaders());
    }
}

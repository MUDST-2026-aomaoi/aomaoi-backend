package com.aomaoi.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Exercises the real {@link SecurityConfig} filter chain (URL authorization rules,
 * 401 entry point, CORS) in a minimal web context. The JWT filter is replaced by a
 * stub that authenticates from an "X-Test-Role" header, so no database or token is needed.
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigRulesTest {

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class TestContext {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    String role = request.getHeader("X-Test-Role");
                    if (role != null) {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken("tester", null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                    }
                    filterChain.doFilter(request, response);
                }
            };
        }

        @Bean
        EchoController echoController() {
            return new EchoController();
        }
    }

    @Controller
    static class EchoController {
        @RequestMapping("/**")
        @ResponseBody
        String echo() {
            return "ok";
        }
    }

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new org.springframework.mock.web.MockServletContext());
        context.register(TestContext.class, SecurityConfig.class);
        context.refresh();

        Filter springSecurity = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurity).build();
    }

    private static MockHttpServletRequestBuilder withRole(MockHttpServletRequestBuilder builder, String role) {
        return builder.header("X-Test-Role", role);
    }

    private int status(MockHttpServletRequestBuilder builder) throws Exception {
        return mvc.perform(builder).andReturn().getResponse().getStatus();
    }

    // ---------- public endpoints ----------

    @Test
    void login_isPublic() throws Exception {
        assertEquals(200, status(post("/api/auth/login")));
    }

    @Test
    void errorEndpoint_isPublic() throws Exception {
        assertEquals(200, status(get("/error")));
    }

    @Test
    void optionsPreflight_isPublic() throws Exception {
        assertNotEquals(401, status(options("/api/workers")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")));
    }

    // ---------- unauthenticated ----------

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        assertEquals(401, status(get("/api/dashboard/overview")));
        assertEquals(401, status(post("/api/work-logs")));
        assertEquals(401, status(post("/api/farms")));
    }

    // ---------- any authenticated user ----------

    @Test
    void anyRequest_workerCanRead() throws Exception {
        assertEquals(200, status(withRole(get("/api/farms"), "WORKER")));
        assertEquals(200, status(withRole(get("/api/workers"), "WORKER")));
    }

    // ---------- superadmin only ----------

    @Test
    void superadminOnly_farmMutations() throws Exception {
        for (String role : List.of("ADMIN", "WORKER")) {
            assertEquals(403, status(withRole(post("/api/farms"), role)), "POST farms as " + role);
            assertEquals(403, status(withRole(put("/api/farms/1"), role)), "PUT farms as " + role);
            assertEquals(403, status(withRole(delete("/api/farms/1"), role)), "DELETE farms as " + role);
        }
        assertEquals(200, status(withRole(post("/api/farms"), "SUPERADMIN")));
        assertEquals(200, status(withRole(put("/api/farms/1"), "SUPERADMIN")));
        assertEquals(200, status(withRole(delete("/api/farms/1"), "SUPERADMIN")));
    }

    @Test
    void superadminOnly_adminMutations() throws Exception {
        for (String role : List.of("ADMIN", "WORKER")) {
            assertEquals(403, status(withRole(post("/api/admins"), role)), "POST admins as " + role);
            assertEquals(403, status(withRole(put("/api/admins/1"), role)), "PUT admins as " + role);
            assertEquals(403, status(withRole(delete("/api/admins/1"), role)), "DELETE admins as " + role);
        }
        assertEquals(200, status(withRole(post("/api/admins"), "SUPERADMIN")));
        assertEquals(200, status(withRole(put("/api/admins/1"), "SUPERADMIN")));
        assertEquals(200, status(withRole(delete("/api/admins/1"), "SUPERADMIN")));
    }

    @Test
    void superadminOnly_superadminNamespace() throws Exception {
        assertEquals(403, status(withRole(get("/api/superadmin/audit-logs"), "ADMIN")));
        assertEquals(200, status(withRole(get("/api/superadmin/audit-logs"), "SUPERADMIN")));
    }

    // ---------- admin or superadmin ----------

    @Test
    void adminOrSuperadmin_workerMutations() throws Exception {
        assertEquals(403, status(withRole(post("/api/workers"), "WORKER")));
        assertEquals(403, status(withRole(put("/api/workers/1"), "WORKER")));
        assertEquals(403, status(withRole(delete("/api/workers/1"), "WORKER")));
        assertEquals(403, status(withRole(post("/api/workers/1/reset-password"), "WORKER")));

        for (String role : List.of("ADMIN", "SUPERADMIN")) {
            assertEquals(200, status(withRole(post("/api/workers"), role)), "POST workers as " + role);
            assertEquals(200, status(withRole(put("/api/workers/1"), role)), "PUT workers as " + role);
            assertEquals(200, status(withRole(delete("/api/workers/1"), role)), "DELETE workers as " + role);
            assertEquals(200, status(withRole(post("/api/workers/1/reset-password"), role)),
                    "reset-password as " + role);
        }
    }

    @Test
    void adminOrSuperadmin_createWorkLog() throws Exception {
        assertEquals(403, status(withRole(post("/api/work-logs"), "WORKER")));
        assertEquals(200, status(withRole(post("/api/work-logs"), "ADMIN")));
        assertEquals(200, status(withRole(post("/api/work-logs"), "SUPERADMIN")));
    }

    // ---------- beans ----------

    @Test
    void passwordEncoder_isBCrypt() {
        PasswordEncoder encoder = new SecurityConfig(null).passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        assertTrue(encoder.matches("secret", encoder.encode("secret")));
        assertNotEquals("secret", encoder.encode("secret"));
    }

    @Test
    void authenticationManager_delegatesToConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertSame(authenticationManager,
                new SecurityConfig(null).authenticationManager(authenticationConfiguration));
    }

    @Test
    void corsConfigurationSource_allowsAllOriginsMethodsAndHeaders() {
        CorsConfigurationSource source = new SecurityConfig(null).corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals(List.of("*"), cors.getAllowedOrigins());
        assertEquals(List.of("*"), cors.getAllowedMethods());
        assertEquals(List.of("*"), cors.getAllowedHeaders());
    }
}

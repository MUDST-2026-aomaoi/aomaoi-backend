package com.aomaoi.backend.config;

import com.aomaoi.backend.service.CustomUserDetailsService;
import com.aomaoi.backend.service.E2eRunService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Loads the real {@link SecurityConfig} filter chain to cover its 401 authentication entry point. */
class SecurityConfigEntryPointTest {

    @Configuration
    @EnableWebMvc
    static class TestContext {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        BusinessOperationMetricsInterceptor businessMetrics(MeterRegistry registry) {
            return new BusinessOperationMetricsInterceptor(registry);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            // No Authorization header in these requests, so the real filter just passes them through.
            return new JwtAuthenticationFilter(mock(JwtUtil.class), mock(CustomUserDetailsService.class));
        }

        @Bean
        E2eRunService e2eRunService() {
            return mock(E2eRunService.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
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

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestContext.class, SecurityConfig.class);
        context.refresh();

        Filter springSecurity = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurity).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void unauthenticatedRequestToProtectedEndpoint_returns401() throws Exception {
        int status = mvc.perform(get("/api/farms")).andReturn().getResponse().getStatus();

        assertEquals(401, status);
    }

    @Test
    void loginEndpoint_isPublic() throws Exception {
        int status = mvc.perform(get("/api/auth/login")).andReturn().getResponse().getStatus();

        assertEquals(200, status);
    }
}

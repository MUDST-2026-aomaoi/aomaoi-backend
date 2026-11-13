package com.aomaoi.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final MeterRegistry meterRegistry;
    private final BusinessOperationMetricsInterceptor businessMetrics;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(businessMetrics);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            com.aomaoi.backend.service.E2eRunService runs,
            org.springframework.transaction.PlatformTransactionManager manager,
            org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/test-support/e2e-readiness").permitAll()
                .requestMatchers("/api/test-support/runs", "/api/test-support/runs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/test-support/daily-summary/run",
                        "/api/test-support/attendance/close-day").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                // Prometheus is exposed only on the private management port (not published by Compose).
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // SuperAdmin only: audit trail, and creating/editing/deleting farms & admins
                .requestMatchers("/api/superadmin/**").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.GET, "/api/worker-balances/me").hasRole("WORKER")
                .requestMatchers(HttpMethod.GET, "/api/salary-advances/me").hasRole("WORKER")
                .requestMatchers("/api/salary-advances", "/api/salary-advances/**",
                        "/api/financial-dashboard", "/api/worker-balances",
                        "/api/worker-balances/*").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admins", "/api/admins/**", "/api/farms", "/api/dashboard/**")
                    .hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/farms").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/farms/**").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/farms/**").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admins").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admins/**").hasRole("SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admins/**").hasRole("SUPERADMIN")
                // Admin (or SuperAdmin): managing workers and recording work logs
                .requestMatchers(HttpMethod.POST, "/api/workers/*/reset-password").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/workers").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/workers/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/workers/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/work-logs").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/attendance", "/api/attendance/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
            )
            .addFilterBefore(new SecurityOutcomeMetricsFilter(meterRegistry), LogoutFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new E2eRequestFilter(runs, manager, jdbc), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Automatically hashes passwords so they aren't stored in plain text
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

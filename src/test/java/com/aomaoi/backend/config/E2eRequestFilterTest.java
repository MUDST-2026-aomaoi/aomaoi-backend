package com.aomaoi.backend.config;

import com.aomaoi.backend.service.E2eRunService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class E2eRequestFilterTest {

    @Mock
    private E2eRunService runs;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private E2eRequestFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        filter = new E2eRequestFilter(runs, transactionManager, jdbc);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_TestSupportPath_BypassesFilter() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/test-support/e2e-readiness");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(runs);
    }

    @Test
    void doFilterInternal_OptionsMethod_BypassesFilter() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(runs);
    }

    @Test
    void doFilterInternal_NoHeader_NormalUser_PassesThrough() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-E2E-Run")).thenReturn(null);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("normal_user", "pass", Collections.emptyList()));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("normal_user"))).thenReturn(Boolean.FALSE);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternal_NoHeader_TestUser_Returns403() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-E2E-Run")).thenReturn(null);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("e2e_super_123", "pass", Collections.emptyList()));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("e2e_super_123"))).thenReturn(Boolean.TRUE);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(403, "Test account requires E2E run headers");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_WithHeader_Success() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-E2E-Run")).thenReturn("run-1");
        when(request.getHeader("X-E2E-Token")).thenReturn("token-1");

        filter.doFilterInternal(request, response, chain);

        verify(runs).enter("run-1", "token-1");
        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void doFilterInternal_WithHeader_ResponseStatusException_SendsError() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-E2E-Run")).thenReturn("run-1");
        when(request.getHeader("X-E2E-Token")).thenReturn("token-1");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid E2E run"))
                .when(runs).enter("run-1", "token-1");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(403, "Invalid E2E run");
    }

    @Test
    void doFilterInternal_WithHeader_RuntimeException_Sends409() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/api/workers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-E2E-Run")).thenReturn("run-1");
        when(request.getHeader("X-E2E-Token")).thenReturn("token-1");

        doThrow(new RuntimeException("Database conflict"))
                .when(runs).enter("run-1", "token-1");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(409, "E2E request rolled back; check ownership or backend logs");
    }
}

package com.aomaoi.backend.config;

import com.aomaoi.backend.service.E2eRunService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;

/** Registered only in the security chain. Normal requests keep their existing transaction behavior. */
public class E2eRequestFilter extends OncePerRequestFilter {
    private final E2eRunService runs;
    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;

    public E2eRequestFilter(E2eRunService runs, PlatformTransactionManager manager, JdbcTemplate jdbc) {
        this.runs = runs;
        this.tx = new TransactionTemplate(manager);
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/test-support/") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String id = request.getHeader("X-E2E-Run");
        if (id == null) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM users WHERE username=? AND e2e_run_id IS NOT NULL)",
                    Boolean.class, auth.getName()))) {
                response.sendError(403, "Test account requires E2E run headers");
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        var cached = new ContentCachingResponseWrapper(response);
        try {
            tx.executeWithoutResult(status -> {
                runs.enter(id, request.getHeader("X-E2E-Token"));
                try {
                    chain.doFilter(request, cached);
                    if (cached.getStatus() >= 400) status.setRollbackOnly();
                } catch (IOException | ServletException e) {
                    throw new IllegalStateException(e);
                }
            });
            cached.copyBodyToResponse();
        } catch (ResponseStatusException e) {
            response.sendError(e.getStatusCode().value(), "Invalid E2E run");
        } catch (RuntimeException e) {
            response.sendError(409, "E2E request rolled back; check ownership or backend logs");
        }
    }
}

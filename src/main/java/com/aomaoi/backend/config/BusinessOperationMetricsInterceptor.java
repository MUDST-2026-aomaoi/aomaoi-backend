package com.aomaoi.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Counts bounded business operation names without recording request or worker identifiers. */
@Component
public class BusinessOperationMetricsInterceptor implements HandlerInterceptor {
    private static final Map<String, String> OPERATIONS = Map.ofEntries(
            Map.entry("WorkLogController#addLog", "work_record_create"),
            Map.entry("SalaryAdvanceController#create", "salary_advance_create"),
            Map.entry("SalaryAdvanceController#cancel", "salary_advance_cancel"),
            Map.entry("WorkerBalanceController#list", "worker_balance_read"),
            Map.entry("WorkerBalanceController#me", "worker_balance_read"),
            Map.entry("WorkerBalanceController#byWorker", "worker_balance_read"),
            Map.entry("FinancialDashboardController#overview", "financial_dashboard_read"));

    private final MeterRegistry registry;

    public BusinessOperationMetricsInterceptor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String operation = operation(handler);
        if (operation != null) {
            registry.counter("aomaoi.business.operation.attempts", "operation", operation).increment();
            request.setAttribute("aomaoi.business.operation", operation);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception) {
        Object value = request.getAttribute("aomaoi.business.operation");
        if (!(value instanceof String operation)) return;
        String outcome = exception != null || response.getStatus() >= 500 ? "technical_failure"
                : response.getStatus() >= 400 ? "rejected" : "success";
        registry.counter("aomaoi.business.operation.outcomes", "operation", operation,
                "outcome", outcome).increment();
    }

    private String operation(Object handler) {
        if (!(handler instanceof HandlerMethod method)) return null;
        String key = method.getBeanType().getSimpleName() + "#" + method.getMethod().getName();
        return OPERATIONS.get(key);
    }
}

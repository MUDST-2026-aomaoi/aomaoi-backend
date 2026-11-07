package com.aomaoi.backend.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.aomaoi.backend.controller.WorkLogController;
import com.aomaoi.backend.controller.SalaryAdvanceController;
import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessOperationMetricsInterceptorTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessOperationMetricsInterceptor interceptor =
            new BusinessOperationMetricsInterceptor(registry);

    @Test
    void countsSuccessfulWorkRecordAfterControllerCompletion() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var handler = new HandlerMethod(org.mockito.Mockito.mock(WorkLogController.class),
                WorkLogController.class.getMethod("addLog", WorkLogRequestDTO.class));

        interceptor.preHandle(request, response, handler);
        response.setStatus(201);
        interceptor.afterCompletion(request, response, handler, null);

        assertEquals(1.0, registry.get("aomaoi.business.operation.attempts")
                .tag("operation", "work_record_create").counter().count());
        assertEquals(1.0, registry.get("aomaoi.business.operation.outcomes")
                .tags("operation", "work_record_create", "outcome", "success").counter().count());
    }

    @Test
    void countsClientRejectionsSeparatelyFromTechnicalFailures() throws Exception {
        var handler = new HandlerMethod(org.mockito.Mockito.mock(SalaryAdvanceController.class),
                SalaryAdvanceController.class.getMethod("create", SalaryAdvanceRequestDTO.class));
        var rejectedRequest = new MockHttpServletRequest();
        var rejectedResponse = new MockHttpServletResponse();
        interceptor.preHandle(rejectedRequest, rejectedResponse, handler);
        rejectedResponse.setStatus(400);
        interceptor.afterCompletion(rejectedRequest, rejectedResponse, handler, null);

        var failedRequest = new MockHttpServletRequest();
        var failedResponse = new MockHttpServletResponse();
        interceptor.preHandle(failedRequest, failedResponse, handler);
        failedResponse.setStatus(500);
        interceptor.afterCompletion(failedRequest, failedResponse, handler, null);

        assertEquals(1.0, registry.get("aomaoi.business.operation.outcomes")
                .tags("operation", "salary_advance_create", "outcome", "rejected").counter().count());
        assertEquals(1.0, registry.get("aomaoi.business.operation.outcomes")
                .tags("operation", "salary_advance_create", "outcome", "technical_failure").counter().count());
    }
}

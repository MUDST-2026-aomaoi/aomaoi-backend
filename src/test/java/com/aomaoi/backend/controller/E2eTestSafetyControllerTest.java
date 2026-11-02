package com.aomaoi.backend.controller;

import com.aomaoi.backend.service.E2eRunService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class E2eTestSafetyControllerTest {

    @Test
    void readinessUsesExplicitSupportConfiguration() {
        var service = mock(E2eRunService.class);
        var controller = new E2eTestSafetyController(service);
        assertEquals(false, controller.readiness().get("safeForE2e"));
        when(service.enabled()).thenReturn(true);
        assertEquals(true, controller.readiness().get("safeForE2e"));
        assertEquals("shared-tracked", controller.readiness().get("mode"));
    }

    @Test
    void create_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = new E2eTestSafetyController(service);
        when(service.create("test-key")).thenReturn(Map.of("id", "run-1", "token", "tok-1"));

        var result = controller.create("test-key");

        assertEquals("run-1", result.get("id"));
        assertEquals("tok-1", result.get("token"));
        verify(service).create("test-key");
    }

    @Test
    void heartbeat_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = new E2eTestSafetyController(service);
        doNothing().when(service).heartbeat("run-1", "tok-1");

        var result = controller.heartbeat("run-1", "tok-1");

        assertTrue(result.get("alive"));
        verify(service).heartbeat("run-1", "tok-1");
    }

    @Test
    void cleanup_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = new E2eTestSafetyController(service);
        when(service.cleanup("run-1", "test-key")).thenReturn(Map.of("deleted", 5, "remaining", 0));

        var result = controller.cleanup("run-1", "test-key");

        assertEquals(5, result.get("deleted"));
        assertEquals(0, result.get("remaining"));
        verify(service).cleanup("run-1", "test-key");
    }
}

package com.aomaoi.backend.controller;

import com.aomaoi.backend.service.E2eRunService;
import org.junit.jupiter.api.Test;
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
}

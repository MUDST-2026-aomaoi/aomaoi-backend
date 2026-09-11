package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.OverviewResponseDTO;
import com.aomaoi.backend.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void getOverview() {
        OverviewResponseDTO overview = new OverviewResponseDTO();
        overview.setTotalWorkers(10L);

        when(dashboardService.getOverview()).thenReturn(overview);

        ResponseEntity<OverviewResponseDTO> response = dashboardController.getOverview();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getTotalWorkers());
    }
}

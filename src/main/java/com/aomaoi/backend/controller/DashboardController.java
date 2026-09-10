package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.OverviewResponseDTO;
import com.aomaoi.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponseDTO> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }
}

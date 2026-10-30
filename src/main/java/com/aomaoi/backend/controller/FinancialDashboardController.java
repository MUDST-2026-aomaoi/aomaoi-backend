package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.FinancialDashboardResponseDTO;
import com.aomaoi.backend.service.FinancialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial-dashboard")
@RequiredArgsConstructor
public class FinancialDashboardController {
    private final FinancialDashboardService service;

    @GetMapping
    public FinancialDashboardResponseDTO overview() {
        return service.getForManager();
    }
}

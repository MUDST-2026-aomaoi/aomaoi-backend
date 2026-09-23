package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.OverviewResponseDTO;
import com.aomaoi.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * DashboardController - จัดการ API สำหรับหน้า Dashboard (ภาพรวมระบบ)
 *
 * Base URL: /api/dashboard
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/dashboard/overview - ดึงข้อมูลภาพรวมระบบ
     *
     * Response 200: {
     *   "totalWorkers": จำนวนคนงานทั้งหมด,
     *   "activeWorkers": จำนวนคนงานที่ active,
     *   "totalWorkLogs": จำนวนบันทึกงานทั้งหมด,
     *   "totalAllTime": ยอดค่าแรงรวมตั้งแต่เริ่มระบบ,
     *   "totalThisMonth": ยอดค่าแรงเดือนนี้,
     *   "todayWorkLogs": จำนวนบันทึกงานวันนี้,
     *   "workersToday": จำนวนคนงานที่ทำงานวันนี้
     * }
     */
    @GetMapping("/overview")
    public ResponseEntity<OverviewResponseDTO> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }
}

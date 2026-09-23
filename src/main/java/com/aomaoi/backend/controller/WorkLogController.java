package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.service.WorkLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * WorkLogController - จัดการ API เกี่ยวกับบันทึกผลงาน/ค่าแรง
 *
 * Base URL: /api/work-logs
 */
@RestController
@RequestMapping("/api/work-logs")
@CrossOrigin(origins = "*")
public class WorkLogController {

    private final WorkLogService workLogService;

    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    /**
     * GET /api/work-logs - ดึงบันทึกงานทั้งหมด (เรียงจากวันที่ล่าสุด)
     * - Admin: เห็นเฉพาะบันทึกของคนงานในฟาร์มตัวเอง
     * - SuperAdmin: เห็นทั้งหมด
     *
     * Response 200: [ { "id", "type", "date", "workerId", "total", ...ฟิลด์เฉพาะประเภท } ]
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLogs() {
        return ResponseEntity.ok(workLogService.getAllLogs());
    }

    /**
     * POST /api/work-logs - เพิ่มบันทึกงานใหม่
     *
     * ระบบจะคำนวณค่าแรง (total) ให้อัตโนมัติตามประเภทงาน:
     * - cutting: { "type": "cutting", "workerId", "date", "rows", "waPerRow" }
     * - planting: { "type": "planting", "workerId", "date", "furrows", "waPerFurrow" }
     * - watering: { "type": "watering", "workerId", "date", "startDate", "endDate", "dailyRate" }
     *   → ระบบจะคำนวณ days = (endDate - startDate + 1) ให้อัตโนมัติ
     * - spraying: { "type": "spraying", "workerId", "date", "tanks" }
     *
     * Response 200: { "id", "type", "date", "workerId", "total", "days", "startDate", "endDate", ... }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addLog(@RequestBody WorkLogRequestDTO dto) {
        return ResponseEntity.ok(workLogService.addLog(dto));
    }
}

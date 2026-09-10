package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-logs")
@CrossOrigin(origins = "*")
public class WorkLogController {

    @Autowired
    private WorkLogService workLogService;

    /**
     * POST /api/work-logs — บันทึกงานใหม่ พร้อมคำนวณค่าจ้างอัตโนมัติ
     */
    @PostMapping
    public ResponseEntity<?> createWorkLog(@RequestBody WorkLogRequestDTO dto) {
        try {
            WorkLog workLog = workLogService.createWorkLog(dto);
            return ResponseEntity.ok(workLog);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/work-logs — ดึงประวัติงานทั้งหมด
     */
    @GetMapping
    public ResponseEntity<List<WorkLog>> getAllWorkLogs() {
        return ResponseEntity.ok(workLogService.getAllWorkLogs());
    }

    /**
     * GET /api/work-logs/worker/{workerId} — ดึงประวัติงานตาม Worker ID
     */
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<WorkLog>> getWorkLogsByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(workLogService.getWorkLogsByWorkerId(workerId));
    }

    /**
     * GET /api/work-logs/search?nickname=xxx — ค้นหาประวัติงานจากชื่อเล่น
     */
    @GetMapping("/search")
    public ResponseEntity<List<WorkLog>> searchByNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(workLogService.getWorkLogsByNickname(nickname));
    }
}

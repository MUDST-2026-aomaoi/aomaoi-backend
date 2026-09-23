package com.aomaoi.backend.controller;

import com.aomaoi.backend.entity.AuditLog;
import com.aomaoi.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuditLogController - จัดการ API สำหรับดูประวัติการใช้งานระบบ (Audit Trail)
 *
 * Base URL: /api/superadmin/audit-logs
 * สิทธิ์การเข้าถึง: เฉพาะ SuperAdmin เท่านั้น
 */
@RestController
@RequestMapping("/api/superadmin/audit-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/superadmin/audit-logs - ดึงประวัติการใช้งานระบบทั้งหมด
     *
     * แสดงรายการว่า ใครทำอะไร เมื่อไหร่ เรียงจากล่าสุด
     * เช่น Admin คนไหนกด Reset Password ให้ Worker คนไหน เมื่อเวลาไหน
     *
     * Response 200: [ { "id", "action", "performedBy", "targetUser", "timestamp", "details" } ]
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }
}

package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WorkerController - จัดการ API เกี่ยวกับข้อมูลคนงาน
 *
 * Base URL: /api/workers
 */
@RestController
@RequestMapping("/api/workers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    /**
     * GET /api/workers - ดึงรายการคนงานทั้งหมด
     * - Admin: เห็นเฉพาะคนงานในฟาร์มตัวเอง
     * - SuperAdmin: เห็นทั้งหมด
     *
     * Response 200: [ { "id", "fullName", "nickname", "phone", "status", "farmId", "user": {...} } ]
     */
    @GetMapping
    public ResponseEntity<List<Worker>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    /**
     * GET /api/workers/{id} - ดึงข้อมูลคนงานตาม ID
     *
     * Response 200: { "id", "fullName", "nickname", "phone", "status", "farmId", "user": {...} }
     */
    @GetMapping("/{id}")
    public ResponseEntity<Worker> getWorkerById(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.getWorkerById(id));
    }

    /**
     * POST /api/workers - เพิ่มคนงานใหม่ (สร้าง User + Worker พร้อมกัน)
     *
     * Request Body: { "username", "tempPassword", "fullName", "nickname", "phone", "avatar" }
     * Response 200: Worker object ที่สร้างสำเร็จ
     * Response 500: RuntimeException ถ้า username ซ้ำ หรือมีช่องว่าง
     */
    @PostMapping
    public ResponseEntity<Worker> addWorker(@RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.addWorker(dto));
    }

    /**
     * PUT /api/workers/{id} - แก้ไขข้อมูลคนงาน
     *
     * Request Body: { "fullName", "nickname", "phone", "avatar", "username" }
     * Response 200: Worker object ที่อัปเดตแล้ว
     */
    @PutMapping("/{id}")
    public ResponseEntity<Worker> updateWorker(@PathVariable Long id, @RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.updateWorker(id, dto));
    }

    /**
     * DELETE /api/workers/{id} - ลบคนงาน (Soft delete: เปลี่ยน status เป็น inactive)
     *
     * Response 200: (empty body)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(@PathVariable Long id) {
        workerService.deleteWorker(id);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/workers/{id}/reset-password - Admin รีเซ็ตรหัสผ่านให้คนงาน
     *
     * รีเซ็ตรหัสผ่านของคนงานกลับเป็น "1234" (ค่าเริ่มต้น)
     * และบังคับให้เป็น First Login ใหม่ (ต้องเปลี่ยนรหัสผ่านตอนเข้าสู่ระบบครั้งถัดไป)
     * บันทึกประวัติลง Audit Log ว่า Admin คนไหนเป็นคนกดรีเซ็ต
     *
     * Response 200: (empty body)
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().build();
        }
        workerService.resetWorkerPassword(id, newPassword, authentication.getName());
        return ResponseEntity.ok().build();
    }
}

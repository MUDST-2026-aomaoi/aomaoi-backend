package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.service.AdminProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminProfileController - จัดการ API เกี่ยวกับโปรไฟล์ Admin (ผู้ดูแลฟาร์ม)
 *
 * Base URL: /api/admins
 */
@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminService;

    /**
     * GET /api/admins - ดึงรายการ Admin ทั้งหมด
     *
     * Response 200: [ { "id", "fullName", "phone", "farmId", "status", "user": {...} } ]
     */
    @GetMapping
    public ResponseEntity<List<AdminProfile>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    /**
     * GET /api/admins/{id} - ดึงข้อมูล Admin ตาม ID
     *
     * Response 200: { "id", "fullName", "phone", "farmId", "status", "user": {...} }
     * Response 500: RuntimeException ถ้าไม่พบ Admin
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminProfile> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }

    /**
     * POST /api/admins - เพิ่ม Admin ใหม่ (สร้าง User + AdminProfile พร้อมกัน)
     *
     * Request Body: { "username", "tempPassword", "fullName", "phone", "farmId", "avatar" }
     * Response 200: AdminProfile object ที่สร้างสำเร็จ
     * Response 500: RuntimeException ถ้า username ซ้ำ
     */
    @PostMapping
    public ResponseEntity<AdminProfile> addAdmin(@RequestBody AdminRequestDTO dto) {
        return ResponseEntity.ok(adminService.addAdmin(dto));
    }

    /**
     * PUT /api/admins/{id} - แก้ไขข้อมูล Admin
     *
     * Request Body: { "fullName", "phone", "farmId", "avatar", "username" }
     * Response 200: AdminProfile object ที่อัปเดตแล้ว
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminProfile> updateAdmin(@PathVariable Long id, @RequestBody AdminRequestDTO dto) {
        return ResponseEntity.ok(adminService.updateAdmin(id, dto));
    }

    /**
     * DELETE /api/admins/{id} - ลบ Admin (Soft delete: เปลี่ยน status เป็น inactive)
     *
     * Response 200: (empty body)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok().build();
    }
}

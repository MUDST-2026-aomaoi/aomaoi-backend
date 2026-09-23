package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.service.FarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FarmController - จัดการ API เกี่ยวกับข้อมูลฟาร์ม
 *
 * Base URL: /api/farms
 */
@RestController
@RequestMapping("/api/farms")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    /**
     * GET /api/farms - ดึงรายการฟาร์มทั้งหมด
     *
     * Response 200: [ { "id", "name", "location", "image", "adminCount", "workerCount", ... } ]
     */
    @GetMapping
    public ResponseEntity<List<Farm>> getAllFarms() {
        return ResponseEntity.ok(farmService.getAllFarms());
    }

    /**
     * POST /api/farms - เพิ่มฟาร์มใหม่
     *
     * Request Body: { "name": "ชื่อฟาร์ม", "location": "ที่ตั้ง", "image": "base64..." }
     * Response 200: Farm object ที่สร้างสำเร็จ พร้อม id
     */
    @PostMapping
    public ResponseEntity<Farm> addFarm(@RequestBody FarmRequestDTO dto) {
        return ResponseEntity.ok(farmService.addFarm(dto));
    }

    /**
     * PUT /api/farms/{id} - แก้ไขข้อมูลฟาร์ม
     *
     * Request Body: { "name": "ชื่อใหม่", "location": "ที่ตั้งใหม่", "image": "base64..." }
     * Response 200: Farm object ที่อัปเดตแล้ว
     */
    @PutMapping("/{id}")
    public ResponseEntity<Farm> updateFarm(@PathVariable Long id, @RequestBody FarmRequestDTO dto) {
        return ResponseEntity.ok(farmService.updateFarm(id, dto));
    }

    /**
     * DELETE /api/farms/{id} - ลบฟาร์ม (Soft delete: เปลี่ยน status เป็น inactive)
     *
     * Response 200: (empty body)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long id) {
        farmService.deleteFarm(id);
        return ResponseEntity.ok().build();
    }
}

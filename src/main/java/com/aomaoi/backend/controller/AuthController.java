package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController - จัดการ API เกี่ยวกับการยืนยันตัวตน (Authentication)
 *
 * Base URL: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login - เข้าสู่ระบบ
     *
     * รับ username และ password จาก Frontend แล้วตรวจสอบกับฐานข้อมูล
     * ถ้าถูกต้องจะสร้าง JWT Token ส่งกลับไป
     *
     * Request Body: { "username": "xxx", "password": "xxx" }
     * Response 200: { "success": true, "token": "jwt...", "user": { "username", "role", "fullName", "id" } }
     * Response 401: { "success": false, "message": "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง" }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest credentials) {
        try {
            Map<String, Object> response = authService.login(credentials);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * POST /api/auth/change-password - เปลี่ยนรหัสผ่านของตัวเอง
     *
     * ผู้ใช้ต้องล็อกอินอยู่แล้ว (ต้องแนบ JWT Token ใน Header)
     * ต้องส่งรหัสผ่านเก่ามาเช็คก่อนเปลี่ยน เพื่อป้องกันการยิง API โดยตรง
     *
     * Request Body: { "oldPassword": "xxx", "newPassword": "xxx" }
     * Response 200: { "success": true, "message": "Password changed successfully" }
     * Response 500: RuntimeException ถ้ารหัสผ่านเก่าไม่ถูกต้อง
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody com.aomaoi.backend.dto.ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        
        String username = authentication.getName();
        authService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }
}

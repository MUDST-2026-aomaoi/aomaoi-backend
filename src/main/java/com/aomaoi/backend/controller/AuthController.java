package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allows the React frontend to call this API without CORS blocking
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest credentials) {
        try {
            // Hand off the credentials to the AuthService to verify against the PostgreSQL database
            Map<String, Object> response = authService.login(credentials);
            
            // If successful, return 200 OK with the Token
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // If the password is wrong or user doesn't exist, Spring Security throws an exception.
            // We catch it here and return a clean 401 error message to React.
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody com.aomaoi.backend.dto.ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        
        String username = authentication.getName();
        authService.changePassword(username, request.getNewPassword());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }
}

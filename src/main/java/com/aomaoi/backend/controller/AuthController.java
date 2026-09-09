package com.aomaoi.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allows the React frontend to call this API without CORS blocking
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // This is a MOCK implementation to connect the frontend and backend.
        // We will move this logic to AuthService and check the database via AuthRepository later!
        Map<String, Object> response = new HashMap<>();

        // Simulating that any user typing "admin123" succeeds
        if ("admin123".equals(password)) {
            
            // Mock user details based on username
            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("isFirstLogin", true);
            
            if (username.equals("superadmin")) {
                user.put("role", "superadmin");
            } else if (username.startsWith("w")) {
                user.put("role", "worker");
            } else {
                user.put("role", "admin");
            }

            response.put("user", user);
            response.put("token", "mock-jwt-token-for-" + username); // Fake token for now

            return ResponseEntity.ok(response);
        } else {
            response.put("message", "รหัสผ่านไม่ถูกต้อง (Mock Backend)");
            return ResponseEntity.status(401).body(response);
        }
    }
}

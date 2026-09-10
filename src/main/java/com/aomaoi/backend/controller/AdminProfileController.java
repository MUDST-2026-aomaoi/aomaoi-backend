package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.service.AdminProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "*")
public class AdminProfileController {

    @Autowired
    private AdminProfileService adminService;

    @GetMapping
    public ResponseEntity<List<AdminProfile>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProfile> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }

    @PostMapping
    public ResponseEntity<AdminProfile> addAdmin(@RequestBody AdminRequestDTO dto) {
        return ResponseEntity.ok(adminService.addAdmin(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProfile> updateAdmin(@PathVariable Long id, @RequestBody AdminRequestDTO dto) {
        return ResponseEntity.ok(adminService.updateAdmin(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok().build();
    }
}

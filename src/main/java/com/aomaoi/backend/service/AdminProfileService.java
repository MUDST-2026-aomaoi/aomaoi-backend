package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.AdminProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminProfileRepository adminRepository;
    private final UserRepository userRepository;
    private final com.aomaoi.backend.repository.FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<AdminProfile> getAllAdmins() {
        return adminRepository.findAll();
    }

    public AdminProfile getAdminById(Long id) {
        return adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Transactional
    public AdminProfile addAdmin(AdminRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getTempPassword()));
        user.setRole("admin");
        user.setIsFirstLogin(true);
        user = userRepository.save(user);

        AdminProfile admin = new AdminProfile();
        admin.setFullName(dto.getFullName());
        admin.setPhone(dto.getPhone());
        admin.setFarmId(dto.getFarmId());
        admin.setAvatar(dto.getAvatar());
        admin.setStatus("pending");
        admin.setUser(user);

        if (dto.getFarmId() != null) {
            com.aomaoi.backend.entity.Farm farm = farmRepository.findById(Long.parseLong(dto.getFarmId())).orElse(null);
            if (farm != null) {
                farm.setAdminCount(farm.getAdminCount() + 1);
                farmRepository.save(farm);
            }
        }

        AdminProfile savedAdmin = adminRepository.save(admin);
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("CREATE_ADMIN", currentUsername, user.getUsername(), "Created new admin account");
        return savedAdmin;
    }

    @Transactional
    public AdminProfile updateAdmin(Long id, AdminRequestDTO dto) {
        AdminProfile admin = getAdminById(id);
        admin.setFullName(dto.getFullName());
        admin.setPhone(dto.getPhone());
        admin.setFarmId(dto.getFarmId());
        admin.setAvatar(dto.getAvatar());

        if (dto.getUsername() != null && !dto.getUsername().equals(admin.getUser().getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            admin.getUser().setUsername(dto.getUsername());
            userRepository.save(admin.getUser());
        }

        AdminProfile savedAdmin = adminRepository.save(admin);
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("UPDATE_ADMIN", currentUsername, admin.getUser().getUsername(), "Updated admin account details");
        return savedAdmin;
    }

    @Transactional
    public void deleteAdmin(Long id) {
        AdminProfile admin = getAdminById(id);
        admin.setStatus("inactive");
        adminRepository.save(admin);
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("DELETE_ADMIN", currentUsername, admin.getUser().getUsername(), "Deactivated admin account");
    }
}

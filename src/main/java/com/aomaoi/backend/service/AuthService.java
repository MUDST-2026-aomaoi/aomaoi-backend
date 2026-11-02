package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.dto.AuthResponseDTO;
import com.aomaoi.backend.dto.AuthUserResponseDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final com.aomaoi.backend.repository.WorkerRepository workerRepository;
    private final AuditLogService auditLogService;

    public AuthResponseDTO login(LoginRequest request) {
        // 1. Let Spring Security verify the username and password securely against the database
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. If we reach this line, the password was CORRECT! Load the user's data from the DB.
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // 4. Format the JSON response exactly how the React frontend expects it!
        String fullName = user.getUsername();
        Long actualId = user.getId();
        String avatar = null;
        
        if ("superadmin".equals(user.getRole())) {
            fullName = "superadmin";
        } else if ("admin".equals(user.getRole())) {
            var adminOpt = adminProfileRepository.findByUserUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                if ("inactive".equals(adminOpt.get().getStatus())) {
                    throw new BadCredentialsException("Admin is inactive");
                }
                fullName = adminOpt.get().getFullName();
                actualId = adminOpt.get().getId();
                avatar = adminOpt.get().getAvatar();
            }
        } else if ("worker".equals(user.getRole())) {
            var workerOpt = workerRepository.findByUserUsername(user.getUsername());
            if (workerOpt.isPresent()) {
                if ("inactive".equals(workerOpt.get().getStatus())) {
                    throw new BadCredentialsException("Worker is inactive");
                }
                fullName = workerOpt.get().getFullName();
                actualId = workerOpt.get().getId();
                avatar = workerOpt.get().getAvatar();
            }
        }
        
        AuthUserResponseDTO userData = new AuthUserResponseDTO(actualId, user.getUsername(), user.getRole(),
                user.getIsFirstLogin(), fullName, avatar);
        return new AuthResponseDTO(true, jwtUtil.generateToken(userDetails, user.getRole()), userData, null);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.isBlank())
            throw new IllegalArgumentException("New password must be at least 6 characters");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("รหัสผ่านเดิมไม่ถูกต้อง");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        userRepository.save(user);

        // Update status to active
        if ("worker".equals(user.getRole())) {
            workerRepository.findByUserUsername(username).ifPresent(w -> {
                w.setStatus("active");
                workerRepository.save(w);
            });
        } else if ("admin".equals(user.getRole())) {
            adminProfileRepository.findByUserUsername(username).ifPresent(a -> {
                a.setStatus("active");
                adminProfileRepository.save(a);
            });
        }

        auditLogService.logAction(
            "CHANGE_PASSWORD",
            username,
            username,
            "User changed own password"
        );
    }

    public String refreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails details = userDetailsService.loadUserByUsername(username);
        return jwtUtil.generateToken(details, user.getRole());
    }
}


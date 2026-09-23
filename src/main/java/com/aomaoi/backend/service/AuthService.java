package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

    public Map<String, Object> login(LoginRequest request) {
        // 1. Let Spring Security verify the username and password securely against the database
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. If we reach this line, the password was CORRECT! Load the user's data from the DB.
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername()).get();

        // 3. Generate the JWT Token (embedding their role)
        String token = jwtUtil.generateToken(userDetails, user.getRole());

        // 4. Format the JSON response exactly how the React frontend expects it!
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        
        userData.put("username", user.getUsername());
        userData.put("role", user.getRole());
        userData.put("isFirstLogin", user.getIsFirstLogin());
        
        String fullName = user.getUsername();
        Long actualId = user.getId();
        String avatar = null;
        
        if ("superadmin".equals(user.getRole())) {
            fullName = "superadmin";
        } else if ("admin".equals(user.getRole())) {
            var adminOpt = adminProfileRepository.findByUserUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                fullName = adminOpt.get().getFullName();
                actualId = adminOpt.get().getId();
                avatar = adminOpt.get().getAvatar();
            }
        } else if ("worker".equals(user.getRole())) {
            var workerOpt = workerRepository.findByUserUsername(user.getUsername());
            if (workerOpt.isPresent()) {
                fullName = workerOpt.get().getFullName();
                actualId = workerOpt.get().getId();
                avatar = workerOpt.get().getAvatar();
            }
        }
        
        userData.put("id", actualId);
        userData.put("fullName", fullName);
        if (avatar != null) {
            userData.put("avatar", avatar);
        }
        
        response.put("success", true);
        response.put("token", token);
        response.put("user", userData);

        return response;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("รหัสผ่านเดิมไม่ถูกต้อง");
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
    }
}


package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // We will use this later for the OTP/Change Password feature

    @Autowired
    private com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;

    @Autowired
    private com.aomaoi.backend.repository.WorkerRepository workerRepository;

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
        
        if ("superadmin".equals(user.getRole())) {
            fullName = "superadmin";
        } else if ("admin".equals(user.getRole())) {
            var adminOpt = adminProfileRepository.findByUserUsername(user.getUsername());
            if (adminOpt.isPresent()) {
                fullName = adminOpt.get().getFullName();
                actualId = adminOpt.get().getId();
            }
        } else if ("worker".equals(user.getRole())) {
            var workerOpt = workerRepository.findByUserUsername(user.getUsername());
            if (workerOpt.isPresent()) {
                fullName = workerOpt.get().getFullName();
                actualId = workerOpt.get().getId();
            }
        }
        
        userData.put("id", actualId);
        userData.put("fullName", fullName);
        
        response.put("success", true);
        response.put("token", token);
        response.put("user", userData);

        return response;
    }

    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
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


package com.aomaoi.backend.service;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final WorkerRepository workerRepository;

    // Spring Security calls this automatically when trying to log someone in
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find the user in our PostgreSQL database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("ไม่พบผู้ใช้งานนี้ในระบบ"));

        // Convert our custom User into a Spring Security User
        boolean enabled = switch (user.getRole()) {
            case "superadmin" -> true;
            case "admin" -> adminProfileRepository.findByUserUsername(username)
                    .map(profile -> !"inactive".equals(profile.getStatus())).orElse(false);
            case "worker" -> workerRepository.findByUserUsername(username)
                    .map(profile -> !"inactive".equals(profile.getStatus())).orElse(false);
            default -> false;
        };
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                enabled, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );
    }
}

package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final FarmRepository farmRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only run this if the 'users' table is completely empty
        if (userRepository.count() == 0) {
            
            // 1. Super Admin account
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("1234")); 
            superAdmin.setRole("superadmin");
            superAdmin.setIsFirstLogin(false);
            userRepository.save(superAdmin);

            System.out.println("🌱 Database Seeded:");
            System.out.println("   - superadmin / 1234 (role: superadmin)");
        }
    }
}

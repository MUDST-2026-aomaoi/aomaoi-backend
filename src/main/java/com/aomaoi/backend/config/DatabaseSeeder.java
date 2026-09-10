package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Only run this if the 'users' table is completely empty
        if (userRepository.count() == 0) {
            
            // 1. Super Admin account
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("admin123")); 
            superAdmin.setRole("superadmin");
            superAdmin.setIsFirstLogin(false);
            userRepository.save(superAdmin);

            // 2. Admin account
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("admin");
            admin.setIsFirstLogin(false);
            userRepository.save(admin);

            // 3. Worker account
            User worker = new User();
            worker.setUsername("worker1");
            worker.setPassword(passwordEncoder.encode("admin123"));
            worker.setRole("worker");
            worker.setIsFirstLogin(true); // Workers should change their password on first login
            userRepository.save(worker);
            
            System.out.println("✅ Database Seeded:");
            System.out.println("   - superadmin / admin123 (role: superadmin)");
            System.out.println("   - admin / admin123 (role: admin)");
            System.out.println("   - worker1 / admin123 (role: worker)");
        }
    }
}

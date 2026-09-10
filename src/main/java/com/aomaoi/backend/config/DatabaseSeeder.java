package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.FarmRepository;
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

    @Autowired
    private com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    
    @Autowired
    private FarmRepository farmRepository;

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

            // 2. Farm "ฟาร์มโชคชัย"
            Farm farm = new Farm();
            farm.setName("ฟาร์มโชคชัย");
            farm.setLocation("นครราชสีมา");
            farm.setAdminCount(1);
            farmRepository.save(farm);

            // 3. Admin account "deedee2"
            User admin = new User();
            admin.setUsername("deedee2");
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole("admin");
            admin.setIsFirstLogin(false);
            userRepository.save(admin);
            
            com.aomaoi.backend.entity.AdminProfile adminProfile = new com.aomaoi.backend.entity.AdminProfile();
            adminProfile.setUser(admin);
            adminProfile.setFullName("Deedee 2");
            adminProfile.setPhone("0812345678");
            adminProfile.setStatus("active");
            adminProfile.setFarmId(String.valueOf(farm.getId()));
            adminProfileRepository.save(adminProfile);

            System.out.println("🌱 Database Seeded:");
            System.out.println("   - superadmin / 1234 (role: superadmin)");
            System.out.println("   - deedee2 / 1234 (role: admin, farm: ฟาร์มโชคชัย)");
        }
    }
}

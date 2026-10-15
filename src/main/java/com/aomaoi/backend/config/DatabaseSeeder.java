package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    // Field initializers are the defaults used outside Spring (unit tests); @Value overrides them at runtime.
    @Value("${app.bootstrap-admin-password:1234}")
    private String bootstrapAdminPassword = "1234";

    // false = create only the superadmin (never resets an existing one); true = full demo data.
    @Value("${app.seed.demo-data:true}")
    private boolean demoData = true;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final FarmRepository farmRepository;
    private final com.aomaoi.backend.repository.WorkerRepository workerRepository;
    private final com.aomaoi.backend.repository.WorkLogRepository workLogRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Super Admin account
        if (!demoData && userRepository.findByUsername("superadmin").isPresent()) {
            return;
        }
        User superAdmin = userRepository.findByUsername("superadmin").orElseGet(User::new);
        superAdmin.setUsername("superadmin");
        superAdmin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
        superAdmin.setRole("superadmin");
        superAdmin.setIsFirstLogin(false);
        userRepository.save(superAdmin);
        if (!demoData) {
            return;
        }

        // 2. Demo Farm
        Farm farm;
        if (farmRepository.count() == 0) {
            farm = new Farm();
            farm.setName("ไร่อ้อยอุดมทรัพย์ (Udomsap Sugarcane Farm)");
            farm.setLocation("กาญจนบุรี (Kanchanaburi)");
            farm.setStatus("active");
            farm.setWorkerCount(2);
            farm.setAdminCount(1);
            farm.setMonthlyWages(4550.0);
            farm.setTotalWages(4550.0);
            farm.setJoinedDate(java.time.LocalDate.now().minusMonths(3));
            farm = farmRepository.save(farm);
        } else {
            farm = farmRepository.findAll().get(0);
        }

        // 3. Admin Account & Profile
        User adminUser = userRepository.findByUsername("admin1").orElseGet(User::new);
        adminUser.setUsername("admin1");
        adminUser.setPassword(passwordEncoder.encode("1234"));
        adminUser.setRole("admin");
        adminUser.setIsFirstLogin(false);
        adminUser = userRepository.save(adminUser);

        if (adminProfileRepository.findByUserUsername("admin1").isEmpty()) {
            com.aomaoi.backend.entity.AdminProfile adminProfile = new com.aomaoi.backend.entity.AdminProfile();
            adminProfile.setUser(adminUser);
            adminProfile.setFullName("สมศักดิ์ รักฟาร์ม");
            adminProfile.setPhone("0812345678");
            adminProfile.setFarmId(farm.getId());
            adminProfile.setStatus("active");
            adminProfile.setJoinedDate(java.time.LocalDate.now().minusMonths(3));
            adminProfileRepository.save(adminProfile);
        }

        // 4. Worker 1 Account & Profile
        User workerUser1 = userRepository.findByUsername("worker1").orElseGet(User::new);
        workerUser1.setUsername("worker1");
        workerUser1.setPassword(passwordEncoder.encode("1234"));
        workerUser1.setRole("worker");
        workerUser1.setIsFirstLogin(false);
        workerUser1 = userRepository.save(workerUser1);

        com.aomaoi.backend.entity.Worker worker1 = workerRepository.findByUserUsername("worker1").orElse(null);
        if (worker1 == null) {
            worker1 = new com.aomaoi.backend.entity.Worker();
            worker1.setUser(workerUser1);
            worker1.setFullName("สมชาย หมายมั่น");
            worker1.setNickname("ชาย");
            worker1.setPhone("0891112222");
            worker1.setFarmId(farm.getId());
            worker1.setStatus("active");
            worker1.setJoinedDate(java.time.LocalDate.now().minusMonths(2));
            worker1 = workerRepository.save(worker1);
        }

        // 5. Worker 2 Account & Profile
        User workerUser2 = userRepository.findByUsername("worker2").orElseGet(User::new);
        workerUser2.setUsername("worker2");
        workerUser2.setPassword(passwordEncoder.encode("1234"));
        workerUser2.setRole("worker");
        workerUser2.setIsFirstLogin(false);
        workerUser2 = userRepository.save(workerUser2);

        com.aomaoi.backend.entity.Worker worker2 = workerRepository.findByUserUsername("worker2").orElse(null);
        if (worker2 == null) {
            worker2 = new com.aomaoi.backend.entity.Worker();
            worker2.setUser(workerUser2);
            worker2.setFullName("สมหญิง ยิ่งเจริญ");
            worker2.setNickname("หญิง");
            worker2.setPhone("0893334444");
            worker2.setFarmId(farm.getId());
            worker2.setStatus("active");
            worker2.setJoinedDate(java.time.LocalDate.now().minusMonths(2));
            worker2 = workerRepository.save(worker2);
        }

        java.time.LocalDate today = java.time.LocalDate.now();

        // 6. Record all four task types if workLogRepository is empty
        if (workLogRepository.count() == 0) {
            // 6.1 Task 1: Cutting (ตัดอ้อย)
            com.aomaoi.backend.entity.WorkLog cuttingLog = new com.aomaoi.backend.entity.WorkLog();
            cuttingLog.setWorker(worker1);
            cuttingLog.setType("cutting");
            cuttingLog.setStartDate(today.minusDays(5));
            cuttingLog.setEndDate(today.minusDays(5));
            cuttingLog.setWorkDate(today.minusDays(5));
            cuttingLog.setRows(10);
            cuttingLog.setWaPerRow(100);
            cuttingLog.setTotal(java.math.BigDecimal.valueOf(2000.00));
            workLogRepository.save(cuttingLog);

            // 6.2 Task 2: Planting (ปลูกอ้อย / Padding)
            com.aomaoi.backend.entity.WorkLog plantingLog = new com.aomaoi.backend.entity.WorkLog();
            plantingLog.setWorker(worker1);
            plantingLog.setType("planting");
            plantingLog.setStartDate(today.minusDays(4));
            plantingLog.setEndDate(today.minusDays(4));
            plantingLog.setWorkDate(today.minusDays(4));
            plantingLog.setFurrows(15);
            plantingLog.setWaPerFurrow(20);
            plantingLog.setTotal(java.math.BigDecimal.valueOf(750.00));
            workLogRepository.save(plantingLog);

            // 6.3 Task 3: Watering (รดน้ำ)
            com.aomaoi.backend.entity.WorkLog wateringLog = new com.aomaoi.backend.entity.WorkLog();
            wateringLog.setWorker(worker2);
            wateringLog.setType("watering");
            wateringLog.setStartDate(today.minusDays(2));
            wateringLog.setEndDate(today);
            wateringLog.setWorkDate(today.minusDays(2));
            wateringLog.setDays(3);
            wateringLog.setDailyRate(java.math.BigDecimal.valueOf(350.00));
            wateringLog.setTotal(java.math.BigDecimal.valueOf(1050.00));
            workLogRepository.save(wateringLog);

            // 6.4 Task 4: Spraying (พ่นยา / Stay)
            com.aomaoi.backend.entity.WorkLog sprayingLog = new com.aomaoi.backend.entity.WorkLog();
            sprayingLog.setWorker(worker2);
            sprayingLog.setType("spraying");
            sprayingLog.setStartDate(today.minusDays(1));
            sprayingLog.setEndDate(today.minusDays(1));
            sprayingLog.setWorkDate(today.minusDays(1));
            sprayingLog.setTanks(5);
            sprayingLog.setTotal(java.math.BigDecimal.valueOf(750.00));
            workLogRepository.save(sprayingLog);
        }

        System.out.println("🌱 Database Seeded with all 4 task types (Cutting, Planting, Watering, Spraying):");
        System.out.println("   - superadmin / 1234 (role: superadmin)");
        System.out.println("   - admin1 / 1234 (role: admin, farm: " + farm.getName() + ")");
        System.out.println("   - worker1 / 1234 (role: worker, Cutting & Planting logs)");
        System.out.println("   - worker2 / 1234 (role: worker, Watering & Spraying logs)");
    }
}

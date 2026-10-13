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
    private final com.aomaoi.backend.repository.WorkerRepository workerRepository;
    private final com.aomaoi.backend.repository.WorkLogRepository workLogRepository;

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

            // 2. Demo Farm
            Farm farm = new Farm();
            farm.setName("ไร่อ้อยอุดมทรัพย์ (Udomsap Sugarcane Farm)");
            farm.setLocation("กาญจนบุรี (Kanchanaburi)");
            farm.setStatus("active");
            farm.setWorkerCount(2);
            farm.setAdminCount(1);
            farm.setMonthlyWages(4550.0);
            farm.setTotalWages(4550.0);
            farm.setJoinedDate(java.time.LocalDate.now().minusMonths(3));
            farm = farmRepository.save(farm);

            // 3. Admin Account & Profile
            User adminUser = new User();
            adminUser.setUsername("admin1");
            adminUser.setPassword(passwordEncoder.encode("1234"));
            adminUser.setRole("admin");
            adminUser.setIsFirstLogin(false);
            adminUser = userRepository.save(adminUser);

            com.aomaoi.backend.entity.AdminProfile adminProfile = new com.aomaoi.backend.entity.AdminProfile();
            adminProfile.setUser(adminUser);
            adminProfile.setFullName("สมศักดิ์ รักฟาร์ม");
            adminProfile.setPhone("0812345678");
            adminProfile.setFarmId(farm.getId().toString());
            adminProfile.setStatus("active");
            adminProfile.setJoinedDate(java.time.LocalDate.now().minusMonths(3));
            adminProfileRepository.save(adminProfile);

            // 4. Worker 1 Account & Profile
            User workerUser1 = new User();
            workerUser1.setUsername("worker1");
            workerUser1.setPassword(passwordEncoder.encode("1234"));
            workerUser1.setRole("worker");
            workerUser1.setIsFirstLogin(false);
            workerUser1 = userRepository.save(workerUser1);

            com.aomaoi.backend.entity.Worker worker1 = new com.aomaoi.backend.entity.Worker();
            worker1.setUser(workerUser1);
            worker1.setFullName("สมชาย หมายมั่น");
            worker1.setNickname("ชาย");
            worker1.setPhone("0891112222");
            worker1.setFarmId(farm.getId().toString());
            worker1.setStatus("active");
            worker1.setJoinedDate(java.time.LocalDate.now().minusMonths(2));
            worker1 = workerRepository.save(worker1);

            // 5. Worker 2 Account & Profile
            User workerUser2 = new User();
            workerUser2.setUsername("worker2");
            workerUser2.setPassword(passwordEncoder.encode("1234"));
            workerUser2.setRole("worker");
            workerUser2.setIsFirstLogin(false);
            workerUser2 = userRepository.save(workerUser2);

            com.aomaoi.backend.entity.Worker worker2 = new com.aomaoi.backend.entity.Worker();
            worker2.setUser(workerUser2);
            worker2.setFullName("สมหญิง ยิ่งเจริญ");
            worker2.setNickname("หญิง");
            worker2.setPhone("0893334444");
            worker2.setFarmId(farm.getId().toString());
            worker2.setStatus("active");
            worker2.setJoinedDate(java.time.LocalDate.now().minusMonths(2));
            worker2 = workerRepository.save(worker2);

            java.time.LocalDate today = java.time.LocalDate.now();

            // 6. Record all four task types:
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
            wateringLog.setStartDate(today.minusDays(3));
            wateringLog.setEndDate(today.minusDays(1));
            wateringLog.setWorkDate(today.minusDays(3));
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

            System.out.println("🌱 Database Seeded with all 4 task types (Cutting, Planting, Watering, Spraying):");
            System.out.println("   - superadmin / 1234 (role: superadmin)");
            System.out.println("   - admin1 / 1234 (role: admin, farm: " + farm.getName() + ")");
            System.out.println("   - worker1 / 1234 (role: worker, Cutting & Planting logs)");
            System.out.println("   - worker2 / 1234 (role: worker, Watering & Spraying logs)");
        }
    }
}

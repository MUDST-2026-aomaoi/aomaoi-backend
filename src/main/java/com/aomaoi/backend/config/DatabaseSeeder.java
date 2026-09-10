package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

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

            // 3. Worker accounts
            User w1User = new User();
            w1User.setUsername("w001");
            w1User.setPassword(passwordEncoder.encode("admin123"));
            w1User.setRole("worker");
            w1User.setIsFirstLogin(true);
            userRepository.save(w1User);

            User w2User = new User();
            w2User.setUsername("w002");
            w2User.setPassword(passwordEncoder.encode("admin123"));
            w2User.setRole("worker");
            w2User.setIsFirstLogin(true);
            userRepository.save(w2User);

            User w3User = new User();
            w3User.setUsername("w003");
            w3User.setPassword(passwordEncoder.encode("admin123"));
            w3User.setRole("worker");
            w3User.setIsFirstLogin(true);
            userRepository.save(w3User);

            System.out.println("✅ Database Seeded (Users):");
            System.out.println("   - superadmin / admin123");
            System.out.println("   - admin / admin123");
            System.out.println("   - w001, w002, w003 / admin123 (workers)");
        }

        // Seed sample Workers and WorkLogs for Dashboard testing
        if (workerRepository.count() == 0) {

            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);

            // Worker 1 - สมชาย (active)
            Worker w1 = new Worker();
            w1.setFullName("สมชาย ใจดี");
            w1.setNickname("ชาย");
            w1.setUsername("w001");
            w1.setPhone("081-234-5678");
            w1.setStatus("active");
            w1.setJoinedDate(LocalDate.of(2026, 1, 10));
            w1.setAvatar("https://i.pravatar.cc/150?img=11");
            workerRepository.save(w1);

            // Worker 2 - สมหญิง (active)
            Worker w2 = new Worker();
            w2.setFullName("สมหญิง รักงาน");
            w2.setNickname("หญิง");
            w2.setUsername("w002");
            w2.setPhone("089-234-5678");
            w2.setStatus("active");
            w2.setJoinedDate(LocalDate.of(2026, 1, 10));
            w2.setAvatar("https://i.pravatar.cc/150?img=47");
            workerRepository.save(w2);

            // Worker 3 - ประเสริฐ (active)
            Worker w3 = new Worker();
            w3.setFullName("ประเสริฐ แข็งขัน");
            w3.setNickname("เสริฐ");
            w3.setUsername("w003");
            w3.setPhone("088-234-5678");
            w3.setStatus("active");
            w3.setJoinedDate(LocalDate.of(2026, 2, 15));
            w3.setAvatar("https://i.pravatar.cc/150?img=13");
            workerRepository.save(w3);

            // Worker 4 - มานะ (pending)
            Worker w4 = new Worker();
            w4.setFullName("มานะ พากเพียร");
            w4.setNickname("มานะ");
            w4.setUsername("w004");
            w4.setPhone("087-234-5678");
            w4.setStatus("pending");
            w4.setJoinedDate(LocalDate.of(2026, 8, 20));
            w4.setAvatar("https://i.pravatar.cc/150?img=14");
            workerRepository.save(w4);

            // Worker 5 - สายฝน (active)
            Worker w5 = new Worker();
            w5.setFullName("สายฝน ชื่นใจ");
            w5.setNickname("ฝน");
            w5.setUsername("w005");
            w5.setPhone("086-234-5678");
            w5.setStatus("active");
            w5.setJoinedDate(LocalDate.of(2026, 3, 5));
            w5.setAvatar("https://i.pravatar.cc/150?img=48");
            workerRepository.save(w5);

            // Worker 6 - วิชัย (inactive)
            Worker w6 = new Worker();
            w6.setFullName("วิชัย บุญมี");
            w6.setNickname("ชัย");
            w6.setUsername("w006");
            w6.setPhone("085-234-5678");
            w6.setStatus("inactive");
            w6.setJoinedDate(LocalDate.of(2025, 11, 1));
            w6.setAvatar("https://i.pravatar.cc/150?img=15");
            workerRepository.save(w6);

            // === WorkLogs: Create entries across the last 6 months ===

            // Today's entries (so dashboard 'today' stats aren't empty)
            createCuttingLog(w1, today, 4, 100);     // 4 × 100 × 2 = 800
            createPlantingLog(w2, today, 5, 20);      // 5 × 20 × 2.5 = 250
            createWateringLog(w3, today, 1, 350);     // 1 × 350 = 350
            createSprayingLog(w5, today, 3);           // 3 × 150 = 450

            // This month (earlier days)
            if (startOfMonth.isBefore(today)) {
                createCuttingLog(w1, startOfMonth, 3, 100);           // 600
                createPlantingLog(w2, startOfMonth, 4, 18);           // 180
                createWateringLog(w1, startOfMonth.plusDays(1), 2, 300); // 600
                createSprayingLog(w3, startOfMonth.plusDays(1), 2);   // 300
                createCuttingLog(w5, startOfMonth.plusDays(2), 5, 100); // 1000
            }

            // Previous months (for trend chart)
            for (int monthsBack = 1; monthsBack <= 5; monthsBack++) {
                LocalDate monthDate = today.minusMonths(monthsBack).withDayOfMonth(5);
                createCuttingLog(w1, monthDate, 3 + monthsBack, 100);
                createPlantingLog(w2, monthDate.plusDays(2), 4, 20);
                createWateringLog(w3, monthDate.plusDays(4), 2, 350);
                createSprayingLog(w5, monthDate.plusDays(6), 2 + monthsBack);
            }

            System.out.println("✅ Sample Data Seeded:");
            System.out.println("   - 6 workers (4 active, 1 pending, 1 inactive)");
            System.out.println("   - WorkLogs across 6 months for dashboard charts");
        }
    }

    private void createCuttingLog(Worker worker, LocalDate date, int rows, int waPerRow) {
        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType("cutting");
        log.setWorkDate(date);
        log.setRows(rows);
        log.setWaPerRow(waPerRow);
        log.setTotal(BigDecimal.valueOf((long) rows * waPerRow * 2));
        workLogRepository.save(log);
    }

    private void createPlantingLog(Worker worker, LocalDate date, int furrows, int waPerFurrow) {
        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType("planting");
        log.setWorkDate(date);
        log.setFurrows(furrows);
        log.setWaPerFurrow(waPerFurrow);
        log.setTotal(BigDecimal.valueOf(furrows * waPerFurrow * 2.5));
        workLogRepository.save(log);
    }

    private void createWateringLog(Worker worker, LocalDate date, int days, int dailyRate) {
        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType("watering");
        log.setWorkDate(date);
        log.setDays(days);
        log.setDailyRate(BigDecimal.valueOf(dailyRate));
        log.setTotal(BigDecimal.valueOf((long) days * dailyRate));
        workLogRepository.save(log);
    }

    private void createSprayingLog(Worker worker, LocalDate date, int tanks) {
        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType("spraying");
        log.setWorkDate(date);
        log.setTanks(tanks);
        log.setTotal(BigDecimal.valueOf((long) tanks * 150));
        workLogRepository.save(log);
    }
}

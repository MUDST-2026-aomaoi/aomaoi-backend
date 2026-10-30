package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.dto.WorkLogResponseDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * WorkLogService - Business Logic สำหรับจัดการบันทึกผลงาน/ค่าแรง
 *
 * รับผิดชอบ:
 * - ดึงข้อมูลบันทึกงานทั้งหมด (กรองตาม Farm ของ Admin ที่ล็อกอินอยู่)
 * - เพิ่มบันทึกงานใหม่พร้อมคำนวณค่าแรงอัตโนมัติ
 * - คำนวณค่าแรงตามประเภทงาน (cutting, planting, watering, spraying)
 * - สรุปยอดค่าแรงรวมทั้งหมดและรายเดือน
 */
@Service
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final WorkerRepository workerRepository;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final AuditLogService auditLogService;

    public WorkLogService(WorkLogRepository workLogRepository, WorkerRepository workerRepository, com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository, AuditLogService auditLogService) {
        this.workLogRepository = workLogRepository;
        this.workerRepository = workerRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * ดึงบันทึกงานทั้งหมด (เรียงจากวันที่ล่าสุด)
     * - ถ้าเป็น Admin: จะเห็นเฉพาะบันทึกงานของคนงานในฟาร์มตัวเอง
     * - ถ้าเป็น SuperAdmin: จะเห็นทั้งหมด
     *
     * @return Stable response DTOs with the existing API field names
     */
    @Transactional(readOnly = true)
    public List<WorkLogResponseDTO> getAllLogs() {
        Authentication auth = requireAuthentication();
        List<WorkLog> logs;
        if (hasRole(auth, "SUPERADMIN")) {
            logs = workLogRepository.findAllByOrderByWorkDateDescIdDesc();
        } else if (hasRole(auth, "ADMIN")) {
            logs = workLogRepository.findByWorkerFarmIdOrderByWorkDateDescIdDesc(requireAdminFarm(auth.getName()));
        } else if (hasRole(auth, "WORKER")) {
            Worker self = workerRepository.findByUserUsername(auth.getName())
                    .orElseThrow(() -> new AccessDeniedException("Worker profile missing"));
            logs = workLogRepository.findByWorkerIdOrderByWorkDateDescIdDesc(self.getId());
        } else {
            throw new AccessDeniedException("Unknown role");
        }
        return logs.stream().map(WorkLogResponseDTO::from).toList();
    }

    /**
     * เพิ่มบันทึกงานใหม่
     * - ตรวจสอบค่าที่ส่งมาว่าไม่ติดลบ
     * - สำหรับงาน watering: คำนวณจำนวนวันอัตโนมัติจาก startDate/endDate
     * - คำนวณค่าแรงตามสูตรของแต่ละประเภทงาน
     * - อัปเดตยอดค่าแรงรวมของฟาร์ม
     *
     * @param dto ข้อมูลบันทึกงานจาก Frontend
     * @return Response DTO containing the saved log and calculated total
     */
    @Transactional
    public WorkLogResponseDTO addLog(WorkLogRequestDTO dto) {
        validateWorkType(dto);
        Authentication auth = requireAuthentication();
        if (!hasRole(auth, "ADMIN") && !hasRole(auth, "SUPERADMIN"))
            throw new AccessDeniedException("Work log creation requires admin access");
        Long workerId;
        try {
            workerId = Long.valueOf(dto.getWorkerId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valid workerId is required", e);
        }
        Worker worker = workerRepository.findLockedById(workerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Worker not found"));
        if (hasRole(auth, "ADMIN") && (worker.getFarmId() == null
                || !worker.getFarmId().equals(requireAdminFarm(auth.getName()))))
            throw new AccessDeniedException("Worker is outside your farm");

        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType(dto.getType());

        // Set type-specific fields
        log.setRows(dto.getRows());
        log.setWaPerRow(dto.getWaPerRow());
        log.setFurrows(dto.getFurrows());
        log.setWaPerFurrow(dto.getWaPerFurrow());
        log.setDailyRate(dto.getDailyRate());
        log.setTanks(dto.getTanks());

        // จัดการวันที่สำหรับทุกประเภทงาน (start date และ end date)
        // สำหรับงานวันเดียว (เช่น ตัดหญ้า, ตัดอ้อย, ฉีดยา): กำหนดให้ทั้ง start date และ end date เป็นวันเดียวกัน
        LocalDate start;
        LocalDate end;

        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            start = parseDate(dto.getStartDate());
            if (dto.getEndDate() != null && !dto.getEndDate().isBlank()) {
                end = parseDate(dto.getEndDate());
            } else {
                end = start;
            }
        } else if (dto.getDate() != null && !dto.getDate().isBlank()) {
            start = parseDate(dto.getDate());
            if (dto.getEndDate() != null && !dto.getEndDate().isBlank()) {
                end = parseDate(dto.getEndDate());
            } else {
                end = start;
            }
        } else if (dto.getDays() != null && "watering".equals(dto.getType())) {
            start = LocalDate.now();
            end = start.plusDays(Math.max(0, dto.getDays() - 1));
        } else {
            throw new IllegalArgumentException("Date (or startDate and endDate) is required");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("วันสิ้นสุดต้องไม่อยู่ก่อนวันเริ่มต้น");
        }

        // ป้องกันไม่ให้มอบหมายงานประเภทเดียวกันให้คนงานคนเดิม 3 ครั้งในวันเดียวกัน
        long existingTaskCount = workLogRepository.countByWorkerIdAndTypeAndWorkDate(worker.getId(), dto.getType(), start);
        if (existingTaskCount >= 2) {
            throw new IllegalArgumentException("ไม่สามารถมอบหมายงานประเภทเดียวกัน (" + dto.getType() + ") ให้คนงานคนเดิม 3 ครั้งในวันเดียวกันได้ (Cannot assign the same task to the same worker three times on the same day)");
        }

        log.setStartDate(start);
        log.setEndDate(end);
        log.setWorkDate(start);

        // สำหรับงานรดน้ำ: คำนวณจำนวนวันจาก startDate/endDate อัตโนมัติ (+1 วัน เพราะนับวันเริ่มต้นด้วย)
        if ("watering".equals(dto.getType())) {
            int calculatedDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
            log.setDays(calculatedDays);
        } else {
            log.setDays(dto.getDays());
        }

        // คำนวณค่าแรงตามประเภทงาน
        log.setTotal(calculateWage(dto, log.getDays()));

        WorkLog saved = workLogRepository.save(log);

        // บันทึกประวัติลง Audit Log ว่า Admin คนไหนเพิ่มงานให้คนงานคนไหน
        String currentUsername = auth.getName();
        String workerUsername = worker.getUser() != null ? worker.getUser().getUsername() : worker.getFullName();
        String details = String.format("Added %s work log for %s (Total: %s THB)", saved.getType(), worker.getFullName(), saved.getTotal());
        this.auditLogService.logAction("CREATE_WORKLOG", currentUsername, workerUsername, details);

        return WorkLogResponseDTO.from(saved);
    }

    /**
     * คำนวณค่าแรงตามประเภทงาน
     *
     * สูตรการคำนวณ:
     * - cutting: rows × waPerRow × 2 บาท
     * - planting: furrows × waPerFurrow × 2.5 บาท
     * - watering: days × dailyRate บาท
     * - spraying: tanks × 150 บาท
     *
     * @param dto ข้อมูลจาก Frontend
     * @param calculatedDays จำนวนวันที่คำนวณแล้ว (สำหรับ watering)
     * @return ค่าแรงรวม (BigDecimal)
     */
    public BigDecimal calculateWage(WorkLogRequestDTO dto, Integer calculatedDays) {
        return switch (dto.getType()) {
            case "cutting" -> {
                if (dto.getRows() == null || dto.getRows() <= 0 || dto.getWaPerRow() == null || dto.getWaPerRow() <= 0)
                    throw new IllegalArgumentException("Cutting requires rows and waPerRow");
                yield BigDecimal.valueOf((long) dto.getRows() * dto.getWaPerRow() * 2);
            }
            case "planting" -> {
                if (dto.getFurrows() == null || dto.getFurrows() <= 0 || dto.getWaPerFurrow() == null || dto.getWaPerFurrow() <= 0)
                    throw new IllegalArgumentException("Planting requires furrows and waPerFurrow");
                yield BigDecimal.valueOf(dto.getFurrows()).multiply(BigDecimal.valueOf(dto.getWaPerFurrow())).multiply(new BigDecimal("2.5"));
            }
            case "watering" -> {
                int days = calculatedDays != null ? calculatedDays : (dto.getDays() != null ? dto.getDays() : 0);
                if (days <= 0 || dto.getDailyRate() == null || dto.getDailyRate().signum() <= 0)
                    throw new IllegalArgumentException("Watering requires days and dailyRate");
                yield dto.getDailyRate().multiply(BigDecimal.valueOf(days));
            }
            case "spraying" -> {
                if (dto.getTanks() == null || dto.getTanks() <= 0)
                    throw new IllegalArgumentException("Spraying requires tanks");
                yield BigDecimal.valueOf((long) dto.getTanks() * 150);
            }
            default -> throw new IllegalArgumentException("Unknown work type: " + dto.getType());
        };
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Date must use yyyy-MM-dd", e);
        }
    }

    private void validateWorkType(WorkLogRequestDTO dto) {
        if (dto == null || dto.getType() == null || dto.getType().isBlank()) {
            throw new IllegalArgumentException("Work type is required");
        }
        if (!List.of("cutting", "planting", "watering", "spraying").contains(dto.getType())) {
            throw new IllegalArgumentException("Unknown work type: " + dto.getType());
        }
    }

    private Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Authentication required");
        return auth;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private Long requireAdminFarm(String username) {
        return adminProfileRepository.findByUserUsername(username)
                .filter(profile -> !"inactive".equals(profile.getStatus()) && profile.getFarmId() != null)
                .map(profile -> profile.getFarmId())
                .orElseThrow(() -> new AccessDeniedException("Admin has no active farm"));
    }

}

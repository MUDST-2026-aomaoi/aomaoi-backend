package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final com.aomaoi.backend.repository.FarmRepository farmRepository;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;

    public WorkLogService(WorkLogRepository workLogRepository, WorkerRepository workerRepository, com.aomaoi.backend.repository.FarmRepository farmRepository, com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository) {
        this.workLogRepository = workLogRepository;
        this.workerRepository = workerRepository;
        this.farmRepository = farmRepository;
        this.adminProfileRepository = adminProfileRepository;
    }

    /**
     * ดึงบันทึกงานทั้งหมด (เรียงจากวันที่ล่าสุด)
     * - ถ้าเป็น Admin: จะเห็นเฉพาะบันทึกงานของคนงานในฟาร์มตัวเอง
     * - ถ้าเป็น SuperAdmin: จะเห็นทั้งหมด
     *
     * @return List ของ Map ที่มีข้อมูล id, type, date, workerId, total และฟิลด์เฉพาะประเภท
     */
    public List<Map<String, Object>> getAllLogs() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        List<WorkLog> logs = workLogRepository.findAllByOrderByWorkDateDescIdDesc();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("superadmin") && !auth.getName().equals("anonymousUser")) {
            String currentUsername = auth.getName();
            return adminProfileRepository.findByUserUsername(currentUsername)
                    .map(adminProfile -> logs.stream()
                            .filter(log -> adminProfile.getFarmId().equals(log.getWorker().getFarmId()))
                            .map(this::logToMap)
                            .collect(Collectors.toList()))
                    .orElseGet(() -> logs.stream().map(this::logToMap).collect(Collectors.toList()));
        }

        return logs.stream().map(this::logToMap).collect(Collectors.toList());
    }

    /**
     * เพิ่มบันทึกงานใหม่
     * - ตรวจสอบค่าที่ส่งมาว่าไม่ติดลบ
     * - สำหรับงาน watering: คำนวณจำนวนวันอัตโนมัติจาก startDate/endDate
     * - คำนวณค่าแรงตามสูตรของแต่ละประเภทงาน
     * - อัปเดตยอดค่าแรงรวมของฟาร์ม
     *
     * @param dto ข้อมูลบันทึกงานจาก Frontend
     * @return Map ข้อมูลบันทึกงานที่บันทึกสำเร็จ พร้อม id และ total ที่คำนวณแล้ว
     */
    public Map<String, Object> addLog(WorkLogRequestDTO dto) {
        validatePositive(dto);

        Worker worker = workerRepository.findById(Long.parseLong(dto.getWorkerId()))
                .orElseThrow(() -> new RuntimeException("Worker not found"));

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

        // สำหรับงานรดน้ำ: คำนวณจำนวนวันจาก startDate/endDate อัตโนมัติ
        // ไม่ใช้ dto.getDate() เพราะฟอร์มรดน้ำไม่มีช่อง "วันที่ทำงาน" แยก (ใช้ startDate แทน)
        if ("watering".equals(dto.getType())) {
            if (dto.getStartDate() != null && dto.getEndDate() != null) {
                LocalDate start = LocalDate.parse(dto.getStartDate());
                LocalDate end = LocalDate.parse(dto.getEndDate());

                if (end.isBefore(start)) {
                    throw new RuntimeException("วันสิ้นสุดต้องไม่อยู่ก่อนวันเริ่มต้น");
                }

                // +1 เพราะนับวันเริ่มต้นด้วย (เช่น 1-3 = 3 วัน ไม่ใช่ 2 วัน)
                int calculatedDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
                log.setDays(calculatedDays);
                log.setStartDate(start);
                log.setEndDate(end);
                log.setWorkDate(start);
            } else if (dto.getDays() != null) {
                // Backward compatible: รองรับการส่ง days มาตรงๆ แบบเก่า
                log.setDays(dto.getDays());
                log.setWorkDate(dto.getDate() != null ? LocalDate.parse(dto.getDate()) : LocalDate.now());
            } else {
                throw new RuntimeException("Watering requires startDate/endDate or days");
            }
        } else {
            log.setDays(dto.getDays());
            log.setWorkDate(LocalDate.parse(dto.getDate()));
        }

        // คำนวณค่าแรงตามประเภทงาน
        log.setTotal(calculateWage(dto, log.getDays()));

        WorkLog saved = workLogRepository.save(log);

        // อัปเดตยอดค่าแรงรวมของฟาร์ม
        if (worker.getFarmId() != null) {
            com.aomaoi.backend.entity.Farm farm = this.farmRepository.findById(Long.parseLong(worker.getFarmId())).orElse(null);
            if (farm != null) {
                farm.setTotalWages(farm.getTotalWages() + log.getTotal().doubleValue());
                farm.setMonthlyWages(farm.getMonthlyWages() + log.getTotal().doubleValue());
                this.farmRepository.save(farm);
            }
        }

        return logToMap(saved);
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
                if (dto.getRows() == null || dto.getWaPerRow() == null)
                    throw new RuntimeException("Cutting requires rows and waPerRow");
                yield BigDecimal.valueOf((long) dto.getRows() * dto.getWaPerRow() * 2);
            }
            case "planting" -> {
                if (dto.getFurrows() == null || dto.getWaPerFurrow() == null)
                    throw new RuntimeException("Planting requires furrows and waPerFurrow");
                yield BigDecimal.valueOf(dto.getFurrows() * dto.getWaPerFurrow() * 2.5);
            }
            case "watering" -> {
                int days = calculatedDays != null ? calculatedDays : (dto.getDays() != null ? dto.getDays() : 0);
                if (days == 0 || dto.getDailyRate() == null)
                    throw new RuntimeException("Watering requires days and dailyRate");
                yield dto.getDailyRate().multiply(BigDecimal.valueOf(days));
            }
            case "spraying" -> {
                if (dto.getTanks() == null)
                    throw new RuntimeException("Spraying requires tanks");
                yield BigDecimal.valueOf((long) dto.getTanks() * 150);
            }
            default -> throw new RuntimeException("Unknown work type: " + dto.getType());
        };
    }

    /**
     * ตรวจสอบว่าค่าตัวเลขทั้งหมดที่ส่งมาต้องไม่ติดลบ
     * ป้องกันการส่งข้อมูลที่ไม่สมเหตุสมผลจาก Frontend หรือ API
     */
    private void validatePositive(WorkLogRequestDTO dto) {
        if (dto.getRows() != null && dto.getRows() < 0)
            throw new RuntimeException("rows cannot be negative");
        if (dto.getWaPerRow() != null && dto.getWaPerRow() < 0)
            throw new RuntimeException("waPerRow cannot be negative");
        if (dto.getFurrows() != null && dto.getFurrows() < 0)
            throw new RuntimeException("furrows cannot be negative");
        if (dto.getWaPerFurrow() != null && dto.getWaPerFurrow() < 0)
            throw new RuntimeException("waPerFurrow cannot be negative");
        if (dto.getDays() != null && dto.getDays() < 0)
            throw new RuntimeException("days cannot be negative");
        if (dto.getDailyRate() != null && dto.getDailyRate().compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("dailyRate cannot be negative");
        if (dto.getTanks() != null && dto.getTanks() < 0)
            throw new RuntimeException("tanks cannot be negative");
    }

    /**
     * คำนวณยอดค่าแรงรวมทั้งหมดตั้งแต่เริ่มระบบ
     * @return ยอดค่าแรงรวมทั้งหมด (BigDecimal)
     */
    public BigDecimal getTotalAllTime() {
        return workLogRepository.findAll().stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * คำนวณยอดค่าแรงรวมของเดือนปัจจุบัน
     * @return ยอดค่าแรงเดือนนี้ (BigDecimal)
     */
    public BigDecimal getTotalThisMonth() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        return workLogRepository.findByWorkDateBetween(startOfMonth, today).stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * แปลง WorkLog Entity เป็น Map สำหรับส่งกลับ Frontend
     * รวมฟิลด์เฉพาะประเภทงาน (เช่น rows, tanks, startDate, endDate) เฉพาะที่มีค่า
     */
    private Map<String, Object> logToMap(WorkLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(log.getId()));
        map.put("type", log.getType());
        map.put("date", log.getWorkDate().toString());
        map.put("workerId", String.valueOf(log.getWorker().getId()));
        map.put("total", log.getTotal().doubleValue());

        if (log.getRows() != null) map.put("rows", log.getRows());
        if (log.getWaPerRow() != null) map.put("waPerRow", log.getWaPerRow());
        if (log.getFurrows() != null) map.put("furrows", log.getFurrows());
        if (log.getWaPerFurrow() != null) map.put("waPerFurrow", log.getWaPerFurrow());
        if (log.getDays() != null) map.put("days", log.getDays());
        if (log.getDailyRate() != null) map.put("dailyRate", log.getDailyRate().doubleValue());
        if (log.getTanks() != null) map.put("tanks", log.getTanks());
        if (log.getStartDate() != null) map.put("startDate", log.getStartDate().toString());
        if (log.getEndDate() != null) map.put("endDate", log.getEndDate().toString());

        return map;
    }
}

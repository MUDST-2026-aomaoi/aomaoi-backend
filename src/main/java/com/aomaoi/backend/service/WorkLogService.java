package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WorkLogService {

    // === อัตราค่าจ้างคงที่ (Wage Rates) ===
    private static final double CUTTING_RATE   = 2.0;    // บาท/วา
    private static final double PLANTING_RATE  = 2.5;    // บาท/วา
    private static final double WATERING_RATE  = 400.0;  // บาท/วัน
    private static final double SPRAYING_RATE  = 150.0;  // บาท/ถัง

    @Autowired
    private WorkLogRepository workLogRepository;

    @Autowired
    private WorkerRepository workerRepository;

    // ============================================================
    //  สูตรคำนวณค่าจ้าง (Wage Calculation)
    // ============================================================

    /**
     * คำนวณค่าจ้างตามประเภทงาน
     *
     * - CUTTING  : จำนวนร่อง × ความยาว(วา) × 2   บาท/วา
     * - PLANTING : จำนวนร่อง × ความยาว(วา) × 2.5 บาท/วา
     * - WATERING : จำนวนวัน × 400 บาท/วัน
     * - SPRAYING : จำนวนถัง × 150 บาท/ถัง
     */
    public double calculateWage(String workType, Integer numberOfRows, Double rowLength,
                                Integer numberOfDays, Integer numberOfTanks) {
        return switch (workType.toUpperCase()) {
            case "CUTTING" -> {
                validateNotNull(numberOfRows, "numberOfRows");
                validateNotNull(rowLength, "rowLength");
                validatePositive(numberOfRows, "numberOfRows");
                validatePositive(rowLength, "rowLength");
                yield numberOfRows * rowLength * CUTTING_RATE;
            }
            case "PLANTING" -> {
                validateNotNull(numberOfRows, "numberOfRows");
                validateNotNull(rowLength, "rowLength");
                validatePositive(numberOfRows, "numberOfRows");
                validatePositive(rowLength, "rowLength");
                yield numberOfRows * rowLength * PLANTING_RATE;
            }
            case "WATERING" -> {
                validateNotNull(numberOfDays, "numberOfDays");
                validatePositive(numberOfDays, "numberOfDays");
                yield numberOfDays * WATERING_RATE;
            }
            case "SPRAYING" -> {
                validateNotNull(numberOfTanks, "numberOfTanks");
                validatePositive(numberOfTanks, "numberOfTanks");
                yield numberOfTanks * SPRAYING_RATE;
            }
            default -> throw new IllegalArgumentException(
                    "Unknown work type: " + workType +
                    ". Valid types are: CUTTING, PLANTING, WATERING, SPRAYING");
        };
    }

    // ============================================================
    //  CRUD Operations
    // ============================================================

    /**
     * บันทึกงานใหม่ — validate ข้อมูล, คำนวณค่าจ้าง, แล้ว save ลง DB
     */
    @Transactional
    public WorkLog createWorkLog(WorkLogRequestDTO dto) {
        // 1. Validate work type
        if (dto.getWorkType() == null || dto.getWorkType().isBlank()) {
            throw new IllegalArgumentException("workType is required");
        }

        // 2. Validate and find worker
        if (dto.getWorkerId() == null) {
            throw new IllegalArgumentException("workerId is required");
        }
        Worker worker = workerRepository.findById(dto.getWorkerId())
                .orElseThrow(() -> new RuntimeException("Worker not found with id: " + dto.getWorkerId()));

        // 3. Calculate wage using the formula
        double wage = calculateWage(
                dto.getWorkType(),
                dto.getNumberOfRows(),
                dto.getRowLength(),
                dto.getNumberOfDays(),
                dto.getNumberOfTanks()
        );

        // 4. Build and save entity
        WorkLog workLog = new WorkLog();
        workLog.setWorker(worker);
        workLog.setWorkType(dto.getWorkType().toUpperCase());
        workLog.setNumberOfRows(dto.getNumberOfRows());
        workLog.setRowLength(dto.getRowLength());
        workLog.setNumberOfDays(dto.getNumberOfDays());
        workLog.setNumberOfTanks(dto.getNumberOfTanks());
        workLog.setWage(wage);
        workLog.setWorkDate(dto.getWorkDate() != null ? dto.getWorkDate() : LocalDate.now());
        workLog.setNote(dto.getNote());

        return workLogRepository.save(workLog);
    }

    /**
     * ดึงประวัติงานทั้งหมด
     */
    public List<WorkLog> getAllWorkLogs() {
        return workLogRepository.findAll();
    }

    /**
     * ดึงประวัติงานตาม Worker ID
     */
    public List<WorkLog> getWorkLogsByWorkerId(Long workerId) {
        return workLogRepository.findByWorkerId(workerId);
    }

    /**
     * ค้นหาประวัติงานจากชื่อเล่น (nickname) ของ Worker
     */
    public List<WorkLog> getWorkLogsByNickname(String nickname) {
        return workLogRepository.findByWorkerNicknameContainingIgnoreCase(nickname);
    }

    /**
     * ดึงประวัติงานตามช่วงวันที่
     */
    public List<WorkLog> getWorkLogsByDateRange(LocalDate startDate, LocalDate endDate) {
        return workLogRepository.findByWorkDateBetween(startDate, endDate);
    }

    // ============================================================
    //  Validation Helpers
    // ============================================================

    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required for this work type");
        }
    }

    private void validatePositive(Number value, String fieldName) {
        if (value.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number (got: " + value + ")");
        }
    }
}

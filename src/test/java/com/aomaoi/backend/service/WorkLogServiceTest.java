package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

    @Mock
    private WorkLogRepository workLogRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkLogService workLogService;

    private Worker testWorker;

    @BeforeEach
    void setUp() {
        testWorker = new Worker();
        testWorker.setId(1L);
        testWorker.setFullName("สมชาย ใจดี");
        testWorker.setNickname("ชาย");
        testWorker.setStatus("active");
    }

    // ============================================================
    //  Sugarcane Cutting: rows × rowLength × 2 baht/วา
    // ============================================================

    @Test
    @DisplayName("CUTTING: 10 rows × 20 วา × 2 = 400 baht")
    void calculateWage_cutting_shouldMultiplyRowsTimesLengthTimes2() {
        double wage = workLogService.calculateWage("CUTTING", 10, 20.0, null, null);
        assertEquals(400.0, wage);
    }

    @Test
    @DisplayName("CUTTING: 5 rows × 15.5 วา × 2 = 155 baht")
    void calculateWage_cutting_withDecimalLength() {
        double wage = workLogService.calculateWage("CUTTING", 5, 15.5, null, null);
        assertEquals(155.0, wage);
    }

    // ============================================================
    //  Sugarcane Planting: rows × rowLength × 2.5 baht/วา
    // ============================================================

    @Test
    @DisplayName("PLANTING: 8 rows × 10 วา × 2.5 = 200 baht")
    void calculateWage_planting_shouldMultiplyRowsTimesLengthTimes2point5() {
        double wage = workLogService.calculateWage("PLANTING", 8, 10.0, null, null);
        assertEquals(200.0, wage);
    }

    @Test
    @DisplayName("PLANTING: 3 rows × 12 วา × 2.5 = 90 baht")
    void calculateWage_planting_anotherCase() {
        double wage = workLogService.calculateWage("PLANTING", 3, 12.0, null, null);
        assertEquals(90.0, wage);
    }

    // ============================================================
    //  Watering: days × 400 baht/day
    // ============================================================

    @Test
    @DisplayName("WATERING: 3 days × 400 = 1200 baht")
    void calculateWage_watering_shouldMultiplyDaysTimes400() {
        double wage = workLogService.calculateWage("WATERING", null, null, 3, null);
        assertEquals(1200.0, wage);
    }

    @Test
    @DisplayName("WATERING: 1 day × 400 = 400 baht")
    void calculateWage_watering_singleDay() {
        double wage = workLogService.calculateWage("WATERING", null, null, 1, null);
        assertEquals(400.0, wage);
    }

    // ============================================================
    //  Pesticide Spraying: tanks × 150 baht/tank
    // ============================================================

    @Test
    @DisplayName("SPRAYING: 5 tanks × 150 = 750 baht")
    void calculateWage_spraying_shouldMultiplyTanksTimes150() {
        double wage = workLogService.calculateWage("SPRAYING", null, null, null, 5);
        assertEquals(750.0, wage);
    }

    @Test
    @DisplayName("SPRAYING: 2 tanks × 150 = 300 baht")
    void calculateWage_spraying_anotherCase() {
        double wage = workLogService.calculateWage("SPRAYING", null, null, null, 2);
        assertEquals(300.0, wage);
    }

    // ============================================================
    //  Validation: Negative values must be rejected
    // ============================================================

    @Test
    @DisplayName("Negative numberOfRows should throw IllegalArgumentException")
    void calculateWage_negativeRows_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("CUTTING", -5, 10.0, null, null));
    }

    @Test
    @DisplayName("Negative rowLength should throw IllegalArgumentException")
    void calculateWage_negativeLength_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("PLANTING", 5, -10.0, null, null));
    }

    @Test
    @DisplayName("Negative numberOfDays should throw IllegalArgumentException")
    void calculateWage_negativeDays_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("WATERING", null, null, -1, null));
    }

    @Test
    @DisplayName("Negative numberOfTanks should throw IllegalArgumentException")
    void calculateWage_negativeTanks_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("SPRAYING", null, null, null, -3));
    }

    @Test
    @DisplayName("Zero numberOfRows should throw IllegalArgumentException")
    void calculateWage_zeroRows_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("CUTTING", 0, 10.0, null, null));
    }

    // ============================================================
    //  Validation: Missing required fields
    // ============================================================

    @Test
    @DisplayName("CUTTING without numberOfRows should throw")
    void calculateWage_cuttingNoRows_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("CUTTING", null, 10.0, null, null));
    }

    @Test
    @DisplayName("WATERING without numberOfDays should throw")
    void calculateWage_wateringNoDays_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("WATERING", null, null, null, null));
    }

    @Test
    @DisplayName("Unknown work type should throw")
    void calculateWage_unknownType_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                workLogService.calculateWage("HARVESTING", 5, 10.0, null, null));
    }

    // ============================================================
    //  Case insensitivity
    // ============================================================

    @Test
    @DisplayName("Work type should be case-insensitive")
    void calculateWage_caseInsensitive() {
        double wage1 = workLogService.calculateWage("cutting", 10, 20.0, null, null);
        double wage2 = workLogService.calculateWage("Cutting", 10, 20.0, null, null);
        assertEquals(400.0, wage1);
        assertEquals(400.0, wage2);
    }

    // ============================================================
    //  Full createWorkLog integration (with mocked repo)
    // ============================================================

    @Test
    @DisplayName("createWorkLog should calculate wage and save correctly")
    void createWorkLog_cutting_shouldCalculateAndSave() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(testWorker));
        when(workLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId(1L);
        dto.setWorkType("CUTTING");
        dto.setNumberOfRows(10);
        dto.setRowLength(20.0);
        dto.setWorkDate(LocalDate.of(2026, 9, 10));

        var workLog = workLogService.createWorkLog(dto);

        assertEquals(400.0, workLog.getWage());
        assertEquals("CUTTING", workLog.getWorkType());
        assertEquals(testWorker, workLog.getWorker());
        assertEquals(LocalDate.of(2026, 9, 10), workLog.getWorkDate());
    }

    @Test
    @DisplayName("createWorkLog with missing workerId should throw")
    void createWorkLog_noWorkerId_shouldThrow() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkType("CUTTING");
        dto.setNumberOfRows(10);
        dto.setRowLength(20.0);

        assertThrows(IllegalArgumentException.class, () ->
                workLogService.createWorkLog(dto));
    }

    @Test
    @DisplayName("createWorkLog with non-existent worker should throw")
    void createWorkLog_workerNotFound_shouldThrow() {
        when(workerRepository.findById(999L)).thenReturn(Optional.empty());

        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId(999L);
        dto.setWorkType("WATERING");
        dto.setNumberOfDays(2);

        assertThrows(RuntimeException.class, () ->
                workLogService.createWorkLog(dto));
    }
}

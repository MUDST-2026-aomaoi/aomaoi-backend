package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkLogServiceTest {

    @Mock
    private WorkLogRepository workLogRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @InjectMocks
    private WorkLogService workLogService;

    private Worker worker;
    private WorkLog workLog;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setFullName("Worker One");
        worker.setFarmId("10");

        workLog = new WorkLog();
        workLog.setId(100L);
        workLog.setWorker(worker);
        workLog.setType("cutting");
        workLog.setWorkDate(LocalDate.now());
        workLog.setRows(10);
        workLog.setWaPerRow(5);
        workLog.setTotal(BigDecimal.valueOf(100.0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllLogs_Superadmin_ReturnsAll() {
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(workLog));

        List<Map<String, Object>> result = workLogService.getAllLogs();

        assertEquals(1, result.size());
        assertEquals("100", result.get(0).get("id"));
        assertEquals("cutting", result.get(0).get("type"));
    }

    @Test
    void getAllLogs_AdminUser_FiltersByFarm() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId("10");

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(workLog));

        List<Map<String, Object>> result = workLogService.getAllLogs();

        assertEquals(1, result.size());
    }

    @Test
    void calculateWage_Cutting() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");
        dto.setRows(10);
        dto.setWaPerRow(5);

        BigDecimal wage = workLogService.calculateWage(dto);

        // 10 * 5 * 2 = 100
        assertEquals(BigDecimal.valueOf(100), wage);
    }

    @Test
    void calculateWage_Cutting_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto));
    }

    @Test
    void calculateWage_Planting() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("planting");
        dto.setFurrows(4);
        dto.setWaPerFurrow(10);

        BigDecimal wage = workLogService.calculateWage(dto);

        // 4 * 10 * 2.5 = 100.0
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(wage));
    }

    @Test
    void calculateWage_Planting_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("planting");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto));
    }

    @Test
    void calculateWage_Watering() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("watering");
        dto.setDays(3);
        dto.setDailyRate(BigDecimal.valueOf(300));

        BigDecimal wage = workLogService.calculateWage(dto);

        // 300 * 3 = 900
        assertEquals(BigDecimal.valueOf(900), wage);
    }

    @Test
    void calculateWage_Watering_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("watering");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto));
    }

    @Test
    void calculateWage_Spraying() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("spraying");
        dto.setTanks(2);

        BigDecimal wage = workLogService.calculateWage(dto);

        // 2 * 150 = 300
        assertEquals(BigDecimal.valueOf(300), wage);
    }

    @Test
    void calculateWage_Spraying_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("spraying");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto));
    }

    @Test
    void calculateWage_UnknownType_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("invalid");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto));
    }

    @Test
    void addLog_Success_UpdatesFarmWages() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate(LocalDate.now().toString());
        dto.setRows(10);
        dto.setWaPerRow(5);

        Farm farm = new Farm();
        farm.setId(10L);
        farm.setTotalWages(500.0);
        farm.setMonthlyWages(200.0);

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(100L);
            return log;
        });

        Map<String, Object> result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals("100", result.get("id"));
        assertEquals(600.0, farm.getTotalWages());
        assertEquals(300.0, farm.getMonthlyWages());
        verify(farmRepository).save(farm);
    }

    @Test
    void addLog_NegativeValue_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate(LocalDate.now().toString());
        dto.setRows(-5);

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_WorkerNotFound_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("99");
        dto.setType("spraying");
        dto.setDate(LocalDate.now().toString());
        dto.setTanks(1);

        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void getTotalAllTime() {
        when(workLogRepository.findAll()).thenReturn(List.of(workLog));

        BigDecimal total = workLogService.getTotalAllTime();

        assertEquals(BigDecimal.valueOf(100.0), total);
    }

    @Test
    void getTotalThisMonth() {
        when(workLogRepository.findByWorkDateBetween(any(), any())).thenReturn(List.of(workLog));

        BigDecimal total = workLogService.getTotalThisMonth();

        assertEquals(BigDecimal.valueOf(100.0), total);
    }
}

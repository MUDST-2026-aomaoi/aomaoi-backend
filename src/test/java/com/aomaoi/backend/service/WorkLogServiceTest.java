package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.dto.WorkLogResponseDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
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
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkLogService workLogService;

    private Worker worker;
    private WorkLog workLog;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setFullName("Worker One");
        worker.setFarmId(10L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("superadmin", "pass", List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))));

        workLog = new WorkLog();
        workLog.setId(100L);
        workLog.setWorker(worker);
        workLog.setType("cutting");
        workLog.setWorkDate(LocalDate.now());
        workLog.setStartDate(LocalDate.now());
        workLog.setEndDate(LocalDate.now());
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

        List<WorkLogResponseDTO> result = workLogService.getAllLogs();

        assertEquals(1, result.size());
        assertEquals("100", result.get(0).id());
        assertEquals("cutting", result.get(0).type());
    }

    @Test
    void getAllLogs_AdminUser_FiltersByFarm() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(10L);

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(workLogRepository.findByWorkerFarmIdOrderByWorkDateDescIdDesc(10L)).thenReturn(List.of(workLog));

        List<WorkLogResponseDTO> result = workLogService.getAllLogs();

        assertEquals(1, result.size());
        verify(workLogRepository, never()).findAllByOrderByWorkDateDescIdDesc();
    }

    @Test
    void getAllLogs_InactiveAdminIsDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(10L);
        profile.setStatus("inactive");
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> workLogService.getAllLogs());
        verify(workLogRepository, never()).findByWorkerFarmIdOrderByWorkDateDescIdDesc(anyLong());
    }

    @Test
    void getAllLogs_WorkerRole_ReturnsOwnLogs() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.of(worker));
        when(workLogRepository.findByWorkerIdOrderByWorkDateDescIdDesc(1L)).thenReturn(List.of(workLog));

        List<WorkLogResponseDTO> result = workLogService.getAllLogs();

        assertEquals(1, result.size());
        assertEquals("100", result.get(0).id());
        verify(workLogRepository).findByWorkerIdOrderByWorkDateDescIdDesc(1L);
    }

    @Test
    void getAllLogs_WorkerProfileMissing_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> workLogService.getAllLogs());
    }

    @Test
    void getAllLogs_UnknownRole_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("guest", "pass", List.of(new SimpleGrantedAuthority("ROLE_GUEST")))
        );

        assertThrows(AccessDeniedException.class, () -> workLogService.getAllLogs());
    }

    @Test
    void getAllLogs_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> workLogService.getAllLogs());
    }

    @Test
    void addLog_AdminCannotWriteAnotherFarm() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(99L);
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("spraying");
        dto.setDate("2026-10-12");
        dto.setTanks(1);
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> workLogService.addLog(dto));
        verify(workLogRepository, never()).save(any());
    }

    @Test
    void addLog_WorkerRole_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");

        assertThrows(AccessDeniedException.class, () -> workLogService.addLog(dto));
        verify(workLogRepository, never()).save(any());
    }

    @Test
    void addLog_InvalidDateFormat_ThrowsIllegalArgumentException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate("invalid-date-format");
        dto.setRows(5);
        dto.setWaPerRow(10);

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));

        assertThrows(IllegalArgumentException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_InvalidWorkerId_ThrowsIllegalArgumentException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("not_a_number");
        dto.setType("cutting");
        dto.setDate("2026-10-12");

        assertThrows(IllegalArgumentException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_NullWorkType_ThrowsIllegalArgumentException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType(null);

        assertThrows(IllegalArgumentException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_AdminNoFarm_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate("2026-10-12");

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void calculateWage_Cutting() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");
        dto.setRows(10);
        dto.setWaPerRow(5);

        BigDecimal wage = workLogService.calculateWage(dto, dto.getDays());

        // 10 * 5 * 2 = 100
        assertEquals(BigDecimal.valueOf(100), wage);
    }

    @Test
    void calculateWage_Cutting_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto, dto.getDays()));
    }

    @Test
    void calculateWage_Planting() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("planting");
        dto.setFurrows(4);
        dto.setWaPerFurrow(10);

        BigDecimal wage = workLogService.calculateWage(dto, dto.getDays());

        // 4 * 10 * 2.5 = 100.0
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(wage));
    }

    @Test
    void calculateWage_Planting_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("planting");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto, dto.getDays()));
    }

    @Test
    void calculateWage_Watering() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("watering");
        dto.setDays(3);
        dto.setDailyRate(BigDecimal.valueOf(300));

        BigDecimal wage = workLogService.calculateWage(dto, dto.getDays());

        // 300 * 3 = 900
        assertEquals(BigDecimal.valueOf(900), wage);
    }

    @Test
    void calculateWage_Watering_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("watering");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto, dto.getDays()));
    }

    @Test
    void calculateWage_Spraying() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("spraying");
        dto.setTanks(2);

        BigDecimal wage = workLogService.calculateWage(dto, dto.getDays());

        // 2 * 150 = 300
        assertEquals(BigDecimal.valueOf(300), wage);
    }

    @Test
    void calculateWage_Spraying_MissingFields_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("spraying");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto, dto.getDays()));
    }

    @Test
    void calculateWage_UnknownType_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("invalid");

        assertThrows(RuntimeException.class, () -> workLogService.calculateWage(dto, dto.getDays()));
    }

    @Test
    void addLog_Success_CalculatesWage() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate(LocalDate.now().toString());
        dto.setRows(10);
        dto.setWaPerRow(5);

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(100L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals("100", result.id());
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
    void addLog_NegativeDailyRate_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setDailyRate(BigDecimal.valueOf(-10));

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_NegativeFurrows_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("planting");
        dto.setFurrows(-1);

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_Watering_DateRange_CalculatesDaysCorrectly() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setStartDate("2026-10-01");
        dto.setEndDate("2026-10-03");
        dto.setDailyRate(BigDecimal.valueOf(300));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(101L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals(3, result.days()); // 1st, 2nd, 3rd = 3 days
        assertEquals(new BigDecimal("900"), result.total());
    }

    @Test
    void addLog_Watering_EndDateBeforeStartDate_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setStartDate("2026-10-05");
        dto.setEndDate("2026-10-01");
        dto.setDailyRate(BigDecimal.valueOf(300));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
        assertEquals("วันสิ้นสุดต้องไม่อยู่ก่อนวันเริ่มต้น", ex.getMessage());
    }

    @Test
    void addLog_Watering_DaysWithoutAnyDate_BackwardCompatible() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setDays(2);
        dto.setDailyRate(BigDecimal.valueOf(250));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(102L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals(2, result.days());
        assertEquals(new BigDecimal("500"), result.total());
    }

    @Test
    void addLog_Watering_ExplicitDateTakesPrecedenceOverDays() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setDate("2026-10-01");
        dto.setDays(2);
        dto.setDailyRate(BigDecimal.valueOf(250));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(104L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertEquals("2026-10-01", result.startDate());
        assertEquals("2026-10-01", result.endDate());
        assertEquals(1, result.days());
        assertEquals(new BigDecimal("250"), result.total());
    }

    @Test
    void addLog_Watering_MissingBothDateRangeAndDays_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setDailyRate(BigDecimal.valueOf(300));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
        assertEquals("Date (or startDate and endDate) is required", ex.getMessage());
    }

    @Test
    void addLog_WorkerNotFound_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("99");
        dto.setType("spraying");
        dto.setDate(LocalDate.now().toString());
        dto.setTanks(1);

        when(workerRepository.findLockedById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_SingleDayTask_SetsStartAndEndDateToSameDay() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate("2026-10-12");
        dto.setRows(5);
        dto.setWaPerRow(10);

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(101L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals("2026-10-12", result.startDate());
        assertEquals("2026-10-12", result.endDate());
        assertEquals("2026-10-12", result.date());
    }

    @Test
    void addLog_MultiDayTask_SetsStartAndEndDateProperly() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setStartDate("2026-10-10");
        dto.setEndDate("2026-10-12");
        dto.setDailyRate(BigDecimal.valueOf(350));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(102L);
            return log;
        });

        WorkLogResponseDTO result = workLogService.addLog(dto);

        assertNotNull(result);
        assertEquals("2026-10-10", result.startDate());
        assertEquals("2026-10-12", result.endDate());
        assertEquals(3, result.days()); // 10, 11, 12 = 3 days
    }

    @Test
    void addLog_EndDateBeforeStartDate_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setStartDate("2026-10-15");
        dto.setEndDate("2026-10-10");
        dto.setDailyRate(BigDecimal.valueOf(350));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));

        assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
    }

    @Test
    void addLog_ThirdSameTaskOnSameDay_ThrowsException() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("watering");
        dto.setDate("2026-10-12");
        dto.setDays(1);
        dto.setDailyRate(BigDecimal.valueOf(350));

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.countByWorkerIdAndTypeAndWorkDate(1L, "watering", LocalDate.parse("2026-10-12")))
                .thenReturn(2L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> workLogService.addLog(dto));
        assertTrue(exception.getMessage().contains("3 ครั้งในวันเดียวกัน") || exception.getMessage().contains("three times"));
    }

    @Test
    void addLog_RecordsAuditLog() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setWorkerId("1");
        dto.setType("cutting");
        dto.setDate("2026-10-12");
        dto.setRows(5);
        dto.setWaPerRow(10);

        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> {
            WorkLog log = i.getArgument(0);
            log.setId(103L);
            return log;
        });

        workLogService.addLog(dto);

        verify(auditLogService).logAction(eq("CREATE_WORKLOG"), anyString(), anyString(), anyString());
    }
}

package com.aomaoi.backend.service;

import com.aomaoi.backend.config.BusinessTime;
import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.dto.WorkLogResponseDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link WorkLogServiceTest}. */
@ExtendWith(MockitoExtension.class)
class WorkLogServiceBranchTest {

    @Mock
    private WorkLogRepository workLogRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkLogService service;

    private Worker worker;
    private final LocalDate today = BusinessTime.today();

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setFullName("Worker One");
        worker.setFarmId(10L);
        loginAs("superadmin", "ROLE_SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "pass", List.of(new SimpleGrantedAuthority(role))));
    }

    private WorkLogRequestDTO dto(String type) {
        WorkLogRequestDTO d = new WorkLogRequestDTO();
        d.setType(type);
        d.setWorkerId("1");
        return d;
    }

    private WorkLogRequestDTO sprayDto() {
        WorkLogRequestDTO d = dto("spraying");
        d.setTanks(2);
        return d;
    }

    private void stubWorkerAndSave() {
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void assertAddLogFails(WorkLogRequestDTO d, Class<? extends RuntimeException> type) {
        assertThrows(type, () -> service.addLog(d));
    }

    // ---------- work type validation ----------

    @Test
    void addLog_nullDto_throws() {
        assertAddLogFails(null, IllegalArgumentException.class);
    }

    @Test
    void addLog_blankType_throws() {
        assertAddLogFails(dto("  "), IllegalArgumentException.class);
    }

    @Test
    void addLog_unknownType_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.addLog(dto("mowing")));
        assertEquals("Unknown work type: mowing", ex.getMessage());
    }

    // ---------- authentication ----------

    @Test
    void addLog_noAuthentication_throws() {
        SecurityContextHolder.clearContext();
        assertAddLogFails(sprayDto(), AccessDeniedException.class);
    }

    @Test
    void addLog_unauthenticatedToken_throws() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));
        assertAddLogFails(sprayDto(), AccessDeniedException.class);
    }

    @Test
    void addLog_anonymousUser_throws() {
        loginAs("anonymousUser", "ROLE_ANONYMOUS");
        assertAddLogFails(sprayDto(), AccessDeniedException.class);
    }

    // ---------- admin farm scoping ----------

    @Test
    void addLog_adminOwnFarm_succeeds() {
        loginAs("adminuser", "ROLE_ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(10L);
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.toString());

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(new BigDecimal("300"), result.total());
    }

    @Test
    void addLog_adminWorkerWithoutFarm_throws() {
        loginAs("adminuser", "ROLE_ADMIN");
        worker.setFarmId(null);
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.toString());

        assertAddLogFails(d, AccessDeniedException.class);
    }

    // ---------- date resolution ----------

    @Test
    void addLog_startDateOnly_endDefaultsToStart() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setStartDate(today.minusDays(1).toString());

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(today.minusDays(1).toString(), result.startDate());
        assertEquals(today.minusDays(1).toString(), result.endDate());
    }

    @Test
    void addLog_startAndEndDate_usesBoth() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setStartDate(today.minusDays(3).toString());
        d.setEndDate(today.minusDays(1).toString());

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(today.minusDays(3).toString(), result.startDate());
        assertEquals(today.minusDays(1).toString(), result.endDate());
    }

    @Test
    void addLog_dateWithEndDate_usesBoth() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.minusDays(2).toString());
        d.setEndDate(today.toString());

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(today.minusDays(2).toString(), result.startDate());
        assertEquals(today.toString(), result.endDate());
    }

    @Test
    void addLog_blankStartDate_fallsBackToDate() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setStartDate(" ");
        d.setDate(today.toString());
        d.setEndDate(" ");

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(today.toString(), result.startDate());
        assertEquals(today.toString(), result.endDate());
    }

    @Test
    void addLog_noDateForNonWatering_throws() {
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        WorkLogRequestDTO d = sprayDto();
        d.setDays(3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.addLog(d));
        assertEquals("Date (or startDate and endDate) is required", ex.getMessage());
    }

    @Test
    void addLog_wateringDaysOnly_derivesEndFromDaysEvenIntoFuture() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = dto("watering");
        d.setDays(3);
        d.setDailyRate(BigDecimal.valueOf(100));

        WorkLogResponseDTO result = service.addLog(d);

        assertEquals(today.toString(), result.startDate());
        assertEquals(today.plusDays(2).toString(), result.endDate());
        assertEquals(0, BigDecimal.valueOf(300).compareTo(result.total()));
    }

    @Test
    void addLog_startDateInFuture_throws() {
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.plusDays(1).toString());

        assertAddLogFails(d, IllegalArgumentException.class);
        verify(workLogRepository, never()).save(any());
    }

    @Test
    void addLog_endDateInFuture_throws() {
        when(workerRepository.findLockedById(1L)).thenReturn(Optional.of(worker));
        WorkLogRequestDTO d = sprayDto();
        d.setStartDate(today.toString());
        d.setEndDate(today.plusDays(1).toString());

        assertAddLogFails(d, IllegalArgumentException.class);
        verify(workLogRepository, never()).save(any());
    }

    // ---------- audit log ----------

    @Test
    void addLog_workerWithoutUserAccount_auditsWithFullName() {
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.toString());

        service.addLog(d);

        verify(auditLogService).logAction(eq("CREATE_WORKLOG"), eq("superadmin"), eq("Worker One"), any());
    }

    @Test
    void addLog_workerWithUserAccount_auditsWithUsername() {
        User user = new User();
        user.setUsername("worker1");
        worker.setUser(user);
        stubWorkerAndSave();
        WorkLogRequestDTO d = sprayDto();
        d.setDate(today.toString());

        service.addLog(d);

        verify(auditLogService).logAction(eq("CREATE_WORKLOG"), eq("superadmin"), eq("worker1"), any());
    }

    // ---------- calculateWage ----------

    private void assertWageFails(WorkLogRequestDTO d, Integer days) {
        assertThrows(IllegalArgumentException.class, () -> service.calculateWage(d, days));
    }

    @Test
    void calculateWage_cutting_rejectsNullOrNonPositiveValues() {
        for (Integer[] values : new Integer[][]{{null, 5}, {0, 5}, {-1, 5}, {5, null}, {5, 0}, {5, -1}}) {
            WorkLogRequestDTO d = dto("cutting");
            d.setRows(values[0]);
            d.setWaPerRow(values[1]);
            assertWageFails(d, null);
        }
    }

    @Test
    void calculateWage_planting_rejectsNullOrNonPositiveValues() {
        for (Integer[] values : new Integer[][]{{null, 5}, {0, 5}, {5, null}, {5, 0}}) {
            WorkLogRequestDTO d = dto("planting");
            d.setFurrows(values[0]);
            d.setWaPerFurrow(values[1]);
            assertWageFails(d, null);
        }
    }

    @Test
    void calculateWage_watering_rejectsInvalidDaysOrRate() {
        WorkLogRequestDTO zeroDays = dto("watering");
        zeroDays.setDailyRate(BigDecimal.TEN);
        assertWageFails(zeroDays, 0);

        WorkLogRequestDTO zeroRate = dto("watering");
        zeroRate.setDailyRate(BigDecimal.ZERO);
        assertWageFails(zeroRate, 2);

        WorkLogRequestDTO noRate = dto("watering");
        assertWageFails(noRate, 2);

        WorkLogRequestDTO noDaysAnywhere = dto("watering");
        noDaysAnywhere.setDailyRate(BigDecimal.TEN);
        assertWageFails(noDaysAnywhere, null);
    }

    @Test
    void calculateWage_watering_fallsBackToDtoDays() {
        WorkLogRequestDTO d = dto("watering");
        d.setDays(4);
        d.setDailyRate(BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(200).compareTo(service.calculateWage(d, null)));
    }

    @Test
    void calculateWage_spraying_rejectsNullOrNonPositiveTanks() {
        WorkLogRequestDTO none = dto("spraying");
        assertWageFails(none, null);

        WorkLogRequestDTO zero = dto("spraying");
        zero.setTanks(0);
        assertWageFails(zero, null);
    }
}

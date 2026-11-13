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

/** Fills the branch gaps left by {@link WorkLogServiceTest}. */
@ExtendWith(MockitoExtension.class)
class WorkLogServiceBranchTest {

    @Mock
    private WorkLogRepository workLogRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;

    @InjectMocks
    private WorkLogService service;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setFarmId("10");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private WorkLog log(long id, Worker w) {
        WorkLog l = new WorkLog();
        l.setId(id);
        l.setWorker(w);
        l.setType("cutting");
        l.setWorkDate(LocalDate.of(2026, 1, 1));
        l.setTotal(BigDecimal.TEN);
        return l;
    }

    // ---------- getAllLogs ----------

    @Test
    void getAllLogs_noAuthentication_returnsAll() {
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(log(1, worker)));

        assertEquals(1, service.getAllLogs().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllLogs_unauthenticatedToken_returnsAll() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "pw"));
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(log(1, worker)));

        assertEquals(1, service.getAllLogs().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllLogs_anonymousUser_returnsAll() {
        loginAs("anonymousUser");
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(log(1, worker)));

        assertEquals(1, service.getAllLogs().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllLogs_adminWithoutProfile_returnsAll() {
        loginAs("ghost");
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(log(1, worker)));
        when(adminProfileRepository.findByUserUsername("ghost")).thenReturn(Optional.empty());

        assertEquals(1, service.getAllLogs().size());
    }

    @Test
    void getAllLogs_adminOtherFarm_filtersOutForeignLogs() {
        Worker other = new Worker();
        other.setId(2L);
        other.setFarmId("99");
        loginAs("adminuser");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId("10");
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc())
                .thenReturn(List.of(log(1, worker), log(2, other)));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        List<Map<String, Object>> result = service.getAllLogs();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).get("id"));
    }

    @Test
    void getAllLogs_mapsAllOptionalFields() {
        WorkLog l = log(5, worker);
        l.setRows(3);
        l.setWaPerRow(4);
        l.setFurrows(5);
        l.setWaPerFurrow(6);
        l.setDays(7);
        l.setDailyRate(BigDecimal.valueOf(300));
        l.setTanks(8);
        l.setStartDate(LocalDate.of(2026, 1, 1));
        l.setEndDate(LocalDate.of(2026, 1, 7));
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(l));

        Map<String, Object> map = service.getAllLogs().get(0);

        assertEquals(3, map.get("rows"));
        assertEquals(4, map.get("waPerRow"));
        assertEquals(5, map.get("furrows"));
        assertEquals(6, map.get("waPerFurrow"));
        assertEquals(7, map.get("days"));
        assertEquals(300.0, map.get("dailyRate"));
        assertEquals(8, map.get("tanks"));
        assertEquals("2026-01-01", map.get("startDate"));
        assertEquals("2026-01-07", map.get("endDate"));
    }

    @Test
    void getAllLogs_omitsNullOptionalFields() {
        when(workLogRepository.findAllByOrderByWorkDateDescIdDesc()).thenReturn(List.of(log(5, worker)));

        Map<String, Object> map = service.getAllLogs().get(0);

        for (String key : List.of("rows", "waPerRow", "furrows", "waPerFurrow", "days", "dailyRate", "tanks",
                "startDate", "endDate")) {
            assertFalse(map.containsKey(key), key);
        }
    }

    // ---------- validation ----------

    private WorkLogRequestDTO dto(String type) {
        WorkLogRequestDTO d = new WorkLogRequestDTO();
        d.setType(type);
        d.setWorkerId("1");
        return d;
    }

    private void assertAddLogFails(WorkLogRequestDTO d, String message) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addLog(d));
        assertEquals(message, ex.getMessage());
    }

    @Test
    void addLog_negativeRows_throws() {
        WorkLogRequestDTO d = dto("cutting");
        d.setRows(-1);
        assertAddLogFails(d, "rows cannot be negative");
    }

    @Test
    void addLog_negativeWaPerRow_throws() {
        WorkLogRequestDTO d = dto("cutting");
        d.setWaPerRow(-1);
        assertAddLogFails(d, "waPerRow cannot be negative");
    }

    @Test
    void addLog_negativeWaPerFurrow_throws() {
        WorkLogRequestDTO d = dto("planting");
        d.setWaPerFurrow(-1);
        assertAddLogFails(d, "waPerFurrow cannot be negative");
    }

    @Test
    void addLog_negativeDays_throws() {
        WorkLogRequestDTO d = dto("watering");
        d.setDays(-1);
        assertAddLogFails(d, "days cannot be negative");
    }

    @Test
    void addLog_negativeTanks_throws() {
        WorkLogRequestDTO d = dto("spraying");
        d.setTanks(-1);
        assertAddLogFails(d, "tanks cannot be negative");
    }

    // ---------- addLog paths ----------

    @Test
    void addLog_zeroValuesPassValidation_workerWithoutFarm() {
        worker.setFarmId(null);
        WorkLogRequestDTO d = dto("cutting");
        d.setDate("2026-02-01");
        d.setRows(0);
        d.setWaPerRow(0);
        d.setFurrows(0);
        d.setWaPerFurrow(0);
        d.setDays(0);
        d.setDailyRate(BigDecimal.ZERO);
        d.setTanks(0);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = service.addLog(d);

        assertEquals(0.0, result.get("total"));
        assertEquals("2026-02-01", result.get("date"));
        verifyNoInteractions(farmRepository);
    }

    @Test
    void addLog_farmNotFound_stillSavesLog() {
        WorkLogRequestDTO d = dto("spraying");
        d.setDate("2026-02-01");
        d.setTanks(2);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> i.getArgument(0));
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.addLog(d);

        assertEquals(300.0, result.get("total"));
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    void addLog_watering_daysWithExplicitDate_usesThatDate() {
        worker.setFarmId(null);
        WorkLogRequestDTO d = dto("watering");
        d.setDays(2);
        d.setDate("2026-03-05");
        d.setDailyRate(BigDecimal.valueOf(100));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = service.addLog(d);

        assertEquals("2026-03-05", result.get("date"));
        assertEquals(200.0, result.get("total"));
    }

    @Test
    void addLog_watering_onlyStartDateWithDays_fallsBackToDays() {
        worker.setFarmId(null);
        WorkLogRequestDTO d = dto("watering");
        d.setStartDate("2026-03-01");
        d.setDays(3);
        d.setDailyRate(BigDecimal.valueOf(100));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workLogRepository.save(any(WorkLog.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = service.addLog(d);

        assertEquals(LocalDate.now().toString(), result.get("date"));
        assertEquals(300.0, result.get("total"));
    }

    @Test
    void addLog_watering_onlyEndDateWithoutDays_throws() {
        WorkLogRequestDTO d = dto("watering");
        d.setEndDate("2026-03-01");
        d.setDailyRate(BigDecimal.TEN);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addLog(d));
        assertEquals("Watering requires startDate/endDate or days", ex.getMessage());
    }

    // ---------- calculateWage ----------

    @Test
    void calculateWage_cutting_eachMissingField() {
        WorkLogRequestDTO onlyRows = dto("cutting");
        onlyRows.setRows(2);
        WorkLogRequestDTO onlyWa = dto("cutting");
        onlyWa.setWaPerRow(2);

        assertThrows(RuntimeException.class, () -> service.calculateWage(onlyRows, null));
        assertThrows(RuntimeException.class, () -> service.calculateWage(onlyWa, null));
    }

    @Test
    void calculateWage_planting_eachMissingField() {
        WorkLogRequestDTO onlyFurrows = dto("planting");
        onlyFurrows.setFurrows(2);
        WorkLogRequestDTO onlyWa = dto("planting");
        onlyWa.setWaPerFurrow(2);

        assertThrows(RuntimeException.class, () -> service.calculateWage(onlyFurrows, null));
        assertThrows(RuntimeException.class, () -> service.calculateWage(onlyWa, null));
    }

    @Test
    void calculateWage_watering_usesDtoDaysWhenCalculatedDaysMissing() {
        WorkLogRequestDTO d = dto("watering");
        d.setDays(4);
        d.setDailyRate(BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(200).compareTo(service.calculateWage(d, null)));
    }

    @Test
    void calculateWage_watering_noDaysAnywhere_throws() {
        WorkLogRequestDTO d = dto("watering");
        d.setDailyRate(BigDecimal.valueOf(50));

        assertThrows(RuntimeException.class, () -> service.calculateWage(d, null));
    }

    @Test
    void calculateWage_watering_missingDailyRate_throws() {
        WorkLogRequestDTO d = dto("watering");

        assertThrows(RuntimeException.class, () -> service.calculateWage(d, 3));
    }
}

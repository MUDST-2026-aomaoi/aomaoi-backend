package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AttendanceBulkRequestDTO;
import com.aomaoi.backend.dto.AttendanceRequestDTO;
import com.aomaoi.backend.dto.AttendanceResponseDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Attendance;
import com.aomaoi.backend.entity.AttendanceStatus;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.AttendanceRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    // 2026-11-12 12:00 in Bangkok
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T05:00:00Z"), ZoneId.of("Asia/Bangkok"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final java.time.LocalTime CLOSING = java.time.LocalTime.of(18, 0);

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private AuditLogService auditLogService;

    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(attendanceRepository, workerRepository, adminProfileRepository, auditLogService, CLOCK, CLOSING);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void adminOfFarm(long farmId) {
        loginAs("admin1", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(farmId);
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));
    }

    private static Worker worker(long id, long farmId, String status, LocalDate joined) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFullName("Worker " + id);
        worker.setFarmId(farmId);
        worker.setStatus(status);
        worker.setJoinedDate(joined);
        User user = new User();
        user.setUsername("w" + id);
        worker.setUser(user);
        return worker;
    }

    private static AttendanceRequestDTO request(long workerId, LocalDate date, String status, String note) {
        AttendanceRequestDTO dto = new AttendanceRequestDTO();
        dto.setWorkerId(workerId);
        dto.setDate(date);
        dto.setStatus(status);
        dto.setNote(note);
        return dto;
    }

    private void workerExists(Worker worker) {
        when(workerRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
    }

    // ---- set ----

    @Test
    void adminMarksAWorkerOfTheirFarmAsPresent() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(10L, TODAY)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceResponseDTO result = service.set(request(10, TODAY, "present", null));

        assertEquals("PRESENT", result.status());
        assertFalse(result.autoRecorded());
        assertEquals("admin1", result.recordedBy());
        verify(auditLogService).logAction("ATTENDANCE_SET", "admin1", "w10", TODAY + ": - -> PRESENT");
    }

    @Test
    void changingAnExistingRowKeepsOneRowAndAuditsTheChange() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        Attendance existing = new Attendance();
        existing.setWorker(worker);
        existing.setAttendanceDate(TODAY);
        existing.setStatus(AttendanceStatus.ABSENT);
        existing.setAutoRecorded(true);
        existing.setRecordedBy("system");
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(10L, TODAY)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);

        AttendanceResponseDTO result = service.set(request(10, TODAY, "LEAVE", "  ป่วย โทรบอกเช้านี้ "));

        assertEquals("LEAVE", result.status());
        assertEquals("ป่วย โทรบอกเช้านี้", result.note());
        assertFalse(existing.isAutoRecorded(), "an Admin edit is no longer auto-recorded");
        assertEquals("admin1", existing.getRecordedBy());
        verify(auditLogService).logAction("ATTENDANCE_SET", "admin1", "w10", TODAY + ": ABSENT -> LEAVE");
    }

    @Test
    void nullStatusClearsTheDay() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        Attendance existing = new Attendance();
        existing.setWorker(worker);
        existing.setStatus(AttendanceStatus.PRESENT);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(10L, TODAY)).thenReturn(Optional.of(existing));

        AttendanceResponseDTO result = service.set(request(10, TODAY, null, null));

        assertNull(result.status());
        verify(attendanceRepository).delete(existing);
        verify(auditLogService).logAction("ATTENDANCE_SET", "admin1", "w10", TODAY + ": PRESENT -> -");
    }

    @Test
    void aPastDayCanNoLongerBeChanged() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        LocalDate yesterday = TODAY.minusDays(1);

        for (String status : new String[]{"PRESENT", "ABSENT", "LEAVE", null}) {
            assertThrows(IllegalArgumentException.class, () -> service.set(request(10, yesterday, status, null)),
                    String.valueOf(status));
        }
        verify(attendanceRepository, never()).save(any());
        verify(attendanceRepository, never()).delete(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void aFutureDayAcceptsOnlyLeaveAndClearingIt() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        LocalDate tomorrow = TODAY.plusDays(1);
        Attendance planned = new Attendance();
        planned.setWorker(worker);
        planned.setAttendanceDate(tomorrow);
        planned.setStatus(AttendanceStatus.LEAVE);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(10L, tomorrow))
                .thenReturn(Optional.empty(), Optional.of(planned));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> service.set(request(10, tomorrow, "PRESENT", null)));
        assertThrows(IllegalArgumentException.class, () -> service.set(request(10, tomorrow, "ABSENT", null)));
        assertEquals("LEAVE", service.set(request(10, tomorrow, "LEAVE", "แจ้งล่วงหน้า")).status());
        // cancelling the planned leave is allowed
        assertNull(service.set(request(10, tomorrow, null, null)).status());
        verify(attendanceRepository).delete(planned);
    }

    @Test
    void today_inBangkokIsNotYesterday_evenWhenUtcIsStillYesterday() {
        // 02:00 Bangkok on the 13th is still the 12th in UTC; "today" is the 13th.
        Clock early = Clock.fixed(Instant.parse("2026-11-12T19:00:00Z"), ZoneId.of("Asia/Bangkok"));
        service = new AttendanceService(attendanceRepository, workerRepository, adminProfileRepository, auditLogService, early, CLOSING);
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(anyLong(), any())).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("PRESENT", service.set(request(10, LocalDate.of(2026, 11, 13), "PRESENT", null)).status());
        // the 12th is already yesterday in Bangkok
        assertThrows(IllegalArgumentException.class, () -> service.set(request(10, LocalDate.of(2026, 11, 12), "PRESENT", null)));
    }

    @Test
    void rejectsADayBeforeTheWorkerJoined() {
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.plusDays(1));
        workerExists(worker);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(anyLong(), any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.set(request(10, TODAY, "ABSENT", null)));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void rejectsAnInactiveWorkerAndABadStatusAndALongNote() {
        adminOfFarm(1);
        Worker inactive = worker(10, 1, "inactive", TODAY.minusDays(30));
        workerExists(inactive);
        assertThrows(IllegalArgumentException.class, () -> service.set(request(10, TODAY, "PRESENT", null)));

        Worker active = worker(11, 1, "active", TODAY.minusDays(30));
        workerExists(active);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(anyLong(), any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.set(request(11, TODAY, "HOLIDAY", null)));
        assertThrows(IllegalArgumentException.class, () -> service.set(request(11, TODAY, "LEAVE", "x".repeat(256))));
    }

    @Test
    void anAdminCannotTouchAWorkerOfAnotherFarm() {
        adminOfFarm(1);
        workerExists(worker(20, 2, "active", TODAY.minusDays(30)));

        assertThrows(AccessDeniedException.class, () -> service.set(request(20, TODAY, "PRESENT", null)));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void aWorkerRoleIsRejectedEvenIfThePathRuleIsBypassed() {
        loginAs("w10", "WORKER");
        workerExists(worker(10, 1, "active", TODAY.minusDays(30)));

        assertThrows(AccessDeniedException.class, () -> service.set(request(10, TODAY, "PRESENT", null)));
        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, null));
    }

    @Test
    void unknownWorkerIsNotFound() {
        loginAs("admin1", "ADMIN");
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.set(request(99, TODAY, "PRESENT", null)));
    }

    @Test
    void superAdminCanMarkAnyFarm() {
        loginAs("root", "SUPERADMIN");
        Worker worker = worker(20, 2, "active", TODAY.minusDays(30));
        workerExists(worker);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(20L, TODAY)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("ABSENT", service.set(request(20, TODAY, "ABSENT", null)).status());
    }

    // ---- list / history ----

    @Test
    void adminListsOnlyTheirOwnFarmAndSuperAdminMustNameTheFarm() {
        adminOfFarm(1);
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, TODAY)).thenReturn(List.of());
        assertTrue(service.listForDate(TODAY, 999L).isEmpty(), "the farmId parameter is ignored for an Admin");
        verify(attendanceRepository).findByWorkerFarmIdAndAttendanceDate(1L, TODAY);

        loginAs("root", "SUPERADMIN");
        assertThrows(IllegalArgumentException.class, () -> service.listForDate(TODAY, null));
    }

    @Test
    void historyReturnsTheMonthForAnAccessibleWorker() {
        adminOfFarm(1);
        workerExists(worker(10, 1, "active", TODAY.minusDays(90)));
        when(attendanceRepository.findByWorkerIdAndAttendanceDateBetweenOrderByAttendanceDate(
                10L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of());

        assertTrue(service.history(10L, YearMonth.of(2026, 11)).isEmpty());

        workerExists(worker(20, 2, "active", TODAY.minusDays(90)));
        assertThrows(AccessDeniedException.class, () -> service.history(20L, YearMonth.of(2026, 11)));
    }

    // ---- bulk ----

    @Test
    void bulkPresentAddsOnlyUncheckedWorkersWhoHadJoined() {
        adminOfFarm(1);
        Worker checked = worker(1, 1, "active", TODAY.minusDays(30));
        Worker unchecked = worker(2, 1, "pending", TODAY.minusDays(30));
        Worker joinedLater = worker(3, 1, "active", TODAY.plusDays(0).plusDays(5));
        Attendance existing = new Attendance();
        existing.setWorker(checked);
        existing.setStatus(AttendanceStatus.LEAVE);
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, TODAY)).thenReturn(List.of(existing));
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive")).thenReturn(List.of(checked, unchecked, joinedLater));

        AttendanceBulkRequestDTO dto = new AttendanceBulkRequestDTO();
        dto.setDate(TODAY);
        int added = service.markUncheckedPresent(dto);

        assertEquals(1, added);
        ArgumentCaptor<Attendance> saved = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository, times(1)).save(saved.capture());
        assertEquals(2L, saved.getValue().getWorker().getId());
        assertEquals(AttendanceStatus.PRESENT, saved.getValue().getStatus());
        verify(auditLogService).logAction(eq("ATTENDANCE_BULK_PRESENT"), eq("admin1"), eq("farm:1"), anyString());
    }

    @Test
    void bulkPresentIsRefusedForAnyDayButToday() {
        adminOfFarm(1);
        for (LocalDate other : List.of(TODAY.minusDays(1), TODAY.plusDays(1))) {
            AttendanceBulkRequestDTO dto = new AttendanceBulkRequestDTO();
            dto.setDate(other);
            assertThrows(IllegalArgumentException.class, () -> service.markUncheckedPresent(dto));
        }
        verify(attendanceRepository, never()).save(any());
    }

    // ---- closing time (18:00) ----

    private AttendanceService serviceAt(String instant) {
        Clock at = Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Bangkok"));
        return new AttendanceService(attendanceRepository, workerRepository, adminProfileRepository, auditLogService, at, CLOSING);
    }

    @Test
    void todayIsOpenUntilSixPmBangkokAndLockedFromThen() {
        assertTrue(serviceAt("2026-11-12T10:59:59Z").isTodayOpen()); // 17:59:59
        assertFalse(serviceAt("2026-11-12T11:00:00Z").isTodayOpen()); // 18:00:00
        assertFalse(serviceAt("2026-11-12T16:59:00Z").isTodayOpen()); // 23:59
        assertTrue(serviceAt("2026-11-12T17:00:00Z").isTodayOpen()); // 00:00 next day: a new day has started
    }

    @Test
    void afterTheClosingTimeTodayCannotBeChangedClearedOrBulkMarked() {
        service = serviceAt("2026-11-12T11:00:01Z"); // 18:00:01 on the 12th
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);

        for (String status : new String[]{"PRESENT", "ABSENT", "LEAVE", null}) {
            assertThrows(IllegalArgumentException.class, () -> service.set(request(10, TODAY, status, null)), String.valueOf(status));
        }
        AttendanceBulkRequestDTO bulk = new AttendanceBulkRequestDTO();
        bulk.setDate(TODAY);
        assertThrows(IllegalArgumentException.class, () -> service.markUncheckedPresent(bulk));
        verify(attendanceRepository, never()).save(any());
        verify(attendanceRepository, never()).delete(any());
    }

    @Test
    void afterTheClosingTimeALeaveCanStillBeRegisteredForTomorrow() {
        service = serviceAt("2026-11-12T11:00:01Z");
        adminOfFarm(1);
        Worker worker = worker(10, 1, "active", TODAY.minusDays(30));
        workerExists(worker);
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(anyLong(), any())).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("LEAVE", service.set(request(10, TODAY.plusDays(1), "LEAVE", "แจ้งล่วงหน้า")).status());
    }
}

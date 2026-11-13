package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AttendanceBulkRequestDTO;
import com.aomaoi.backend.dto.AttendanceRequestDTO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

/** Fills the branch gaps left by {@link AttendanceServiceTest}. */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceBranchTest {

    // 2026-11-12 12:00 in Bangkok
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T05:00:00Z"), ZoneId.of("Asia/Bangkok"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

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
        service = new AttendanceService(attendanceRepository, workerRepository, adminProfileRepository,
                auditLogService, CLOCK, LocalTime.of(18, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static Worker worker(long id, Long farmId) {
        Worker w = new Worker();
        w.setId(id);
        w.setFullName("Worker " + id);
        w.setFarmId(farmId);
        w.setStatus("active");
        return w;
    }

    private static AttendanceRequestDTO request(Long workerId, LocalDate date, String status, String note) {
        AttendanceRequestDTO dto = new AttendanceRequestDTO();
        dto.setWorkerId(workerId);
        dto.setDate(date);
        dto.setStatus(status);
        dto.setNote(note);
        return dto;
    }

    // ---------- listForDate / history ----------

    @Test
    void listForDate_nullDate_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.listForDate(null, 1L));
    }

    @Test
    void listForDate_superadminWithoutFarmId_throws() {
        loginAs("root", "SUPERADMIN");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.listForDate(TODAY, null));
        assertEquals("farmId is required", ex.getMessage());
    }

    @Test
    void listForDate_superadminWithFarmId_usesGivenFarm() {
        loginAs("root", "SUPERADMIN");
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(5L, TODAY)).thenReturn(List.of());

        assertTrue(service.listForDate(TODAY, 5L).isEmpty());
    }

    @Test
    void listForDate_workerRole_denied() {
        loginAs("w1", "WORKER");

        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, 1L));
    }

    @Test
    void listForDate_unauthenticated_denied() {
        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, 1L));
    }

    @Test
    void listForDate_anonymousUser_denied() {
        loginAs("anonymousUser", "ANONYMOUS");

        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, 1L));
    }

    @Test
    void listForDate_unauthenticatedToken_denied() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));

        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, 1L));
    }

    @Test
    void listForDate_adminWithInactiveProfile_denied() {
        loginAs("admin1", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(1L);
        profile.setStatus("inactive");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, null));
    }

    @Test
    void listForDate_adminWithoutFarm_denied() {
        loginAs("admin1", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> service.listForDate(TODAY, null));
    }

    @Test
    void history_nullMonth_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.history(1L, null));
    }

    @Test
    void history_unknownWorker_throws() {
        when(workerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.history(9L, YearMonth.of(2026, 11)));
    }

    @Test
    void history_adminOfOtherFarm_denied() {
        loginAs("admin1", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(1L);
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));
        when(workerRepository.findById(2L)).thenReturn(Optional.of(worker(2L, 99L)));

        assertThrows(AccessDeniedException.class, () -> service.history(2L, YearMonth.of(2026, 11)));
    }

    @Test
    void history_adminWorkerWithoutFarm_denied() {
        loginAs("admin1", "ADMIN");
        when(workerRepository.findById(2L)).thenReturn(Optional.of(worker(2L, null)));

        assertThrows(AccessDeniedException.class, () -> service.history(2L, YearMonth.of(2026, 11)));
    }

    // ---------- set ----------

    @Test
    void set_nullDto_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.set(null));
    }

    @Test
    void set_missingWorkerId_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.set(request(null, TODAY, "PRESENT", null)));
    }

    @Test
    void set_missingDate_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.set(request(1L, null, "PRESENT", null)));
    }

    @Test
    void set_clearingWithBlankStatus_deletesExistingRowAndAuditsAgainstUserlessWorker() {
        loginAs("root", "SUPERADMIN");
        Worker w = worker(1L, 1L); // no User -> audit target falls back to the id
        Attendance existing = new Attendance();
        existing.setWorker(w);
        existing.setStatus(AttendanceStatus.PRESENT);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(w));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.of(existing));

        var response = service.set(request(1L, TODAY, "  ", null));

        assertNull(response.status());
        verify(attendanceRepository).delete(existing);
        verify(auditLogService).logAction(eq("ATTENDANCE_SET"), eq("root"), eq("1"), anyString());
    }

    @Test
    void set_clearingWhenNothingRecorded_doesNotDelete() {
        loginAs("root", "SUPERADMIN");
        Worker w = worker(1L, 1L);
        User user = new User();
        user.setUsername("w1");
        w.setUser(user);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(w));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.empty());

        service.set(request(1L, TODAY, null, null));

        verify(attendanceRepository, never()).delete(any());
        verify(auditLogService).logAction(eq("ATTENDANCE_SET"), eq("root"), eq("w1"), anyString());
    }

    @Test
    void set_invalidStatus_throws() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 1L)));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.set(request(1L, TODAY, "SICK", null)));
        assertEquals("status must be PRESENT, LEAVE or ABSENT", ex.getMessage());
    }

    @Test
    void set_futureDayWithNonLeaveStatus_throws() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 1L)));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY.plusDays(1))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.set(request(1L, TODAY.plusDays(1), "present", null)));
    }

    @Test
    void set_noteTooLong_throws() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 1L)));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.set(request(1L, TODAY, "PRESENT", "x".repeat(256))));
    }

    @Test
    void set_blankNote_isStoredAsNull_andTrimmedNoteIsKept() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker(1L, 1L)));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        assertNull(service.set(request(1L, TODAY, "PRESENT", "   ")).note());
        assertEquals("late", service.set(request(1L, TODAY, "PRESENT", "  late ")).note());
    }

    @Test
    void set_existingRowIsUpdatedInPlace() {
        loginAs("root", "SUPERADMIN");
        Worker w = worker(1L, 1L);
        Attendance existing = new Attendance();
        existing.setWorker(w);
        existing.setStatus(AttendanceStatus.PRESENT);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(w));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(existing)).thenReturn(existing);

        var response = service.set(request(1L, TODAY, "ABSENT", null));

        assertEquals("ABSENT", response.status());
        assertEquals(AttendanceStatus.ABSENT, existing.getStatus());
        assertEquals("root", existing.getRecordedBy());
    }

    @Test
    void set_dateBeforeJoinedDate_throws() {
        loginAs("root", "SUPERADMIN");
        Worker w = worker(1L, 1L);
        w.setJoinedDate(TODAY.plusDays(1));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(w));
        when(attendanceRepository.findByWorkerIdAndAttendanceDate(1L, TODAY)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.set(request(1L, TODAY, "PRESENT", null)));
    }

    // ---------- markUncheckedPresent ----------

    @Test
    void bulk_nullDto_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.markUncheckedPresent(null));
    }

    @Test
    void bulk_nullDate_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.markUncheckedPresent(new AttendanceBulkRequestDTO()));
    }

    @Test
    void bulk_skipsAlreadyCheckedAndNotYetJoinedWorkers() {
        loginAs("root", "SUPERADMIN");
        AttendanceBulkRequestDTO dto = new AttendanceBulkRequestDTO();
        dto.setDate(TODAY);
        dto.setFarmId(1L);

        Worker checked = worker(1L, 1L);
        Worker notYetJoined = worker(2L, 1L);
        notYetJoined.setJoinedDate(TODAY.plusDays(3));
        Worker joinedEarlier = worker(3L, 1L);
        joinedEarlier.setJoinedDate(TODAY.minusDays(3));
        Worker noJoinedDate = worker(4L, 1L);

        Attendance already = new Attendance();
        already.setWorker(checked);
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, TODAY)).thenReturn(List.of(already));
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive"))
                .thenReturn(List.of(checked, notYetJoined, joinedEarlier, noJoinedDate));

        int added = service.markUncheckedPresent(dto);

        assertEquals(2, added);
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
    }

    @Test
    void bulk_notToday_throws() {
        loginAs("root", "SUPERADMIN");
        AttendanceBulkRequestDTO dto = new AttendanceBulkRequestDTO();
        dto.setDate(TODAY.minusDays(1));
        dto.setFarmId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.markUncheckedPresent(dto));
    }

    @Test
    void bulk_afterClosingTime_throws() {
        Clock evening = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneId.of("Asia/Bangkok")); // 19:00
        AttendanceService late = new AttendanceService(attendanceRepository, workerRepository,
                adminProfileRepository, auditLogService, evening, LocalTime.of(18, 0));
        loginAs("root", "SUPERADMIN");
        AttendanceBulkRequestDTO dto = new AttendanceBulkRequestDTO();
        dto.setDate(TODAY);
        dto.setFarmId(1L);

        assertFalse(late.isTodayOpen());
        assertThrows(IllegalArgumentException.class, () -> late.markUncheckedPresent(dto));
    }
}

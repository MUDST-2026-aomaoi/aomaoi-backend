package com.aomaoi.backend.entity;

import com.aomaoi.backend.config.BusinessTime;
import com.aomaoi.backend.dto.WorkLogResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {

    private static User userNamed(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    // ---------- Worker ----------

    @Test
    void worker_onCreate_setsJoinedDateWhenMissing() {
        Worker worker = new Worker();

        worker.onCreate();

        assertEquals(BusinessTime.today(), worker.getJoinedDate());
    }

    @Test
    void worker_onCreate_keepsExistingJoinedDate() {
        Worker worker = new Worker();
        LocalDate joined = LocalDate.of(2024, 1, 15);
        worker.setJoinedDate(joined);

        worker.onCreate();

        assertEquals(joined, worker.getJoinedDate());
    }

    @Test
    void worker_getUsername_returnsUserUsernameOrNull() {
        Worker worker = new Worker();
        assertNull(worker.getUsername());

        worker.setUser(userNamed("worker1"));
        assertEquals("worker1", worker.getUsername());
    }

    // ---------- AdminProfile ----------

    @Test
    void adminProfile_onCreate_setsJoinedDateWhenMissing() {
        AdminProfile admin = new AdminProfile();

        admin.onCreate();

        assertEquals(BusinessTime.today(), admin.getJoinedDate());
    }

    @Test
    void adminProfile_onCreate_keepsExistingJoinedDate() {
        AdminProfile admin = new AdminProfile();
        LocalDate joined = LocalDate.of(2023, 6, 1);
        admin.setJoinedDate(joined);

        admin.onCreate();

        assertEquals(joined, admin.getJoinedDate());
    }

    @Test
    void adminProfile_getUsername_returnsUserUsernameOrNull() {
        AdminProfile admin = new AdminProfile();
        assertNull(admin.getUsername());

        admin.setUser(userNamed("admin1"));
        assertEquals("admin1", admin.getUsername());
    }

    // ---------- AuditLog ----------

    @Test
    void auditLog_onCreate_setsTimestampWhenMissing() {
        AuditLog log = new AuditLog();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        log.onCreate();

        assertNotNull(log.getTimestamp());
        assertFalse(log.getTimestamp().isBefore(before));
    }

    @Test
    void auditLog_onCreate_keepsExistingTimestamp() {
        AuditLog log = new AuditLog();
        LocalDateTime ts = LocalDateTime.of(2024, 5, 20, 10, 30);
        log.setTimestamp(ts);

        log.onCreate();

        assertEquals(ts, log.getTimestamp());
    }

    // ---------- Attendance ----------

    @Test
    void attendance_onCreate_stampsCreatedAndUpdated() {
        Attendance attendance = new Attendance();
        Instant before = Instant.now().minusSeconds(1);

        attendance.onCreate();

        assertNotNull(attendance.getCreatedAt());
        assertEquals(attendance.getCreatedAt(), attendance.getUpdatedAt());
        assertFalse(attendance.getCreatedAt().isBefore(before));
    }

    @Test
    void attendance_onUpdate_refreshesUpdatedAtOnly() throws Exception {
        Attendance attendance = new Attendance();
        attendance.onCreate();
        Instant created = attendance.getCreatedAt();
        Thread.sleep(5);

        attendance.onUpdate();

        assertEquals(created, attendance.getCreatedAt());
        assertTrue(attendance.getUpdatedAt().isAfter(created));
    }

    // ---------- WorkLogResponseDTO ----------

    private WorkLog workLog() {
        Worker worker = new Worker();
        worker.setId(4L);
        WorkLog log = new WorkLog();
        log.setId(9L);
        log.setWorker(worker);
        log.setType("watering");
        log.setWorkDate(LocalDate.of(2026, 3, 10));
        log.setTotal(BigDecimal.TEN);
        return log;
    }

    @Test
    void workLogResponse_withoutRange_usesWorkDateForStartAndEnd() {
        WorkLogResponseDTO dto = WorkLogResponseDTO.from(workLog());

        assertEquals("9", dto.id());
        assertEquals("4", dto.workerId());
        assertEquals("2026-03-10", dto.date());
        assertEquals("2026-03-10", dto.startDate());
        assertEquals("2026-03-10", dto.endDate());
    }

    @Test
    void workLogResponse_withRange_usesStartAndEnd() {
        WorkLog log = workLog();
        log.setStartDate(LocalDate.of(2026, 3, 1));
        log.setEndDate(LocalDate.of(2026, 3, 5));

        WorkLogResponseDTO dto = WorkLogResponseDTO.from(log);

        assertEquals("2026-03-01", dto.startDate());
        assertEquals("2026-03-05", dto.endDate());
    }

    @Test
    void workLogResponse_startWithoutEnd_endDefaultsToStart() {
        WorkLog log = workLog();
        log.setStartDate(LocalDate.of(2026, 3, 2));

        WorkLogResponseDTO dto = WorkLogResponseDTO.from(log);

        assertEquals("2026-03-02", dto.startDate());
        assertEquals("2026-03-02", dto.endDate());
    }

    @Test
    void workLogResponse_endWithoutStart_keepsEndDate() {
        WorkLog log = workLog();
        log.setEndDate(LocalDate.of(2026, 3, 20));

        WorkLogResponseDTO dto = WorkLogResponseDTO.from(log);

        assertEquals("2026-03-10", dto.startDate());
        assertEquals("2026-03-20", dto.endDate());
    }
}

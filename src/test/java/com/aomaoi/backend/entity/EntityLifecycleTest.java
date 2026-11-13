package com.aomaoi.backend.entity;

import org.junit.jupiter.api.Test;

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

        assertEquals(LocalDate.now(), worker.getJoinedDate());
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

    @Test
    void worker_defaultStatusIsActive() {
        assertEquals("active", new Worker().getStatus());
    }

    // ---------- AdminProfile ----------

    @Test
    void adminProfile_onCreate_setsJoinedDateWhenMissing() {
        AdminProfile admin = new AdminProfile();

        admin.onCreate();

        assertEquals(LocalDate.now(), admin.getJoinedDate());
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

    @Test
    void adminProfile_defaultStatusIsPending() {
        assertEquals("pending", new AdminProfile().getStatus());
    }

    // ---------- Farm ----------

    @Test
    void farm_onCreate_setsJoinedDateWhenMissing() {
        Farm farm = new Farm();

        farm.onCreate();

        assertEquals(LocalDate.now(), farm.getJoinedDate());
    }

    @Test
    void farm_onCreate_keepsExistingJoinedDate() {
        Farm farm = new Farm();
        LocalDate joined = LocalDate.of(2022, 12, 31);
        farm.setJoinedDate(joined);

        farm.onCreate();

        assertEquals(joined, farm.getJoinedDate());
    }

    @Test
    void farm_defaults() {
        Farm farm = new Farm();

        assertEquals(0, farm.getWorkerCount());
        assertEquals(0, farm.getAdminCount());
        assertEquals(0.0, farm.getMonthlyWages());
        assertEquals(0.0, farm.getTotalWages());
        assertEquals("active", farm.getStatus());
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

    // ---------- Lombok @Data (equals / hashCode / toString) ----------

    @Test
    void lombokData_equalsHashCodeToString() {
        Farm a = new Farm();
        a.setId(1L);
        a.setName("F");
        Farm b = new Farm();
        b.setId(1L);
        b.setName("F");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("name=F"));

        b.setName("G");
        assertNotEquals(a, b);

        AuditLog l1 = new AuditLog();
        l1.setAction("A");
        AuditLog l2 = new AuditLog();
        l2.setAction("A");
        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());
        assertTrue(l1.toString().contains("action=A"));
    }
}

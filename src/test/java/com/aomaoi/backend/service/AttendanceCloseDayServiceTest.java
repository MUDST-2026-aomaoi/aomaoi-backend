package com.aomaoi.backend.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** The SQL itself is covered against a real PostgreSQL in PostgreSqlMigrationIntegrationTest. */
@ExtendWith(MockitoExtension.class)
class AttendanceCloseDayServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private AuditLogService auditLogService;

    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

    private AttendanceCloseDayService service() {
        return new AttendanceCloseDayService(jdbc, metrics, auditLogService);
    }

    private double count(String outcome) {
        var counter = metrics.find("aomaoi.attendance.close_day").tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void closesEveryActiveFarmAndAuditsTheTotal() {
        when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L, 2L));
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(3, 0);

        int created = service().closeAllFarms(DAY);

        assertEquals(3, created);
        verify(auditLogService).logAction(eq("ATTENDANCE_CLOSE_DAY"), eq("system"), eq("all-farms"), anyString());
        assertEquals(1, count("closed"));
        assertEquals(1, count("nothing"));
    }

    @Test
    void doesNotAuditWhenNothingWasCreatedOnADayOff() {
        when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L));
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);

        assertEquals(0, service().closeAllFarms(DAY));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void aFailingFarmDoesNotStopTheOthers() {
        when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L, 2L));
        when(jdbc.update(anyString(), any(Object[].class)))
                .thenThrow(new IllegalStateException("boom")).thenReturn(2);

        assertEquals(2, service().closeAllFarms(DAY));
        assertEquals(1, count("failed"));
    }

    @Test
    void closeFarmPassesTheDateAndFarmToEveryPlaceholder() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

        service().closeFarm(7L, DAY);

        verify(jdbc).update(anyString(), eq(DAY), eq(DAY), eq(DAY), eq(7L), eq(DAY), eq(7L), eq(DAY), eq(7L), eq(DAY), eq(DAY));
    }
}

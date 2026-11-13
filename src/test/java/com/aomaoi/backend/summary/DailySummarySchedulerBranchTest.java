package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.service.AttendanceCloseDayService;
import com.aomaoi.backend.service.AuditLogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link DailySummarySchedulerTest}. */
@ExtendWith(MockitoExtension.class)
class DailySummarySchedulerBranchTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T11:00:00Z"), ZoneId.of("Asia/Bangkok"));

    @Mock
    private AdminProfileRepository adminRepository;
    @Mock
    private DailySummaryService summaryService;
    @Mock
    private SummaryMailer mailer;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AttendanceCloseDayService closeDay;

    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    private DailySummaryScheduler scheduler;

    private final DailySummary summary =
            new DailySummary("ฟาร์ม", DAY, null, List.of("Anan"), List.of(), 1, 2, BigDecimal.ZERO);

    @BeforeEach
    void setUp() {
        scheduler = new DailySummaryScheduler(adminRepository, summaryService, new SummaryEmailBuilder(), mailer,
                jdbc, auditLogService, metrics, CLOCK, closeDay, true, false);
    }

    private static AdminProfile admin() {
        User user = new User();
        user.setUsername("admin7");
        AdminProfile admin = new AdminProfile();
        admin.setId(7L);
        admin.setEmail("a@example.com");
        admin.setFarmId(1L);
        admin.setUser(user);
        return admin;
    }

    private double count(String outcome) {
        var counter = metrics.find("aomaoi.daily_summary.emails").tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void runForAllAdmins_smtpNotConfigured_skipsWithoutQueryingAdmins() {
        when(mailer.isConfigured()).thenReturn(false);

        scheduler.runForAllAdmins(DAY);

        assertEquals(1, count("skipped"));
        verifyNoInteractions(adminRepository, summaryService, jdbc);
        verify(mailer, never()).send(anyString(), any());
    }

    @Test
    void sendTo_failureWithVeryLongMessage_truncatesStoredReasonTo500Chars() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);
        doThrow(new IllegalStateException("x".repeat(2000))).when(mailer).send(anyString(), any());

        var outcome = scheduler.sendTo(admin(), DAY);

        assertEquals(DailySummaryScheduler.Outcome.FAILED, outcome);
        verify(jdbc).update(contains("status = 'FAILED'"),
                org.mockito.ArgumentMatchers.argThat((String reason) -> reason.length() == 500),
                eq(7L), eq(DAY));
    }

    @Test
    void sendTo_failureWithoutMessage_recordsExceptionNameOnly() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);
        doThrow(new IllegalStateException()).when(mailer).send(anyString(), any());

        assertEquals(DailySummaryScheduler.Outcome.FAILED, scheduler.sendTo(admin(), DAY));
        verify(jdbc).update(contains("status = 'FAILED'"), eq("IllegalStateException: null"), eq(7L), eq(DAY));
    }
}

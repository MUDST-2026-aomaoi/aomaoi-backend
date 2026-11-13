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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySummarySchedulerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    // 18:00 in Bangkok = 11:00 UTC
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

    private final SummaryEmailBuilder emailBuilder = new SummaryEmailBuilder();
    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    private DailySummaryScheduler scheduler;

    private final DailySummary summary = new DailySummary("ฟาร์ม", DAY, null, List.of("Anan"), List.of(), 1, 2, BigDecimal.ZERO);

    @BeforeEach
    void setUp() {
        scheduler = newScheduler(true, false);
    }

    private DailySummaryScheduler newScheduler(boolean enabled, boolean closeDayEnabled) {
        return new DailySummaryScheduler(adminRepository, summaryService, emailBuilder, mailer, jdbc,
                auditLogService, metrics, CLOCK, closeDay, enabled, closeDayEnabled);
    }

    private static AdminProfile admin(Long id, String email, Long farmId) {
        User user = new User();
        user.setUsername("admin" + id);
        AdminProfile admin = new AdminProfile();
        admin.setId(id);
        admin.setEmail(email);
        admin.setFarmId(farmId);
        admin.setUser(user);
        return admin;
    }

    private double count(String outcome) {
        var counter = metrics.find("aomaoi.daily_summary.emails").tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void sendsTheSummaryAndMarksTheLogAsSent() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);

        var outcome = scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY);

        assertEquals(DailySummaryScheduler.Outcome.SENT, outcome);
        verify(mailer).send(eq("a@example.com"), any(SummaryEmail.class));
        verify(jdbc).update(contains("status = 'SENT'"), eq(7L), eq(DAY));
        verify(auditLogService).logAction(eq("DAILY_SUMMARY_SENT"), eq("system"), eq("admin7"), anyString());
        assertEquals(1, count("sent"));
    }

    @Test
    void doesNotSendAgainWhenAnotherReplicaOrAnEarlierRunAlreadyHandledTheDay() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(0);
        when(jdbc.update(contains("SET status = 'SENDING'"), eq(7L), eq(DAY), eq(DailySummaryScheduler.MAX_ATTEMPTS)))
                .thenReturn(0);

        var outcome = scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY);

        assertEquals(DailySummaryScheduler.Outcome.ALREADY_HANDLED, outcome);
        verify(mailer, never()).send(anyString(), any());
    }

    @Test
    void retriesAFailedSendThatStillHasAttemptsLeft() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(0);
        when(jdbc.update(contains("SET status = 'SENDING'"), eq(7L), eq(DAY), eq(DailySummaryScheduler.MAX_ATTEMPTS)))
                .thenReturn(1);

        var outcome = scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY);

        assertEquals(DailySummaryScheduler.Outcome.SENT, outcome);
        verify(mailer).send(eq("a@example.com"), any(SummaryEmail.class));
    }

    @Test
    void recordsAFailureWhenTheMailServerRejectsTheMessage() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);
        doThrow(new IllegalStateException("connection refused")).when(mailer).send(anyString(), any());

        var outcome = scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY);

        assertEquals(DailySummaryScheduler.Outcome.FAILED, outcome);
        verify(jdbc).update(contains("status = 'FAILED'"), contains("connection refused"), eq(7L), eq(DAY));
        verify(auditLogService, never()).logAction(anyString(), anyString(), anyString(), anyString());
        assertEquals(1, count("failed"));
    }

    @Test
    void skipsAdminsWithoutEmailOrFarm() {
        assertEquals(DailySummaryScheduler.Outcome.SKIPPED_NO_RECIPIENT, scheduler.sendTo(admin(1L, null, 1L), DAY));
        assertEquals(DailySummaryScheduler.Outcome.SKIPPED_NO_RECIPIENT, scheduler.sendTo(admin(2L, " ", 1L), DAY));
        assertEquals(DailySummaryScheduler.Outcome.SKIPPED_NO_RECIPIENT, scheduler.sendTo(admin(3L, "a@example.com", null), DAY));
        verifyNoInteractions(mailer, summaryService, jdbc);
    }

    @Test
    void skipsWhenSmtpIsNotConfigured() {
        when(mailer.isConfigured()).thenReturn(false);

        assertEquals(DailySummaryScheduler.Outcome.SKIPPED_NOT_CONFIGURED,
                scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY));
        verify(mailer, never()).send(anyString(), any());
        verifyNoInteractions(jdbc);
    }

    @Test
    void skipsADayOffWithoutClaimingTheDay() {
        when(mailer.isConfigured()).thenReturn(true);
        when(summaryService.build(1L, DAY)).thenReturn(Optional.empty());

        assertEquals(DailySummaryScheduler.Outcome.SKIPPED_NO_ATTENDANCE,
                scheduler.sendTo(admin(7L, "a@example.com", 1L), DAY));
        verifyNoInteractions(jdbc);
    }

    @Test
    void scheduledRunDoesNothingWhileTheFeatureIsDisabled() {
        newScheduler(false, false).runScheduled();

        verifyNoInteractions(adminRepository, mailer, summaryService, jdbc, closeDay);
    }

    @Test
    void scheduledRunUsesTodayInBangkokAndOnlyActiveAdminsWithAnEmail() {
        when(mailer.isConfigured()).thenReturn(true);
        when(adminRepository.findByStatusAndEmailIsNotNullAndFarmIdIsNotNull("active"))
                .thenReturn(List.of(admin(7L, "a@example.com", 1L)));
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);

        scheduler.runScheduled();

        verify(mailer).send(eq("a@example.com"), any(SummaryEmail.class));
    }

    @Test
    void scheduledRunClosesTheDayBeforeSendingTheEmails() {
        var scheduler = newScheduler(true, true);
        when(mailer.isConfigured()).thenReturn(true);
        when(adminRepository.findByStatusAndEmailIsNotNullAndFarmIdIsNotNull("active"))
                .thenReturn(List.of(admin(7L, "a@example.com", 1L)));
        when(summaryService.build(1L, DAY)).thenReturn(Optional.of(summary));
        when(jdbc.update(contains("INSERT INTO daily_summary_log"), eq(7L), eq(DAY))).thenReturn(1);

        scheduler.runScheduled();

        var order = inOrder(closeDay, mailer);
        order.verify(closeDay).closeAllFarms(DAY);
        order.verify(mailer).send(eq("a@example.com"), any(SummaryEmail.class));
    }

    @Test
    void closeDayRunsEvenWhenTheEmailFeatureIsOff() {
        newScheduler(false, true).runScheduled();

        verify(closeDay).closeAllFarms(DAY);
        verifyNoInteractions(adminRepository, mailer, summaryService);
    }

    @Test
    void aFailingCloseDayDoesNotStopTheEmails() {
        var scheduler = newScheduler(true, true);
        when(closeDay.closeAllFarms(DAY)).thenThrow(new IllegalStateException("db down"));
        when(mailer.isConfigured()).thenReturn(true);
        when(adminRepository.findByStatusAndEmailIsNotNullAndFarmIdIsNotNull("active")).thenReturn(List.of());

        scheduler.runScheduled();

        verify(adminRepository).findByStatusAndEmailIsNotNullAndFarmIdIsNotNull("active");
    }
}

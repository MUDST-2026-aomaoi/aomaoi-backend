package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.service.AuditLogService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Sends each active Admin (with an e-mail and a farm) the end-of-day summary of their farm.
 *
 * <p>Several app replicas may run this at the same time, so a row in {@code daily_summary_log}
 * (unique per admin and day) is claimed first and only the claimer sends. Failed sends are retried
 * by the later runs of the same cron (every 15 minutes until 21:00), at most {@link #MAX_ATTEMPTS} times.
 */
@Component
public class DailySummaryScheduler {

    static final int MAX_ATTEMPTS = 3;
    private static final Logger LOG = LoggerFactory.getLogger(DailySummaryScheduler.class);

    private final AdminProfileRepository adminRepository;
    private final DailySummaryService summaryService;
    private final SummaryEmailBuilder emailBuilder;
    private final SummaryMailer mailer;
    private final JdbcTemplate jdbc;
    private final AuditLogService auditLogService;
    private final MeterRegistry metrics;
    private final Clock clock;
    private final boolean enabled;

    public DailySummaryScheduler(AdminProfileRepository adminRepository, DailySummaryService summaryService,
                                 SummaryEmailBuilder emailBuilder, SummaryMailer mailer, JdbcTemplate jdbc,
                                 AuditLogService auditLogService, MeterRegistry metrics, Clock clock,
                                 @Value("${app.daily-summary.enabled:false}") boolean enabled) {
        this.adminRepository = adminRepository;
        this.summaryService = summaryService;
        this.emailBuilder = emailBuilder;
        this.mailer = mailer;
        this.jdbc = jdbc;
        this.auditLogService = auditLogService;
        this.metrics = metrics;
        this.clock = clock;
        this.enabled = enabled;
    }

    /** 18:00 first send, then 18:15 ... 20:45 only retries failures (already-sent admins are skipped). */
    @Scheduled(cron = "${app.daily-summary.cron:0 0/15 18-20 * * *}", zone = "${app.business-zone:Asia/Bangkok}")
    public void runScheduled() {
        if (!enabled) return;
        runForAllAdmins(LocalDate.now(clock));
    }

    public void runForAllAdmins(LocalDate date) {
        if (!mailer.isConfigured()) {
            LOG.warn("Daily summary skipped: SMTP is not configured");
            count("skipped");
            return;
        }
        for (AdminProfile admin : adminRepository.findByStatusAndEmailIsNotNullAndFarmIdIsNotNull("active")) {
            sendTo(admin, date);
        }
    }

    /** Sends one admin's summary (also used by the test-support trigger). Returns what happened. */
    public Outcome sendTo(AdminProfile admin, LocalDate date) {
        if (admin.getEmail() == null || admin.getEmail().isBlank() || admin.getFarmId() == null) {
            count("skipped");
            return Outcome.SKIPPED_NO_RECIPIENT;
        }
        if (!mailer.isConfigured()) {
            count("skipped");
            return Outcome.SKIPPED_NOT_CONFIGURED;
        }
        Optional<DailySummary> summary = summaryService.build(admin.getFarmId(), date);
        if (summary.isEmpty()) {
            count("skipped");
            return Outcome.SKIPPED_NO_WORKERS;
        }
        if (!claim(admin.getId(), date)) {
            return Outcome.ALREADY_HANDLED;
        }
        try {
            mailer.send(admin.getEmail(), emailBuilder.build(summary.get()));
            jdbc.update("UPDATE daily_summary_log SET status = 'SENT', sent_at = now(), error = NULL, updated_at = now() "
                    + "WHERE admin_id = ? AND summary_date = ?", admin.getId(), date);
            count("sent");
            auditLogService.logAction("DAILY_SUMMARY_SENT", "system", admin.getUsername(), "Sent daily summary for " + date);
            return Outcome.SENT;
        } catch (RuntimeException e) {
            String reason = truncate(e.getClass().getSimpleName() + ": " + e.getMessage());
            jdbc.update("UPDATE daily_summary_log SET status = 'FAILED', error = ?, updated_at = now() "
                    + "WHERE admin_id = ? AND summary_date = ?", reason, admin.getId(), date);
            LOG.warn("Daily summary for admin {} failed: {}", admin.getId(), reason);
            count("failed");
            return Outcome.FAILED;
        }
    }

    /** True when this caller now owns the send (new row, or a failed/stuck row with attempts left). */
    private boolean claim(Long adminId, LocalDate date) {
        int inserted = jdbc.update("INSERT INTO daily_summary_log (admin_id, summary_date, status) "
                + "VALUES (?, ?, 'SENDING') ON CONFLICT (admin_id, summary_date) DO NOTHING", adminId, date);
        if (inserted == 1) return true;
        return jdbc.update("UPDATE daily_summary_log SET status = 'SENDING', attempts = attempts + 1, updated_at = now() "
                + "WHERE admin_id = ? AND summary_date = ? AND attempts < ? "
                + "AND (status = 'FAILED' OR (status = 'SENDING' AND updated_at < now() - interval '30 minutes'))",
                adminId, date, MAX_ATTEMPTS) == 1;
    }

    private void count(String outcome) {
        metrics.counter("aomaoi.daily_summary.emails", "outcome", outcome).increment();
    }

    private static String truncate(String value) {
        return value == null ? "" : value.length() <= 500 ? value : value.substring(0, 500);
    }

    public enum Outcome {
        SENT, FAILED, ALREADY_HANDLED, SKIPPED_NO_RECIPIENT, SKIPPED_NOT_CONFIGURED, SKIPPED_NO_WORKERS
    }
}

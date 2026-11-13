package com.aomaoi.backend.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 18:00 "close the day": every worker nobody checked gets a row so the day has a permanent record.
 *
 * <ul>
 *   <li>unchecked + a work log covers the day -> PRESENT (they clearly worked)</li>
 *   <li>unchecked + no work log -> ABSENT</li>
 *   <li>day off: a farm with no attendance row (other than leave, which can be registered in advance) and no
 *       work log at all that day is left untouched</li>
 * </ul>
 * Rows are flagged {@code auto_recorded} and can still be edited by the Admin afterwards. One INSERT ... SELECT
 * with ON CONFLICT DO NOTHING per farm, so running it again, or from two app replicas, adds nothing twice.
 * Workers owned by an E2E run are skipped (their ownership trigger would reject a writer outside the run).
 */
@Service
public class AttendanceCloseDayService {

    private static final Logger LOG = LoggerFactory.getLogger(AttendanceCloseDayService.class);

    private static final String CLOSE_FARM_SQL = """
            INSERT INTO attendance (worker_id, attendance_date, status, auto_recorded, recorded_by)
            SELECT w.id, ?,
                   CASE WHEN EXISTS (SELECT 1 FROM work_logs l
                                     WHERE l.worker_id = w.id AND l.start_date <= ? AND l.end_date >= ?)
                        THEN 'PRESENT' ELSE 'ABSENT' END,
                   true, 'system'
            FROM worker w
            WHERE w.farm_id = ?
              AND w.status <> 'inactive'
              AND w.e2e_run_id IS NULL
              AND (w.joined_date IS NULL OR w.joined_date <= ?)
              AND (EXISTS (SELECT 1 FROM attendance a JOIN worker aw ON aw.id = a.worker_id
                           WHERE aw.farm_id = ? AND a.attendance_date = ? AND a.status <> 'LEAVE')
                   OR EXISTS (SELECT 1 FROM work_logs fl JOIN worker fw ON fw.id = fl.worker_id
                              WHERE fw.farm_id = ? AND fl.start_date <= ? AND fl.end_date >= ?))
            ON CONFLICT (worker_id, attendance_date) DO NOTHING
            """;

    private final JdbcTemplate jdbc;
    private final MeterRegistry metrics;
    private final AuditLogService auditLogService;

    public AttendanceCloseDayService(JdbcTemplate jdbc, MeterRegistry metrics, AuditLogService auditLogService) {
        this.jdbc = jdbc;
        this.metrics = metrics;
        this.auditLogService = auditLogService;
    }

    /** Closes the day for every active farm. Returns how many rows were created in total. */
    public int closeAllFarms(LocalDate date) {
        List<Long> farmIds = jdbc.queryForList("SELECT id FROM farm WHERE status IS DISTINCT FROM 'inactive'", Long.class);
        int created = 0;
        for (Long farmId : farmIds) {
            try {
                created += closeFarm(farmId, date);
            } catch (RuntimeException e) {
                LOG.warn("Close-day failed for farm {}: {}", farmId, e.getMessage());
                metrics.counter("aomaoi.attendance.close_day", "outcome", "failed").increment();
            }
        }
        if (created > 0) {
            // audit_logs.target_user is NOT NULL
            auditLogService.logAction("ATTENDANCE_CLOSE_DAY", "system", "all-farms",
                    date + ": recorded " + created + " unchecked workers");
        }
        return created;
    }

    /** Closes the day for one farm. Returns how many rows were created (0 on a day off or when already closed). */
    public int closeFarm(Long farmId, LocalDate date) {
        int created = jdbc.update(CLOSE_FARM_SQL, date, date, date, farmId, date, farmId, date, farmId, date, date);
        metrics.counter("aomaoi.attendance.close_day", "outcome", created > 0 ? "closed" : "nothing").increment();
        return created;
    }
}

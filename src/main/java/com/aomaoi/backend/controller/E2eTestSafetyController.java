package com.aomaoi.backend.controller;

import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.service.AttendanceCloseDayService;
import com.aomaoi.backend.service.AttendanceService;
import com.aomaoi.backend.service.E2eRunService;
import com.aomaoi.backend.summary.DailySummaryScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/test-support")
@RequiredArgsConstructor
public class E2eTestSafetyController {
    private final E2eRunService runs;
    private final DailySummaryScheduler dailySummary;
    private final AdminProfileRepository admins;
    private final Clock clock;
    private final AttendanceCloseDayService closeDay;
    private final AttendanceService attendance;

    @GetMapping("/e2e-readiness")
    public Map<String, Object> readiness() {
        // attendanceOpen: false after the closing time, when today can no longer be edited (E2E specs skip then)
        return Map.of("safeForE2e", runs.enabled(), "mode", "shared-tracked", "attendanceOpen", attendance.isTodayOpen());
    }

    @PostMapping("/runs")
    public Map<String, String> create(@RequestHeader("X-E2E-Support-Key") String key) {
        return runs.create(key);
    }

    @PostMapping("/runs/{id}/heartbeat")
    public Map<String, Boolean> heartbeat(@PathVariable String id,
                                        @RequestHeader("X-E2E-Token") String token) {
        runs.heartbeat(id, token);
        return Map.of("alive", true);
    }

    /**
     * Sends the end-of-day summary of ONE admin on demand (demo / E2E). Guarded by the support key and
     * limited to a single admin so a shared database never mails every real admin by accident.
     * The per-day claim still applies: calling it twice for the same admin and date sends once.
     */
    @PostMapping("/daily-summary/run")
    public Map<String, String> runDailySummary(@RequestHeader("X-E2E-Support-Key") String key,
                                               @RequestParam Long adminId,
                                               @RequestParam(required = false) LocalDate date) {
        runs.authorize(key);
        var admin = admins.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        var outcome = dailySummary.sendTo(admin, date != null ? date : LocalDate.now(clock));
        return Map.of("outcome", outcome.name());
    }

    /**
     * Runs the 18:00 close-day for ONE farm on demand (demo / E2E). Support key required. Workers owned by an
     * E2E run are never touched by close-day, so this only affects regular data of that farm.
     */
    @PostMapping("/attendance/close-day")
    public Map<String, Integer> closeDay(@RequestHeader("X-E2E-Support-Key") String key,
                                         @RequestParam Long farmId,
                                         @RequestParam(required = false) LocalDate date) {
        runs.authorize(key);
        return Map.of("created", closeDay.closeFarm(farmId, date != null ? date : LocalDate.now(clock)));
    }

    @DeleteMapping("/runs/{id}")
    public Map<String, Object> cleanup(@PathVariable String id,
                                      @RequestHeader("X-E2E-Support-Key") String key) {
        return runs.cleanup(id, key);
    }
}

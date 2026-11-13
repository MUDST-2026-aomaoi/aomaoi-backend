package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AttendanceBulkRequestDTO;
import com.aomaoi.backend.dto.AttendanceRequestDTO;
import com.aomaoi.backend.dto.AttendanceResponseDTO;
import com.aomaoi.backend.entity.Attendance;
import com.aomaoi.backend.entity.AttendanceStatus;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.AttendanceRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Admin-recorded attendance. An Admin only touches workers of their own farm; a SuperAdmin any farm.
 * Dates are judged in the business time zone (the day starts at 00:00). Today accepts any status until the
 * closing time (18:00) and is locked after it (the close-day job records whoever was not checked); a future
 * day accepts only LEAVE (advance notice) and clearing that leave; a past day can no longer be changed.
 */
@Service
public class AttendanceService {

    private static final int MAX_NOTE_LENGTH = 255;

    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final LocalTime closingTime;

    public AttendanceService(AttendanceRepository attendanceRepository, WorkerRepository workerRepository,
                             AdminProfileRepository adminProfileRepository, AuditLogService auditLogService, Clock clock,
                             @Value("${app.attendance.closing-time:18:00}") LocalTime closingTime) {
        this.attendanceRepository = attendanceRepository;
        this.workerRepository = workerRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.closingTime = closingTime;
    }

    /** True while today's attendance can still be changed (before the closing time). */
    public boolean isTodayOpen() {
        return LocalTime.now(clock).isBefore(closingTime);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> listForDate(LocalDate date, Long farmIdParam) {
        if (date == null) throw new IllegalArgumentException("date is required");
        Long farmId = resolveFarm(farmIdParam);
        return attendanceRepository.findByWorkerFarmIdAndAttendanceDate(farmId, date).stream()
                .map(AttendanceResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> history(Long workerId, YearMonth month) {
        if (month == null) throw new IllegalArgumentException("month is required");
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new NoSuchElementException("Worker not found"));
        requireWorkerAccess(worker);
        return attendanceRepository
                .findByWorkerIdAndAttendanceDateBetweenOrderByAttendanceDate(workerId, month.atDay(1), month.atEndOfMonth())
                .stream().map(AttendanceResponseDTO::from).toList();
    }

    /** Sets, changes or (status = null) clears one worker's day. */
    @Transactional
    public AttendanceResponseDTO set(AttendanceRequestDTO dto) {
        if (dto == null || dto.getWorkerId() == null || dto.getDate() == null) {
            throw new IllegalArgumentException("workerId and date are required");
        }
        Authentication auth = requireAuthentication();
        Worker worker = workerRepository.findById(dto.getWorkerId())
                .orElseThrow(() -> new NoSuchElementException("Worker not found"));
        requireWorkerAccess(worker);
        if ("inactive".equals(worker.getStatus())) throw new IllegalArgumentException("Worker is inactive");

        LocalDate date = dto.getDate();
        requireNotPast(date);
        Optional<Attendance> existing = attendanceRepository.findByWorkerIdAndAttendanceDate(worker.getId(), date);
        String before = existing.map(a -> a.getStatus().name()).orElse("-");

        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            existing.ifPresent(attendanceRepository::delete);
            audit(auth, worker, date, before, "-");
            return AttendanceResponseDTO.cleared(worker.getId(), date);
        }

        AttendanceStatus status = parseStatus(dto.getStatus());
        if (date.isAfter(LocalDate.now(clock)) && status != AttendanceStatus.LEAVE) {
            throw new IllegalArgumentException("A future day can only be marked as leave");
        }
        requireNotBeforeJoined(worker, date);
        String note = normalizeNote(dto.getNote());

        Attendance row = existing.orElseGet(Attendance::new);
        row.setWorker(worker);
        row.setAttendanceDate(date);
        row.setStatus(status);
        row.setNote(note);
        row.setAutoRecorded(false);
        row.setRecordedBy(auth.getName());
        Attendance saved = attendanceRepository.save(row);
        audit(auth, worker, date, before, status.name());
        return AttendanceResponseDTO.from(saved);
    }

    /** Marks every unchecked, non-inactive worker of the farm as present. Returns how many were added. */
    @Transactional
    public int markUncheckedPresent(AttendanceBulkRequestDTO dto) {
        if (dto == null || dto.getDate() == null) throw new IllegalArgumentException("date is required");
        Authentication auth = requireAuthentication();
        Long farmId = resolveFarm(dto.getFarmId());
        LocalDate date = dto.getDate();
        requireToday(date);

        Set<Long> alreadyChecked = new HashSet<>();
        attendanceRepository.findByWorkerFarmIdAndAttendanceDate(farmId, date)
                .forEach(a -> alreadyChecked.add(a.getWorker().getId()));

        int added = 0;
        for (Worker worker : workerRepository.findByFarmIdAndStatusNot(farmId, "inactive")) {
            if (alreadyChecked.contains(worker.getId())) continue;
            if (worker.getJoinedDate() != null && date.isBefore(worker.getJoinedDate())) continue;
            Attendance row = new Attendance();
            row.setWorker(worker);
            row.setAttendanceDate(date);
            row.setStatus(AttendanceStatus.PRESENT);
            row.setAutoRecorded(false);
            row.setRecordedBy(auth.getName());
            attendanceRepository.save(row);
            added++;
        }
        // audit_logs.target_user is NOT NULL: the "target" of a bulk action is the farm
        auditLogService.logAction("ATTENDANCE_BULK_PRESENT", auth.getName(), "farm:" + farmId,
                date + ": marked " + added + " unchecked workers present");
        return added;
    }

    private void requireToday(LocalDate date) {
        if (!date.equals(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Attendance can only be recorded for today");
        }
        requireOpen();
    }

    private void requireNotPast(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("A past day can no longer be changed");
        }
        if (date.equals(today)) requireOpen();
    }

    private void requireOpen() {
        if (!isTodayOpen()) {
            throw new IllegalArgumentException("Attendance for today is closed (after " + closingTime + ")");
        }
    }

    private void requireNotBeforeJoined(Worker worker, LocalDate date) {
        if (worker.getJoinedDate() != null && date.isBefore(worker.getJoinedDate())) {
            throw new IllegalArgumentException("Date is before the worker joined");
        }
    }

    private static AttendanceStatus parseStatus(String value) {
        try {
            return AttendanceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be PRESENT, LEAVE or ABSENT");
        }
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) return null;
        String trimmed = note.trim();
        if (trimmed.length() > MAX_NOTE_LENGTH) throw new IllegalArgumentException("note is too long");
        return trimmed;
    }

    private void audit(Authentication auth, Worker worker, LocalDate date, String before, String after) {
        String target = worker.getUser() != null ? worker.getUser().getUsername() : String.valueOf(worker.getId());
        auditLogService.logAction("ATTENDANCE_SET", auth.getName(), target, date + ": " + before + " -> " + after);
    }

    /** Admin: always their own farm. SuperAdmin: the farm given in the request. */
    private Long resolveFarm(Long farmIdParam) {
        Authentication auth = requireAuthentication();
        if (hasRole(auth, "ADMIN")) return requireAdminFarm(auth.getName());
        if (hasRole(auth, "SUPERADMIN")) {
            if (farmIdParam == null) throw new IllegalArgumentException("farmId is required");
            return farmIdParam;
        }
        throw new AccessDeniedException("Attendance requires admin access");
    }

    private void requireWorkerAccess(Worker worker) {
        Authentication auth = requireAuthentication();
        if (hasRole(auth, "SUPERADMIN")) return;
        if (hasRole(auth, "ADMIN") && worker.getFarmId() != null
                && worker.getFarmId().equals(requireAdminFarm(auth.getName()))) return;
        throw new AccessDeniedException("Worker is outside your access scope");
    }

    private Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Authentication required");
        return auth;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private Long requireAdminFarm(String username) {
        return adminProfileRepository.findByUserUsername(username)
                .filter(profile -> !"inactive".equals(profile.getStatus()) && profile.getFarmId() != null)
                .map(profile -> profile.getFarmId())
                .orElseThrow(() -> new AccessDeniedException("Admin has no active farm"));
    }
}

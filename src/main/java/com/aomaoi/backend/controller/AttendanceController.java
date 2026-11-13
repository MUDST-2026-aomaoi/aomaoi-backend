package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.AttendanceBulkRequestDTO;
import com.aomaoi.backend.dto.AttendanceRequestDTO;
import com.aomaoi.backend.dto.AttendanceResponseDTO;
import com.aomaoi.backend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Attendance (มา / ลา / ขาด) recorded by Admins. Base URL: /api/attendance (ADMIN / SUPERADMIN only).
 */
@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** Rows already recorded for a day (workers without a row are "not checked yet"). */
    @GetMapping
    public ResponseEntity<List<AttendanceResponseDTO>> forDate(@RequestParam LocalDate date,
                                                               @RequestParam(required = false) Long farmId) {
        return ResponseEntity.ok(attendanceService.listForDate(date, farmId));
    }

    /** One worker's attendance for a month (detail page calendar). */
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<AttendanceResponseDTO>> forWorker(@PathVariable Long workerId,
                                                                 @RequestParam YearMonth month) {
        return ResponseEntity.ok(attendanceService.history(workerId, month));
    }

    /** Set / change / clear (status = null) one worker's day. */
    @PutMapping
    public ResponseEntity<AttendanceResponseDTO> set(@Valid @RequestBody AttendanceRequestDTO request) {
        return ResponseEntity.ok(attendanceService.set(request));
    }

    /** "ทุกคนมา": mark every unchecked worker of the farm as present. */
    @PostMapping("/bulk-present")
    public ResponseEntity<Map<String, Integer>> bulkPresent(@Valid @RequestBody AttendanceBulkRequestDTO request) {
        return ResponseEntity.ok(Map.of("added", attendanceService.markUncheckedPresent(request)));
    }
}

package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.Attendance;

import java.time.LocalDate;

/** status is null when the day was cleared (not checked). */
public record AttendanceResponseDTO(Long workerId, LocalDate date, String status, String note,
                                    boolean autoRecorded, String recordedBy) {
    public static AttendanceResponseDTO from(Attendance a) {
        return new AttendanceResponseDTO(a.getWorker().getId(), a.getAttendanceDate(), a.getStatus().name(),
                a.getNote(), a.isAutoRecorded(), a.getRecordedBy());
    }

    public static AttendanceResponseDTO cleared(Long workerId, LocalDate date) {
        return new AttendanceResponseDTO(workerId, date, null, null, false, null);
    }
}

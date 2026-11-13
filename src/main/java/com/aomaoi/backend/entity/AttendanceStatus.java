package com.aomaoi.backend.entity;

/** Attendance of a worker for one whole day. "Not checked yet" is the absence of a row, not a status. */
public enum AttendanceStatus {
    PRESENT,
    LEAVE,
    ABSENT
}

package com.aomaoi.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** status = PRESENT | LEAVE | ABSENT, or null to clear the day back to "not checked". */
@Data
public class AttendanceRequestDTO {
    @NotNull
    private Long workerId;
    @NotNull
    private LocalDate date;
    private String status;
    @Size(max = 255)
    private String note;
}

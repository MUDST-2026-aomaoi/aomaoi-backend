package com.aomaoi.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceBulkRequestDTO {
    @NotNull
    private LocalDate date;
    /** Only needed for SuperAdmin; an Admin always acts on their own farm. */
    private Long farmId;
}

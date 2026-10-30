package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.AdvanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryAdvanceResponseDTO(
        Long id,
        Long workerId,
        String workerFullName,
        String workerNickname,
        LocalDate advanceDate,
        AdvanceType type,
        BigDecimal amount,
        String itemName,
        String recordedBy,
        Instant createdAt,
        Instant cancelledAt,
        String cancelledBy) {
}

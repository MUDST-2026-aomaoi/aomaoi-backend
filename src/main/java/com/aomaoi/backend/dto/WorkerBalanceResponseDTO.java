package com.aomaoi.backend.dto;

import java.math.BigDecimal;

public record WorkerBalanceResponseDTO(
        Long workerId,
        String fullName,
        String nickname,
        BigDecimal totalEarnings,
        BigDecimal totalAdvances,
        BigDecimal remainingBalance,
        boolean inDebt,
        BigDecimal outstandingDebt) {
}

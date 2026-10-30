package com.aomaoi.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record FinancialDashboardResponseDTO(
        long workerCount,
        long workLogCount,
        Map<String, Long> workLogsByType,
        BigDecimal totalEarnings,
        BigDecimal cashAdvances,
        BigDecimal goodsAdvances,
        BigDecimal totalAdvances,
        BigDecimal netBalance,
        BigDecimal totalOutstandingDebt) {
}

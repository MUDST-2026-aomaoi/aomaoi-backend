package com.aomaoi.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OverviewResponseDTO {
    private long totalWorkers;
    private long activeWorkers;
    private long totalWorkLogs;
    private long todayWorkLogs;
    private long workersToday;
    private BigDecimal totalAllTime;
    private BigDecimal totalThisMonth;
}

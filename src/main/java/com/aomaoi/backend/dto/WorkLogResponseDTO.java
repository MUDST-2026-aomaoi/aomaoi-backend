package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.WorkLog;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Preserves the existing work-log JSON keys and string ID representation. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkLogResponseDTO(
        String id, String type, String date, String startDate, String endDate, String workerId,
        BigDecimal total, Integer rows, Integer waPerRow, Integer furrows, Integer waPerFurrow,
        Integer days, BigDecimal dailyRate, Integer tanks) {
    public static WorkLogResponseDTO from(WorkLog log) {
        String start = log.getStartDate() != null ? log.getStartDate().toString() : log.getWorkDate().toString();
        String end = log.getEndDate() != null ? log.getEndDate().toString() : start;
        return new WorkLogResponseDTO(String.valueOf(log.getId()), log.getType(), start, start, end,
                String.valueOf(log.getWorker().getId()), log.getTotal(), log.getRows(), log.getWaPerRow(),
                log.getFurrows(), log.getWaPerFurrow(), log.getDays(), log.getDailyRate(), log.getTanks());
    }
}

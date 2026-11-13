package com.aomaoi.backend.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** What an Admin sees at the end of the day for one farm. */
public record DailySummary(
        String farmName,
        LocalDate date,
        TopWorker topWorker,
        List<String> absentWorkerNames,
        int activeWorkerCount,
        BigDecimal totalWage) {

    /** Worker with the highest total wage for the day (null in the summary when nobody logged work). */
    public record TopWorker(String name, BigDecimal totalWage, long jobCount) { }
}

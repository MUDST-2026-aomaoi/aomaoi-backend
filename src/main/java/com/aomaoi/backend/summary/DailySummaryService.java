package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final FarmRepository farmRepository;
    private final WorkerRepository workerRepository;
    private final WorkLogRepository workLogRepository;
    private final AbsenceRule absenceRule;

    /**
     * Builds the summary for a farm and day. Empty when the farm has no workers to report on.
     * Top performer = highest total wage of logs recorded on that day (a multi-day watering log is
     * counted on its start date, because wages are stored per log, not per day).
     */
    @Transactional(readOnly = true)
    public Optional<DailySummary> build(Long farmId, LocalDate date) {
        Farm farm = farmRepository.findById(farmId).orElse(null);
        if (farm == null) return Optional.empty();

        int activeWorkers = workerRepository.findByFarmIdAndStatusNot(farmId, "inactive").size();
        if (activeWorkers == 0) return Optional.empty();

        List<Object[]> totals = workLogRepository.dailyTotalsByWorker(farmId, date);
        DailySummary.TopWorker top = null;
        BigDecimal dayTotal = BigDecimal.ZERO;
        for (Object[] row : totals) {
            BigDecimal wage = (BigDecimal) row[2];
            dayTotal = dayTotal.add(wage);
            if (top == null) {
                top = new DailySummary.TopWorker((String) row[1], wage, ((Number) row[3]).longValue());
            }
        }

        List<String> absent = absenceRule.absentOn(farmId, date).stream()
                .map(worker -> worker.getFullName())
                .toList();
        return Optional.of(new DailySummary(farm.getName(), date, top, absent, activeWorkers, dayTotal));
    }
}

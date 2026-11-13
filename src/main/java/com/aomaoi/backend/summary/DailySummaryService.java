package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Attendance;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.AttendanceRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final FarmRepository farmRepository;
    private final WorkLogRepository workLogRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Builds the summary for a farm and day from the recorded attendance. Empty on a day off: no attendance
     * row, or only leave registered in advance and no work log (the close-day job leaves such days untouched).
     * Top performer = highest total wage of logs recorded on that day (a multi-day watering log is counted
     * on its start date, because wages are stored per log, not per day).
     */
    @Transactional(readOnly = true)
    public Optional<DailySummary> build(Long farmId, LocalDate date) {
        Farm farm = farmRepository.findById(farmId).orElse(null);
        if (farm == null) return Optional.empty();

        List<Attendance> rows = attendanceRepository.findByWorkerFarmIdAndAttendanceDate(farmId, date);
        // Day off: nothing but leave registered in advance and no work at all.
        boolean attendanceChecked = rows.stream().anyMatch(a -> a.getStatus() != com.aomaoi.backend.entity.AttendanceStatus.LEAVE);
        if (rows.isEmpty() || (!attendanceChecked && workLogRepository.workerIdsCoveringDate(farmId, date).isEmpty())) {
            return Optional.empty();
        }

        Comparator<String> byName = Comparator.nullsLast(String::compareToIgnoreCase);
        List<String> absent = rows.stream()
                .filter(a -> a.getStatus() == com.aomaoi.backend.entity.AttendanceStatus.ABSENT)
                .map(a -> a.getWorker().getFullName()).sorted(byName).toList();
        List<DailySummary.LeaveWorker> leave = rows.stream()
                .filter(a -> a.getStatus() == com.aomaoi.backend.entity.AttendanceStatus.LEAVE)
                .sorted(Comparator.comparing(a -> a.getWorker().getFullName(), byName))
                .map(a -> new DailySummary.LeaveWorker(a.getWorker().getFullName(), a.getNote())).toList();
        int present = (int) rows.stream()
                .filter(a -> a.getStatus() == com.aomaoi.backend.entity.AttendanceStatus.PRESENT).count();

        DailySummary.TopWorker top = null;
        BigDecimal dayTotal = BigDecimal.ZERO;
        for (Object[] row : workLogRepository.dailyTotalsByWorker(farmId, date)) {
            BigDecimal wage = (BigDecimal) row[2];
            dayTotal = dayTotal.add(wage);
            if (top == null) {
                top = new DailySummary.TopWorker((String) row[1], wage, ((Number) row[3]).longValue());
            }
        }
        return Optional.of(new DailySummary(farm.getName(), date, top, absent, leave, present, rows.size(), dayTotal));
    }
}

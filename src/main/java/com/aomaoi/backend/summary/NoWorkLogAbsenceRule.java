package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Current rule: a worker of the farm (not deactivated) is absent when no work log covers the day.
 * A log covers the day when start date &lt;= day &lt;= end date, so multi-day watering counts.
 */
@Component
@RequiredArgsConstructor
public class NoWorkLogAbsenceRule implements AbsenceRule {

    private final WorkerRepository workerRepository;
    private final WorkLogRepository workLogRepository;

    @Override
    public List<Worker> absentOn(Long farmId, LocalDate date) {
        Set<Long> covered = new HashSet<>(workLogRepository.workerIdsCoveringDate(farmId, date));
        return workerRepository.findByFarmIdAndStatusNot(farmId, "inactive").stream()
                .filter(worker -> !covered.contains(worker.getId()))
                .sorted(Comparator.comparing(Worker::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }
}

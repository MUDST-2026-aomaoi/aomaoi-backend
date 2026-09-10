package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.OverviewResponseDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final WorkerRepository workerRepository;
    private final WorkLogRepository workLogRepository;

    public DashboardService(WorkerRepository workerRepository,
                            WorkLogRepository workLogRepository) {
        this.workerRepository = workerRepository;
        this.workLogRepository = workLogRepository;
    }

    public OverviewResponseDTO getOverview() {
        OverviewResponseDTO dto = new OverviewResponseDTO();

        // Worker counts
        dto.setTotalWorkers(workerRepository.count());
        dto.setActiveWorkers(workerRepository.countByStatus("active"));

        // All work logs
        List<WorkLog> allLogs = workLogRepository.findAll();
        dto.setTotalWorkLogs((long) allLogs.size());

        // Total all time
        dto.setTotalAllTime(
            allLogs.stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        // This month's work logs
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        List<WorkLog> monthLogs = workLogRepository.findByWorkDateBetween(startOfMonth, today);

        dto.setTotalThisMonth(
            monthLogs.stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        // Today's logs
        List<WorkLog> todayLogs = monthLogs.stream()
                .filter(log -> log.getWorkDate().equals(today))
                .collect(Collectors.toList());
        dto.setTodayWorkLogs((long) todayLogs.size());

        // Unique workers who worked today
        dto.setWorkersToday(
            todayLogs.stream()
                .map(log -> log.getWorker().getId())
                .distinct()
                .count()
        );

        return dto;
    }
}

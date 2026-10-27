package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerBalanceResponseDTO;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkerBalanceService {
    private final FinancialAccessService access;
    private final WorkLogRepository workLogRepository;
    private final SalaryAdvanceRepository advanceRepository;

    @Transactional(readOnly = true)
    public List<WorkerBalanceResponseDTO> listForManager(int page, int size) {
        if (page < 0 || size < 1 || size > 500) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 500");
        }
        return balancesForWorkers(access.managedWorkersPage(page, size).getContent());
    }

    @Transactional(readOnly = true)
    public WorkerBalanceResponseDTO getForManager(Long workerId) {
        return balancesForWorkers(List.of(access.requireManagedWorker(workerId))).getFirst();
    }

    @Transactional(readOnly = true)
    public WorkerBalanceResponseDTO getSelf() {
        return balancesForWorkers(List.of(access.requireSelf())).getFirst();
    }

    List<WorkerBalanceResponseDTO> balancesForWorkers(List<Worker> workers) {
        if (workers.isEmpty()) return List.of();
        List<Long> ids = workers.stream().map(Worker::getId).toList();
        Map<Long, BigDecimal> earnings = toTotals(workLogRepository.totalsByWorkerIds(ids));
        Map<Long, BigDecimal> advances = toTotals(advanceRepository.totalsByWorkerIds(ids));
        return workers.stream().map(worker -> {
            BigDecimal earned = earnings.getOrDefault(worker.getId(), BigDecimal.ZERO);
            BigDecimal advanced = advances.getOrDefault(worker.getId(), BigDecimal.ZERO);
            BigDecimal balance = earned.subtract(advanced);
            return new WorkerBalanceResponseDTO(worker.getId(), worker.getFullName(), worker.getNickname(),
                    earned, advanced, balance, balance.signum() < 0, balance.negate().max(BigDecimal.ZERO));
        }).toList();
    }

    private Map<Long, BigDecimal> toTotals(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> (BigDecimal) row[1]));
    }
}

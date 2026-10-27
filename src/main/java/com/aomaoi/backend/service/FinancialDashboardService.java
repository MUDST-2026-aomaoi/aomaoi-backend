package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FinancialDashboardResponseDTO;
import com.aomaoi.backend.dto.WorkerBalanceResponseDTO;
import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@RequiredArgsConstructor
public class FinancialDashboardService {
    private final FinancialAccessService access;
    private final WorkerBalanceService balanceService;
    private final SalaryAdvanceRepository advanceRepository;
    private final WorkLogRepository workLogRepository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public FinancialDashboardResponseDTO getForManager() {
        BigDecimal earnings = BigDecimal.ZERO;
        BigDecimal debt = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal goods = BigDecimal.ZERO;
        long workerCount = 0;
        Map<String, Long> byType = new HashMap<>();
        int pageNumber = 0;
        Page<Worker> page;
        do {
            page = access.managedWorkersPage(pageNumber++, 250);
            List<Worker> workers = page.getContent();
            if (workers.isEmpty()) break;
            workerCount += workers.size();
            List<Long> ids = workers.stream().map(Worker::getId).toList();
            for (WorkerBalanceResponseDTO balance : balanceService.balancesForWorkers(workers)) {
                earnings = earnings.add(balance.totalEarnings());
                debt = debt.add(balance.outstandingDebt());
            }
            cash = cash.add(advanceRepository.sumActiveByTypeAndWorkerIds(AdvanceType.CASH, ids));
            goods = goods.add(advanceRepository.sumActiveByTypeAndWorkerIds(AdvanceType.GOODS, ids));
            for (Object[] row : workLogRepository.countsByTypeForWorkerIds(ids)) {
                byType.merge((String) row[0], ((Number) row[1]).longValue(), Long::sum);
            }
        } while (page.hasNext());
        if (workerCount == 0) {
            return new FinancialDashboardResponseDTO(0, 0, Map.of(), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        long workLogCount = byType.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal totalAdvances = cash.add(goods);
        return new FinancialDashboardResponseDTO(workerCount, workLogCount, Map.copyOf(byType),
                earnings, cash, goods, totalAdvances, earnings.subtract(totalAdvances), debt);
    }
}

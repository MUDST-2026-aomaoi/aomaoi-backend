package com.aomaoi.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.aomaoi.backend.dto.WorkerBalanceResponseDTO;
import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class FinancialDashboardServiceTest {
    @Mock FinancialAccessService access;
    @Mock WorkerBalanceService balances;
    @Mock SalaryAdvanceRepository advances;
    @Mock WorkLogRepository logs;
    @InjectMocks FinancialDashboardService service;

    @Test
    void reportsEarningsAdvanceTypesAndPerWorkerDebt() {
        Worker a = new Worker();
        a.setId(1L);
        Worker b = new Worker();
        b.setId(2L);
        List<Worker> workers = List.of(a, b);
        List<Long> ids = List.of(1L, 2L);
        when(access.managedWorkersPage(0, 250)).thenReturn(new PageImpl<>(workers));
        when(balances.balancesForWorkers(workers)).thenReturn(List.of(
                new WorkerBalanceResponseDTO(1L, "A", "a", money("1000"), money("1300"),
                        money("-300"), true, money("300")),
                new WorkerBalanceResponseDTO(2L, "B", "b", money("500"), money("100"),
                        money("400"), false, BigDecimal.ZERO)));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.CASH, ids)).thenReturn(money("1200"));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.GOODS, ids)).thenReturn(money("200"));
        when(logs.countsByTypeForWorkerIds(ids))
                .thenReturn(List.<Object[]>of(new Object[] {"cutting", 2L}, new Object[] {"watering", 1L}));

        var result = service.getForManager();

        assertEquals(2, result.workerCount());
        assertEquals(3, result.workLogCount());
        assertEquals(2L, result.workLogsByType().get("cutting"));
        assertEquals(money("1500"), result.totalEarnings());
        assertEquals(money("1400"), result.totalAdvances());
        assertEquals(money("100"), result.netBalance());
        assertEquals(money("300"), result.totalOutstandingDebt());
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}

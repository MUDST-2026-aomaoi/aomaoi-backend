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
        assertEquals(money("1200"), result.cashAdvances());
        assertEquals(money("200"), result.goodsAdvances());
        assertEquals(money("1400"), result.totalAdvances());
        assertEquals(money("100"), result.netBalance());
        assertEquals(money("300"), result.totalOutstandingDebt());
    }

    @Test
    void emptyFarmReturnsZeroSummaryWithoutQueriesForTotals() {
        when(access.managedWorkersPage(0, 250)).thenReturn(new PageImpl<>(List.of()));

        var result = service.getForManager();

        assertEquals(0, result.workerCount());
        assertEquals(0, result.workLogCount());
        assertEquals(java.util.Map.of(), result.workLogsByType());
        assertEquals(BigDecimal.ZERO, result.totalEarnings());
        assertEquals(BigDecimal.ZERO, result.cashAdvances());
        assertEquals(BigDecimal.ZERO, result.goodsAdvances());
        assertEquals(BigDecimal.ZERO, result.totalAdvances());
        assertEquals(BigDecimal.ZERO, result.netBalance());
        assertEquals(BigDecimal.ZERO, result.totalOutstandingDebt());
        org.mockito.Mockito.verifyNoInteractions(balances, advances, logs);
    }

    @Test
    void aggregatesWorkersAndWorkLogTypesAcrossPages() {
        List<Worker> firstPage = java.util.stream.IntStream.rangeClosed(1, 250)
                .mapToObj(id -> worker(id)).toList();
        Worker lastWorker = worker(251);
        List<Worker> lastPage = List.of(lastWorker);
        List<Long> firstIds = firstPage.stream().map(Worker::getId).toList();
        List<Long> lastIds = List.of(251L);
        var page0 = org.springframework.data.domain.PageRequest.of(0, 250);
        var page1 = org.springframework.data.domain.PageRequest.of(1, 250);
        when(access.managedWorkersPage(0, 250))
                .thenReturn(new PageImpl<>(firstPage, page0, 251));
        when(access.managedWorkersPage(1, 250))
                .thenReturn(new PageImpl<>(lastPage, page1, 251));
        when(balances.balancesForWorkers(firstPage)).thenReturn(firstPage.stream()
                .map(w -> new WorkerBalanceResponseDTO(w.getId(), "A", "a", money("10"), money("0"),
                        money("10"), false, money("0"))).toList());
        when(balances.balancesForWorkers(lastPage)).thenReturn(List.of(
                new WorkerBalanceResponseDTO(251L, "B", "b", money("20"), money("0"),
                        money("20"), false, money("0"))));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.CASH, firstIds)).thenReturn(money("250"));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.GOODS, firstIds)).thenReturn(money("0"));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.CASH, lastIds)).thenReturn(money("25"));
        when(advances.sumActiveByTypeAndWorkerIds(AdvanceType.GOODS, lastIds)).thenReturn(money("5"));
        when(logs.countsByTypeForWorkerIds(firstIds))
                .thenReturn(List.<Object[]>of(new Object[] {"cutting", 250L}));
        when(logs.countsByTypeForWorkerIds(lastIds))
                .thenReturn(List.<Object[]>of(new Object[] {"cutting", 1L}, new Object[] {"planting", 1L}));

        var result = service.getForManager();

        assertEquals(251, result.workerCount());
        assertEquals(252, result.workLogCount());
        assertEquals(251L, result.workLogsByType().get("cutting"));
        assertEquals(1L, result.workLogsByType().get("planting"));
        assertEquals(money("2520"), result.totalEarnings());
        assertEquals(money("275"), result.cashAdvances());
        assertEquals(money("5"), result.goodsAdvances());
        assertEquals(money("280"), result.totalAdvances());
        assertEquals(money("2240"), result.netBalance());
    }

    private Worker worker(long id) {
        Worker worker = new Worker();
        worker.setId(id);
        return worker;
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}

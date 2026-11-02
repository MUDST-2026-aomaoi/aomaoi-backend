package com.aomaoi.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

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
class WorkerBalanceServiceTest {
    @Mock FinancialAccessService access;
    @Mock WorkLogRepository logs;
    @Mock SalaryAdvanceRepository advances;
    @InjectMocks WorkerBalanceService service;

    @Test
    void debtMeansAdvancesExceedAllEarnedWages() {
        Worker worker = worker();
        when(access.requireSelf()).thenReturn(worker);
        when(logs.totalsByWorkerIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("1000.00")}));
        when(advances.totalsByWorkerIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("1300.00")}));

        var result = service.getSelf();

        assertEquals(new BigDecimal("-300.00"), result.remainingBalance());
        assertEquals(new BigDecimal("300.00"), result.outstandingDebt());
        assertTrue(result.inDebt());
    }

    @Test
    void noAdvancesMeansNoDebt() {
        Worker worker = worker();
        when(access.requireManagedWorker(1L)).thenReturn(worker);
        when(logs.totalsByWorkerIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("1000.00")}));
        when(advances.totalsByWorkerIds(List.of(1L))).thenReturn(List.of());

        var result = service.getForManager(1L);

        assertEquals(new BigDecimal("1000.00"), result.remainingBalance());
        assertEquals(BigDecimal.ZERO, result.outstandingDebt());
        assertFalse(result.inDebt());
    }

    @Test
    void advancesEqualEarningsMeansNoDebt() {
        Worker worker = worker();
        when(access.requireSelf()).thenReturn(worker);
        when(logs.totalsByWorkerIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("1000.00")}));
        when(advances.totalsByWorkerIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, new BigDecimal("1000.00")}));

        var result = service.getSelf();

        assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.outstandingDebt()));
        assertFalse(result.inDebt());
    }

    @Test
    void noEarningsAndNoAdvancesProducesZeroBalance() {
        when(access.requireManagedWorker(1L)).thenReturn(worker());
        when(logs.totalsByWorkerIds(List.of(1L))).thenReturn(List.of());
        when(advances.totalsByWorkerIds(List.of(1L))).thenReturn(List.of());

        var result = service.getForManager(1L);

        assertEquals(BigDecimal.ZERO, result.totalEarnings());
        assertEquals(BigDecimal.ZERO, result.totalAdvances());
        assertEquals(BigDecimal.ZERO, result.remainingBalance());
        assertEquals(BigDecimal.ZERO, result.outstandingDebt());
        assertFalse(result.inDebt());
    }

    @Test
    void managerListReadsOnlyRequestedPage() {
        Worker worker = worker();
        when(access.managedWorkersPage(2, 10)).thenReturn(new PageImpl<>(List.of(worker)));
        when(logs.totalsByWorkerIds(List.of(1L))).thenReturn(List.of());
        when(advances.totalsByWorkerIds(List.of(1L))).thenReturn(List.of());

        assertEquals(1, service.listForManager(2, 10).size());
    }

    @Test
    void rejectsInvalidPaginationBeforeReadingWorkers() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.listForManager(-1, 10));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.listForManager(0, 0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.listForManager(0, 501));
        verifyNoInteractions(access, logs, advances);
    }

    private Worker worker() {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setFullName("สมชาย");
        worker.setNickname("ชาย");
        return worker;
    }
}

package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.OverviewResponseDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkLogRepository workLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Worker worker;
    private WorkLog log1;
    private WorkLog log2;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);

        log1 = new WorkLog();
        log1.setId(10L);
        log1.setWorker(worker);
        log1.setWorkDate(LocalDate.now());
        log1.setTotal(BigDecimal.valueOf(150.0));

        log2 = new WorkLog();
        log2.setId(11L);
        log2.setWorker(worker);
        log2.setWorkDate(LocalDate.now().minusDays(2));
        log2.setTotal(BigDecimal.valueOf(200.0));
    }

    @Test
    void getOverview_ReturnsAccurateMetrics() {
        when(workerRepository.count()).thenReturn(5L);
        when(workerRepository.countByStatus("active")).thenReturn(3L);
        when(workLogRepository.findAll()).thenReturn(List.of(log1, log2));
        when(workLogRepository.findByWorkDateBetween(any(), any())).thenReturn(List.of(log1, log2));

        OverviewResponseDTO overview = dashboardService.getOverview();

        assertNotNull(overview);
        assertEquals(5L, overview.getTotalWorkers());
        assertEquals(3L, overview.getActiveWorkers());
        assertEquals(2L, overview.getTotalWorkLogs());
        assertEquals(BigDecimal.valueOf(350.0), overview.getTotalAllTime());
        assertEquals(BigDecimal.valueOf(350.0), overview.getTotalThisMonth());
        assertEquals(1L, overview.getTodayWorkLogs());
        assertEquals(1L, overview.getWorkersToday());
    }

    @Test
    void getOverview_EmptyDatabase_ReturnsZeros() {
        when(workerRepository.count()).thenReturn(0L);
        when(workerRepository.countByStatus("active")).thenReturn(0L);
        when(workLogRepository.findAll()).thenReturn(Collections.emptyList());
        when(workLogRepository.findByWorkDateBetween(any(), any())).thenReturn(Collections.emptyList());

        OverviewResponseDTO overview = dashboardService.getOverview();

        assertNotNull(overview);
        assertEquals(0L, overview.getTotalWorkers());
        assertEquals(0L, overview.getActiveWorkers());
        assertEquals(0L, overview.getTotalWorkLogs());
        assertEquals(BigDecimal.ZERO, overview.getTotalAllTime());
        assertEquals(BigDecimal.ZERO, overview.getTotalThisMonth());
        assertEquals(0L, overview.getTodayWorkLogs());
        assertEquals(0L, overview.getWorkersToday());
    }

    @Test
    void getOverview_MultipleWorkersSameDay_CountsDistinctWorkers() {
        Worker worker2 = new Worker();
        worker2.setId(2L);

        WorkLog log3 = new WorkLog();
        log3.setId(12L);
        log3.setWorker(worker); // same worker as log1
        log3.setWorkDate(LocalDate.now());
        log3.setTotal(BigDecimal.valueOf(100.0));

        WorkLog log4 = new WorkLog();
        log4.setId(13L);
        log4.setWorker(worker2); // distinct worker
        log4.setWorkDate(LocalDate.now());
        log4.setTotal(BigDecimal.valueOf(250.0));

        when(workerRepository.count()).thenReturn(2L);
        when(workerRepository.countByStatus("active")).thenReturn(2L);
        when(workLogRepository.findAll()).thenReturn(List.of(log1, log3, log4));
        when(workLogRepository.findByWorkDateBetween(any(), any())).thenReturn(List.of(log1, log3, log4));

        OverviewResponseDTO overview = dashboardService.getOverview();

        assertNotNull(overview);
        assertEquals(3L, overview.getTodayWorkLogs());
        assertEquals(2L, overview.getWorkersToday()); // worker 1 and worker 2
        assertEquals(BigDecimal.valueOf(500.0), overview.getTotalThisMonth());
    }
}

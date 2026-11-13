package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailySummaryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Mock
    private FarmRepository farmRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private WorkLogRepository workLogRepository;
    @Mock
    private AbsenceRule absenceRule;
    @InjectMocks
    private DailySummaryService service;

    private static Farm farm() {
        Farm farm = new Farm();
        farm.setId(1L);
        farm.setName("ฟาร์มเพื่อพ่อ");
        return farm;
    }

    private static Worker worker(long id, String name) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFullName(name);
        return worker;
    }

    @Test
    void firstRowOfTheWageQueryIsTheTopPerformerAndWagesAreSummed() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive"))
                .thenReturn(List.of(worker(1, "Anan"), worker(2, "Bee"), worker(3, "Cher")));
        // the repository already orders by wage desc: Anan 1,500 (2 jobs), Bee 600 (1 job)
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of(
                new Object[]{1L, "Anan", new BigDecimal("1500.00"), 2L},
                new Object[]{2L, "Bee", new BigDecimal("600.00"), 1L}));
        when(absenceRule.absentOn(1L, DAY)).thenReturn(List.of(worker(3, "Cher")));

        DailySummary summary = service.build(1L, DAY).orElseThrow();

        assertEquals("ฟาร์มเพื่อพ่อ", summary.farmName());
        assertEquals("Anan", summary.topWorker().name());
        assertEquals(new BigDecimal("1500.00"), summary.topWorker().totalWage());
        assertEquals(2L, summary.topWorker().jobCount());
        assertEquals(new BigDecimal("2100.00"), summary.totalWage());
        assertEquals(List.of("Cher"), summary.absentWorkerNames());
        assertEquals(3, summary.activeWorkerCount());
    }

    @Test
    void noLogsMeansNoTopPerformerAndEveryoneAbsent() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive"))
                .thenReturn(List.of(worker(1, "Anan"), worker(2, "Bee")));
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of());
        when(absenceRule.absentOn(1L, DAY)).thenReturn(List.of(worker(1, "Anan"), worker(2, "Bee")));

        DailySummary summary = service.build(1L, DAY).orElseThrow();

        assertNull(summary.topWorker());
        assertEquals(BigDecimal.ZERO, summary.totalWage());
        assertEquals(List.of("Anan", "Bee"), summary.absentWorkerNames());
    }

    @Test
    void farmWithoutWorkersHasNothingToReport() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive")).thenReturn(List.of());

        assertTrue(service.build(1L, DAY).isEmpty());
    }

    @Test
    void unknownFarmHasNothingToReport() {
        when(farmRepository.findById(9L)).thenReturn(Optional.empty());

        assertTrue(service.build(9L, DAY).isEmpty());
    }
}

package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoWorkLogAbsenceRuleTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private WorkLogRepository workLogRepository;
    @InjectMocks
    private NoWorkLogAbsenceRule rule;

    private static Worker worker(long id, String name) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFullName(name);
        return worker;
    }

    @Test
    void workersWithoutALogCoveringTheDayAreAbsent_sortedByName() {
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive"))
                .thenReturn(List.of(worker(1, "Cher"), worker(2, "Anan"), worker(3, "Bee")));
        // worker 3 is covered, e.g. by a multi-day watering log
        when(workLogRepository.workerIdsCoveringDate(1L, DAY)).thenReturn(List.of(3L));

        List<String> names = rule.absentOn(1L, DAY).stream().map(Worker::getFullName).toList();

        assertEquals(List.of("Anan", "Cher"), names);
    }

    @Test
    void everyoneIsAbsentWhenNothingWasLogged() {
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive"))
                .thenReturn(List.of(worker(1, "Anan"), worker(2, "Bee")));
        when(workLogRepository.workerIdsCoveringDate(1L, DAY)).thenReturn(List.of());

        assertEquals(2, rule.absentOn(1L, DAY).size());
    }

    @Test
    void nobodyIsAbsentWhenEveryoneWorked() {
        when(workerRepository.findByFarmIdAndStatusNot(1L, "inactive")).thenReturn(List.of(worker(1, "Anan")));
        when(workLogRepository.workerIdsCoveringDate(1L, DAY)).thenReturn(List.of(1L));

        assertTrue(rule.absentOn(1L, DAY).isEmpty());
    }
}

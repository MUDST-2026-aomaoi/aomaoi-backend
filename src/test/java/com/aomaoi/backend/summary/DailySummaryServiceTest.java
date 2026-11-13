package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Attendance;
import com.aomaoi.backend.entity.AttendanceStatus;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AttendanceRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
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
    private WorkLogRepository workLogRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @InjectMocks
    private DailySummaryService service;

    private static Farm farm() {
        Farm farm = new Farm();
        farm.setId(1L);
        farm.setName("ฟาร์มเพื่อพ่อ");
        return farm;
    }

    private static Attendance row(long workerId, String name, AttendanceStatus status, String note) {
        Worker worker = new Worker();
        worker.setId(workerId);
        worker.setFullName(name);
        Attendance attendance = new Attendance();
        attendance.setWorker(worker);
        attendance.setAttendanceDate(DAY);
        attendance.setStatus(status);
        attendance.setNote(note);
        return attendance;
    }

    @Test
    void splitsAttendanceIntoPresentAbsentAndLeaveWithNotes() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY)).thenReturn(List.of(
                row(1, "Anan", AttendanceStatus.PRESENT, null),
                row(2, "Cher", AttendanceStatus.ABSENT, null),
                row(3, "Bee", AttendanceStatus.LEAVE, "ป่วย โทรบอกเช้านี้"),
                row(4, "Aom", AttendanceStatus.ABSENT, null)));
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of());

        DailySummary summary = service.build(1L, DAY).orElseThrow();

        assertEquals("ฟาร์มเพื่อพ่อ", summary.farmName());
        assertEquals(List.of("Aom", "Cher"), summary.absentWorkerNames());
        assertEquals(1, summary.leaveWorkers().size());
        assertEquals("Bee", summary.leaveWorkers().get(0).name());
        assertEquals("ป่วย โทรบอกเช้านี้", summary.leaveWorkers().get(0).note());
        assertEquals(1, summary.presentCount());
        assertEquals(4, summary.checkedWorkerCount());
    }

    @Test
    void firstRowOfTheWageQueryIsTheTopPerformerAndWagesAreSummed() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY))
                .thenReturn(List.of(row(1, "Anan", AttendanceStatus.PRESENT, null)));
        // the repository already orders by wage desc: Anan 1,500 (2 jobs), Bee 600 (1 job)
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of(
                new Object[]{1L, "Anan", new BigDecimal("1500.00"), 2L},
                new Object[]{2L, "Bee", new BigDecimal("600.00"), 1L}));

        DailySummary summary = service.build(1L, DAY).orElseThrow();

        assertEquals("Anan", summary.topWorker().name());
        assertEquals(new BigDecimal("1500.00"), summary.topWorker().totalWage());
        assertEquals(2L, summary.topWorker().jobCount());
        assertEquals(new BigDecimal("2100.00"), summary.totalWage());
    }

    @Test
    void noWorkLogsMeansNoTopPerformer() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY))
                .thenReturn(List.of(row(1, "Anan", AttendanceStatus.ABSENT, null)));
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of());

        DailySummary summary = service.build(1L, DAY).orElseThrow();

        assertNull(summary.topWorker());
        assertEquals(BigDecimal.ZERO, summary.totalWage());
    }

    @Test
    void dayOffHasNothingToReport() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY)).thenReturn(List.of());

        assertTrue(service.build(1L, DAY).isEmpty());
    }

    @Test
    void aDayWithOnlyLeaveRegisteredInAdvanceAndNoWorkIsADayOff() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY))
                .thenReturn(List.of(row(1, "Anan", AttendanceStatus.LEAVE, "ลาล่วงหน้า")));
        when(workLogRepository.workerIdsCoveringDate(1L, DAY)).thenReturn(List.of());

        assertTrue(service.build(1L, DAY).isEmpty());
    }

    @Test
    void leaveRegisteredInAdvanceStillCountsOnAWorkingDay() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm()));
        when(attendanceRepository.findByWorkerFarmIdAndAttendanceDate(1L, DAY)).thenReturn(List.of(
                row(1, "Anan", AttendanceStatus.LEAVE, null), row(2, "Bee", AttendanceStatus.PRESENT, null)));
        when(workLogRepository.dailyTotalsByWorker(1L, DAY)).thenReturn(List.of());

        assertEquals(1, service.build(1L, DAY).orElseThrow().leaveWorkers().size());
    }

    @Test
    void unknownFarmHasNothingToReport() {
        when(farmRepository.findById(9L)).thenReturn(Optional.empty());

        assertTrue(service.build(9L, DAY).isEmpty());
    }
}

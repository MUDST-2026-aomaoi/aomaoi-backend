package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.AttendanceBulkRequestDTO;
import com.aomaoi.backend.dto.AttendanceRequestDTO;
import com.aomaoi.backend.dto.AttendanceResponseDTO;
import com.aomaoi.backend.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController controller;

    @Test
    void forDate_delegatesToService() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        List<AttendanceResponseDTO> rows = List.of(mock(AttendanceResponseDTO.class));
        when(attendanceService.listForDate(date, 7L)).thenReturn(rows);

        ResponseEntity<List<AttendanceResponseDTO>> response = controller.forDate(date, 7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(rows, response.getBody());
    }

    @Test
    void forDate_withoutFarmId_passesNull() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        when(attendanceService.listForDate(date, null)).thenReturn(List.of());

        ResponseEntity<List<AttendanceResponseDTO>> response = controller.forDate(date, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void forWorker_delegatesToService() {
        YearMonth month = YearMonth.of(2026, 5);
        List<AttendanceResponseDTO> rows = List.of(mock(AttendanceResponseDTO.class));
        when(attendanceService.history(3L, month)).thenReturn(rows);

        ResponseEntity<List<AttendanceResponseDTO>> response = controller.forWorker(3L, month);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(rows, response.getBody());
    }

    @Test
    void set_delegatesToService() {
        AttendanceRequestDTO request = new AttendanceRequestDTO();
        AttendanceResponseDTO saved = mock(AttendanceResponseDTO.class);
        when(attendanceService.set(request)).thenReturn(saved);

        ResponseEntity<AttendanceResponseDTO> response = controller.set(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(saved, response.getBody());
    }

    @Test
    void bulkPresent_wrapsAddedCount() {
        AttendanceBulkRequestDTO request = new AttendanceBulkRequestDTO();
        when(attendanceService.markUncheckedPresent(request)).thenReturn(4);

        ResponseEntity<Map<String, Integer>> response = controller.bulkPresent(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("added", 4), response.getBody());
    }
}

package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.dto.WorkLogResponseDTO;
import java.math.BigDecimal;
import com.aomaoi.backend.service.WorkLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkLogControllerTest {

    @Mock
    private WorkLogService workLogService;

    @InjectMocks
    private WorkLogController workLogController;

    @Test
    void getAllLogs() {
        WorkLogResponseDTO log = new WorkLogResponseDTO("1", "cutting", "2026-01-01", "2026-01-01",
                "2026-01-01", "2", new BigDecimal("10.00"), null, null, null, null, null, null, null);
        when(workLogService.getAllLogs()).thenReturn(List.of(log));

        ResponseEntity<List<WorkLogResponseDTO>> response = workLogController.getAllLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void addLog() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        WorkLogResponseDTO log = new WorkLogResponseDTO("1", "cutting", "2026-01-01", "2026-01-01",
                "2026-01-01", "2", new BigDecimal("10.00"), null, null, null, null, null, null, null);
        when(workLogService.addLog(dto)).thenReturn(log);

        ResponseEntity<WorkLogResponseDTO> response = workLogController.addLog(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("1", response.getBody().id());
    }
}

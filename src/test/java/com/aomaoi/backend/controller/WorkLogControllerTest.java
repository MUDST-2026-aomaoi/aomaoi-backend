package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.service.WorkLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("id", "1");
        when(workLogService.getAllLogs()).thenReturn(List.of(logMap));

        ResponseEntity<List<Map<String, Object>>> response = workLogController.getAllLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void addLog() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("id", "1");
        when(workLogService.addLog(dto)).thenReturn(logMap);

        ResponseEntity<Map<String, Object>> response = workLogController.addLog(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("1", response.getBody().get("id"));
    }
}

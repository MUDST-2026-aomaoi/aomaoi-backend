package com.aomaoi.backend.controller;

import com.aomaoi.backend.entity.AuditLog;
import com.aomaoi.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("CREATE_WORKER");
        auditLog.setPerformedBy("superadmin");
        auditLog.setTargetUser("worker1");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails("Created new worker account");
    }

    @Test
    void getAuditLogs_ReturnsListOfLogs() {
        when(auditLogService.getAllLogs()).thenReturn(List.of(auditLog));

        ResponseEntity<List<AuditLog>> response = auditLogController.getAuditLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("CREATE_WORKER", response.getBody().get(0).getAction());
        verify(auditLogService).getAllLogs();
    }
}

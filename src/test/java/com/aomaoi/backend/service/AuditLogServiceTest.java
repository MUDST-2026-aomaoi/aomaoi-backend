package com.aomaoi.backend.service;

import com.aomaoi.backend.entity.AuditLog;
import com.aomaoi.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("CREATE_WORKER");
        auditLog.setPerformedBy("admin");
        auditLog.setTargetUser("worker1");
        auditLog.setDetails("Created new worker account");
    }

    @Test
    void logAction_SavesAuditLog() {
        auditLogService.logAction("RESET_PASSWORD", "admin1", "worker2", "Reset password for worker");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("RESET_PASSWORD", saved.getAction());
        assertEquals("admin1", saved.getPerformedBy());
        assertEquals("worker2", saved.getTargetUser());
        assertEquals("Reset password for worker", saved.getDetails());
    }

    @Test
    void getAllLogs_ReturnsLogsOrderedByTimestampDesc() {
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(auditLog));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CREATE_WORKER", result.get(0).getAction());
        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }
}

package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerBalanceResponseDTO;
import com.aomaoi.backend.service.WorkerBalanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerBalanceControllerTest {

    @Mock
    private WorkerBalanceService service;

    @InjectMocks
    private WorkerBalanceController controller;

    @Test
    void list_passesPagingToService() {
        List<WorkerBalanceResponseDTO> rows = List.of(mock(WorkerBalanceResponseDTO.class));
        when(service.listForManager(1, 25)).thenReturn(rows);

        assertSame(rows, controller.list(1, 25));
    }

    @Test
    void me_returnsOwnBalance() {
        WorkerBalanceResponseDTO balance = mock(WorkerBalanceResponseDTO.class);
        when(service.getSelf()).thenReturn(balance);

        assertSame(balance, controller.me());
    }

    @Test
    void byWorker_delegatesToService() {
        WorkerBalanceResponseDTO balance = mock(WorkerBalanceResponseDTO.class);
        when(service.getForManager(5L)).thenReturn(balance);

        assertSame(balance, controller.byWorker(5L));
    }
}

package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.dto.SalaryAdvanceResponseDTO;
import com.aomaoi.backend.service.SalaryAdvanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryAdvanceControllerTest {

    @Mock
    private SalaryAdvanceService service;

    @InjectMocks
    private SalaryAdvanceController controller;

    @Test
    void create_returns201WithBody() {
        SalaryAdvanceRequestDTO request = mock(SalaryAdvanceRequestDTO.class);
        SalaryAdvanceResponseDTO created = mock(SalaryAdvanceResponseDTO.class);
        when(service.create(request)).thenReturn(created);

        ResponseEntity<SalaryAdvanceResponseDTO> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void list_passesPagingToService() {
        List<SalaryAdvanceResponseDTO> rows = List.of(mock(SalaryAdvanceResponseDTO.class));
        when(service.listForManager(2, 50)).thenReturn(rows);

        assertSame(rows, controller.list(2, 50));
    }

    @Test
    void me_returnsOwnAdvances() {
        List<SalaryAdvanceResponseDTO> rows = List.of(mock(SalaryAdvanceResponseDTO.class));
        when(service.listSelf()).thenReturn(rows);

        assertSame(rows, controller.me());
    }

    @Test
    void cancel_delegatesToService() {
        SalaryAdvanceResponseDTO cancelled = mock(SalaryAdvanceResponseDTO.class);
        when(service.cancel(9L)).thenReturn(cancelled);

        assertSame(cancelled, controller.cancel(9L));
    }
}

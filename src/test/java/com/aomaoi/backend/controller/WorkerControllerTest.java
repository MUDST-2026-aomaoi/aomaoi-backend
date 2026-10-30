package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.dto.WorkerResponseDTO;
import com.aomaoi.backend.dto.ResetPasswordRequestDTO;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerControllerTest {

    @Mock
    private WorkerService workerService;

    @InjectMocks
    private WorkerController workerController;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setFullName("Worker One");
    }

    @Test
    void getAllWorkers() {
        when(workerService.getAllWorkers()).thenReturn(List.of(worker));

        ResponseEntity<List<WorkerResponseDTO>> response = workerController.getAllWorkers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getWorkerById() {
        when(workerService.getWorkerById(1L)).thenReturn(worker);

        ResponseEntity<WorkerResponseDTO> response = workerController.getWorkerById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Worker One", response.getBody().fullName());
    }

    @Test
    void addWorker() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        when(workerService.addWorker(dto)).thenReturn(worker);

        ResponseEntity<WorkerResponseDTO> response = workerController.addWorker(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void updateWorker() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        when(workerService.updateWorker(1L, dto)).thenReturn(worker);

        ResponseEntity<WorkerResponseDTO> response = workerController.updateWorker(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteWorker() {
        doNothing().when(workerService).deleteWorker(1L);

        ResponseEntity<Void> response = workerController.deleteWorker(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workerService).deleteWorker(1L);
    }

    @Test
    void resetPassword_Success() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("adminuser");
        doNothing().when(workerService).resetWorkerPassword(1L, "newPassword123", "adminuser");

        ResponseEntity<Void> response = workerController.resetPassword(1L, new ResetPasswordRequestDTO("newPassword123"), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workerService).resetWorkerPassword(1L, "newPassword123", "adminuser");
    }

}


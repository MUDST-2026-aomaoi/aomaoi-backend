package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.dto.FarmResponseDTO;
import com.aomaoi.backend.service.FarmService;
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
class FarmControllerTest {

    @Mock
    private FarmService farmService;

    @InjectMocks
    private FarmController farmController;

    @Test
    void getAllFarms() {
        FarmResponseDTO farm = new FarmResponseDTO(1L, "Farm One", null, null,
                0, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, "active", null);
        when(farmService.getAllFarms()).thenReturn(List.of(farm));

        ResponseEntity<List<FarmResponseDTO>> response = farmController.getAllFarms();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void addFarm() {
        FarmRequestDTO dto = new FarmRequestDTO();
        when(farmService.addFarm(dto)).thenReturn(new FarmResponseDTO(1L, "Farm One", null, null,
                0, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, "active", null));

        ResponseEntity<FarmResponseDTO> response = farmController.addFarm(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void updateFarm() {
        FarmRequestDTO dto = new FarmRequestDTO();
        when(farmService.updateFarm(1L, dto)).thenReturn(new FarmResponseDTO(1L, "Farm One", null, null,
                0, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, "active", null));

        ResponseEntity<FarmResponseDTO> response = farmController.updateFarm(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteFarm() {
        doNothing().when(farmService).deleteFarm(1L);

        ResponseEntity<Void> response = farmController.deleteFarm(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(farmService).deleteFarm(1L);
    }
}

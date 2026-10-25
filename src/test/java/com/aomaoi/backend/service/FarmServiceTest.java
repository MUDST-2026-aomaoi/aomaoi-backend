package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private WorkLogRepository workLogRepository;

    @InjectMocks
    private FarmService farmService;

    private Farm farm;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1L);
        farm.setName("Green Farm");
        farm.setLocation("Chiang Mai");
        farm.setImage("farm.png");
        farm.setStatus("active");
    }

    @Test
    void getAllFarms() {
        when(farmRepository.findAll()).thenReturn(List.of(farm));

        List<Farm> result = farmService.getAllFarms();

        assertEquals(1, result.size());
        assertEquals("Green Farm", result.get(0).getName());
    }

    @Test
    void getFarmById_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));

        Farm result = farmService.getFarmById(1L);

        assertNotNull(result);
        assertEquals("Green Farm", result.getName());
    }

    @Test
    void getFarmById_DerivesCurrentMetrics() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(workerRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(4L);
        when(adminProfileRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(1L);
        when(workLogRepository.sumTotalByFarmId(1L)).thenReturn(new BigDecimal("1250.00"));
        when(workLogRepository.sumTotalByFarmIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("300.00"));

        Farm result = farmService.getFarmById(1L);

        assertEquals(4, result.getWorkerCount());
        assertEquals(1, result.getAdminCount());
        assertEquals(1250.0, result.getTotalWages());
        assertEquals(300.0, result.getMonthlyWages());
    }

    @Test
    void getFarmById_NotFound_ThrowsException() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> farmService.getFarmById(99L));
    }

    @Test
    void addFarm_Success() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("New Farm");
        dto.setLocation("Bangkok");
        dto.setImage("new.jpg");

        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        Farm created = farmService.addFarm(dto);

        assertNotNull(created);
        assertEquals("New Farm", created.getName());
        assertEquals("Bangkok", created.getLocation());
    }

    @Test
    void updateFarm_Success() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("Updated Farm");
        dto.setLocation("Phuket");
        dto.setImage("updated.jpg");

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        Farm updated = farmService.updateFarm(1L, dto);

        assertEquals("Updated Farm", updated.getName());
        assertEquals("Phuket", updated.getLocation());
        assertEquals("updated.jpg", updated.getImage());
    }

    @Test
    void updateFarm_NullImage_KeepsExistingImage() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("Updated Farm");
        dto.setLocation("Phuket");
        dto.setImage(null);

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        Farm updated = farmService.updateFarm(1L, dto);

        assertEquals("Updated Farm", updated.getName());
        assertEquals("farm.png", updated.getImage()); // Kept original image
    }

    @Test
    void deleteFarm_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        farmService.deleteFarm(1L);

        assertEquals("inactive", farm.getStatus());
        verify(farmRepository).save(farm);
    }
}

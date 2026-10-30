package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.dto.FarmResponseDTO;
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
import java.util.NoSuchElementException;
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

        List<FarmResponseDTO> result = farmService.getAllFarms();

        assertEquals(1, result.size());
        assertEquals("Green Farm", result.get(0).name());
    }

    @Test
    void getFarmById_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));

        FarmResponseDTO result = farmService.getFarmById(1L);

        assertNotNull(result);
        assertEquals("Green Farm", result.name());
    }

    @Test
    void getFarmById_DerivesCurrentMetrics() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(workerRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(4L);
        when(adminProfileRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(1L);
        when(workLogRepository.sumTotalByFarmId(1L)).thenReturn(new BigDecimal("1250.00"));
        when(workLogRepository.sumTotalByFarmIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("300.00"));

        FarmResponseDTO result = farmService.getFarmById(1L);

        assertEquals(4, result.workerCount());
        assertEquals(1, result.adminCount());
        assertEquals(new BigDecimal("1250.00"), result.totalWages());
        assertEquals(new BigDecimal("300.00"), result.monthlyWages());
    }

    @Test
    void getFarmById_NullTotals_DefaultsToZero() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(workerRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(0L);
        when(adminProfileRepository.countByFarmIdAndStatusNot(1L, "inactive")).thenReturn(0L);
        when(workLogRepository.sumTotalByFarmId(1L)).thenReturn(null);
        when(workLogRepository.sumTotalByFarmIdAndWorkDateBetween(eq(1L), any(), any())).thenReturn(null);

        FarmResponseDTO result = farmService.getFarmById(1L);

        assertEquals(BigDecimal.ZERO, result.totalWages());
        assertEquals(BigDecimal.ZERO, result.monthlyWages());
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

        FarmResponseDTO created = farmService.addFarm(dto);

        assertNotNull(created);
        assertEquals("New Farm", created.name());
        assertEquals("Bangkok", created.location());
    }

    @Test
    void addFarm_NullDto_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> farmService.addFarm(null));
    }

    @Test
    void updateFarm_Success() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("Updated Farm");
        dto.setLocation("Phuket");
        dto.setImage("updated.jpg");

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        FarmResponseDTO updated = farmService.updateFarm(1L, dto);

        assertEquals("Updated Farm", updated.name());
        assertEquals("Phuket", updated.location());
        assertEquals("updated.jpg", updated.image());
    }

    @Test
    void updateFarm_NotFound_ThrowsNoSuchElementException() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("Updated Farm");
        dto.setLocation("Phuket");

        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> farmService.updateFarm(99L, dto));
    }

    @Test
    void updateFarm_NullImage_KeepsExistingImage() {
        FarmRequestDTO dto = new FarmRequestDTO();
        dto.setName("Updated Farm");
        dto.setLocation("Phuket");
        dto.setImage(null);

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        FarmResponseDTO updated = farmService.updateFarm(1L, dto);

        assertEquals("Updated Farm", updated.name());
        assertEquals("farm.png", updated.image()); // Kept original image
    }

    @Test
    void deleteFarm_Success() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        farmService.deleteFarm(1L);

        assertEquals("inactive", farm.getStatus());
        verify(farmRepository).save(farm);
    }

    @Test
    void deleteFarm_NotFound_ThrowsNoSuchElementException() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> farmService.deleteFarm(99L));
    }
}

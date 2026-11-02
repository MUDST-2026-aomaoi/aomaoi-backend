package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.dto.FarmResponseDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final WorkerRepository workerRepository;
    private final AdminProfileRepository adminRepository;
    private final WorkLogRepository workLogRepository;

    @Transactional(readOnly = true)
    public List<FarmResponseDTO> getAllFarms() {
        return farmRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FarmResponseDTO getFarmById(Long id) {
        return toResponse(findFarm(id));
    }

    public FarmResponseDTO addFarm(FarmRequestDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Farm request is required");
        Farm farm = new Farm();
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        farm.setImage(dto.getImage());
        return toResponse(farmRepository.save(farm));
    }

    public FarmResponseDTO updateFarm(Long id, FarmRequestDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Farm request is required");
        Farm farm = findFarm(id);
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        if (dto.getImage() != null) {
            farm.setImage(dto.getImage());
        }
        return toResponse(farmRepository.save(farm));
    }

    public void deleteFarm(Long id) {
        Farm farm = findFarm(id);
        farm.setStatus("inactive");
        farmRepository.save(farm);
    }

    private FarmResponseDTO toResponse(Farm farm) {
        Long id = farm.getId();
        int workerCount = Math.toIntExact(workerRepository.countByFarmIdAndStatusNot(id, "inactive"));
        int adminCount = Math.toIntExact(adminRepository.countByFarmIdAndStatusNot(id, "inactive"));
        var total = workLogRepository.sumTotalByFarmId(id);
        LocalDate today = LocalDate.now();
        var month = workLogRepository.sumTotalByFarmIdAndWorkDateBetween(id, today.withDayOfMonth(1), today);
        return new FarmResponseDTO(farm.getId(), farm.getName(), farm.getLocation(), farm.getImage(),
                workerCount, adminCount, month == null ? BigDecimal.ZERO : month,
                total == null ? BigDecimal.ZERO : total, farm.getStatus(), farm.getJoinedDate());
    }

    private Farm findFarm(Long id) {
        return farmRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Farm not found"));
    }

}

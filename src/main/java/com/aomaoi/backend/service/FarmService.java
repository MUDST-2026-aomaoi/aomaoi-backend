package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final WorkerRepository workerRepository;
    private final AdminProfileRepository adminRepository;
    private final WorkLogRepository workLogRepository;

    public List<Farm> getAllFarms() {
        return farmRepository.findAll().stream().map(this::withMetrics).toList();
    }

    public Farm getFarmById(Long id) {
        return withMetrics(farmRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Farm not found")));
    }

    public Farm addFarm(FarmRequestDTO dto) {
        requireDetails(dto);
        Farm farm = new Farm();
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        farm.setImage(dto.getImage());
        return farmRepository.save(farm);
    }

    public Farm updateFarm(Long id, FarmRequestDTO dto) {
        requireDetails(dto);
        Farm farm = getFarmById(id);
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        if (dto.getImage() != null) {
            farm.setImage(dto.getImage());
        }
        return farmRepository.save(farm);
    }

    public void deleteFarm(Long id) {
        Farm farm = getFarmById(id);
        farm.setStatus("inactive");
        farmRepository.save(farm);
    }

    private Farm withMetrics(Farm farm) {
        Long id = farm.getId();
        farm.setWorkerCount(Math.toIntExact(workerRepository.countByFarmIdAndStatusNot(id, "inactive")));
        farm.setAdminCount(Math.toIntExact(adminRepository.countByFarmIdAndStatusNot(id, "inactive")));
        var total = workLogRepository.sumTotalByFarmId(id);
        LocalDate today = LocalDate.now();
        var month = workLogRepository.sumTotalByFarmIdAndWorkDateBetween(id, today.withDayOfMonth(1), today);
        farm.setTotalWages(total == null ? 0.0 : total.doubleValue());
        farm.setMonthlyWages(month == null ? 0.0 : month.doubleValue());
        return farm;
    }

    private void requireDetails(FarmRequestDTO dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()
                || dto.getLocation() == null || dto.getLocation().isBlank())
            throw new IllegalArgumentException("Farm name and location are required");
    }
}

package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.WorkLogRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final WorkerRepository workerRepository;
    private final com.aomaoi.backend.repository.FarmRepository farmRepository;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;

    public WorkLogService(WorkLogRepository workLogRepository, WorkerRepository workerRepository, com.aomaoi.backend.repository.FarmRepository farmRepository, com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository) {
        this.workLogRepository = workLogRepository;
        this.workerRepository = workerRepository;
        this.farmRepository = farmRepository;
        this.adminProfileRepository = adminProfileRepository;
    }

    public List<Map<String, Object>> getAllLogs() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        List<WorkLog> logs = workLogRepository.findAllByOrderByWorkDateDescIdDesc();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("superadmin") && !auth.getName().equals("anonymousUser")) {
            String currentUsername = auth.getName();
            return adminProfileRepository.findByUserUsername(currentUsername)
                    .map(adminProfile -> logs.stream()
                            .filter(log -> adminProfile.getFarmId().equals(log.getWorker().getFarmId()))
                            .map(this::logToMap)
                            .collect(Collectors.toList()))
                    .orElseGet(() -> logs.stream().map(this::logToMap).collect(Collectors.toList()));
        }

        return logs.stream().map(this::logToMap).collect(Collectors.toList());
    }

    public Map<String, Object> addLog(WorkLogRequestDTO dto) {
        // Validate: no negative values
        validatePositive(dto);

        Worker worker = workerRepository.findById(Long.parseLong(dto.getWorkerId()))
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        WorkLog log = new WorkLog();
        log.setWorker(worker);
        log.setType(dto.getType());
        log.setWorkDate(LocalDate.parse(dto.getDate()));

        // Set type-specific fields
        log.setRows(dto.getRows());
        log.setWaPerRow(dto.getWaPerRow());
        log.setFurrows(dto.getFurrows());
        log.setWaPerFurrow(dto.getWaPerFurrow());
        log.setDays(dto.getDays());
        log.setDailyRate(dto.getDailyRate());
        log.setTanks(dto.getTanks());

        // Calculate total wage based on type
        log.setTotal(calculateWage(dto));

        WorkLog saved = workLogRepository.save(log);

        // Update the Farm's payroll
        if (worker.getFarmId() != null) {
            com.aomaoi.backend.entity.Farm farm = this.farmRepository.findById(Long.parseLong(worker.getFarmId())).orElse(null);
            if (farm != null) {
                farm.setTotalWages(farm.getTotalWages() + log.getTotal().doubleValue());
                farm.setMonthlyWages(farm.getMonthlyWages() + log.getTotal().doubleValue());
                this.farmRepository.save(farm);
            }
        }

        return logToMap(saved);
    }

    public BigDecimal calculateWage(WorkLogRequestDTO dto) {
        return switch (dto.getType()) {
            case "cutting" -> {
                if (dto.getRows() == null || dto.getWaPerRow() == null)
                    throw new RuntimeException("Cutting requires rows and waPerRow");
                yield BigDecimal.valueOf((long) dto.getRows() * dto.getWaPerRow() * 2);
            }
            case "planting" -> {
                if (dto.getFurrows() == null || dto.getWaPerFurrow() == null)
                    throw new RuntimeException("Planting requires furrows and waPerFurrow");
                yield BigDecimal.valueOf(dto.getFurrows() * dto.getWaPerFurrow() * 2.5);
            }
            case "watering" -> {
                if (dto.getDays() == null || dto.getDailyRate() == null)
                    throw new RuntimeException("Watering requires days and dailyRate");
                yield dto.getDailyRate().multiply(BigDecimal.valueOf(dto.getDays()));
            }
            case "spraying" -> {
                if (dto.getTanks() == null)
                    throw new RuntimeException("Spraying requires tanks");
                yield BigDecimal.valueOf((long) dto.getTanks() * 150);
            }
            default -> throw new RuntimeException("Unknown work type: " + dto.getType());
        };
    }

    private void validatePositive(WorkLogRequestDTO dto) {
        if (dto.getRows() != null && dto.getRows() < 0)
            throw new RuntimeException("rows cannot be negative");
        if (dto.getWaPerRow() != null && dto.getWaPerRow() < 0)
            throw new RuntimeException("waPerRow cannot be negative");
        if (dto.getFurrows() != null && dto.getFurrows() < 0)
            throw new RuntimeException("furrows cannot be negative");
        if (dto.getWaPerFurrow() != null && dto.getWaPerFurrow() < 0)
            throw new RuntimeException("waPerFurrow cannot be negative");
        if (dto.getDays() != null && dto.getDays() < 0)
            throw new RuntimeException("days cannot be negative");
        if (dto.getDailyRate() != null && dto.getDailyRate().compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("dailyRate cannot be negative");
        if (dto.getTanks() != null && dto.getTanks() < 0)
            throw new RuntimeException("tanks cannot be negative");
    }

    public BigDecimal getTotalAllTime() {
        return workLogRepository.findAll().stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalThisMonth() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        return workLogRepository.findByWorkDateBetween(startOfMonth, today).stream()
                .map(WorkLog::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> logToMap(WorkLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(log.getId()));
        map.put("type", log.getType());
        map.put("date", log.getWorkDate().toString());
        map.put("workerId", String.valueOf(log.getWorker().getId()));
        map.put("total", log.getTotal().doubleValue());

        // Include type-specific fields
        if (log.getRows() != null) map.put("rows", log.getRows());
        if (log.getWaPerRow() != null) map.put("waPerRow", log.getWaPerRow());
        if (log.getFurrows() != null) map.put("furrows", log.getFurrows());
        if (log.getWaPerFurrow() != null) map.put("waPerFurrow", log.getWaPerFurrow());
        if (log.getDays() != null) map.put("days", log.getDays());
        if (log.getDailyRate() != null) map.put("dailyRate", log.getDailyRate().doubleValue());
        if (log.getTanks() != null) map.put("tanks", log.getTanks());

        return map;
    }
}

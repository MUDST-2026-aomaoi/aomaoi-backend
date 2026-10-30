package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.dto.SalaryAdvanceResponseDTO;
import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.SalaryAdvance;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class SalaryAdvanceService {
    private final SalaryAdvanceRepository advanceRepository;
    private final FinancialAccessService access;
    private final AuditLogService auditLogService;

    @Transactional
    public SalaryAdvanceResponseDTO create(SalaryAdvanceRequestDTO request) {
        String actor = access.requireManager().getName();
        validate(request);
        Worker worker = access.requireManagedWorker(request.workerId());
        if (worker.getFarmId() == null) {
            throw new IllegalArgumentException("Worker must belong to a farm before recording an advance");
        }
        if ("inactive".equals(worker.getStatus())) {
            throw new IllegalArgumentException("Cannot create an advance for an inactive worker");
        }
        SalaryAdvance advance = new SalaryAdvance();
        advance.setWorker(worker);
        advance.setAdvanceDate(request.advanceDate());
        advance.setType(request.type());
        advance.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        advance.setItemName(request.type() == AdvanceType.GOODS ? request.itemName().trim() : null);
        advance.setRecordedBy(actor);
        advance.setCreatedAt(Instant.now());
        SalaryAdvance saved = advanceRepository.save(advance);
        auditLogService.logAction("CREATE_SALARY_ADVANCE", actor, auditTarget(worker),
                "Recorded " + saved.getType() + " advance #" + saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceResponseDTO> listForManager(int page, int size) {
        validatePage(page, size);
        Long farmId = access.managerFarmIdOrNull();
        var pageable = PageRequest.of(page, size);
        var advances = farmId == null
                ? advanceRepository.findAllByOrderByAdvanceDateDescIdDesc(pageable)
                : advanceRepository.findByWorkerFarmIdOrderByAdvanceDateDescIdDesc(farmId, pageable);
        return advances.stream().map(this::toResponse).toList();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 500) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 500");
        }
    }

    @Transactional
    public SalaryAdvanceResponseDTO cancel(Long id) {
        String actor = access.requireManager().getName();
        SalaryAdvance advance = advanceRepository.findLockedById(id)
                .orElseThrow(() -> new NoSuchElementException("Salary advance not found"));
        access.requireManagedWorker(advance.getWorker().getId());
        if (advance.getCancelledAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Salary advance already cancelled");
        }
        advance.setCancelledAt(Instant.now());
        advance.setCancelledBy(actor);
        SalaryAdvance saved = advanceRepository.save(advance);
        auditLogService.logAction("CANCEL_SALARY_ADVANCE", actor, auditTarget(advance.getWorker()),
                "Cancelled salary advance #" + saved.getId());
        return toResponse(saved);
    }

    private void validate(SalaryAdvanceRequestDTO request) {
        if (request == null || request.workerId() == null || request.workerId() <= 0
                || request.advanceDate() == null || request.type() == null || request.amount() == null) {
            throw new IllegalArgumentException("Worker, date, type and amount are required");
        }
        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount must have at most two decimal places", e);
        }
        if (amount.signum() <= 0 || amount.precision() > 19) {
            throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,2)");
        }
        if (request.type() == AdvanceType.GOODS) {
            if (request.itemName() == null || request.itemName().isBlank()
                    || request.itemName().trim().length() > 255) {
                throw new IllegalArgumentException("Goods advance requires an item name up to 255 characters");
            }
        } else if (request.itemName() != null && !request.itemName().isBlank()) {
            throw new IllegalArgumentException("Cash advance cannot have an item name");
        }
    }

    private SalaryAdvanceResponseDTO toResponse(SalaryAdvance advance) {
        Worker worker = advance.getWorker();
        return new SalaryAdvanceResponseDTO(advance.getId(), worker.getId(), worker.getFullName(),
                worker.getNickname(), advance.getAdvanceDate(), advance.getType(), advance.getAmount(),
                advance.getItemName(), advance.getRecordedBy(), advance.getCreatedAt(),
                advance.getCancelledAt(), advance.getCancelledBy());
    }

    private String auditTarget(Worker worker) {
        return worker.getUsername() == null ? "worker:" + worker.getId() : worker.getUsername();
    }
}

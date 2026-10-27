package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.dto.SalaryAdvanceResponseDTO;
import com.aomaoi.backend.service.SalaryAdvanceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/salary-advances")
@RequiredArgsConstructor
public class SalaryAdvanceController {
    private final SalaryAdvanceService service;

    @PostMapping
    public ResponseEntity<SalaryAdvanceResponseDTO> create(@Valid @RequestBody SalaryAdvanceRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public List<SalaryAdvanceResponseDTO> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "100") int size) {
        return service.listForManager(page, size);
    }

    @DeleteMapping("/{id}")
    public SalaryAdvanceResponseDTO cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

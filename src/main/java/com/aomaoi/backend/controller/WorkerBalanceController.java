package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerBalanceResponseDTO;
import com.aomaoi.backend.service.WorkerBalanceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/worker-balances")
@RequiredArgsConstructor
public class WorkerBalanceController {
    private final WorkerBalanceService service;

    @GetMapping
    public List<WorkerBalanceResponseDTO> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "100") int size) {
        return service.listForManager(page, size);
    }

    @GetMapping("/me")
    public WorkerBalanceResponseDTO me() {
        return service.getSelf();
    }

    @GetMapping("/{workerId}")
    public WorkerBalanceResponseDTO byWorker(@PathVariable Long workerId) {
        return service.getForManager(workerId);
    }
}

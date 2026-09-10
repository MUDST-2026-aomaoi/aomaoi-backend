package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.service.WorkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
@CrossOrigin(origins = "*")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addWorker(@RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.addWorker(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWorker(@PathVariable Long id, @RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.updateWorker(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWorker(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.softDeleteWorker(id));
    }
}

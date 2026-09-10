package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@CrossOrigin(origins = "*")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    @GetMapping
    public ResponseEntity<List<Worker>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Worker> getWorkerById(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.getWorkerById(id));
    }

    @PostMapping
    public ResponseEntity<Worker> addWorker(@RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.addWorker(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Worker> updateWorker(@PathVariable Long id, @RequestBody WorkerRequestDTO dto) {
        return ResponseEntity.ok(workerService.updateWorker(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(@PathVariable Long id) {
        workerService.deleteWorker(id);
        return ResponseEntity.ok().build();
    }
}

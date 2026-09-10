package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.WorkLogRequestDTO;
import com.aomaoi.backend.service.WorkLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-logs")
@CrossOrigin(origins = "*")
public class WorkLogController {

    private final WorkLogService workLogService;

    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLogs() {
        return ResponseEntity.ok(workLogService.getAllLogs());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addLog(@RequestBody WorkLogRequestDTO dto) {
        return ResponseEntity.ok(workLogService.addLog(dto));
    }
}

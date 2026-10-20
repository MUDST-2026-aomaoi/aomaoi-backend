package com.aomaoi.backend.controller;

import com.aomaoi.backend.service.E2eRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test-support")
@RequiredArgsConstructor
public class E2eTestSafetyController {
    private final E2eRunService runs;

    @GetMapping("/e2e-readiness")
    public Map<String, Object> readiness() {
        return Map.of("safeForE2e", runs.enabled(), "mode", "shared-tracked");
    }

    @PostMapping("/runs")
    public Map<String, String> create(@RequestHeader("X-E2E-Support-Key") String key) {
        return runs.create(key);
    }

    @PostMapping("/runs/{id}/heartbeat")
    public Map<String, Boolean> heartbeat(@PathVariable String id,
                                        @RequestHeader("X-E2E-Token") String token) {
        runs.heartbeat(id, token);
        return Map.of("alive", true);
    }

    @DeleteMapping("/runs/{id}")
    public Map<String, Object> cleanup(@PathVariable String id,
                                      @RequestHeader("X-E2E-Support-Key") String key) {
        return runs.cleanup(id, key);
    }
}

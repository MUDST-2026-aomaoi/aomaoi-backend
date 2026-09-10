package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.service.FarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@CrossOrigin(origins = "*")
public class FarmController {

    @Autowired
    private FarmService farmService;

    @GetMapping
    public ResponseEntity<List<Farm>> getAllFarms() {
        return ResponseEntity.ok(farmService.getAllFarms());
    }

    @PostMapping
    public ResponseEntity<Farm> addFarm(@RequestBody FarmRequestDTO dto) {
        return ResponseEntity.ok(farmService.addFarm(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Farm> updateFarm(@PathVariable Long id, @RequestBody FarmRequestDTO dto) {
        return ResponseEntity.ok(farmService.updateFarm(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long id) {
        farmService.deleteFarm(id);
        return ResponseEntity.ok().build();
    }
}

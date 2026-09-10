package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.FarmRequestDTO;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.repository.FarmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FarmService {

    @Autowired
    private FarmRepository farmRepository;

    public List<Farm> getAllFarms() {
        return farmRepository.findAll();
    }

    public Farm getFarmById(Long id) {
        return farmRepository.findById(id).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    public Farm addFarm(FarmRequestDTO dto) {
        Farm farm = new Farm();
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        return farmRepository.save(farm);
    }

    public Farm updateFarm(Long id, FarmRequestDTO dto) {
        Farm farm = getFarmById(id);
        farm.setName(dto.getName());
        farm.setLocation(dto.getLocation());
        return farmRepository.save(farm);
    }

    public void deleteFarm(Long id) {
        Farm farm = getFarmById(id);
        farm.setStatus("inactive");
        farmRepository.save(farm);
    }
}

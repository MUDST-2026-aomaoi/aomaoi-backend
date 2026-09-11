package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerService {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;

    @Autowired
    private com.aomaoi.backend.repository.FarmRepository farmRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Worker> getAllWorkers() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("superadmin") && !auth.getName().equals("anonymousUser")) {
            String currentUsername = auth.getName();
            return adminProfileRepository.findByUserUsername(currentUsername)
                    .map(adminProfile -> workerRepository.findAll().stream()
                            .filter(worker -> adminProfile.getFarmId().equals(worker.getFarmId()))
                            .toList())
                    .orElseGet(() -> workerRepository.findAll());
        }
        return workerRepository.findAll();
    }

    public List<Worker> getActiveWorkers() {
        return workerRepository.findByStatus("active");
    }

    public Worker getWorkerById(Long id) {
        return workerRepository.findById(id).orElseThrow(() -> new RuntimeException("Worker not found"));
    }

    @Transactional
    public Worker addWorker(WorkerRequestDTO dto) {
        if (dto.getUsername() == null || !dto.getUsername().matches("^[\\w\\p{Punct}]+$")) {
            throw new RuntimeException("Username cannot contain spaces and must contain only characters, numbers, and special characters.");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getTempPassword()));
        user.setRole("worker");
        user.setIsFirstLogin(true);
        user = userRepository.save(user);

        Worker worker = new Worker();
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());
        worker.setStatus("pending");
        worker.setUser(user);

        // Get currently logged-in user and assign farmId if they are an admin
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String currentUsername = auth.getName();
            adminProfileRepository.findByUserUsername(currentUsername).ifPresent(adminProfile -> {
                worker.setFarmId(adminProfile.getFarmId());
                if (adminProfile.getFarmId() != null) {
                    com.aomaoi.backend.entity.Farm farm = farmRepository.findById(Long.parseLong(adminProfile.getFarmId())).orElse(null);
                    if (farm != null) {
                        farm.setWorkerCount(farm.getWorkerCount() + 1);
                        farmRepository.save(farm);
                    }
                }
            });
        }

        return workerRepository.save(worker);
    }

    @Transactional
    public Worker updateWorker(Long id, WorkerRequestDTO dto) {
        Worker worker = getWorkerById(id);
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());

        if (dto.getUsername() != null && !dto.getUsername().equals(worker.getUser().getUsername())) {
            if (!dto.getUsername().matches("^[\\w\\p{Punct}]+$")) {
                throw new RuntimeException("Username cannot contain spaces and must contain only characters, numbers, and special characters.");
            }
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            worker.getUser().setUsername(dto.getUsername());
            userRepository.save(worker.getUser());
        }

        return workerRepository.save(worker);
    }

    @Transactional
    public void deleteWorker(Long id) {
        Worker worker = getWorkerById(id);
        worker.setStatus("inactive");
        workerRepository.save(worker);
    }
}

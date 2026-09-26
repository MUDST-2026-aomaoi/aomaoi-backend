package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final com.aomaoi.backend.repository.FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

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
        String currentUsername = "system";
        if (auth != null && auth.isAuthenticated()) {
            currentUsername = auth.getName();
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

        Worker savedWorker = workerRepository.save(worker);
        auditLogService.logAction("CREATE_WORKER", currentUsername, user.getUsername(), "Created new worker account");
        return savedWorker;
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

        Worker savedWorker = workerRepository.save(worker);
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("UPDATE_WORKER", currentUsername, worker.getUser().getUsername(), "Updated worker account details");
        return savedWorker;
    }

    @Transactional
    public void deleteWorker(Long id) {
        Worker worker = getWorkerById(id);
        worker.setStatus("inactive");
        workerRepository.save(worker);
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("DELETE_WORKER", currentUsername, worker.getUser().getUsername(), "Deactivated worker account");
    }

    @Transactional
    public void resetWorkerPassword(Long workerId, String newPassword, String adminUsername) {
        Worker worker = getWorkerById(workerId);

        // If the caller is an admin (not superadmin), they may only reset workers in their own farm.
        adminProfileRepository.findByUserUsername(adminUsername).ifPresent(adminProfile -> {
            if (!adminProfile.getFarmId().equals(worker.getFarmId())) {
                throw new org.springframework.security.access.AccessDeniedException("Cannot reset a worker outside your farm");
            }
        });

        User user = worker.getUser();

        // Update to new password provided by admin
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(true); // Force them to change it again
        worker.setStatus("pending"); // Set status back to pending
        
        userRepository.save(user);
        workerRepository.save(worker);
        
        auditLogService.logAction(
            "RESET_PASSWORD", 
            adminUsername, 
            user.getUsername(), 
            "Reset worker password"
        );
    }
}

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final com.aomaoi.backend.repository.AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<Worker> getAllWorkers() {
        Authentication auth = requireAuthentication();
        if (hasRole(auth, "SUPERADMIN")) return workerRepository.findAll();
        if (hasRole(auth, "ADMIN")) return workerRepository.findByFarmId(requireAdminFarm(auth.getName()));
        if (hasRole(auth, "WORKER")) return workerRepository.findByUserUsername(auth.getName())
                .map(List::of).orElseGet(List::of);
        throw new AccessDeniedException("Unknown role");
    }

    public Worker getWorkerById(Long id) {
        Worker worker = workerRepository.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Worker not found"));
        requireWorkerAccess(worker, false);
        return worker;
    }

    @Transactional
    public Worker addWorker(WorkerRequestDTO dto) {
        Authentication auth = requireAuthentication();
        if (!hasRole(auth, "ADMIN") && !hasRole(auth, "SUPERADMIN"))
            throw new AccessDeniedException("Worker management requires admin access");
        if (dto.getTempPassword() == null || dto.getTempPassword().length() < 6)
            throw new IllegalArgumentException("Temporary password must be at least 6 characters");
        if (dto.getFullName() == null || dto.getFullName().isBlank())
            throw new IllegalArgumentException("Worker full name is required");
        if (dto.getUsername() == null || !dto.getUsername().matches("^[\\w\\p{Punct}]+$")) {
            throw new IllegalArgumentException("Username cannot contain spaces and must contain only characters, numbers, and special characters.");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
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
        String currentUsername = auth.getName();
        if (hasRole(auth, "ADMIN")) worker.setFarmId(requireAdminFarm(currentUsername));

        Worker savedWorker = workerRepository.save(worker);
        auditLogService.logAction("CREATE_WORKER", currentUsername, user.getUsername(), "Created new worker account");
        return savedWorker;
    }

    @Transactional
    public Worker updateWorker(Long id, WorkerRequestDTO dto) {
        Worker worker = getWorkerById(id);
        requireWorkerAccess(worker, true);
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());

        if (dto.getUsername() != null && !dto.getUsername().equals(worker.getUser().getUsername())) {
            if (!dto.getUsername().matches("^[\\w\\p{Punct}]+$")) {
                throw new IllegalArgumentException("Username cannot contain spaces and must contain only characters, numbers, and special characters.");
            }
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new IllegalArgumentException("Username already exists");
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
        requireWorkerAccess(worker, true);
        worker.setStatus("inactive");
        workerRepository.save(worker);
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "system";
        auditLogService.logAction("DELETE_WORKER", currentUsername, worker.getUser().getUsername(), "Deactivated worker account");
    }

    @Transactional
    public void resetWorkerPassword(Long workerId, String newPassword, String adminUsername) {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");
        Worker worker = getWorkerById(workerId);
        requireWorkerAccess(worker, true);

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

    private Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Authentication required");
        return auth;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private Long requireAdminFarm(String username) {
        return adminProfileRepository.findByUserUsername(username)
                .map(profile -> profile.getFarmId())
                .orElseThrow(() -> new AccessDeniedException("Admin has no farm"));
    }

    private void requireWorkerAccess(Worker worker, boolean write) {
        Authentication auth = requireAuthentication();
        if (hasRole(auth, "SUPERADMIN")) return;
        if (hasRole(auth, "ADMIN") && worker.getFarmId() != null
                && worker.getFarmId().equals(requireAdminFarm(auth.getName()))) return;
        if (!write && hasRole(auth, "WORKER") && worker.getUser() != null
                && auth.getName().equals(worker.getUser().getUsername())) return;
        throw new AccessDeniedException("Worker is outside your access scope");
    }
}

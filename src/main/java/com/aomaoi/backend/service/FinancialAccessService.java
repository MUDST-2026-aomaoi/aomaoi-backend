package com.aomaoi.backend.service;

import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class FinancialAccessService {
    private final WorkerRepository workerRepository;
    private final AdminProfileRepository adminProfileRepository;

    public Authentication requireManager() {
        Authentication actor = requireActor();
        if (!hasRole(actor, "ADMIN") && !hasRole(actor, "SUPERADMIN")) {
            throw new AccessDeniedException("Admin access required");
        }
        return actor;
    }

    public Worker requireManagedWorker(Long id) {
        Authentication actor = requireManager();
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Worker not found"));
        if (hasRole(actor, "ADMIN") && (worker.getFarmId() == null
                || !worker.getFarmId().equals(requireAdminFarm(actor.getName())))) {
            throw new AccessDeniedException("Worker is outside your farm");
        }
        return worker;
    }

    public Page<Worker> managedWorkersPage(int page, int size) {
        Authentication actor = requireManager();
        var pageable = PageRequest.of(page, size, Sort.by("id"));
        if (hasRole(actor, "SUPERADMIN")) return workerRepository.findAll(pageable);
        return workerRepository.findByFarmId(requireAdminFarm(actor.getName()), pageable);
    }

    public Long managerFarmIdOrNull() {
        Authentication actor = requireManager();
        return hasRole(actor, "SUPERADMIN") ? null : requireAdminFarm(actor.getName());
    }

    public Worker requireSelf() {
        Authentication actor = requireActor();
        if (!hasRole(actor, "WORKER")) throw new AccessDeniedException("Worker access required");
        return workerRepository.findByUserUsername(actor.getName())
                .orElseThrow(() -> new AccessDeniedException("Worker profile missing"));
    }

    private Authentication requireActor() {
        Authentication actor = SecurityContextHolder.getContext().getAuthentication();
        if (actor == null || !actor.isAuthenticated() || "anonymousUser".equals(actor.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return actor;
    }

    private boolean hasRole(Authentication actor, String role) {
        return actor.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    private Long requireAdminFarm(String username) {
        return adminProfileRepository.findByUserUsername(username)
                .filter(profile -> !"inactive".equals(profile.getStatus()))
                .map(profile -> profile.getFarmId())
                .orElseThrow(() -> new AccessDeniedException("Admin has no active farm"));
    }
}

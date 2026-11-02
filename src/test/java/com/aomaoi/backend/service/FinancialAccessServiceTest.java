package com.aomaoi.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class FinancialAccessServiceTest {
    @Mock WorkerRepository workers;
    @Mock AdminProfileRepository admins;
    @InjectMocks FinancialAccessService access;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCannotManageWorkerFromAnotherFarm() {
        authenticate("admin1", "ADMIN");
        Worker worker = new Worker();
        worker.setId(7L);
        worker.setFarmId(2L);
        AdminProfile admin = new AdminProfile();
        admin.setFarmId(1L);
        admin.setStatus("active");
        when(workers.findById(7L)).thenReturn(Optional.of(worker));
        when(admins.findByUserUsername("admin1")).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () -> access.requireManagedWorker(7L));
    }

    @Test
    void inactiveAdminCannotAccessFarmFinancialData() {
        authenticate("admin1", "ADMIN");
        AdminProfile admin = new AdminProfile();
        admin.setFarmId(1L);
        admin.setStatus("inactive");
        when(admins.findByUserUsername("admin1")).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () -> access.managedWorkersPage(0, 100));
    }

    @Test
    void adminWithoutFarmCannotAccessFarmFinancialData() {
        authenticate("admin1", "ADMIN");
        AdminProfile admin = new AdminProfile();
        admin.setStatus("active");
        when(admins.findByUserUsername("admin1")).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () -> access.managedWorkersPage(0, 100));
    }

    @Test
    void workerCannotManageAdvances() {
        authenticate("worker1", "WORKER");
        assertThrows(AccessDeniedException.class, access::requireManager);
    }

    @Test
    void adminListIsScopedToTheirFarm() {
        authenticate("admin1", "ADMIN");
        AdminProfile admin = new AdminProfile();
        admin.setFarmId(1L);
        admin.setStatus("active");
        Worker worker = new Worker();
        worker.setFarmId(1L);
        when(admins.findByUserUsername("admin1")).thenReturn(Optional.of(admin));
        var pageable = PageRequest.of(0, 100, Sort.by("id"));
        when(workers.findByFarmId(1L, pageable)).thenReturn(new PageImpl<>(List.of(worker)));

        assertEquals(List.of(worker), access.managedWorkersPage(0, 100).getContent());
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}

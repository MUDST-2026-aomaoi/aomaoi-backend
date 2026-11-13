package com.aomaoi.backend.service;

import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link FinancialAccessServiceTest}. */
@ExtendWith(MockitoExtension.class)
class FinancialAccessServiceBranchTest {

    @Mock
    WorkerRepository workers;
    @Mock
    AdminProfileRepository admins;
    @InjectMocks
    FinancialAccessService access;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void adminWithFarm(long farmId) {
        authenticate("admin1", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(farmId);
        profile.setStatus("active");
        when(admins.findByUserUsername("admin1")).thenReturn(Optional.of(profile));
    }

    private Worker worker(Long farmId) {
        Worker worker = new Worker();
        worker.setId(7L);
        worker.setFarmId(farmId);
        return worker;
    }

    // ---------- authentication ----------

    @Test
    void requireManager_withoutAuthentication_denied() {
        assertThrows(AccessDeniedException.class, access::requireManager);
    }

    @Test
    void requireManager_unauthenticatedToken_denied() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));

        assertThrows(AccessDeniedException.class, access::requireManager);
    }

    @Test
    void requireManager_anonymousUser_denied() {
        authenticate("anonymousUser", "ANONYMOUS");

        assertThrows(AccessDeniedException.class, access::requireManager);
    }

    @Test
    void requireManager_adminAndSuperadminAllowed() {
        authenticate("admin1", "ADMIN");
        assertEquals("admin1", access.requireManager().getName());

        authenticate("root", "SUPERADMIN");
        assertEquals("root", access.requireManager().getName());
    }

    // ---------- requireManagedWorker ----------

    @Test
    void requireManagedWorker_unknownWorker_throws() {
        authenticate("root", "SUPERADMIN");
        when(workers.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> access.requireManagedWorker(7L));
    }

    @Test
    void requireManagedWorker_superadminReachesAnyFarm() {
        authenticate("root", "SUPERADMIN");
        Worker worker = worker(99L);
        when(workers.findById(7L)).thenReturn(Optional.of(worker));

        assertSame(worker, access.requireManagedWorker(7L));
        verifyNoInteractions(admins);
    }

    @Test
    void requireManagedWorker_adminWithWorkerWithoutFarm_denied() {
        authenticate("admin1", "ADMIN");
        when(workers.findById(7L)).thenReturn(Optional.of(worker(null)));

        assertThrows(AccessDeniedException.class, () -> access.requireManagedWorker(7L));
    }

    @Test
    void requireManagedWorker_adminOwnFarm_allowed() {
        adminWithFarm(1L);
        Worker worker = worker(1L);
        when(workers.findById(7L)).thenReturn(Optional.of(worker));

        assertSame(worker, access.requireManagedWorker(7L));
    }

    // ---------- managedWorkersPage / managerFarmIdOrNull ----------

    @Test
    void managedWorkersPage_superadminSeesAllFarms() {
        authenticate("root", "SUPERADMIN");
        var pageable = PageRequest.of(0, 10, Sort.by("id"));
        Worker worker = worker(3L);
        when(workers.findAll(pageable)).thenReturn(new PageImpl<>(List.of(worker)));

        assertEquals(List.of(worker), access.managedWorkersPage(0, 10).getContent());
        verifyNoInteractions(admins);
    }

    @Test
    void managerFarmIdOrNull_superadminIsUnscoped() {
        authenticate("root", "SUPERADMIN");

        assertNull(access.managerFarmIdOrNull());
    }

    @Test
    void managerFarmIdOrNull_adminIsScopedToOwnFarm() {
        adminWithFarm(4L);

        assertEquals(4L, access.managerFarmIdOrNull());
    }

    // ---------- requireSelf ----------

    @Test
    void requireSelf_nonWorker_denied() {
        authenticate("admin1", "ADMIN");

        assertThrows(AccessDeniedException.class, access::requireSelf);
    }

    @Test
    void requireSelf_workerWithoutProfile_denied() {
        authenticate("worker1", "WORKER");
        when(workers.findByUserUsername("worker1")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, access::requireSelf);
    }

    @Test
    void requireSelf_workerWithProfile_returnsWorker() {
        authenticate("worker1", "WORKER");
        Worker worker = worker(1L);
        User user = new User();
        user.setUsername("worker1");
        worker.setUser(user);
        when(workers.findByUserUsername("worker1")).thenReturn(Optional.of(worker));

        assertSame(worker, access.requireSelf());
    }
}

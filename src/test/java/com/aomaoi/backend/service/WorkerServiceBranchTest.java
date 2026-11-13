package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link WorkerServiceTest}. */
@ExtendWith(MockitoExtension.class)
class WorkerServiceBranchTest {

    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkerService service;

    private User user;
    private Worker worker;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("worker1");
        worker = new Worker();
        worker.setId(1L);
        worker.setFarmId(10L);
        worker.setUser(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "pw", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void adminOfFarm(long farmId) {
        loginAs("adminuser", "ADMIN");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(farmId);
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));
    }

    private WorkerRequestDTO dto(String username) {
        WorkerRequestDTO d = new WorkerRequestDTO();
        d.setFullName("Full Name");
        d.setUsername(username);
        d.setTempPassword("temp");
        return d;
    }

    // ---------- getAllWorkers / getWorkerById ----------

    @Test
    void getAllWorkers_noAuthentication_denied() {
        assertThrows(AccessDeniedException.class, () -> service.getAllWorkers());
    }

    @Test
    void getAllWorkers_unauthenticatedToken_denied() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));

        assertThrows(AccessDeniedException.class, () -> service.getAllWorkers());
    }

    @Test
    void getAllWorkers_anonymousUser_denied() {
        loginAs("anonymousUser", "ANONYMOUS");

        assertThrows(AccessDeniedException.class, () -> service.getAllWorkers());
    }

    @Test
    void getAllWorkers_workerWithoutProfile_returnsEmpty() {
        loginAs("worker1", "WORKER");
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.empty());

        assertTrue(service.getAllWorkers().isEmpty());
    }

    @Test
    void getAllWorkers_workerWithProfile_returnsOnlySelf() {
        loginAs("worker1", "WORKER");
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.of(worker));

        assertEquals(List.of(worker), service.getAllWorkers());
    }

    @Test
    void getWorkerById_workerReadsSelf_butNotOthers() {
        loginAs("worker1", "WORKER");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        assertSame(worker, service.getWorkerById(1L));

        Worker other = new Worker();
        other.setId(2L);
        User otherUser = new User();
        otherUser.setUsername("worker2");
        other.setUser(otherUser);
        when(workerRepository.findById(2L)).thenReturn(Optional.of(other));
        assertThrows(AccessDeniedException.class, () -> service.getWorkerById(2L));
    }

    @Test
    void getWorkerById_workerReadingAccountlessWorker_denied() {
        loginAs("worker1", "WORKER");
        Worker orphan = new Worker();
        orphan.setId(3L);
        when(workerRepository.findById(3L)).thenReturn(Optional.of(orphan));

        assertThrows(AccessDeniedException.class, () -> service.getWorkerById(3L));
    }

    @Test
    void getWorkerById_adminWorkerWithoutFarm_denied() {
        loginAs("adminuser", "ADMIN");
        worker.setFarmId(null);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(AccessDeniedException.class, () -> service.getWorkerById(1L));
    }

    // ---------- updateWorker ----------

    @Test
    void updateWorker_nullUsername_keepsExistingUsername() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        service.updateWorker(1L, dto(null));

        assertEquals("worker1", user.getUsername());
        verify(userRepository, never()).save(any());
        verify(auditLogService).logAction(eq("UPDATE_WORKER"), eq("root"), eq("worker1"), any());
    }

    @Test
    void updateWorker_sameUsername_skipsUniquenessCheck() {
        loginAs("root", "SUPERADMIN");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        service.updateWorker(1L, dto("worker1"));

        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateWorker_adminCannotWriteWorkerOfAnotherFarm() {
        adminOfFarm(99L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(AccessDeniedException.class, () -> service.updateWorker(1L, dto("worker1")));
        verify(workerRepository, never()).save(any());
    }

    @Test
    void updateWorker_workerCannotWriteEvenOwnProfile() {
        loginAs("worker1", "WORKER");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(AccessDeniedException.class, () -> service.updateWorker(1L, dto("worker1")));
    }

    // ---------- deleteWorker ----------

    @Test
    void deleteWorker_adminOwnFarm_deactivates() {
        adminOfFarm(10L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        service.deleteWorker(1L);

        assertEquals("inactive", worker.getStatus());
        verify(auditLogService).logAction(eq("DELETE_WORKER"), eq("adminuser"), eq("worker1"), any());
    }

    // ---------- resetWorkerPassword ----------

    @Test
    void resetWorkerPassword_nullPassword_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.resetWorkerPassword(1L, null, "adminuser"));
        verifyNoInteractions(workerRepository);
    }

    @Test
    void resetWorkerPassword_shortPassword_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.resetWorkerPassword(1L, "12345", "adminuser"));
        verifyNoInteractions(workerRepository);
    }

    @Test
    void resetWorkerPassword_adminOtherFarm_denied() {
        adminOfFarm(99L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(AccessDeniedException.class, () -> service.resetWorkerPassword(1L, "newpassword", "adminuser"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetWorkerPassword_adminOwnFarm_resetsAndForcesFirstLogin() {
        adminOfFarm(10L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(passwordEncoder.encode("newpassword")).thenReturn("enc");

        service.resetWorkerPassword(1L, "newpassword", "adminuser");

        assertEquals("enc", user.getPassword());
        assertTrue(user.getIsFirstLogin());
        assertEquals("pending", worker.getStatus());
        verify(auditLogService).logAction("RESET_PASSWORD", "adminuser", "worker1", "Reset worker password");
    }
}

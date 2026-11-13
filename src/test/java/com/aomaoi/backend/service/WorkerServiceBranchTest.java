package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private FarmRepository farmRepository;
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
        worker.setFarmId("10");
        worker.setUser(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private WorkerRequestDTO dto(String username) {
        WorkerRequestDTO d = new WorkerRequestDTO();
        d.setFullName("Full Name");
        d.setUsername(username);
        d.setTempPassword("temp");
        return d;
    }

    private void stubAddWorker(String username) {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode("temp")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- getAllWorkers ----------

    @Test
    void getAllWorkers_noAuthentication_returnsAll() {
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        assertEquals(1, service.getAllWorkers().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllWorkers_unauthenticatedToken_returnsAll() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "pw"));
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        assertEquals(1, service.getAllWorkers().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllWorkers_anonymousUser_returnsAll() {
        loginAs("anonymousUser");
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        assertEquals(1, service.getAllWorkers().size());
        verifyNoInteractions(adminProfileRepository);
    }

    @Test
    void getAllWorkers_adminWithoutProfile_returnsAll() {
        loginAs("ghost");
        when(adminProfileRepository.findByUserUsername("ghost")).thenReturn(Optional.empty());
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        assertEquals(1, service.getAllWorkers().size());
    }

    // ---------- addWorker ----------

    @Test
    void addWorker_nullUsername_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addWorker(dto(null)));
        assertTrue(ex.getMessage().startsWith("Username cannot contain spaces"));
    }

    @Test
    void addWorker_usernameWithSpace_throws() {
        assertThrows(RuntimeException.class, () -> service.addWorker(dto("bad name")));
        verifyNoInteractions(userRepository);
    }

    @Test
    void addWorker_unauthenticatedToken_auditsAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "pw"));
        stubAddWorker("newworker");

        service.addWorker(dto("newworker"));

        verifyNoInteractions(adminProfileRepository);
        verify(auditLogService).logAction(eq("CREATE_WORKER"), eq("system"), eq("newworker"), any());
    }

    @Test
    void addWorker_callerWithoutAdminProfile_leavesFarmUnset() {
        loginAs("superadmin");
        stubAddWorker("newworker");
        when(adminProfileRepository.findByUserUsername("superadmin")).thenReturn(Optional.empty());

        Worker saved = service.addWorker(dto("newworker"));

        assertNull(saved.getFarmId());
        verifyNoInteractions(farmRepository);
        verify(auditLogService).logAction(eq("CREATE_WORKER"), eq("superadmin"), eq("newworker"), any());
    }

    @Test
    void addWorker_adminWithoutFarm_skipsFarmCounter() {
        loginAs("adminuser");
        stubAddWorker("newworker");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(null);
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        Worker saved = service.addWorker(dto("newworker"));

        assertNull(saved.getFarmId());
        verifyNoInteractions(farmRepository);
    }

    @Test
    void addWorker_adminFarmNotFound_doesNotSaveFarm() {
        loginAs("adminuser");
        stubAddWorker("newworker");
        AdminProfile profile = new AdminProfile();
        profile.setFarmId("10");
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        Worker saved = service.addWorker(dto("newworker"));

        assertEquals("10", saved.getFarmId());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    // ---------- updateWorker ----------

    @Test
    void updateWorker_nullUsername_keepsExistingUsername() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        service.updateWorker(1L, dto(null));

        assertEquals("worker1", user.getUsername());
        verify(userRepository, never()).save(any());
        verify(auditLogService).logAction(eq("UPDATE_WORKER"), eq("system"), eq("worker1"), any());
    }

    @Test
    void updateWorker_sameUsername_skipsUniquenessCheck() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        service.updateWorker(1L, dto("worker1"));

        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateWorker_invalidNewUsername_throws() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(RuntimeException.class, () -> service.updateWorker(1L, dto("bad name")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWorker_withAuthenticatedCaller_auditsCallerName() {
        loginAs("adminuser");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        service.updateWorker(1L, dto(null));

        verify(auditLogService).logAction(eq("UPDATE_WORKER"), eq("adminuser"), eq("worker1"), any());
    }

    // ---------- deleteWorker ----------

    @Test
    void deleteWorker_withAuthenticatedCaller_auditsCallerName() {
        loginAs("adminuser");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        service.deleteWorker(1L);

        assertEquals("inactive", worker.getStatus());
        verify(auditLogService).logAction(eq("DELETE_WORKER"), eq("adminuser"), eq("worker1"), any());
    }

    @Test
    void deleteWorker_withoutAuthentication_auditsAsSystem() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        service.deleteWorker(1L);

        verify(auditLogService).logAction(eq("DELETE_WORKER"), eq("system"), eq("worker1"), any());
    }

    // ---------- resetWorkerPassword ----------

    @Test
    void resetWorkerPassword_adminOwnFarm_succeeds() {
        AdminProfile profile = new AdminProfile();
        profile.setFarmId("10");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode("newpw")).thenReturn("enc");

        service.resetWorkerPassword(1L, "newpw", "adminuser");

        assertEquals("enc", user.getPassword());
        assertTrue(user.getIsFirstLogin());
        assertEquals("pending", worker.getStatus());
    }

    @Test
    void resetWorkerPassword_adminOtherFarm_accessDenied() {
        AdminProfile profile = new AdminProfile();
        profile.setFarmId("99");
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.resetWorkerPassword(1L, "newpw", "adminuser"));
        verify(userRepository, never()).save(any());
    }
}

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

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
    private WorkerService workerService;

    private Worker worker;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("workeruser");
        user.setRole("worker");

        worker = new Worker();
        worker.setId(1L);
        worker.setFullName("Somchai Jaidee");
        worker.setNickname("Chai");
        worker.setPhone("0812345678");
        worker.setFarmId(10L);
        worker.setStatus("active");
        worker.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "superadmin", "pass", List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllWorkers_Superadmin_ReturnsAll() {
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        List<Worker> result = workerService.getAllWorkers();

        assertEquals(1, result.size());
    }

    @Test
    void getAllWorkers_AdminUser_ReturnsFilteredByFarm() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(10L);

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(workerRepository.findByFarmId(10L)).thenReturn(List.of(worker));

        List<Worker> result = workerService.getAllWorkers();

        assertEquals(1, result.size());
        verify(workerRepository, never()).findAll();
        assertEquals(10L, result.get(0).getFarmId());
    }

    @Test
    void getAllWorkers_WorkerRole_ReturnsOwnProfile() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.of(worker));

        List<Worker> result = workerService.getAllWorkers();

        assertEquals(1, result.size());
        assertEquals("Somchai Jaidee", result.get(0).getFullName());
    }

    @Test
    void getAllWorkers_UnknownRole_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("guest", "pass", List.of(new SimpleGrantedAuthority("ROLE_GUEST")))
        );

        assertThrows(AccessDeniedException.class, () -> workerService.getAllWorkers());
    }

    @Test
    void getAllWorkers_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> workerService.getAllWorkers());
    }

    @Test
    void getWorkerById_Success() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        Worker result = workerService.getWorkerById(1L);

        assertNotNull(result);
        assertEquals("Somchai Jaidee", result.getFullName());
    }

    @Test
    void getWorkerById_WorkerOwnProfile_Success() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        Worker result = workerService.getWorkerById(1L);

        assertNotNull(result);
        assertEquals("Somchai Jaidee", result.getFullName());
    }

    @Test
    void getWorkerById_WorkerOtherProfile_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherworker", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertThrows(AccessDeniedException.class, () -> workerService.getWorkerById(1L));
    }

    @Test
    void getWorkerById_AdminOtherFarm_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(99L);

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));

        assertThrows(AccessDeniedException.class, () -> workerService.getWorkerById(1L));
    }

    @Test
    void getWorkerById_NotFound_ThrowsException() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workerService.getWorkerById(99L));
    }

    @Test
    void addWorker_Success() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("new_worker");
        dto.setTempPassword("pass123");
        dto.setFullName("New Worker");
        dto.setNickname("New");

        when(userRepository.existsByUsername("new_worker")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        Worker created = workerService.addWorker(dto);

        assertNotNull(created);
        assertEquals("New Worker", created.getFullName());
        assertEquals("pending", created.getStatus());
    }

    @Test
    void addWorker_WorkerRole_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("workeruser", "pass", List.of(new SimpleGrantedAuthority("ROLE_WORKER")))
        );
        WorkerRequestDTO dto = new WorkerRequestDTO();

        assertThrows(AccessDeniedException.class, () -> workerService.addWorker(dto));
    }

    @Test
    void addWorker_DuplicateUsername_ThrowsException() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("duplicate");
        dto.setTempPassword("pass123");
        dto.setFullName("Duplicate Worker");

        when(userRepository.existsByUsername("duplicate")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> workerService.addWorker(dto));
    }

    @Test
    void addWorker_WithAdminAuth_AssignsFarm() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(5L);

        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("worker_with_farm");
        dto.setTempPassword("pass123");
        dto.setFullName("Farm Worker");

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(userRepository.existsByUsername("worker_with_farm")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        Worker created = workerService.addWorker(dto);

        assertEquals(5L, created.getFarmId());
    }

    @Test
    void updateWorker_Success() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("updated_worker");
        dto.setFullName("Updated Name");

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(userRepository.existsByUsername("updated_worker")).thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        Worker updated = workerService.updateWorker(1L, dto);

        assertEquals("Updated Name", updated.getFullName());
        assertEquals("updated_worker", updated.getUser().getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void updateWorker_DuplicateUsername_ThrowsException() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("existinguser");

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> workerService.updateWorker(1L, dto));
    }

    @Test
    void updateWorker_AdminCannotEditAnotherFarm() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(99L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class,
                () -> workerService.updateWorker(1L, new WorkerRequestDTO()));
        verify(workerRepository, never()).save(any());
    }

    @Test
    void getAllWorkers_InactiveAdminIsDenied() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(10L);
        profile.setStatus("inactive");
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> workerService.getAllWorkers());
        verify(workerRepository, never()).findByFarmId(anyLong());
    }

    @Test
    void deleteWorker_Success() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        workerService.deleteWorker(1L);

        assertEquals("inactive", worker.getStatus());
        verify(workerRepository).save(worker);
    }

    @Test
    void deleteWorker_AdminOtherFarm_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile profile = new AdminProfile();
        profile.setFarmId(99L);
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> workerService.deleteWorker(1L));
        verify(workerRepository, never()).save(any());
    }

    @Test
    void resetWorkerPassword_AdminSameFarm_Success() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(10L);

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded_newpass");

        workerService.resetWorkerPassword(1L, "newpass123", "adminuser");

        assertEquals("encoded_newpass", user.getPassword());
        assertTrue(user.getIsFirstLogin());
        assertEquals("pending", worker.getStatus());

        verify(userRepository).save(user);
        verify(workerRepository).save(worker);
        verify(auditLogService).logAction("RESET_PASSWORD", "adminuser", "workeruser", "Reset worker password");
    }

    @Test
    void resetWorkerPassword_DifferentFarm_ThrowsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId(99L); // Different from worker's farmId "10"

        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));

        assertThrows(AccessDeniedException.class,
                () -> workerService.resetWorkerPassword(1L, "newpass123", "adminuser"));
    }

    @Test
    void resetWorkerPassword_PasswordTooShort_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> workerService.resetWorkerPassword(1L, "12345", "adminuser"));
    }

    @Test
    void resetWorkerPassword_WorkerNotFound_ThrowsException() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> workerService.resetWorkerPassword(99L, "newpass123", "adminuser"));
    }
}

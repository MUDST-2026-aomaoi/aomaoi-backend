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
        worker.setFarmId("10");
        worker.setStatus("active");
        worker.setUser(user);
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
        adminProfile.setFarmId("10");

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(workerRepository.findAll()).thenReturn(List.of(worker));

        List<Worker> result = workerService.getAllWorkers();

        assertEquals(1, result.size());
        assertEquals("10", result.get(0).getFarmId());
    }

    @Test
    void getActiveWorkers() {
        when(workerRepository.findByStatus("active")).thenReturn(List.of(worker));

        List<Worker> result = workerService.getActiveWorkers();

        assertEquals(1, result.size());
    }

    @Test
    void getWorkerById_Success() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        Worker result = workerService.getWorkerById(1L);

        assertNotNull(result);
        assertEquals("Somchai Jaidee", result.getFullName());
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
    void addWorker_InvalidUsernameWithSpace_ThrowsException() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("invalid user");

        assertThrows(RuntimeException.class, () -> workerService.addWorker(dto));
    }

    @Test
    void addWorker_DuplicateUsername_ThrowsException() {
        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("duplicate");

        when(userRepository.existsByUsername("duplicate")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> workerService.addWorker(dto));
    }

    @Test
    void addWorker_WithAdminAuth_AssignsFarmAndIncrementsWorkerCount() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setFarmId("5");

        Farm farm = new Farm();
        farm.setId(5L);
        farm.setWorkerCount(2);

        WorkerRequestDTO dto = new WorkerRequestDTO();
        dto.setUsername("worker_with_farm");
        dto.setTempPassword("pass");
        dto.setFullName("Farm Worker");

        when(adminProfileRepository.findByUserUsername("adminuser")).thenReturn(Optional.of(adminProfile));
        when(farmRepository.findById(5L)).thenReturn(Optional.of(farm));
        when(userRepository.existsByUsername("worker_with_farm")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        Worker created = workerService.addWorker(dto);

        assertEquals("5", created.getFarmId());
        assertEquals(3, farm.getWorkerCount());
        verify(farmRepository).save(farm);
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
    void deleteWorker_Success() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(i -> i.getArgument(0));

        workerService.deleteWorker(1L);

        assertEquals("inactive", worker.getStatus());
        verify(workerRepository).save(worker);
    }
}

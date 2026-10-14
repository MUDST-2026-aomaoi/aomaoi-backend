package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.entity.WorkLog;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.WorkLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkLogRepository workLogRepository;

    @InjectMocks
    private DatabaseSeeder databaseSeeder;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("1234")).thenReturn("encoded1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void run_DatabaseEmpty_SeedsAccountsProfilesAndAllWorkTypes() throws Exception {
        when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> {
            Farm farm = invocation.getArgument(0);
            farm.setId(1L);
            return farm;
        });
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        databaseSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());
        List<User> users = userCaptor.getAllValues();
        assertEquals(List.of("superadmin", "admin1", "worker1", "worker2"),
                users.stream().map(User::getUsername).toList());
        assertEquals(List.of("superadmin", "admin", "worker", "worker"),
                users.stream().map(User::getRole).toList());
        users.forEach(user -> {
            assertEquals("encoded1234", user.getPassword());
            assertFalse(user.getIsFirstLogin());
        });

        ArgumentCaptor<AdminProfile> adminCaptor = ArgumentCaptor.forClass(AdminProfile.class);
        verify(adminProfileRepository).save(adminCaptor.capture());
        assertSame(users.get(1), adminCaptor.getValue().getUser());
        assertEquals("1", adminCaptor.getValue().getFarmId());

        ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository, times(2)).save(workerCaptor.capture());
        List<Worker> workers = workerCaptor.getAllValues();
        assertSame(users.get(2), workers.get(0).getUser());
        assertSame(users.get(3), workers.get(1).getUser());
        workers.forEach(worker -> assertEquals("1", worker.getFarmId()));

        ArgumentCaptor<WorkLog> logCaptor = ArgumentCaptor.forClass(WorkLog.class);
        verify(workLogRepository, times(4)).save(logCaptor.capture());
        List<WorkLog> logs = logCaptor.getAllValues();
        assertEquals(List.of("cutting", "planting", "watering", "spraying"),
                logs.stream().map(WorkLog::getType).toList());
        Map<String, BigDecimal> wages = Map.of(
                "cutting", new BigDecimal("2000"), "planting", new BigDecimal("750"),
                "watering", new BigDecimal("1050"), "spraying", new BigDecimal("750"));
        logs.forEach(log -> {
            assertNotNull(log.getStartDate());
            assertNotNull(log.getEndDate());
            assertEquals(log.getStartDate(), log.getWorkDate());
            assertFalse(log.getEndDate().isBefore(log.getStartDate()));
            assertEquals(0, wages.get(log.getType()).compareTo(log.getTotal()));
        });
        assertSame(workers.get(0), logs.get(0).getWorker());
        assertSame(workers.get(0), logs.get(1).getWorker());
        assertSame(workers.get(1), logs.get(2).getWorker());
        assertSame(workers.get(1), logs.get(3).getWorker());
        assertEquals(3, logs.get(2).getDays());
        assertEquals(logs.get(2).getStartDate().plusDays(2), logs.get(2).getEndDate());
    }

    @Test
    void run_DemoDataExists_ReusesAccountsWithoutDuplicatingProfilesOrLogs() throws Exception {
        List<String> usernames = List.of("superadmin", "admin1", "worker1", "worker2");
        List<User> users = usernames.stream().map(username -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword("previous-password");
            user.setIsFirstLogin(true);
            return user;
        }).toList();
        users.forEach(user -> when(userRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.of(user)));
        Farm farm = new Farm();
        farm.setId(1L);
        when(farmRepository.count()).thenReturn(1L);
        when(farmRepository.findAll()).thenReturn(List.of(farm));
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(new AdminProfile()));
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.of(new Worker()));
        when(workerRepository.findByUserUsername("worker2")).thenReturn(Optional.of(new Worker()));
        when(workLogRepository.count()).thenReturn(4L);

        databaseSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());
        for (int i = 0; i < users.size(); i++) {
            assertSame(users.get(i), userCaptor.getAllValues().get(i));
            assertEquals("encoded1234", users.get(i).getPassword());
            assertFalse(users.get(i).getIsFirstLogin());
        }
        verify(farmRepository, never()).save(any());
        verify(adminProfileRepository, never()).save(any());
        verify(workerRepository, never()).save(any());
        verify(workLogRepository, never()).save(any());
    }
}

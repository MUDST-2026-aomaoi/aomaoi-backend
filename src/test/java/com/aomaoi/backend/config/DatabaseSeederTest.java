package com.aomaoi.backend.config;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private DatabaseSeeder databaseSeeder;

    @Test
    void run_DatabaseEmpty_SeedsSuperAdmin() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("1234")).thenReturn("encoded1234");

        databaseSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("superadmin", savedUser.getUsername());
        assertEquals("encoded1234", savedUser.getPassword());
        assertEquals("superadmin", savedUser.getRole());
        assertFalse(savedUser.getIsFirstLogin());
    }

    @Test
    void run_DatabaseNotEmpty_DoesNotSeed() throws Exception {
        when(userRepository.count()).thenReturn(5L);

        databaseSeeder.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}

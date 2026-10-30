package com.aomaoi.backend.service;

import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import com.aomaoi.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setRole("admin");
        user.setIsFirstLogin(false);
    }

    @Test
    void loadUserByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        AdminProfile profile = new AdminProfile();
        profile.setStatus("active");
        when(adminProfileRepository.findByUserUsername("testuser")).thenReturn(Optional.of(profile));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encoded_password", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_InactiveAdmin_IsDisabled() {
        AdminProfile profile = new AdminProfile();
        profile.setStatus("inactive");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(adminProfileRepository.findByUserUsername("testuser")).thenReturn(Optional.of(profile));

        assertFalse(customUserDetailsService.loadUserByUsername("testuser").isEnabled());
    }

    @Test
    void loadUserByUsername_AdminProfileMissing_IsDisabled() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(adminProfileRepository.findByUserUsername("testuser")).thenReturn(Optional.empty());

        assertFalse(customUserDetailsService.loadUserByUsername("testuser").isEnabled());
    }

    @Test
    void loadUserByUsername_WorkerActive_IsEnabled() {
        User workerUser = new User();
        workerUser.setId(2L);
        workerUser.setUsername("workeruser");
        workerUser.setPassword("encoded_password");
        workerUser.setRole("worker");

        Worker profile = new Worker();
        profile.setStatus("active");

        when(userRepository.findByUsername("workeruser")).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.of(profile));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("workeruser");

        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
    }

    @Test
    void loadUserByUsername_WorkerInactive_IsDisabled() {
        User workerUser = new User();
        workerUser.setId(2L);
        workerUser.setUsername("workeruser");
        workerUser.setPassword("encoded_password");
        workerUser.setRole("worker");

        Worker profile = new Worker();
        profile.setStatus("inactive");

        when(userRepository.findByUsername("workeruser")).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.of(profile));

        assertFalse(customUserDetailsService.loadUserByUsername("workeruser").isEnabled());
    }

    @Test
    void loadUserByUsername_WorkerProfileMissing_IsDisabled() {
        User workerUser = new User();
        workerUser.setId(2L);
        workerUser.setUsername("workeruser");
        workerUser.setPassword("encoded_password");
        workerUser.setRole("worker");

        when(userRepository.findByUsername("workeruser")).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserUsername("workeruser")).thenReturn(Optional.empty());

        assertFalse(customUserDetailsService.loadUserByUsername("workeruser").isEnabled());
    }

    @Test
    void loadUserByUsername_Superadmin_AlwaysEnabled() {
        User superadmin = new User();
        superadmin.setId(3L);
        superadmin.setUsername("superadmin");
        superadmin.setPassword("encoded_password");
        superadmin.setRole("superadmin");

        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superadmin));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("superadmin");

        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")));
    }

    @Test
    void loadUserByUsername_UnknownRole_IsDisabled() {
        User unknownUser = new User();
        unknownUser.setId(4L);
        unknownUser.setUsername("guest");
        unknownUser.setPassword("encoded_password");
        unknownUser.setRole("guest");

        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(unknownUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("guest");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("unknown");
        });
    }
}

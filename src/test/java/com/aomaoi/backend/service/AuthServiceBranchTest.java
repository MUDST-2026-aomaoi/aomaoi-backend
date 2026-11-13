package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link AuthServiceTest}. */
@ExtendWith(MockitoExtension.class)
class AuthServiceBranchTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private AuthService authService;

    private User user(long id, String username, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("encoded");
        u.setRole(role);
        u.setIsFirstLogin(false);
        return u;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loginAs(User u) {
        LoginRequest request = new LoginRequest();
        request.setUsername(u.getUsername());
        request.setPassword("pw");
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password("encoded").authorities("ROLE_X").build();
        when(userDetailsService.loadUserByUsername(u.getUsername())).thenReturn(details);
        when(userRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
        when(jwtUtil.generateToken(any(), any())).thenReturn("tok");
        return (Map<String, Object>) authService.login(request).get("user");
    }

    @Test
    void login_adminWithoutProfile_fallsBackToUserData() {
        User u = user(7L, "admin-noprofile", "admin");
        when(adminProfileRepository.findByUserUsername("admin-noprofile")).thenReturn(Optional.empty());

        Map<String, Object> data = loginAs(u);

        assertEquals("admin-noprofile", data.get("fullName"));
        assertEquals(7L, data.get("id"));
        assertFalse(data.containsKey("avatar"));
    }

    @Test
    void login_workerWithoutProfile_fallsBackToUserData() {
        User u = user(8L, "worker-noprofile", "worker");
        when(workerRepository.findByUserUsername("worker-noprofile")).thenReturn(Optional.empty());

        Map<String, Object> data = loginAs(u);

        assertEquals("worker-noprofile", data.get("fullName"));
        assertEquals(8L, data.get("id"));
        assertFalse(data.containsKey("avatar"));
    }

    @Test
    void login_unknownRole_usesUsernameAsFullName() {
        User u = user(9L, "mystery", "guest");

        Map<String, Object> data = loginAs(u);

        assertEquals("mystery", data.get("fullName"));
        assertEquals(9L, data.get("id"));
        assertEquals("guest", data.get("role"));
        verifyNoInteractions(adminProfileRepository, workerRepository);
    }

    @Test
    void changePassword_superadmin_updatesPasswordOnly() {
        User u = user(3L, "superadmin", "superadmin");
        u.setIsFirstLogin(true);
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-encoded");

        authService.changePassword("superadmin", "old", "new");

        assertEquals("new-encoded", u.getPassword());
        assertFalse(u.getIsFirstLogin());
        verify(userRepository).save(u);
        verifyNoInteractions(adminProfileRepository, workerRepository);
    }

    @Test
    void changePassword_adminWithoutProfile_doesNotSaveProfile() {
        User u = user(4L, "admin-noprofile", "admin");
        when(userRepository.findByUsername("admin-noprofile")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-encoded");
        when(adminProfileRepository.findByUserUsername("admin-noprofile")).thenReturn(Optional.empty());

        authService.changePassword("admin-noprofile", "old", "new");

        verify(adminProfileRepository, never()).save(any());
    }

    @Test
    void changePassword_workerWithoutProfile_doesNotSaveProfile() {
        User u = user(5L, "worker-noprofile", "worker");
        when(userRepository.findByUsername("worker-noprofile")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-encoded");
        when(workerRepository.findByUserUsername("worker-noprofile")).thenReturn(Optional.empty());

        authService.changePassword("worker-noprofile", "old", "new");

        verify(workerRepository, never()).save(any());
    }
}

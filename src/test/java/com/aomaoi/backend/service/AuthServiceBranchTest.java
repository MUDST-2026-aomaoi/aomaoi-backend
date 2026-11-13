package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.AuthResponseDTO;
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
    @Mock
    private AuditLogService auditLogService;

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

    private AuthResponseDTO loginAs(User u) {
        LoginRequest request = new LoginRequest();
        request.setUsername(u.getUsername());
        request.setPassword("pw");
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password("encoded").authorities("ROLE_X").build();
        when(userDetailsService.loadUserByUsername(u.getUsername())).thenReturn(details);
        when(userRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
        when(jwtUtil.generateToken(any(), any())).thenReturn("tok");
        return authService.login(request);
    }

    // ---------- login ----------

    @Test
    void login_adminWithoutProfile_fallsBackToUserData() {
        when(adminProfileRepository.findByUserUsername("admin-noprofile")).thenReturn(Optional.empty());

        AuthResponseDTO response = loginAs(user(7L, "admin-noprofile", "admin"));

        assertEquals("admin-noprofile", response.user().fullName());
        assertEquals(7L, response.user().id());
        assertNull(response.user().avatar());
    }

    @Test
    void login_workerWithoutProfile_fallsBackToUserData() {
        when(workerRepository.findByUserUsername("worker-noprofile")).thenReturn(Optional.empty());

        AuthResponseDTO response = loginAs(user(8L, "worker-noprofile", "worker"));

        assertEquals("worker-noprofile", response.user().fullName());
        assertEquals(8L, response.user().id());
        assertNull(response.user().avatar());
    }

    @Test
    void login_unknownRole_usesUsernameAsFullName() {
        AuthResponseDTO response = loginAs(user(9L, "mystery", "guest"));

        assertEquals("mystery", response.user().fullName());
        assertEquals("guest", response.user().role());
        verifyNoInteractions(adminProfileRepository, workerRepository);
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_nullNewPassword_rejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword("u", "old", null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void changePassword_tooShortNewPassword_rejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword("u", "old", "12345"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void changePassword_blankNewPassword_rejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword("u", "old", "       "));
        verifyNoInteractions(userRepository);
    }

    @Test
    void changePassword_unknownUser_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.changePassword("ghost", "old", "newpassword"));
    }

    @Test
    void changePassword_wrongOldPassword_rejected() {
        User u = user(1L, "u", "worker");
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.changePassword("u", "bad", "newpassword"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_superadmin_updatesPasswordOnly() {
        User u = user(3L, "superadmin", "superadmin");
        u.setIsFirstLogin(true);
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");

        authService.changePassword("superadmin", "old", "newpassword");

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
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");
        when(adminProfileRepository.findByUserUsername("admin-noprofile")).thenReturn(Optional.empty());

        authService.changePassword("admin-noprofile", "old", "newpassword");

        verify(adminProfileRepository, never()).save(any());
    }

    @Test
    void changePassword_workerWithoutProfile_doesNotSaveProfile() {
        User u = user(5L, "worker-noprofile", "worker");
        when(userRepository.findByUsername("worker-noprofile")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");
        when(workerRepository.findByUserUsername("worker-noprofile")).thenReturn(Optional.empty());

        authService.changePassword("worker-noprofile", "old", "newpassword");

        verify(workerRepository, never()).save(any());
    }
}

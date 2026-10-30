package com.aomaoi.backend.service;

import com.aomaoi.backend.config.JwtUtil;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private User adminUser;
    private User workerUser;
    private User superadminUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin1");
        adminUser.setPassword("pass");
        adminUser.setRole("admin");
        adminUser.setIsFirstLogin(true);

        workerUser = new User();
        workerUser.setId(2L);
        workerUser.setUsername("worker1");
        workerUser.setPassword("pass");
        workerUser.setRole("worker");
        workerUser.setIsFirstLogin(true);

        superadminUser = new User();
        superadminUser.setId(3L);
        superadminUser.setUsername("superadmin");
        superadminUser.setPassword("pass");
        superadminUser.setRole("superadmin");
        superadminUser.setIsFirstLogin(false);

        userDetails = org.springframework.security.core.userdetails.User.withUsername("admin1")
                .password("pass")
                .authorities("ROLE_ADMIN")
                .build();
    }

    @Test
    void login_Superadmin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("superadmin");
        request.setPassword("pass");

        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(userDetails);
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superadminUser));
        when(jwtUtil.generateToken(any(), eq("superadmin"))).thenReturn("mock_token");

        Map<String, Object> result = authService.login(request);

        assertTrue((Boolean) result.get("success"));
        assertEquals("mock_token", result.get("token"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals("superadmin", userMap.get("fullName"));
        assertEquals("superadmin", userMap.get("role"));
    }

    @Test
    void login_AdminProfileFound_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin1");
        request.setPassword("pass");

        AdminProfile profile = new AdminProfile();
        profile.setId(10L);
        profile.setFullName("Admin Name");
        profile.setAvatar("avatar.png");

        when(userDetailsService.loadUserByUsername("admin1")).thenReturn(userDetails);
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken(any(), eq("admin"))).thenReturn("mock_token");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));

        Map<String, Object> result = authService.login(request);

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals("Admin Name", userMap.get("fullName"));
        assertEquals(10L, userMap.get("id"));
        assertEquals("avatar.png", userMap.get("avatar"));
    }

    @Test
    void login_WorkerProfileFound_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("worker1");
        request.setPassword("pass");

        Worker worker = new Worker();
        worker.setId(20L);
        worker.setFullName("Worker Name");
        worker.setAvatar("worker_avatar.png");

        when(userDetailsService.loadUserByUsername("worker1")).thenReturn(userDetails);
        when(userRepository.findByUsername("worker1")).thenReturn(Optional.of(workerUser));
        when(jwtUtil.generateToken(any(), eq("worker"))).thenReturn("mock_token");
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.of(worker));

        Map<String, Object> result = authService.login(request);

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals("Worker Name", userMap.get("fullName"));
        assertEquals(20L, userMap.get("id"));
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("pass");

        when(userDetailsService.loadUserByUsername("unknown")).thenReturn(userDetails);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> authService.login(request));
    }

    @Test
    void changePassword_Worker_Success() {
        when(userRepository.findByUsername("worker1")).thenReturn(Optional.of(workerUser));
        when(passwordEncoder.matches("oldpass", workerUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_newpass");

        Worker worker = new Worker();
        worker.setStatus("pending");
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.of(worker));

        authService.changePassword("worker1", "oldpass", "newpass");

        assertEquals("encoded_newpass", workerUser.getPassword());
        assertFalse(workerUser.getIsFirstLogin());
        assertEquals("active", worker.getStatus());

        verify(userRepository).save(workerUser);
        verify(workerRepository).save(worker);
        verify(auditLogService).logAction("CHANGE_PASSWORD", "worker1", "worker1", "User changed own password");
    }

    @Test
    void changePassword_Admin_Success() {
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("oldpass", adminUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_newpass");

        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setStatus("pending");
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(adminProfile));

        authService.changePassword("admin1", "oldpass", "newpass");

        assertEquals("encoded_newpass", adminUser.getPassword());
        assertFalse(adminUser.getIsFirstLogin());
        assertEquals("active", adminProfile.getStatus());

        verify(userRepository).save(adminUser);
        verify(adminProfileRepository).save(adminProfile);
        verify(auditLogService).logAction("CHANGE_PASSWORD", "admin1", "admin1", "User changed own password");
    }

    @Test
    void changePassword_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.changePassword("unknown", "oldpass", "newpass"));
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrongpass", adminUser.getPassword())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword("admin1", "wrongpass", "newpass"));
        assertEquals("รหัสผ่านเดิมไม่ถูกต้อง", ex.getMessage());
    }

    @Test
    void changePassword_Superadmin_Success() {
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superadminUser));
        when(passwordEncoder.matches("oldpass", superadminUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_newpass");

        authService.changePassword("superadmin", "oldpass", "newpass");

        assertEquals("encoded_newpass", superadminUser.getPassword());
        assertFalse(superadminUser.getIsFirstLogin());
        verify(userRepository).save(superadminUser);
        verify(workerRepository, never()).save(any());
        verify(adminProfileRepository, never()).save(any());
    }

    @Test
    void login_AdminInactive_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin1");
        request.setPassword("pass");

        AdminProfile profile = new AdminProfile();
        profile.setId(10L);
        profile.setStatus("inactive");

        when(userDetailsService.loadUserByUsername("admin1")).thenReturn(userDetails);
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(adminProfileRepository.findByUserUsername("admin1")).thenReturn(Optional.of(profile));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Admin is inactive", ex.getMessage());
    }

    @Test
    void login_WorkerInactive_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("worker1");
        request.setPassword("pass");

        Worker worker = new Worker();
        worker.setId(20L);
        worker.setStatus("inactive");

        when(userDetailsService.loadUserByUsername("worker1")).thenReturn(userDetails);
        when(userRepository.findByUsername("worker1")).thenReturn(Optional.of(workerUser));
        when(workerRepository.findByUserUsername("worker1")).thenReturn(Optional.of(worker));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Worker is inactive", ex.getMessage());
    }

    @Test
    void refreshToken_Success() {
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(userDetailsService.loadUserByUsername("admin1")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails, "admin")).thenReturn("refreshed_jwt_token");

        String token = authService.refreshToken("admin1");

        assertNotNull(token);
        assertEquals("refreshed_jwt_token", token);
        verify(jwtUtil).generateToken(userDetails, "admin");
    }

    @Test
    void refreshToken_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.refreshToken("unknown"));
        assertEquals("User not found", ex.getMessage());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}

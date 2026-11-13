package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.UserRepository;
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

/** Fills the branch gaps left by {@link AdminProfileServiceTest}. */
@ExtendWith(MockitoExtension.class)
class AdminProfileServiceBranchTest {

    @Mock
    private AdminProfileRepository adminRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminProfileService service;

    private User user;
    private AdminProfile admin;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("admin1");
        admin = new AdminProfile();
        admin.setId(1L);
        admin.setUser(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))));
    }

    private AdminRequestDTO dto(String username, String farmId) {
        AdminRequestDTO d = new AdminRequestDTO();
        d.setFullName("Full Name");
        d.setUsername(username);
        d.setFarmId(farmId);
        d.setTempPassword("temp");
        return d;
    }

    private void stubAddAdmin() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode("temp")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void addAdmin_withoutFarm_skipsFarmLookup_andAuditsAsSystem() {
        stubAddAdmin();

        AdminProfile saved = service.addAdmin(dto("newadmin", null));

        assertEquals("pending", saved.getStatus());
        verifyNoInteractions(farmRepository);
        verify(auditLogService).logAction(eq("CREATE_ADMIN"), eq("system"), eq("newadmin"), any());
    }

    @Test
    void addAdmin_farmNotFound_doesNotSaveFarm() {
        stubAddAdmin();
        when(farmRepository.findById(5L)).thenReturn(Optional.empty());

        service.addAdmin(dto("newadmin", "5"));

        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    void addAdmin_withAuthenticatedCaller_auditsCallerName() {
        loginAs("superadmin");
        stubAddAdmin();

        service.addAdmin(dto("newadmin", null));

        verify(auditLogService).logAction(eq("CREATE_ADMIN"), eq("superadmin"), eq("newadmin"), any());
    }

    @Test
    void updateAdmin_nullUsername_keepsExistingUsername() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto(null, "10"));

        assertEquals("admin1", user.getUsername());
        verify(userRepository, never()).save(any());
        verify(auditLogService).logAction(eq("UPDATE_ADMIN"), eq("system"), eq("admin1"), any());
    }

    @Test
    void updateAdmin_sameUsername_skipsUniquenessCheck() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto("admin1", "10"));

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAdmin_withAuthenticatedCaller_auditsCallerName() {
        loginAs("superadmin");
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto(null, "10"));

        verify(auditLogService).logAction(eq("UPDATE_ADMIN"), eq("superadmin"), eq("admin1"), any());
    }

    @Test
    void deleteAdmin_withAuthenticatedCaller_auditsCallerName() {
        loginAs("superadmin");
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        service.deleteAdmin(1L);

        assertEquals("inactive", admin.getStatus());
        verify(auditLogService).logAction(eq("DELETE_ADMIN"), eq("superadmin"), eq("admin1"), any());
    }

    @Test
    void deleteAdmin_withoutAuthentication_auditsAsSystem() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        service.deleteAdmin(1L);

        verify(auditLogService).logAction(eq("DELETE_ADMIN"), eq("system"), eq("admin1"), any());
    }
}

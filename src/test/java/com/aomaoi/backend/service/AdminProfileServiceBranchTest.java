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
import java.util.NoSuchElementException;
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

    private AdminRequestDTO dto(String username, Long farmId, String email) {
        AdminRequestDTO d = new AdminRequestDTO();
        d.setFullName("Full Name");
        d.setUsername(username);
        d.setFarmId(farmId);
        d.setEmail(email);
        d.setTempPassword("temp");
        return d;
    }

    private void stubAddAdmin() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(farmRepository.findById(5L)).thenReturn(Optional.of(new Farm()));
        when(passwordEncoder.encode("temp")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- addAdmin ----------

    @Test
    void addAdmin_nullDto_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addAdmin(null));
    }

    @Test
    void addAdmin_nullFarmId_rejected() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.addAdmin(dto("newadmin", null, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void addAdmin_nonPositiveFarmId_rejected() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.addAdmin(dto("newadmin", 0L, null)));
    }

    @Test
    void addAdmin_unknownFarm_rejected() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(farmRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.addAdmin(dto("newadmin", 5L, null)));
    }

    @Test
    void addAdmin_normalizesEmail_andAuditsAsSystemWithoutAuthentication() {
        stubAddAdmin();

        AdminProfile saved = service.addAdmin(dto("newadmin", 5L, "  Boss@Example.COM "));

        assertEquals("boss@example.com", saved.getEmail());
        verify(auditLogService).logAction(eq("CREATE_ADMIN"), eq("system"), eq("newadmin"), any());
    }

    @Test
    void addAdmin_blankEmailStoredAsNull_andAuditsCallerName() {
        loginAs("superadmin");
        stubAddAdmin();

        AdminProfile saved = service.addAdmin(dto("newadmin", 5L, "   "));

        assertNull(saved.getEmail());
        verify(auditLogService).logAction(eq("CREATE_ADMIN"), eq("superadmin"), eq("newadmin"), any());
    }

    // ---------- updateAdmin ----------

    @Test
    void updateAdmin_withFarmId_validatesAndAssignsFarm() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(farmRepository.findById(5L)).thenReturn(Optional.of(new Farm()));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto(null, 5L, null));

        assertEquals(5L, admin.getFarmId());
    }

    @Test
    void updateAdmin_invalidFarmId_rejected() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> service.updateAdmin(1L, dto(null, -3L, null)));
        verify(adminRepository, never()).save(any());
    }

    @Test
    void updateAdmin_withoutFarmId_keepsExistingFarm() {
        admin.setFarmId(8L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto(null, null, null));

        assertEquals(8L, admin.getFarmId());
        verifyNoInteractions(farmRepository);
        verify(auditLogService).logAction(eq("UPDATE_ADMIN"), eq("system"), eq("admin1"), any());
    }

    @Test
    void updateAdmin_sameUsername_skipsUniquenessCheck() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto("admin1", null, null));

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAdmin_withAuthenticatedCaller_auditsCallerName() {
        loginAs("superadmin");
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(admin)).thenReturn(admin);

        service.updateAdmin(1L, dto(null, null, null));

        verify(auditLogService).logAction(eq("UPDATE_ADMIN"), eq("superadmin"), eq("admin1"), any());
    }

    // ---------- deleteAdmin ----------

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

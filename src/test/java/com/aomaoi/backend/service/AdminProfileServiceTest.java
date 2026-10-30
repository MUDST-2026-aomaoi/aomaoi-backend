package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.entity.Farm;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.repository.FarmRepository;
import com.aomaoi.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProfileServiceTest {

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
    private AdminProfileService adminProfileService;

    private AdminProfile admin;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("adminuser");
        user.setRole("admin");

        admin = new AdminProfile();
        admin.setId(1L);
        admin.setFullName("Admin FullName");
        admin.setPhone("0812345678");
        admin.setFarmId(10L);
        admin.setAvatar("avatar.png");
        admin.setStatus("active");
        admin.setUser(user);
    }

    @Test
    void getAllAdmins() {
        when(adminRepository.findAll()).thenReturn(List.of(admin));

        List<AdminProfile> result = adminProfileService.getAllAdmins();

        assertEquals(1, result.size());
        assertEquals("Admin FullName", result.get(0).getFullName());
    }

    @Test
    void getAdminById_Success() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        AdminProfile result = adminProfileService.getAdminById(1L);

        assertNotNull(result);
        assertEquals("Admin FullName", result.getFullName());
    }

    @Test
    void getAdminById_NotFound_ThrowsException() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adminProfileService.getAdminById(99L));
    }

    @Test
    void addAdmin_Success() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("newadmin");
        dto.setTempPassword("temp123");
        dto.setFullName("New Admin");
        dto.setPhone("0899999999");
        dto.setFarmId(10L);
        dto.setAvatar("pic.jpg");

        Farm farm = new Farm();
        farm.setId(10L);

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode("temp123")).thenReturn("encoded_temp");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));

        AdminProfile created = adminProfileService.addAdmin(dto);

        assertNotNull(created);
        assertEquals("New Admin", created.getFullName());
        assertEquals("pending", created.getStatus());
    }

    @Test
    void addAdmin_NullDto_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> adminProfileService.addAdmin(null));
    }

    @Test
    void addAdmin_UsernameExists_ThrowsException() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("existingadmin");
        dto.setTempPassword("temp123");
        dto.setFullName("Existing Admin");

        when(userRepository.existsByUsername("existingadmin")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> adminProfileService.addAdmin(dto));
    }

    @Test
    void updateAdmin_Success() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("updateduser");
        dto.setFullName("Updated Admin");
        dto.setPhone("0800000000");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));

        AdminProfile updated = adminProfileService.updateAdmin(1L, dto);

        assertEquals("Updated Admin", updated.getFullName());
        assertEquals("updateduser", updated.getUser().getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void updateAdmin_SameUsername_DoesNotCheckDuplicate() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("adminuser");
        dto.setFullName("Updated Admin");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));

        AdminProfile updated = adminProfileService.updateAdmin(1L, dto);

        assertEquals("Updated Admin", updated.getFullName());
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateAdmin_NotFound_ThrowsNoSuchElementException() {
        AdminRequestDTO dto = new AdminRequestDTO();
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> adminProfileService.updateAdmin(99L, dto));
    }

    @Test
    void updateAdmin_DuplicateUsername_ThrowsException() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("existinguser");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminProfileService.updateAdmin(1L, dto));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void addAdmin_FarmNotFound_RejectsBeforeSavingUser() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("admin2");
        dto.setFullName("Admin Two");
        dto.setTempPassword("temp123");
        dto.setFarmId(99L);
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> adminProfileService.addAdmin(dto));

        assertEquals("Farm not found", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(adminRepository, never()).save(any());
        verify(farmRepository, never()).save(any());
    }

    @Test
    void addAdmin_MissingFarm_RejectsBeforeSavingUser() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("admin2");
        dto.setFullName("Admin Two");
        dto.setTempPassword("temp123");

        assertThrows(IllegalArgumentException.class, () -> adminProfileService.addAdmin(dto));
        verify(userRepository, never()).save(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void updateAdmin_UnknownFarm_RejectsChange() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setFarmId(99L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> adminProfileService.updateAdmin(1L, dto));
        assertEquals(10L, admin.getFarmId());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void deleteAdmin_Success() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));

        adminProfileService.deleteAdmin(1L);

        assertEquals("inactive", admin.getStatus());
        verify(adminRepository).save(admin);
    }

    @Test
    void deleteAdmin_NotFound_ThrowsNoSuchElementException() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> adminProfileService.deleteAdmin(99L));
    }
}

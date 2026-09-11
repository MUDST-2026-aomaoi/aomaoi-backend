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
        admin.setFarmId("10");
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
        dto.setFarmId("10");
        dto.setAvatar("pic.jpg");

        Farm farm = new Farm();
        farm.setId(10L);
        farm.setAdminCount(1);

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode("temp123")).thenReturn("encoded_temp");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(adminRepository.save(any(AdminProfile.class))).thenAnswer(i -> i.getArgument(0));

        AdminProfile created = adminProfileService.addAdmin(dto);

        assertNotNull(created);
        assertEquals("New Admin", created.getFullName());
        assertEquals("pending", created.getStatus());
        assertEquals(2, farm.getAdminCount());
        verify(farmRepository).save(farm);
    }

    @Test
    void addAdmin_UsernameExists_ThrowsException() {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setUsername("existingadmin");

        when(userRepository.existsByUsername("existingadmin")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> adminProfileService.addAdmin(dto));
    }

   
}

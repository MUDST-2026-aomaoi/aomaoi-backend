package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.AdminRequestDTO;
import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.service.AdminProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProfileControllerTest {

    @Mock
    private AdminProfileService adminService;

    @InjectMocks
    private AdminProfileController adminController;

    private AdminProfile admin;

    @BeforeEach
    void setUp() {
        admin = new AdminProfile();
        admin.setId(1L);
        admin.setFullName("Admin One");
    }

    @Test
    void getAllAdmins() {
        when(adminService.getAllAdmins()).thenReturn(List.of(admin));

        ResponseEntity<List<AdminProfile>> response = adminController.getAllAdmins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
    @Test
    void getAdminById() {
        when(adminService.getAdminById(1L)).thenReturn(admin);

        ResponseEntity<AdminProfile> response = adminController.getAdminById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Admin One", response.getBody().getFullName());
    }

    @Test
    void addAdmin() {
        AdminRequestDTO dto = new AdminRequestDTO();
        when(adminService.addAdmin(dto)).thenReturn(admin);

        ResponseEntity<AdminProfile> response = adminController.addAdmin(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void updateAdmin() {
        AdminRequestDTO dto = new AdminRequestDTO();
        when(adminService.updateAdmin(1L, dto)).thenReturn(admin);

        ResponseEntity<AdminProfile> response = adminController.updateAdmin(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteAdmin() {
        doNothing().when(adminService).deleteAdmin(1L);

        ResponseEntity<Void> response = adminController.deleteAdmin(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).deleteAdmin(1L);
    }

    
}

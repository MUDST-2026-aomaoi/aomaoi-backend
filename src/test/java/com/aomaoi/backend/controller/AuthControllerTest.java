package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.ChangePasswordRequest;
import com.aomaoi.backend.dto.LoginRequest;
import com.aomaoi.backend.dto.AuthResponseDTO;
import com.aomaoi.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
    }

    @Test
    void login_Success() {
        AuthResponseDTO serviceResponse = new AuthResponseDTO(true, "dummy_token", null, null);

        when(authService.login(loginRequest)).thenReturn(serviceResponse);

        ResponseEntity<AuthResponseDTO> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
        assertEquals("dummy_token", response.getBody().token());
    }

    @Test
    void login_Failure_Returns401() {
        when(authService.login(loginRequest)).thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        ResponseEntity<AuthResponseDTO> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
        assertEquals("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง", response.getBody().message());
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldpassword");
        request.setNewPassword("newpassword");

        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");

        doNothing().when(authService).changePassword("testuser", "oldpassword", "newpassword");
        when(authService.refreshToken("testuser")).thenReturn("fresh_token");

        ResponseEntity<AuthResponseDTO> response = authController.changePassword(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
        assertEquals("Password changed successfully", response.getBody().message());
        assertEquals("fresh_token", response.getBody().token());
        verify(authService).changePassword("testuser", "oldpassword", "newpassword");
    }
}

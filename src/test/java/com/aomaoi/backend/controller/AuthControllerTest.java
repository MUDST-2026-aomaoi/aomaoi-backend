package com.aomaoi.backend.controller;

import com.aomaoi.backend.dto.ChangePasswordRequest;
import com.aomaoi.backend.dto.LoginRequest;
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

import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("success", true);
        serviceResponse.put("token", "dummy_token");

        when(authService.login(loginRequest)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, Object>> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("dummy_token", response.getBody().get("token"));
    }

    @Test
    void login_Failure_Returns401() {
        when(authService.login(loginRequest)).thenThrow(new RuntimeException("Bad credentials"));

        ResponseEntity<Map<String, Object>> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง", response.getBody().get("message"));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newpassword");

        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");

        doNothing().when(authService).changePassword("testuser", "newpassword");

        ResponseEntity<Map<String, Object>> response = authController.changePassword(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Password changed successfully", response.getBody().get("message"));
        verify(authService).changePassword("testuser", "newpassword");
    }
}

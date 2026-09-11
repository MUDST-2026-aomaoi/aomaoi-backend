package com.aomaoi.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;


class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "aomaoi_super_secret_key_for_farm_accounting_system_make_it_long");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L); // 1 hour

        userDetails = new User("testuser", "password", Collections.emptyList());
    }

    @Test
    void testGenerateAndExtractToken() {
        String token = jwtUtil.generateToken(userDetails, "ADMIN");
        assertNotNull(token);

        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username);

        String role = jwtUtil.extractRole(token);
        assertEquals("ADMIN", role);
    }

    @Test
    void testValidateToken_Success() {
        String token = jwtUtil.generateToken(userDetails, "WORKER");
        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WrongUser() {
        String token = jwtUtil.generateToken(userDetails, "WORKER");
        UserDetails otherUser = new User("otheruser", "password", Collections.emptyList());
        boolean isValid = jwtUtil.validateToken(token, otherUser);
        assertFalse(isValid);
    }

    
}

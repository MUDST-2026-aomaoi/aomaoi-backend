package com.aomaoi.backend.config;

import com.aomaoi.backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_NoAuthorizationHeader_ContinuesChainWithoutAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_HeaderNotBearer_ContinuesChainWithoutAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic 123456");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidToken_HandlesExceptionAndContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(jwtUtil.extractUsername("invalid_token")).thenThrow(new RuntimeException("Malformed token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractUsername("valid_token")).thenReturn("adminuser");

        UserDetails userDetails = new User("adminuser", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("adminuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid_token", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("adminuser", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_TokenValidationFails_DoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired_token");
        when(jwtUtil.extractUsername("expired_token")).thenReturn("adminuser");

        UserDetails userDetails = new User("adminuser", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("adminuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("expired_token", userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UserNotFound_HandlesExceptionGracefully() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractUsername("valid_token")).thenReturn("deleteduser");

        when(userDetailsService.loadUserByUsername("deleteduser")).thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AlreadyAuthenticated_SkipsSettingAuth() throws ServletException, IOException {
        UserDetails existingUser = new User("existing", "pass", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(existingUser, null, existingUser.getAuthorities())
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractUsername("valid_token")).thenReturn("adminuser");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Security context should keep existing authentication without calling userDetailsService
        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }
}

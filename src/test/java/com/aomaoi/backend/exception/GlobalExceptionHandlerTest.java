package com.aomaoi.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void handleRuntimeException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException ex = new RuntimeException("Error occurred");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().get("message"));
    }

    @Test
    void handleAccessDenied() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Access is denied");

        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access is denied", response.getBody().get("message"));
    }

    @Test
    void handleStatus_WithReason() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.web.server.ResponseStatusException ex =
                new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        ResponseEntity<Map<String, String>> response = handler.handleStatus(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().get("message"));
    }

    @Test
    void handleStatus_WithoutReason() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.web.server.ResponseStatusException ex =
                new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, String>> response = handler.handleStatus(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request rejected", response.getBody().get("message"));
    }

    @Test
    void handleInvalid() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input argument");

        ResponseEntity<Map<String, String>> response = handler.handleInvalid(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid input argument", response.getBody().get("message"));
    }

    @Test
    void handleMissing() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NoSuchElementException ex = new NoSuchElementException("Resource not found");

        ResponseEntity<Map<String, String>> response = handler.handleMissing(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().get("message"));
    }

    @Test
    void handleValidation_WithFieldError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult br =
                mock(org.springframework.validation.BindingResult.class);
        org.springframework.validation.FieldError fe =
                new org.springframework.validation.FieldError("loginRequest", "username", "must not be blank");

        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(fe));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid username", response.getBody().get("message"));
    }

    @Test
    void handleValidation_EmptyFieldErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult br =
                mock(org.springframework.validation.BindingResult.class);

        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request", response.getBody().get("message"));
    }
}

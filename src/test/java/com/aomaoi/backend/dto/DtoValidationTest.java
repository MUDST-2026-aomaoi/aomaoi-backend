package com.aomaoi.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void loginRequest_Valid_NoViolations() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("pass123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void loginRequest_BlankFields_HasViolations() {
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("   ");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertEquals(2, violations.size());
    }

    @Test
    void changePasswordRequest_Valid_NoViolations() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldpass");
        req.setNewPassword("newpass123");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void changePasswordRequest_ShortNewPassword_HasViolation() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldpass");
        req.setNewPassword("123");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(req);
        assertEquals(1, violations.size());
        assertEquals("newPassword", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void workLogRequestDTO_Valid_NoViolations() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("cutting");
        dto.setWorkerId("1");

        Set<ConstraintViolation<WorkLogRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void workLogRequestDTO_BlankTypeAndWorkerId_HasViolations() {
        WorkLogRequestDTO dto = new WorkLogRequestDTO();
        dto.setType("");
        dto.setWorkerId(" ");

        Set<ConstraintViolation<WorkLogRequestDTO>> violations = validator.validate(dto);
        assertEquals(2, violations.size());
    }
}

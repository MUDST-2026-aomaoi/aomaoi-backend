package com.aomaoi.backend.dto;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.entity.AdvanceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void salaryAdvanceRequestDTO_ValidCashAndGoods_HaveNoViolations() {
        var cash = new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27), AdvanceType.CASH,
                new BigDecimal("100.00"), null);
        var goods = new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27), AdvanceType.GOODS,
                new BigDecimal("100.00"), "ปุ๋ย");

        assertTrue(validator.validate(cash).isEmpty());
        assertTrue(validator.validate(goods).isEmpty());
    }

    @Test
    void salaryAdvanceRequestDTO_RequiredFieldsMissing_HaveViolations() {
        var request = new SalaryAdvanceRequestDTO(null, null, null, null, null);

        Set<ConstraintViolation<SalaryAdvanceRequestDTO>> violations = validator.validate(request);

        Set<String> properties = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("workerId", "advanceDate", "type", "amount"), properties);
    }

    @Test
    void salaryAdvanceRequestDTO_AmountOutsideNumericPrecision_HasViolation() {
        var request = new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27), AdvanceType.CASH,
                new BigDecimal("100000000000000000.00"), null);

        Set<ConstraintViolation<SalaryAdvanceRequestDTO>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void salaryAdvanceRequestDTO_AmountBelowMinimumOrTooPrecise_HasViolation() {
        var zero = new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27), AdvanceType.CASH,
                BigDecimal.ZERO, null);
        var overScale = new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27), AdvanceType.CASH,
                new BigDecimal("10.001"), null);

        assertTrue(validator.validate(zero).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        assertTrue(validator.validate(overScale).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }
}

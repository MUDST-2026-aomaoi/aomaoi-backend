package com.aomaoi.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void farmRequiresNameAndLocation() {
        assertEquals(2, validator.validate(new FarmRequestDTO()).size());
    }

    @Test
    void workerRequiresFullNameAndRejectsWhitespaceInUsername() {
        WorkerRequestDTO request = new WorkerRequestDTO();
        request.setUsername("name with spaces");
        assertEquals(2, validator.validate(request).size());
    }

    @Test
    void workerCreateRequiresUsernameAndTemporaryPassword() {
        WorkerRequestDTO request = new WorkerRequestDTO();
        request.setFullName("Worker");
        assertEquals(2, validator.validate(request, WorkerRequestDTO.Create.class).size());
        request.setUsername("worker1");
        request.setTempPassword("pass123");
        assertTrue(validator.validate(request, WorkerRequestDTO.Create.class).isEmpty());
    }

    @Test
    void workerUpdateAllowsOmittingUsernameAndTemporaryPassword() {
        WorkerRequestDTO request = new WorkerRequestDTO();
        request.setFullName("Worker");
        assertTrue(validator.validate(request, Default.class).isEmpty());
    }

    @Test
    void adminCreateRequiresUsernameAndTemporaryPasswordButUpdateDoesNot() {
        AdminRequestDTO request = new AdminRequestDTO();
        request.setFullName("Admin");
        assertEquals(2, validator.validate(request, AdminRequestDTO.Create.class).size());
        assertTrue(validator.validate(request, Default.class).isEmpty());
        request.setUsername("admin1");
        request.setTempPassword("pass123");
        assertTrue(validator.validate(request, AdminRequestDTO.Create.class).isEmpty());
    }

    @Test
    void resetPasswordRequiresAtLeastSixCharacters() {
        assertEquals(1, validator.validate(new ResetPasswordRequestDTO("123")).size());
        assertTrue(validator.validate(new ResetPasswordRequestDTO("123456")).isEmpty());
    }
}

package com.aomaoi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank @Size(min = 6, max = 128) String newPassword) {
}

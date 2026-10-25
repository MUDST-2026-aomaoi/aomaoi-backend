package com.aomaoi.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ChangePasswordRequest {
    private String username;
    @NotBlank private String oldPassword;
    @NotBlank @Size(min = 6) private String newPassword;
}

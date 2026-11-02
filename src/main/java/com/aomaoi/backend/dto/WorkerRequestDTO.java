package com.aomaoi.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;

@Data
public class WorkerRequestDTO {
    public interface Create extends Default {}

    @NotBlank @Size(max = 255)
    private String fullName;
    private String nickname;
    @NotBlank(groups = Create.class)
    @Pattern(regexp = "^[\\w\\p{Punct}]+$", message = "Username cannot contain spaces")
    @Size(max = 255)
    private String username;
    private String phone;
    @NotBlank(groups = Create.class)
    @Size(min = 6, message = "Temporary password must be at least 6 characters", groups = Create.class)
    private String tempPassword;
    private String avatar;
}

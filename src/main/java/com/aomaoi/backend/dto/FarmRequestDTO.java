package com.aomaoi.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class FarmRequestDTO {
    @NotBlank @Size(max = 255)
    private String name;
    @NotBlank @Size(max = 255)
    private String location;
    private String image;
}

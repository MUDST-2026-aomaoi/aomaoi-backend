package com.aomaoi.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthUserResponseDTO(Long id, String username, String role, Boolean isFirstLogin,
                                  String fullName, String avatar) {
}

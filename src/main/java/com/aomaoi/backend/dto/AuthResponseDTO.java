package com.aomaoi.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Login/password response. Optional fields retain the existing JSON shape per operation. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponseDTO(Boolean success, String token, AuthUserResponseDTO user, String message) {
}

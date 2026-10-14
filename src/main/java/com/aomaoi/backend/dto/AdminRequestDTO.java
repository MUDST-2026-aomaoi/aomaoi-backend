package com.aomaoi.backend.dto;

import lombok.Data;

@Data
public class AdminRequestDTO {
    private String fullName;
    private String username;
    private String phone;
    private String tempPassword;
    private Long farmId;
    private String avatar;
}

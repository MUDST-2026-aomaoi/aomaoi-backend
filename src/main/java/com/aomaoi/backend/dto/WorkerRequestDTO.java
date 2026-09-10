package com.aomaoi.backend.dto;

import lombok.Data;

@Data
public class WorkerRequestDTO {
    private String fullName;
    private String nickname;
    private String username;
    private String phone;
    private String avatar;
    private String tempPassword;
}

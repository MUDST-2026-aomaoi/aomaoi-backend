package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.AdminProfile;
import java.time.LocalDate;

/** Stable API representation for an admin profile. */
public record AdminProfileResponseDTO(
        Long id, String fullName, String phone, Long farmId, String avatar,
        String status, LocalDate joinedDate, String username) {
    public static AdminProfileResponseDTO from(AdminProfile admin) {
        return new AdminProfileResponseDTO(admin.getId(), admin.getFullName(), admin.getPhone(),
                admin.getFarmId(), admin.getAvatar(), admin.getStatus(), admin.getJoinedDate(), admin.getUsername());
    }
}

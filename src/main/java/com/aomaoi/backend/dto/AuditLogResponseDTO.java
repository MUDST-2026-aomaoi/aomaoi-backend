package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponseDTO(Long id, String action, String performedBy, String targetUser,
                                  LocalDateTime timestamp, String details) {
    public static AuditLogResponseDTO from(AuditLog log) {
        return new AuditLogResponseDTO(log.getId(), log.getAction(), log.getPerformedBy(),
                log.getTargetUser(), log.getTimestamp(), log.getDetails());
    }
}

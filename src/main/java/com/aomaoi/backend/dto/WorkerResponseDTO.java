package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.Worker;
import java.time.LocalDate;

/** Stable API representation for a worker, independent of the JPA entity. */
public record WorkerResponseDTO(
        Long id, String fullName, String nickname, String phone, String avatar,
        String status, Long farmId, LocalDate joinedDate, String username) {
    public static WorkerResponseDTO from(Worker worker) {
        return new WorkerResponseDTO(worker.getId(), worker.getFullName(), worker.getNickname(),
                worker.getPhone(), worker.getAvatar(), worker.getStatus(), worker.getFarmId(),
                worker.getJoinedDate(), worker.getUsername());
    }
}

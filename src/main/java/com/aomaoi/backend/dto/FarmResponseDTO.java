package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.Farm;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Stable API representation; money remains decimal through serialization. */
public record FarmResponseDTO(
        Long id, String name, String location, String image, Integer workerCount, Integer adminCount,
        BigDecimal monthlyWages, BigDecimal totalWages, String status, LocalDate joinedDate) {
}

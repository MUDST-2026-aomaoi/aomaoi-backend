package com.aomaoi.backend.dto;

import com.aomaoi.backend.entity.AdvanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryAdvanceRequestDTO(
        @NotNull Long workerId,
        @NotNull LocalDate advanceDate,
        @NotNull AdvanceType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        String itemName) {
}

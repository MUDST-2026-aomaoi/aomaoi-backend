package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "work_logs")
@Data
public class WorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private String type; // cutting, planting, watering, spraying

    @Column(nullable = false)
    private LocalDate workDate;

    // Type-specific fields (nullable since each type uses different ones)
    private Integer rows;          // cutting
    private Integer waPerRow;      // cutting
    private Integer furrows;       // planting
    private Integer waPerFurrow;   // planting
    private Integer days;          // watering
    private BigDecimal dailyRate;  // watering
    private Integer tanks;         // spraying

    @Column(nullable = false)
    private BigDecimal total; // calculated wage
}

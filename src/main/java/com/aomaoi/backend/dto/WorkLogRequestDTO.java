package com.aomaoi.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkLogRequestDTO {
    private String type;        // cutting, planting, watering, spraying
    private String date;        // yyyy-MM-dd
    private String workerId;    // worker ID
    private Integer rows;       // cutting
    private Integer waPerRow;   // cutting
    private Integer furrows;    // planting
    private Integer waPerFurrow;// planting
    private Integer days;       // watering
    private BigDecimal dailyRate; // watering
    private Integer tanks;      // spraying
}

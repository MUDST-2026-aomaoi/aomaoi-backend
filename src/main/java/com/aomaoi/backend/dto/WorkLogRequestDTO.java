package com.aomaoi.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class WorkLogRequestDTO {

    /**
     * ID ของ Worker ที่บันทึกงาน
     */
    private Long workerId;

    /**
     * ประเภทงาน: CUTTING, PLANTING, WATERING, SPRAYING
     */
    private String workType;

    /**
     * จำนวนร่อง (ใช้กับ CUTTING, PLANTING)
     */
    private Integer numberOfRows;

    /**
     * ความยาวร่อง วา (ใช้กับ CUTTING, PLANTING)
     */
    private Double rowLength;

    /**
     * จำนวนวันทำงาน (ใช้กับ WATERING)
     */
    private Integer numberOfDays;

    /**
     * จำนวนถังยา (ใช้กับ SPRAYING)
     */
    private Integer numberOfTanks;

    /**
     * วันที่ทำงาน
     */
    private LocalDate workDate;

    /**
     * หมายเหตุ
     */
    private String note;
}

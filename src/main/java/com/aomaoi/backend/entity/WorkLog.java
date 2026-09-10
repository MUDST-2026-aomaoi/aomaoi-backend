package com.aomaoi.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_logs")
@Data
public class WorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ประเภทงาน: CUTTING (ตัดอ้อย), PLANTING (ปลูกอ้อย), WATERING (รดน้ำ), SPRAYING (ฉีดยา)
     */
    @Column(nullable = false)
    private String workType;

    /**
     * จำนวนร่อง — ใช้กับงาน CUTTING และ PLANTING
     */
    private Integer numberOfRows;

    /**
     * ความยาวร่อง (หน่วย: วา) — ใช้กับงาน CUTTING และ PLANTING
     */
    private Double rowLength;

    /**
     * จำนวนวันทำงาน — ใช้กับงาน WATERING
     */
    private Integer numberOfDays;

    /**
     * จำนวนถังยา — ใช้กับงาน SPRAYING
     */
    private Integer numberOfTanks;

    /**
     * ค่าจ้างที่คำนวณแล้ว (บาท)
     */
    @Column(nullable = false)
    private Double wage;

    /**
     * วันที่ทำงาน
     */
    @Column(nullable = false)
    private LocalDate workDate;

    /**
     * หมายเหตุ
     */
    private String note;

    /**
     * ความสัมพันธ์กับ Worker (Many work logs -> One worker)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "worker_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Worker worker;

    /**
     * วันเวลาที่สร้างรายการ
     */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

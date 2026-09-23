package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WorkLog Entity - ตารางบันทึกผลงาน/ค่าแรงของคนงาน
 * 
 * เก็บข้อมูลการทำงานของคนงานแต่ละคน โดยแบ่งออกเป็น 4 ประเภท:
 * - cutting (ตัดอ้อย): คำนวณจาก rows × waPerRow × 2
 * - planting (ปลูกอ้อย): คำนวณจาก furrows × waPerFurrow × 2.5
 * - watering (รดน้ำ): คำนวณจาก จำนวนวัน (startDate ถึง endDate) × dailyRate
 * - spraying (ฉีดยา): คำนวณจาก tanks × 150
 */
@Entity
@Table(name = "work_logs")
@Data
public class WorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** คนงานเจ้าของบันทึกงานนี้ (Foreign Key → workers.id) */
    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    /** ประเภทงาน: cutting, planting, watering, spraying */
    @Column(nullable = false)
    private String type;

    /** วันที่บันทึกงาน (สำหรับงานประเภทอื่นที่ไม่ใช่ watering) */
    @Column(nullable = false)
    private LocalDate workDate;

    // === ฟิลด์เฉพาะงานตัดอ้อย (cutting) ===
    /** จำนวนแถว */
    private Integer rows;
    /** จำนวนวาต่อแถว */
    private Integer waPerRow;

    // === ฟิลด์เฉพาะงานปลูกอ้อย (planting) ===
    /** จำนวนร่อง */
    private Integer furrows;
    /** จำนวนวาต่อร่อง */
    private Integer waPerFurrow;

    // === ฟิลด์เฉพาะงานรดน้ำ (watering) ===
    /** จำนวนวัน (คำนวณอัตโนมัติจาก endDate - startDate) */
    private Integer days;
    /** ค่าแรงต่อวัน (บาท) */
    private BigDecimal dailyRate;
    /** วันเริ่มต้นรดน้ำ */
    private LocalDate startDate;
    /** วันสิ้นสุดรดน้ำ */
    private LocalDate endDate;

    // === ฟิลด์เฉพาะงานฉีดยา (spraying) ===
    /** จำนวนถังยา */
    private Integer tanks;

    /** ค่าแรงรวมที่คำนวณแล้ว (บาท) */
    @Column(nullable = false)
    private BigDecimal total;
}

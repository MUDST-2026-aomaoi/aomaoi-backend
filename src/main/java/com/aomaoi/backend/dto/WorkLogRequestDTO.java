package com.aomaoi.backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

/**
 * WorkLogRequestDTO - DTO สำหรับรับข้อมูลการบันทึกงานจาก Frontend
 * 
 * Frontend จะส่ง JSON มาตาม type ของงาน:
 * - cutting: ต้องส่ง rows, waPerRow
 * - planting: ต้องส่ง furrows, waPerFurrow
 * - watering: ต้องส่ง startDate, endDate, dailyRate (ระบบจะคำนวณ days ให้อัตโนมัติ)
 * - spraying: ต้องส่ง tanks
 */
@Data
public class WorkLogRequestDTO {
    /** ประเภทงาน: cutting, planting, watering, spraying */
    @NotBlank private String type;

    /** วันที่บันทึกงาน (yyyy-MM-dd) - รองรับงานวันเดียว หรือส่ง startDate/endDate แทนได้ */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Date must use yyyy-MM-dd")
    private String date;

    /** ID ของคนงาน */
    @NotNull @Pattern(regexp = "^[1-9]\\d*$", message = "workerId must be a positive integer")
    private String workerId;

    // === ฟิลด์ช่วงวันที่ทำงานสำหรับทุกประเภทงาน ===
    /** วันเริ่มต้นทำงาน (yyyy-MM-dd) */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Date must use yyyy-MM-dd")
    private String startDate;

    /** วันสิ้นสุดทำงาน (yyyy-MM-dd) - สำหรับงานวันเดียวจะเป็นวันเดียวกับ startDate */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Date must use yyyy-MM-dd")
    private String endDate;

    // === ฟิลด์สำหรับงานตัดอ้อย (cutting) ===
    /** จำนวนแถว */
    @PositiveOrZero private Integer rows;
    /** จำนวนวาต่อแถว */
    @PositiveOrZero private Integer waPerRow;

    // === ฟิลด์สำหรับงานปลูกอ้อย (planting) ===
    /** จำนวนร่อง */
    @PositiveOrZero private Integer furrows;
    /** จำนวนวาต่อร่อง */
    @PositiveOrZero private Integer waPerFurrow;

    // === ฟิลด์สำหรับงานรดน้ำ (watering) ===
    /** จำนวนวัน (ถ้าไม่ส่งมา ระบบจะคำนวณจาก startDate/endDate) */
    @PositiveOrZero private Integer days;
    /** ค่าแรงต่อวัน (บาท) */
    @PositiveOrZero @Digits(integer = 17, fraction = 2) private BigDecimal dailyRate;

    // === ฟิลด์สำหรับงานฉีดยา (spraying) ===
    /** จำนวนถังยา */
    @PositiveOrZero private Integer tanks;
}

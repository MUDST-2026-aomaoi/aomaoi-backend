package com.aomaoi.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

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
    private String type;

    /** วันที่บันทึกงาน (yyyy-MM-dd) - ใช้กับงานที่ไม่ใช่ watering */
    private String date;

    /** ID ของคนงาน */
    private String workerId;

    // === ฟิลด์สำหรับงานตัดอ้อย (cutting) ===
    /** จำนวนแถว */
    private Integer rows;
    /** จำนวนวาต่อแถว */
    private Integer waPerRow;

    // === ฟิลด์สำหรับงานปลูกอ้อย (planting) ===
    /** จำนวนร่อง */
    private Integer furrows;
    /** จำนวนวาต่อร่อง */
    private Integer waPerFurrow;

    // === ฟิลด์สำหรับงานรดน้ำ (watering) ===
    /** จำนวนวัน (ถ้าไม่ส่งมา ระบบจะคำนวณจาก startDate/endDate) */
    private Integer days;
    /** ค่าแรงต่อวัน (บาท) */
    private BigDecimal dailyRate;
    /** วันเริ่มต้นรดน้ำ (yyyy-MM-dd) */
    private String startDate;
    /** วันสิ้นสุดรดน้ำ (yyyy-MM-dd) */
    private String endDate;

    // === ฟิลด์สำหรับงานฉีดยา (spraying) ===
    /** จำนวนถังยา */
    private Integer tanks;
}

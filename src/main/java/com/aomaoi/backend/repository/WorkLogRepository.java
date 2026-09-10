package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    /**
     * ดึงประวัติงานทั้งหมดของ Worker ตาม ID
     */
    List<WorkLog> findByWorkerId(Long workerId);

    /**
     * ค้นหาประวัติงานจากชื่อเล่นของ Worker (ค้นหาแบบ partial match, case-insensitive)
     */
    List<WorkLog> findByWorkerNicknameContainingIgnoreCase(String nickname);

    /**
     * กรองประวัติงานตามช่วงวันที่
     */
    List<WorkLog> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * ดึงประวัติงานของ Worker ในช่วงวันที่ที่กำหนด
     */
    List<WorkLog> findByWorkerIdAndWorkDateBetween(Long workerId, LocalDate startDate, LocalDate endDate);
}

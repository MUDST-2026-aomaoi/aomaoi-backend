package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.Attendance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByWorkerIdAndAttendanceDate(Long workerId, LocalDate attendanceDate);

    @EntityGraph(attributePaths = "worker")
    List<Attendance> findByWorkerFarmIdAndAttendanceDate(Long farmId, LocalDate attendanceDate);

    List<Attendance> findByWorkerIdAndAttendanceDateBetweenOrderByAttendanceDate(Long workerId, LocalDate from, LocalDate to);

    boolean existsByWorkerFarmIdAndAttendanceDate(Long farmId, LocalDate attendanceDate);
}

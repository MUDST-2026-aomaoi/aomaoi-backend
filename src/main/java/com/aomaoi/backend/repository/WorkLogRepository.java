package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    List<WorkLog> findByWorkDateBetween(LocalDate start, LocalDate end);

    List<WorkLog> findByWorkerIdAndWorkDateBetween(Long workerId, LocalDate start, LocalDate end);

    List<WorkLog> findByWorkerNicknameContainingIgnoreCase(String nickname);

    List<WorkLog> findAllByOrderByWorkDateDescIdDesc();
}

package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    List<WorkLog> findByWorkerFarmIdOrderByWorkDateDescIdDesc(Long farmId);
    List<WorkLog> findByWorkerIdOrderByWorkDateDescIdDesc(Long workerId);

    @Query("select coalesce(sum(w.total), 0) from WorkLog w where w.worker.farmId = :farmId")
    BigDecimal sumTotalByFarmId(@Param("farmId") Long farmId);

    @Query("select coalesce(sum(w.total), 0) from WorkLog w where w.worker.farmId = :farmId and w.workDate between :start and :end")
    BigDecimal sumTotalByFarmIdAndWorkDateBetween(@Param("farmId") Long farmId,
                                                    @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<WorkLog> findByWorkDateBetween(LocalDate start, LocalDate end);

    List<WorkLog> findAllByOrderByWorkDateDescIdDesc();

    long countByWorkerIdAndTypeAndWorkDate(Long workerId, String type, LocalDate workDate);
}

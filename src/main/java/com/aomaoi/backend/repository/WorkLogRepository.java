package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
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

    @Query("select w.worker.id, sum(w.total) from WorkLog w "
            + "where w.worker.id in :workerIds group by w.worker.id")
    List<Object[]> totalsByWorkerIds(@Param("workerIds") Collection<Long> workerIds);

    @Query("select w.type, count(w) from WorkLog w "
            + "where w.worker.id in :workerIds group by w.type")
    List<Object[]> countsByTypeForWorkerIds(@Param("workerIds") Collection<Long> workerIds);

    /**
     * Wage per worker for logs recorded on {@code date} (work_date = start date), highest first.
     * Rows: workerId, fullName, total wage, job count. Inactive workers are excluded.
     */
    @Query("select w.worker.id, w.worker.fullName, sum(w.total), count(w) from WorkLog w "
            + "where w.worker.farmId = :farmId and w.worker.status <> 'inactive' and w.workDate = :date "
            + "group by w.worker.id, w.worker.fullName "
            + "order by sum(w.total) desc, count(w) desc, w.worker.fullName asc")
    List<Object[]> dailyTotalsByWorker(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    /** Ids of workers of the farm that have a log whose date range covers {@code date}. */
    @Query("select distinct w.worker.id from WorkLog w "
            + "where w.worker.farmId = :farmId and w.startDate <= :date and w.endDate >= :date")
    List<Long> workerIdsCoveringDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    long countByWorkerIdAndTypeAndWorkDate(Long workerId, String type, LocalDate workDate);
}

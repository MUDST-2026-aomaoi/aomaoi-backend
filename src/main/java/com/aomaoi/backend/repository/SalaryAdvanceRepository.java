package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.SalaryAdvance;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SalaryAdvance a where a.id = :id")
    Optional<SalaryAdvance> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "worker")
    Page<SalaryAdvance> findAllByOrderByAdvanceDateDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "worker")
    Page<SalaryAdvance> findByWorkerFarmIdOrderByAdvanceDateDescIdDesc(Long farmId, Pageable pageable);

    @Query("select a.worker.id, sum(a.amount) from SalaryAdvance a "
            + "where a.cancelledAt is null and a.worker.id in :workerIds group by a.worker.id")
    List<Object[]> totalsByWorkerIds(@Param("workerIds") Collection<Long> workerIds);

    @Query("select coalesce(sum(a.amount), 0) from SalaryAdvance a "
            + "where a.cancelledAt is null and a.type = :type and a.worker.id in :workerIds")
    BigDecimal sumActiveByTypeAndWorkerIds(@Param("type") AdvanceType type,
                                            @Param("workerIds") Collection<Long> workerIds);
}

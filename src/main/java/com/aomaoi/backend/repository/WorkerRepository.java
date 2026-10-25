package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    List<Worker> findByFarmId(Long farmId);
    long countByStatus(String status);
    java.util.Optional<Worker> findByUserUsername(String username);
    long countByFarmIdAndStatusNot(Long farmId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Worker w where w.id = :id")
    Optional<Worker> findLockedById(@Param("id") Long id);
}

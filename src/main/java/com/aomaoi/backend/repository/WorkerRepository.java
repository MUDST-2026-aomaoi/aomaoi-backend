package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findByStatusNot(String status);

    long countByStatus(String status);

    boolean existsByUsername(String username);
}

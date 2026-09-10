package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    boolean existsByNickname(String nickname);
    List<Worker> findByStatus(String status);
}

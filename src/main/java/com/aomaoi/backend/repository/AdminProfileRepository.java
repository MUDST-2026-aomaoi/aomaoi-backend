package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<AdminProfile> findByUserUsername(String username);
    long countByFarmIdAndStatusNot(Long farmId, String status);

    @Override
    @EntityGraph(attributePaths = "user")
    List<AdminProfile> findAll();

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<AdminProfile> findById(Long id);
}

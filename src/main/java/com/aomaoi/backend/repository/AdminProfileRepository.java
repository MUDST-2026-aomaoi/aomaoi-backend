package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    Optional<AdminProfile> findByUserUsername(String username);
    long countByFarmIdAndStatusNot(Long farmId, String status);
}

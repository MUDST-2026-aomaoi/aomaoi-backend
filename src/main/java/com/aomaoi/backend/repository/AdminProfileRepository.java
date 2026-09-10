package com.aomaoi.backend.repository;

import com.aomaoi.backend.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    List<AdminProfile> findByStatus(String status);
}

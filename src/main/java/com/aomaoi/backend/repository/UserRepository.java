package com.aomaoi.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.aomaoi.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // e.g. Optional<User> findByUsername(String username);
}


package com.aomaoi.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.aomaoi.backend.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Data JPA will automatically write the SQL to find a user by their username!
    Optional<User> findByUsername(String username);
    
    // Checks if a username already exists (useful when an Admin is adding a new Worker)
    boolean existsByUsername(String username);
}

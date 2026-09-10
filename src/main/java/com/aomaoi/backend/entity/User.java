package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data // Lombok automatically creates Getters, Setters, and Constructors for us!
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String role; // Will store "superadmin", "admin", or "worker"
    
    @Column(name = "is_first_login", nullable = false)
    private Boolean isFirstLogin = true; // Defaults to true for the OTP flow
}

package com.aomaoi.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // 'user' is often a reserved keyword in Postgres
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Add username, password, role here
}


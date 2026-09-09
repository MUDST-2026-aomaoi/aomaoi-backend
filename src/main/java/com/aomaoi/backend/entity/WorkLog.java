package com.aomaoi.backend.entity;

import jakarta.persistence.*;

@Entity
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}


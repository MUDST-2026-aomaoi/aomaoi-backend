package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "workers")
@Data
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String nickname;

    @Column(unique = true)
    private String username;

    private String phone;

    @Column(nullable = false)
    private String status = "active"; // active, pending, inactive

    @Column(nullable = false)
    private LocalDate joinedDate;

    private String avatar;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}

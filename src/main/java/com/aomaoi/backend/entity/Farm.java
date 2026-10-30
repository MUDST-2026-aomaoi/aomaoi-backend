package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    
    @Column(columnDefinition = "TEXT")
    private String image;
    
    private String status = "active";
    private LocalDate joinedDate;

    @PrePersist
    protected void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }
    }
}

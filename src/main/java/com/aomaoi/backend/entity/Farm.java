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
    
    private Integer workerCount = 0;
    private Integer adminCount = 0;
    private Double monthlyWages = 0.0;
    private Double totalWages = 0.0;
    private String status = "active";
    private LocalDate joinedDate;

    @PrePersist
    protected void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }
    }
}

package com.aomaoi.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Entity
@Data
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String nickname;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String avatar;
    private String status = "active";
    private String farmId;
    
    private LocalDate joinedDate;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }
    
    @PrePersist
    protected void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }
    }
}

package com.hemo.backend.entity;


import jakarta.persistence.*;   // JPA annotations
import lombok.Data;            // Lombok
import java.time.LocalDateTime; // Date-time

@Entity
@Table(name = "searches")
@Data
public class Search {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long searchId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String hospitalName;
    private String hospitalPincode;

    private String bloodGroup;
    private String bloodComponent;

    private LocalDateTime createdAt = LocalDateTime.now();
}

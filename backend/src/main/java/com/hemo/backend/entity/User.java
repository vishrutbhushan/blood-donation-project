package com.hemo.backend.entity;

import jakarta.persistence.*;   // JPA annotations
import lombok.Data;            // Lombok
import java.time.LocalDateTime; // Date-time

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;

    @Column(unique = true)
    private String phone;

    private LocalDateTime createdAt = LocalDateTime.now();
}
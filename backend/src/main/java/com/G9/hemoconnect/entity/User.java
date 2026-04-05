package com.G9.hemoconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"user\"")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;
    
    @Column(name = "pincode", length = 10)
    private String pincode;
    
    @Column(name = "blood_group", length = 3)
    private String bloodGroup;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

package com.G9.hemoconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "search")
@Data
@NoArgsConstructor
public class Search {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "search_id")
    private UUID searchId;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "blood_group", nullable = false, length = 3)
    private String bloodGroup;
    
    @Column(name = "lat", precision = 9, scale = 6)
    private BigDecimal lat;
    
    @Column(name = "lng", precision = 9, scale = 6)
    private BigDecimal lng;
    
    @Column(name = "searched_at", nullable = false)
    private OffsetDateTime searchedAt;
}

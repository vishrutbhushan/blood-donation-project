package com.G9.hemoconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp")
@Data
@NoArgsConstructor
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id")
    private UUID otpId;
    
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;
    
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Column(name = "expiry_time", nullable = false)
    private OffsetDateTime expiryTime;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

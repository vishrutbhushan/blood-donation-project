package com.G9.hemoconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "response")
@Data
@NoArgsConstructor
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "response_id")
    private UUID responseId;
    
    @ManyToOne
    @JoinColumn(name = "request_fk", nullable = false)
    private Request request;
    
    @ManyToOne
    @JoinColumn(name = "donor_fk", nullable = false)
    private User donor;
    
    @Column(name = "replied", nullable = false, length = 1)
    private Character replied;
    
    @Column(name = "responded_at", nullable = false)
    private OffsetDateTime respondedAt;
}

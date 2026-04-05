package com.G9.hemoconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request")
@Data
@NoArgsConstructor
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "req_id")
    private UUID reqId;
    
    @ManyToOne
    @JoinColumn(name = "search_fk", nullable = false)
    private Search search;
    
    @ManyToOne
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "parent_req_fk")
    private Request parentRequest;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

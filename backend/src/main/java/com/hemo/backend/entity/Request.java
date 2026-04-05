package com.hemo.backend.entity;

import jakarta.persistence.*;   // JPA annotations
import lombok.Data;            // Lombok
import java.time.LocalDateTime; // Date-time

@Entity
@Table(name = "requests")
@Data
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "search_id")
    private Search search;

    private String bloodGroup;
    private String component;

    private Integer unitsRequested = 1;
    private Integer numberOfDonorsContacted = 0;

    private String status = "ACTIVE";

    @ManyToOne
    @JoinColumn(name = "parent_request_id")
    private Request parentRequest;

    private LocalDateTime createdAt = LocalDateTime.now();
}

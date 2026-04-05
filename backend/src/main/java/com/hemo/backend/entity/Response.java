package com.hemo.backend.entity;

import jakarta.persistence.*;   // JPA annotations
import lombok.Data;            // Lombok
import java.time.LocalDateTime; // Date-time

@Entity
@Table(name = "responses")
@Data
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long responseId;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private Request request;

    private String donorId;
    private String donorName;
    private String phoneNumber;

    private String bloodGroup;
    private String location;

    private String responseStatus; // YES / NO

    private LocalDateTime respondedAt = LocalDateTime.now();
}

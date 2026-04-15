package com.hemo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "responses")
@Data
@NoArgsConstructor
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long responseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "donor_id", length = 100)
    private String donorId;

    @Column(name = "donor_name", length = 100)
    private String donorName;

    @Column(name = "abha_id", length = 14)
    private String abhaId;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "blood_group", length = 20)
    private String bloodGroup;

    private String location;

    @Column(name = "response_status", length = 10)
    private String responseStatus;

    @CreationTimestamp
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}

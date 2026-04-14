package com.hemo.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "requests")
@Data
@NoArgsConstructor
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_id", nullable = false)
    private Search search;

    @Column(name = "blood_group", nullable = false, length = 20)
    private String bloodGroup;

    @Column(nullable = false, length = 20)
    private String component;

    @Column(name = "units_requested")
    private Integer unitsRequested;

    @Column(name = "number_of_donors_contacted")
    private Integer numberOfDonorsContacted;

    @Column(length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_request_id")
    private Request parentRequest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "request")
    private List<Response> responses = new ArrayList<>();

    @PrePersist
    void initializeDefaults() {
        if (unitsRequested == null) {
            unitsRequested = 1;
        }
        if (numberOfDonorsContacted == null) {
            numberOfDonorsContacted = 0;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
        if (lastNotifiedAt == null) {
            lastNotifiedAt = LocalDateTime.now();
        }
    }
}

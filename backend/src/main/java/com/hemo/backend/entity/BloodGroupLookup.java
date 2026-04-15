package com.hemo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blood_group_lookup")
@Data
@NoArgsConstructor
public class BloodGroupLookup {

    @Id
    @Column(name = "blood_group_code", length = 20)
    private String bloodGroupCode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}
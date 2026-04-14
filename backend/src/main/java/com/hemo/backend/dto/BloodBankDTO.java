package com.hemo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodBankDTO {
    private Long bankId;
    private String sourceId;
    private String sourceBankId;
    private String bankName;
    private String category;
    private String phone;
    private String email;
    private String pincode;
    private String city;
    private String state;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}

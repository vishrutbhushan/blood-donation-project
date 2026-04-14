package com.hemo.backend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonorCandidateDTO {
    String donorId;
    String name;
    String bloodGroup;
    String phone;
    String pincode;
    String location;
    String source;
    Double distanceKm;
}

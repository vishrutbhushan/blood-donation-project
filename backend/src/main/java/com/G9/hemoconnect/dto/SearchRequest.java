package com.G9.hemoconnect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchRequest {
    private String patientName;
    private String bloodGroup;
    private String component;
    private String hospitalName;
    private String hospitalPincode;
}

package com.hemo.backend.dto;

import lombok.Data;

@Data
public class SearchCreateRequestDTO {
    private String hospitalName;
    private String hospitalPincode;
    private String bloodGroup;
    private String bloodComponent;
}

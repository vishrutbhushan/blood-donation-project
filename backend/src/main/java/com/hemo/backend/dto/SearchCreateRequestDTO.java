package com.hemo.backend.dto;

import lombok.Data;

@Data
public class SearchCreateRequestDTO {
    private String hospitalPincode;
    private String bloodGroup;
    private String bloodComponent;
}

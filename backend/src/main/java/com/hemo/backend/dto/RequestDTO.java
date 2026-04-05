package com.hemo.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Data
public class RequestDTO {

    @NotBlank
    private String bloodGroup;

    @NotBlank
    private String component;

    @Min(1)
    private int unitsRequested;
}

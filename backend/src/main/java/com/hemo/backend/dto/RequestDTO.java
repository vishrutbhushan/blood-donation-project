package com.hemo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RequestDTO {

    @NotBlank
    private String bloodGroup;

    @NotBlank
    private String component;

    @Min(1)
    private int unitsRequested;

    @Min(0)
    private int matchedCount;
}

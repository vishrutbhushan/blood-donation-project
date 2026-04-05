package com.hemo.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;


@Data
public class ResponseDTO {

    @NotBlank
    private String donorId;

    @NotBlank
    private String donorName;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String bloodGroup;

    @NotBlank
    private String location;

    @NotBlank
    private String responseStatus; // YES / NO
}
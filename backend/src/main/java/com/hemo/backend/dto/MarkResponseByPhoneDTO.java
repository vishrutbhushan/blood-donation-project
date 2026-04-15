package com.hemo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkResponseByPhoneDTO {
    @NotBlank
    private String phoneNumber;
}

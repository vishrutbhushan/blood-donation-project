package com.hemo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerifyRequestDTO {

    @NotBlank(message = "ABHA is required")
    @Pattern(regexp = "^\\d{14}$", message = "ABHA must be 14 digits")
    private String abhaId;

    @NotBlank(message = "OTP is required")
    private String otp;
}

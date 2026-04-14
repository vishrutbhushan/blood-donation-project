package com.hemo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpVerifyResponseDTO {
    private boolean verified;
    private String abhaId;
    private String name;
    private String phone;
}

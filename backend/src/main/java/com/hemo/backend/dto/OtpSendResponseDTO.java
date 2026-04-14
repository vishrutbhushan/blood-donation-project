package com.hemo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpSendResponseDTO {
    private boolean sent;
    private String abhaId;
    private String name;
    private String phone;
}

package com.hemo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthProfileDTO {
    private String name;
    private String phone;
}

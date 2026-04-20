package com.who.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoDonorDTO {
    private String name;
    private String abha_hash;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private String blood_group;
    private Integer age;
    private String last_donated;
    private Long created_at;
    private Long updated_at;
    private boolean deleted;
}

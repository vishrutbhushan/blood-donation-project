package com.redcross.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossEtlDonorDTO {
    private String donor_id;
    private String name;
    private String blood_group;
    private Integer age;
    private String phone;
    private String email;
    private String address_current;
    private String city_current;
    private String state_current;
    private String pincode_current;
    private String bank_id;
    private String last_donated_on;
    private String last_donated_blood_bank;
    private String update_time;
    private boolean deleted;
}

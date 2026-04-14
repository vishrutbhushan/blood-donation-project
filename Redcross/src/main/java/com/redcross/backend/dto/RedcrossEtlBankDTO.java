package com.redcross.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossEtlBankDTO {
    private String bank_id;
    private String bank_name;
    private String category;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;
    private String created_at;
    private String update_time;
    private boolean deleted;
}

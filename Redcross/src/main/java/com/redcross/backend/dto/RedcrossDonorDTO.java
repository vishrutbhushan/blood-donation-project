package com.redcross.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossDonorDTO {
    private String full_name;
    private String national_id;
    private String contact_number;
    private String address;
    private String blood_type;
    private Integer age;
    private String last_donation_date;
    private Long created_at;
    private Long updated_at;
    private String pincode;
    private boolean deleted;
}

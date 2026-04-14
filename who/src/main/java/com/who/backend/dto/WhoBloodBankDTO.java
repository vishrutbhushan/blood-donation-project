package com.who.backend.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoBloodBankDTO {
    private String name;
    private String category;
    private String phone;
    private String email;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private Long created_at;
    private Long updated_at;
    private boolean deleted;
    private List<WhoInventoryDTO> blood_inventory = new ArrayList<>();
}

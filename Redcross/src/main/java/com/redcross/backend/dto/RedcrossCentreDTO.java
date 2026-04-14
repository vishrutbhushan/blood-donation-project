package com.redcross.backend.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossCentreDTO {
    private String name;
    private String category;
    private String contact_number;
    private String email;
    private String full_address;
    private String postal_code;
    private Long created_at;
    private Long updated_at;
    private boolean deleted;
    private List<RedcrossInventoryDTO> blood_inventory = new ArrayList<>();
}

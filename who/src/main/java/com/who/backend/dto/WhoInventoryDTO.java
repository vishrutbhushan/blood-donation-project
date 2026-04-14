package com.who.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoInventoryDTO {
    private String blood_group;
    private String component_type;
    private Integer units_available;
    private Long last_updated;
}

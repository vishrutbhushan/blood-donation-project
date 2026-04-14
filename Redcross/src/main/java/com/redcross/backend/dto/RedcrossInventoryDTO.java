package com.redcross.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossInventoryDTO {
    private String blood_group;
    private String component;
    private Integer quantity;
    private Long updated_at;
}

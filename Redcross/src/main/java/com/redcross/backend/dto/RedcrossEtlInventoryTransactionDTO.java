package com.redcross.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossEtlInventoryTransactionDTO {
    private String transaction_id;
    private String source_event_id;
    private String bank_id;
    private String donor_id;
    private String blood_group;
    private String component;
    private String transaction_type;
    private Integer units_delta;
    private Integer running_balance_after;
    private String expiry_date;
    private String event_timestamp;
    private String update_time;
    private boolean deleted;
}
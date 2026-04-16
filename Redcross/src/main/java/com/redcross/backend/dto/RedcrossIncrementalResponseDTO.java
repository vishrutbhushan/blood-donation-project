package com.redcross.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedcrossIncrementalResponseDTO {
    private List<RedcrossEtlBankDTO> centres;
    private List<RedcrossEtlDonorDTO> people;
    private List<RedcrossEtlInventoryTransactionDTO> inventory_transactions;
}

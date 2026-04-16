package com.who.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoIncrementalResponseDTO {
    private List<WhoEtlBankDTO> blood_banks;
    private List<WhoEtlDonorDTO> donors;
    private List<WhoEtlInventoryTransactionDTO> inventory_transactions;
}

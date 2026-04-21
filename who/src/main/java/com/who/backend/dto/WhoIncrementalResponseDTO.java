package com.who.backend.dto;

import java.util.List;

public class WhoIncrementalResponseDTO {
    private List<WhoEtlBankDTO> blood_banks;
    private List<WhoEtlDonorDTO> donors;
    private List<WhoEtlInventoryTransactionDTO> inventory_transactions;

    public WhoIncrementalResponseDTO() {
    }

    public WhoIncrementalResponseDTO(
            List<WhoEtlBankDTO> blood_banks,
            List<WhoEtlDonorDTO> donors,
            List<WhoEtlInventoryTransactionDTO> inventory_transactions) {
        this.blood_banks = blood_banks;
        this.donors = donors;
        this.inventory_transactions = inventory_transactions;
    }

    public List<WhoEtlBankDTO> getBlood_banks() {
        return blood_banks;
    }

    public void setBlood_banks(List<WhoEtlBankDTO> blood_banks) {
        this.blood_banks = blood_banks;
    }

    public List<WhoEtlDonorDTO> getDonors() {
        return donors;
    }

    public void setDonors(List<WhoEtlDonorDTO> donors) {
        this.donors = donors;
    }

    public List<WhoEtlInventoryTransactionDTO> getInventory_transactions() {
        return inventory_transactions;
    }

    public void setInventory_transactions(List<WhoEtlInventoryTransactionDTO> inventory_transactions) {
        this.inventory_transactions = inventory_transactions;
    }
}

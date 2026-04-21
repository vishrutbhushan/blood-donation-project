package com.redcross.backend.dto;

import java.util.List;

public class RedcrossIncrementalResponseDTO {
    private List<RedcrossEtlBankDTO> centres;
    private List<RedcrossEtlDonorDTO> people;
    private List<RedcrossEtlInventoryTransactionDTO> inventory_transactions;

    public RedcrossIncrementalResponseDTO() {
    }

    public RedcrossIncrementalResponseDTO(
            List<RedcrossEtlBankDTO> centres,
            List<RedcrossEtlDonorDTO> people,
            List<RedcrossEtlInventoryTransactionDTO> inventory_transactions) {
        this.centres = centres;
        this.people = people;
        this.inventory_transactions = inventory_transactions;
    }

    public List<RedcrossEtlBankDTO> getCentres() {
        return centres;
    }

    public void setCentres(List<RedcrossEtlBankDTO> centres) {
        this.centres = centres;
    }

    public List<RedcrossEtlDonorDTO> getPeople() {
        return people;
    }

    public void setPeople(List<RedcrossEtlDonorDTO> people) {
        this.people = people;
    }

    public List<RedcrossEtlInventoryTransactionDTO> getInventory_transactions() {
        return inventory_transactions;
    }

    public void setInventory_transactions(List<RedcrossEtlInventoryTransactionDTO> inventory_transactions) {
        this.inventory_transactions = inventory_transactions;
    }
}

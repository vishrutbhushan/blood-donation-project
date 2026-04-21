package etl.service;

import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.EtlBatch;
import etl.model.InventoryTransaction;
import etl.util.TimeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EtlBatchAccumulator {
    private final Map<String, Map<String, BloodBank>> banksBySource = new HashMap<>();
    private final Map<String, Map<String, Donor>> donorsBySource = new HashMap<>();
    private final Map<String, Map<String, InventoryTransaction>> latestInventoryBySource = new HashMap<>();
    private final Map<String, Map<String, InventoryTransaction>> pendingInventoryBySource = new HashMap<>();

    public void reset() {
        banksBySource.clear();
        donorsBySource.clear();
        latestInventoryBySource.clear();
        pendingInventoryBySource.clear();
    }

    public void merge(String source, EtlBatch batch) {
        Map<String, BloodBank> bankMemory = banksBySource.computeIfAbsent(source, key -> new HashMap<>());
        Map<String, Donor> donorMemory = donorsBySource.computeIfAbsent(source, key -> new HashMap<>());
        Map<String, InventoryTransaction> latestInventory = latestInventoryBySource.computeIfAbsent(source, key -> new HashMap<>());
        Map<String, InventoryTransaction> pendingInventory = pendingInventoryBySource.computeIfAbsent(source, key -> new HashMap<>());

        for (BloodBank bank : batch.getBanks()) {
            if (bank.getBankId() != null && !bank.getBankId().isBlank()) {
                bankMemory.put(bank.getBankId(), bank);
            }
        }

        for (Donor donor : batch.getDonors()) {
            if (donor.getDonorId() != null && !donor.getDonorId().isBlank()) {
                donorMemory.put(donor.getDonorId(), donor);
            }
        }

        for (InventoryTransaction transaction : batch.getInventoryTransactions()) {
            if (transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
                continue;
            }

            pendingInventory.put(transaction.getTransactionId(), transaction);
            if (transaction.getBankId() == null || transaction.getBankId().isBlank()) {
                continue;
            }
            if (transaction.getBloodGroup() == null || transaction.getComponent() == null) {
                continue;
            }

            String inventoryKey = transaction.getBankId() + ":" + transaction.getBloodGroup() + ":" + transaction.getComponent();
            InventoryTransaction existing = latestInventory.get(inventoryKey);
            String transactionTs = transaction.getUpdatedAt() == null || transaction.getUpdatedAt().isBlank()
                ? transaction.getEventTimestamp()
                : transaction.getUpdatedAt();
            String existingTs = existing == null || existing.getUpdatedAt() == null || existing.getUpdatedAt().isBlank()
                ? (existing == null ? null : existing.getEventTimestamp())
                : existing.getUpdatedAt();
            if (existing == null || !TimeUtil.toDateTime(transactionTs).isBefore(TimeUtil.toDateTime(existingTs))) {
                latestInventory.put(inventoryKey, transaction);
            }
        }
    }

    public List<BloodBank> collectLatestBanks() {
        Map<String, BloodBank> merged = new LinkedHashMap<>();
        for (Map<String, BloodBank> recordsById : banksBySource.values()) {
            mergeLatestBanksInto(merged, recordsById.values());
        }
        return new ArrayList<>(merged.values());
    }

    public List<Donor> collectLatestDonors(List<BloodBank> latestBanks) {
        Map<String, Donor> merged = new LinkedHashMap<>();
        for (Map<String, Donor> recordsById : donorsBySource.values()) {
            mergeLatestDonorsInto(merged, recordsById.values());
        }
        List<Donor> latestDonors = new ArrayList<>(merged.values());
        resolveForeignKeys(latestDonors, latestBanks);
        return latestDonors;
    }

    public List<InventoryTransaction> collectPendingInventoryTransactions() {
        List<InventoryTransaction> rows = new ArrayList<>();
        for (Map<String, InventoryTransaction> recordsById : pendingInventoryBySource.values()) {
            rows.addAll(recordsById.values());
        }
        return rows;
    }

    public List<InventoryTransaction> collectCurrentInventoryState() {
        List<InventoryTransaction> rows = new ArrayList<>();
        for (Map<String, InventoryTransaction> recordsByKey : latestInventoryBySource.values()) {
            rows.addAll(recordsByKey.values());
        }
        return rows;
    }

    public void clearPendingInventoryTransactions() {
        pendingInventoryBySource.clear();
    }

    private void mergeLatestBanksInto(Map<String, BloodBank> merged, Iterable<BloodBank> records) {
        for (BloodBank row : records) {
            if (row.getSource() == null || row.getBankId() == null) {
                continue;
            }
            String key = row.getSource() + ":" + row.getBankId();
            BloodBank existing = merged.get(key);
            if (existing == null || !TimeUtil.toDateTime(row.getUpdatedAt()).isBefore(TimeUtil.toDateTime(existing.getUpdatedAt()))) {
                merged.put(key, row);
            }
        }
    }

    private void mergeLatestDonorsInto(Map<String, Donor> merged, Iterable<Donor> records) {
        for (Donor row : records) {
            if (row.getSource() == null || row.getDonorId() == null) {
                continue;
            }
            String key = row.getSource() + ":" + row.getDonorId();
            Donor existing = merged.get(key);
            if (existing == null || !TimeUtil.toDateTime(row.getUpdatedAt()).isBefore(TimeUtil.toDateTime(existing.getUpdatedAt()))) {
                merged.put(key, row);
            }
        }
    }

    private void resolveForeignKeys(List<Donor> donors, List<BloodBank> banks) {
        Map<String, String> bankIdBySourceAndName = new HashMap<>();
        for (BloodBank bank : banks) {
            if (bank.getSource() == null || bank.getBankName() == null || bank.getBankId() == null) {
                continue;
            }
            bankIdBySourceAndName.put(bank.getSource() + ":" + bank.getBankName(), bank.getBankId());
        }

        for (Donor donor : donors) {
            if (donor.getSource() == null || donor.getLastDonatedBloodBank() == null) {
                continue;
            }
            String key = donor.getSource() + ":" + donor.getLastDonatedBloodBank();
            String bankId = bankIdBySourceAndName.get(key);
            if (bankId != null) {
                donor.setBankId(bankId);
            }
        }
    }
}
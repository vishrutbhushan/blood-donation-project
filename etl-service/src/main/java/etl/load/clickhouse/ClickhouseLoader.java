package etl.load.clickhouse;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.InventoryTransaction;
import etl.util.ClickhouseText;
import etl.util.TimeUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClickhouseLoader {
    private static final Logger log = LoggerFactory.getLogger(ClickhouseLoader.class);
    private static final int BULK_INSERT_BATCH_SIZE = 500;
    private final RestClient clickhouse;

    public ClickhouseLoader(RestClient.Builder builder) {
        this.clickhouse = builder
                .baseUrl(Objects.requireNonNull(Constants.CLICKHOUSE_URL, "CLICKHOUSE_URL"))
                .defaultHeader("X-ClickHouse-User", Constants.CLICKHOUSE_USER)
                .defaultHeader("X-ClickHouse-Key", Constants.CLICKHOUSE_PASSWORD)
                .build();
    }

    public void loadBanks(List<BloodBank> banks) {
        log.info("api.enter ClickhouseLoader.loadBanks");
        if (banks == null || banks.isEmpty()) {
            log.info("api.exit ClickhouseLoader.loadBanks");
            return;
        }

        List<String> locationRows = new ArrayList<>();
        List<String> bankRows = new ArrayList<>();
        List<String> counterRows = new ArrayList<>();
        for (BloodBank b : banks) {
            int sourceId = sourceId(b.getSource());

            long locationId = locationId(b.getSource(), b.getPincode(), b.getAddress());
            long bankId = bankId(b.getSource(), b.getBankId());
            long version = versionOf(b.getUpdatedAt());
            int isDeleted = isDelete(b.getOp()) ? 1 : 0;

                locationRows.add("(" + locationId + "," + ClickhouseText.quote(b.getPincode()) + ","
                    + ClickhouseText.quote(b.getCity()) + "," + ClickhouseText.quote(b.getState()) + ","
                    + ClickhouseText.quote(b.getAddress()) + "," + (b.getLat() != null ? b.getLat() : "0.0") + ","
                    + (b.getLon() != null ? b.getLon() : "0.0") + "," + ClickhouseText.dateTime(b.getUpdatedAt()) + ")");
                bankRows.add("(" + bankId + "," + sourceId + "," + ClickhouseText.quote(b.getBankId()) + ","
                    + ClickhouseText.quote(b.getBankName()) + "," + ClickhouseText.quote(b.getCategory()) + ","
                    + ClickhouseText.quote(b.getPhone()) + "," + ClickhouseText.quote(b.getEmail()) + ","
                    + locationId + "," + ClickhouseText.dateTime(ClickhouseText.fallback(b.getCreatedAt(), b.getUpdatedAt()))
                    + "," + ClickhouseText.dateTime(b.getUpdatedAt()) + "," + isDeleted + "," + version + ")");
            counterRows.add(counterTuple(b.getUpdatedAt(), b.getSource(), "incremental", "bank", 1));
        }

        flushInsertValues("INSERT INTO blood_ops.dim_location "
                + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ",
                locationRows);
        flushInsertValues("INSERT INTO blood_ops.dim_blood_bank "
                + "(bank_id, source_id, source_bank_id, bank_name, category, phone, email, location_id, created_at, updated_at, is_deleted, version) VALUES ",
                bankRows);
        flushInsertValues("INSERT INTO blood_ops.source_ingestion_hourly_agg "
                + "(event_hour, source, api_name, record_type, record_count) VALUES ", counterRows);

        log.info("api.exit ClickhouseLoader.loadBanks");
    }

    public void loadDonors(List<Donor> donors) {
        log.info("api.enter ClickhouseLoader.loadDonors");
        if (donors == null || donors.isEmpty()) {
            log.info("api.exit ClickhouseLoader.loadDonors");
            return;
        }

        List<String> locationRows = new ArrayList<>();
        List<String> donorRows = new ArrayList<>();
        List<String> snapshotRows = new ArrayList<>();
        List<String> counterRows = new ArrayList<>();
        for (Donor d : donors) {
            int sourceId = sourceId(d.getSource());

            int bloodGroupId = bloodGroupId(d.getBloodGroup());

            long locationId = locationId(d.getSource(), d.getPincodeCurrent(), d.getAddressCurrent());
            long donorSk = donorSk(d.getSource(), d.getDonorId());
            long bankId = bankId(d.getSource(), d.getBankId());
            LocalDateTime updatedAt = TimeUtil.toDateTime(d.getUpdatedAt());
            long version = updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
            int isDeleted = isDelete(d.getOp()) ? 1 : 0;

                locationRows.add("(" + locationId + "," + ClickhouseText.quote(d.getPincodeCurrent()) + ","
                    + ClickhouseText.quote(d.getCityCurrent()) + "," + ClickhouseText.quote(d.getStateCurrent()) + ","
                    + ClickhouseText.quote(d.getAddressCurrent()) + ","
                    + (d.getLat() != null ? d.getLat() : "0.0") + "," + (d.getLon() != null ? d.getLon() : "0.0") + ","
                    + ClickhouseText.dateTime(d.getUpdatedAt()) + ")");
                    donorRows.add("(" + donorSk + "," + sourceId + "," + ClickhouseText.quote(d.getDonorId()) + "," 
                        + bankId + "," + ClickhouseText.quote(d.getName()) + "," 
                        + ClickhouseText.quote(identityHash(d.getSource(), d.getDonorId(), d.getPhone())) + "," 
                        + ClickhouseText.quote(d.getPhone()) + "," + locationId + "," + bloodGroupId + "," 
                        + age(d.getAge()) + "," + dateOrDefault(d.getLastDonatedOn()) + "," 
                        + ClickhouseText.dateTime(d.getUpdatedAt()) + "," + ClickhouseText.dateTime(d.getUpdatedAt()) 
                        + "," + isDeleted + "," + version + ")");

            int eligible = eligibleDonor(d.getLastDonatedOn(), updatedAt) ? 1 : 0;
            int donorCount = isDeleted == 1 ? 0 : 1;
            snapshotRows.add("(" + stableUInt64("fact-donor:" + d.getSource() + ":" + d.getDonorId() + ":" + version)
                    + "," + sourceId + "," + ClickhouseText.quote(d.getSource()) + "," + donorSk + "," + bankId + "," + locationId + ","
                    + bloodGroupId + "," + toEventTimeId(updatedAt) + "," + toEventDateId(updatedAt) + ","
                    + age(d.getAge()) + "," + donorCount + "," + eligible + "," + isDeleted + ","
                    + dateOrDefault(d.getLastDonatedOn()) + "," + ClickhouseText.dateTime(d.getUpdatedAt()) + "," + version + ")");
            counterRows.add(counterTuple(d.getUpdatedAt(), d.getSource(), "incremental", "donor", 1));
        }

        flushInsertValues("INSERT INTO blood_ops.dim_location "
                + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ",
                locationRows);
        flushInsertValues("INSERT INTO blood_ops.dim_donor "
                + "(donor_sk, source_id, source_donor_id, bank_id, donor_name, donor_identity_hash, phone, location_id, blood_group_id, age, last_donated_date, created_at, updated_at, is_deleted, version) VALUES ",
                donorRows);
        flushInsertValues("INSERT INTO blood_ops.fact_donor_snapshot "
                + "(donor_fact_id, source_id, source_system, donor_sk, bank_id, location_id, blood_group_id, event_time_id, event_date_id, age, donor_count, eligible_donor_count, is_deleted, last_donated_date, snapshot_updated_at, version) VALUES ",
                snapshotRows);
        flushInsertValues("INSERT INTO blood_ops.source_ingestion_hourly_agg "
                + "(event_hour, source, api_name, record_type, record_count) VALUES ", counterRows);

        log.info("api.exit ClickhouseLoader.loadDonors");
    }

    public void loadInventoryDay(LocalDate businessDate, List<InventoryTransaction> transactions) {
        log.info("api.enter ClickhouseLoader.loadInventoryDay");
        String day = businessDate.toString();

        if (transactions == null || transactions.isEmpty()) {
            log.info("api.exit ClickhouseLoader.loadInventoryDay");
            return;
        }

        Map<String, List<InventoryTransaction>> grouped = new HashMap<>();
        for (InventoryTransaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            if (transaction.getSource() == null || transaction.getBankId() == null
                    || transaction.getBloodGroup() == null || transaction.getComponent() == null) {
                continue;
            }
            LocalDate eventDay = TimeUtil.toDateTime(transaction.getEventTimestamp()).toLocalDate();
            if (!businessDate.equals(eventDay)) {
                continue;
            }
            String key = transaction.getSource() + ":" + transaction.getBankId() + ":" + transaction.getBloodGroup()
                    + ":" + transaction.getComponent();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }

        long version = System.currentTimeMillis();
        List<String> factRows = new ArrayList<>();
        for (List<InventoryTransaction> group : grouped.values()) {
            group.sort(Comparator.comparing(txn -> TimeUtil.toDateTime(txn.getEventTimestamp())));
            InventoryTransaction first = group.get(0);
            InventoryTransaction last = group.get(group.size() - 1);

            int inflowUnits = 0;
            int outflowUnits = 0;
            int donationEvents = 0;
            int withdrawalEvents = 0;

            for (InventoryTransaction txn : group) {
                int delta = txn.getUnitsDelta() == null ? 0 : txn.getUnitsDelta();
                String type = txn.getTransactionType() == null ? ""
                        : txn.getTransactionType().trim().toUpperCase(Locale.ROOT);
                if ("INFLOW".equals(type)) {
                    if (delta > 0) {
                        inflowUnits += delta;
                    }
                    donationEvents += 1;
                } else if ("OUTFLOW".equals(type)) {
                    if (delta < 0) {
                        outflowUnits += Math.abs(delta);
                    }
                    withdrawalEvents += 1;
                }
            }

            int firstDelta = first.getUnitsDelta() == null ? 0 : first.getUnitsDelta();
            int firstBalance = first.getRunningBalanceAfter() == null ? 0 : first.getRunningBalanceAfter();
            int lastBalance = last.getRunningBalanceAfter() == null ? 0 : last.getRunningBalanceAfter();
            int openingBalanceUnits = firstBalance - firstDelta;

                int sourceId = sourceId(first.getSource());
            long bankId = bankId(first.getSource(), first.getBankId());
                factRows.add("(toDate('" + day + "')," + sourceId + "," + ClickhouseText.quote(first.getSource()) + "," + bankId + ","
                    + ClickhouseText.quote(normalizeBloodGroup(first.getBloodGroup())) + "," + ClickhouseText.quote(first.getComponent()) + ","
                    + openingBalanceUnits + "," + inflowUnits + "," + outflowUnits + "," + lastBalance + ","
                    + donationEvents + "," + withdrawalEvents + "," + version + ")");
        }

        flushInsertValues("INSERT INTO blood_ops.fact_inventory_day "
                + "(event_date, source_id, source_system, bank_id, blood_group, component, opening_balance_units, inflow_units, outflow_units, "
                + "closing_balance_units, donation_events_count, withdrawal_events_count, version) VALUES ", factRows);

        log.info("api.exit ClickhouseLoader.loadInventoryDay");
    }

    public void aggregateDonorDay(LocalDate businessDate) {
        log.info("api.enter ClickhouseLoader.aggregateDonorDay");
        String day = businessDate.toString();
        long version = System.currentTimeMillis();

        sql("INSERT INTO blood_ops.fact_donor_day "
                + "(event_date, source_id, source_system, bank_id, blood_group_id, total_donors, eligible_donors, version) "
                + "SELECT "
            + "toDate('" + day + "') AS event_date, "
                + "source_id, "
                + "source_system, "
                + "bank_id, "
                + "blood_group_id, "
                + "toUInt32(sum(donor_count)) AS total_donors, "
                + "toUInt32(sum(eligible_donor_count)) AS eligible_donors, "
                + version + " AS version "
                + "FROM blood_ops.fact_donor_snapshot "
            + "WHERE toDate(snapshot_updated_at) = toDate('" + day + "') "
                + "AND is_deleted = 0 "
                + "GROUP BY source_id, source_system, bank_id, blood_group_id");

        log.info("api.exit ClickhouseLoader.aggregateDonorDay");
    }

    public void recordLoadAudit(
            String batchId,
            String sourceSystem,
            String targetDataset,
            long startedAtMillis,
            long endedAtMillis,
            long rowsRead,
            long rowsWritten,
            String status,
            String message) {
        log.info("api.enter ClickhouseLoader.recordLoadAudit");
        sql("INSERT INTO blood_ops.meta_load_audit "
                + "(batch_id, source_system, target_dataset, started_at, ended_at, rows_read, rows_written, status, message) VALUES ("
                + ClickhouseText.quote(batchId) + ","
                + ClickhouseText.quote(sourceSystem) + ","
                + ClickhouseText.quote(targetDataset) + ","
                + ClickhouseText.dateTimeFromMillis(startedAtMillis) + ","
                + ClickhouseText.dateTimeFromMillis(endedAtMillis) + ","
                + rowsRead + ","
                + rowsWritten + ","
                + ClickhouseText.quote(status) + ","
                + ClickhouseText.quote(message) + ")");

        log.info("api.exit ClickhouseLoader.recordLoadAudit");
    }

    private void sql(String query) {
        clickhouse.post()
                .contentType(Objects.requireNonNull(MediaType.TEXT_PLAIN))
                .body(Objects.requireNonNull(query, "query"))
                .retrieve()
                .body(String.class);
    }

    private void flushInsertValues(String insertPrefix, List<String> valueRows) {
        if (valueRows.isEmpty()) {
            return;
        }
        for (int start = 0; start < valueRows.size(); start += BULK_INSERT_BATCH_SIZE) {
            int end = Math.min(start + BULK_INSERT_BATCH_SIZE, valueRows.size());
            sql(insertPrefix + String.join(",", valueRows.subList(start, end)));
        }
    }

    private String counterTuple(String updatedAt, String source, String apiName, String recordType, int count) {
        LocalDateTime at = TimeUtil.toDateTime(updatedAt).truncatedTo(ChronoUnit.HOURS);
        String eventHour = ClickhouseText.dateTime(at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return "(" + eventHour + "," + ClickhouseText.quote(source) + "," + ClickhouseText.quote(apiName) + ","
            + ClickhouseText.quote(recordType) + "," + count + ")";
    }

    private int sourceId(String source) {
        if (Constants.SOURCE_REDCROSS.equalsIgnoreCase(source)) {
            return 1;
        }
        if (Constants.SOURCE_WHO.equalsIgnoreCase(source)) {
            return 2;
        }
        throw new IllegalArgumentException("Unknown source: " + source);
    }

    private int bloodGroupId(String bloodGroup) {
        String normalized = normalizeBloodGroup(bloodGroup);
        return switch (normalized) {
            case "A+" -> 1;
            case "A-" -> 2;
            case "B+" -> 3;
            case "B-" -> 4;
            case "AB+" -> 5;
            case "AB-" -> 6;
            case "O+" -> 7;
            case "O-" -> 8;
            case "BOMBAY", "HH", "OH" -> 9;
            case "RH NULL", "RHNULL", "RH-NULL" -> 10;
            default -> throw new IllegalArgumentException("Unknown blood group: " + bloodGroup);
        };
    }

    private String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isBlank()) {
            return "UNKNOWN";
        }
        return bloodGroup.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeComponent(String component) {
        if (component == null || component.isBlank()) {
            return "";
        }
        return component.trim().toUpperCase(Locale.ROOT);
    }

    private int componentId(String component) {
        return switch (normalizeComponent(component)) {
            case "WHOLE BLOOD" -> 1;
            case "PACKED RBC" -> 2;
            case "PLATELETS" -> 3;
            case "PLASMA" -> 4;
            case "FRESH FROZEN PLASMA" -> 5;
            case "CRYOPRECIPITATE" -> 6;
            default -> throw new IllegalArgumentException("Unknown component: " + component);
        };
    }

    private boolean isDelete(String op) {
        return Constants.OP_DELETE.equalsIgnoreCase(op);
    }

    private long bankId(String source, String sourceBankId) {
        return stableUInt64("bank:" + source + ":" + sourceBankId);
    }

    private long donorSk(String source, String donorId) {
        return stableUInt64("donor:" + source + ":" + donorId);
    }

    private long locationId(String source, String pincode, String address) {
        return stableUInt64("loc:" + source + ":" + ClickhouseText.blank(pincode) + ":" + ClickhouseText.blank(address));
    }

    private long stableUInt64(String key) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private long versionOf(String updatedAt) {
        return TimeUtil.toDateTime(updatedAt).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private int toEventTimeId(LocalDateTime dateTime) {
        return (int) dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    private int toEventDateId(LocalDateTime dateTime) {
        return Integer.parseInt(dateTime.toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    private boolean eligibleDonor(String lastDonatedOn, LocalDateTime updatedAt) {
        if (lastDonatedOn == null || lastDonatedOn.isBlank()) {
            return true;
        }
        LocalDate last = LocalDate.parse(lastDonatedOn);
        return last.plusDays(90).isBefore(updatedAt.toLocalDate())
                || last.plusDays(90).isEqual(updatedAt.toLocalDate());
    }

    private int age(Integer age) {
        if (age == null) {
            return 0;
        }
        if (age < 0) {
            return 0;
        }
        return Math.min(age, 120);
    }

    private String dateOrDefault(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return "toDate('1970-01-01')";
        }
        return "toDate('" + dateText + "')";
    }

    private String identityHash(String source, String donorId, String phone) {
        return source + ":" + ClickhouseText.blank(donorId) + ":" + ClickhouseText.blank(phone);
    }
}

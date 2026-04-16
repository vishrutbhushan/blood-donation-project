package etl.load.clickhouse;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.InventoryTransaction;
import etl.util.TimeUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClickhouseLoader {
    private final RestClient clickhouse;
    private final Set<String> seededSources = new HashSet<>();
    private final Set<String> seededBloodGroups = new HashSet<>();

    public ClickhouseLoader(RestClient.Builder builder) {
        this.clickhouse = builder
            .baseUrl(Objects.requireNonNull(Constants.CLICKHOUSE_URL, "CLICKHOUSE_URL"))
            .defaultHeader("X-ClickHouse-User", Constants.CLICKHOUSE_USER)
            .defaultHeader("X-ClickHouse-Key", Constants.CLICKHOUSE_PASSWORD)
            .build();
    }

    public void ensureAnalyticsTables() {
        sql("CREATE DATABASE IF NOT EXISTS blood_ops");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_transaction ("
            + "source_transaction_id String,"
            + "source_event_id String,"
            + "source_id UInt8,"
            + "bank_id UInt64,"
            + "donor_sk UInt64,"
            + "blood_group LowCardinality(String),"
            + "component LowCardinality(String),"
            + "transaction_type LowCardinality(String),"
            + "units_delta Int32,"
            + "running_balance_after Int32,"
            + "expiry_date Nullable(Date),"
            + "event_time DateTime,"
            + "is_deleted UInt8 DEFAULT 0,"
            + "ingested_at DateTime DEFAULT now(),"
            + "version UInt64"
            + ") ENGINE = ReplacingMergeTree(version) "
            + "PARTITION BY toYYYYMM(toDate(event_time)) "
            + "ORDER BY (source_id, source_transaction_id)");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_day ("
            + "event_date Date,"
            + "source_id UInt8,"
            + "bank_id UInt64,"
            + "blood_group LowCardinality(String),"
            + "component LowCardinality(String),"
            + "opening_balance_units Int32,"
            + "inflow_units Int32,"
            + "outflow_units Int32,"
            + "adjustment_units Int32,"
            + "closing_balance_units Int32,"
            + "donation_events_count UInt32,"
            + "withdrawal_events_count UInt32,"
            + "updated_at DateTime DEFAULT now(),"
            + "version UInt64"
            + ") ENGINE = ReplacingMergeTree(version) "
            + "PARTITION BY toYYYYMM(event_date) "
            + "ORDER BY (event_date, source_id, bank_id, blood_group, component)");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.fact_donor_day ("
            + "event_date Date,"
            + "source_id UInt8,"
            + "bank_id UInt64,"
            + "blood_group_id UInt8,"
            + "total_donors UInt32,"
            + "eligible_donors UInt32,"
            + "updated_at DateTime DEFAULT now(),"
            + "version UInt64"
            + ") ENGINE = ReplacingMergeTree(version) "
            + "PARTITION BY toYYYYMM(event_date) "
            + "ORDER BY (event_date, source_id, bank_id, blood_group_id)");
    }

    public void loadBanks(List<BloodBank> banks) {
        if (banks == null || banks.isEmpty()) {
            return;
        }
        for (BloodBank b : banks) {
            int sourceId = sourceId(b.getSource());
            if (sourceId == 0) {
                continue;
            }
            seedSource(sourceId, b.getSource());

            long locationId = locationId(b.getSource(), b.getPincode(), b.getAddress());
            long bankId = bankId(b.getSource(), b.getBankId());
            long version = versionOf(b.getUpdatedAt());
            int isDeleted = isDelete(b.getOp()) ? 1 : 0;

            sql("INSERT INTO blood_ops.dim_location "
                + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ("
                + locationId + ","
                + q(b.getPincode()) + ","
                + q(b.getCity()) + ","
                + q(b.getState()) + ","
                + q(b.getAddress()) + ","
                + (b.getLat() != null ? b.getLat() : "0.0") + ","
                + (b.getLon() != null ? b.getLon() : "0.0") + ","
                + dt(b.getUpdatedAt()) + ");");

            sql("INSERT INTO blood_ops.dim_blood_bank "
                + "(bank_id, source_id, source_bank_id, bank_name, category, phone, email, location_id, created_at, updated_at, is_deleted, version) VALUES ("
                + bankId + ","
                + sourceId + ","
                + q(b.getBankId()) + ","
                + q(b.getBankName()) + ","
                + q(b.getCategory()) + ","
                + q(b.getPhone()) + ","
                + q(b.getEmail()) + ","
                + locationId + ","
                + dt(orElse(b.getCreatedAt(), b.getUpdatedAt())) + ","
                + dt(b.getUpdatedAt()) + ","
                + isDeleted + ","
                + version + ")");

            ingestCounter(b.getUpdatedAt(), b.getSource(), "incremental", "bank", 1);
        }
    }

    public void loadDonors(List<Donor> donors) {
        if (donors == null || donors.isEmpty()) {
            return;
        }
        for (Donor d : donors) {
            int sourceId = sourceId(d.getSource());
            if (sourceId == 0) {
                continue;
            }
            seedSource(sourceId, d.getSource());

            int bloodGroupId = bloodGroupId(d.getBloodGroup());
            seedBloodGroup(bloodGroupId, d.getBloodGroup());

            long locationId = locationId(d.getSource(), d.getPincodeCurrent(), d.getAddressCurrent());
            long donorSk = donorSk(d.getSource(), d.getDonorId());
            long bankId = bankId(d.getSource(), d.getBankId());
            LocalDateTime updatedAt = TimeUtil.toDateTime(d.getUpdatedAt());
            long version = updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
            int isDeleted = isDelete(d.getOp()) ? 1 : 0;

            sql("INSERT INTO blood_ops.dim_location "
                + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ("
                + locationId + ","
                + q(d.getPincodeCurrent()) + ","
                + q(d.getCityCurrent()) + ","
                + q(d.getStateCurrent()) + ","
                + q(d.getAddressCurrent()) + ","
                + (d.getLat() != null ? d.getLat() : "0.0") + ","
                + (d.getLon() != null ? d.getLon() : "0.0") + ","
                + dt(d.getUpdatedAt()) + ")");

            sql("INSERT INTO blood_ops.dim_donor "
                + "(donor_sk, source_id, source_donor_id, bank_id, donor_name, donor_identity_hash, phone, location_id, blood_group_id, age, last_donated_date, created_at, updated_at, is_deleted, version) VALUES ("
                + donorSk + ","
                + sourceId + ","
                + q(d.getDonorId()) + ","
                + bankId + ","
                + q(d.getName()) + ","
                + q(identityHash(d.getSource(), d.getDonorId(), d.getPhone())) + ","
                + q(d.getPhone()) + ","
                + locationId + ","
                + bloodGroupId + ","
                + age(d.getAge()) + ","
                + dateOrDefault(d.getLastDonatedOn()) + ","
                + dt(d.getUpdatedAt()) + ","
                + dt(d.getUpdatedAt()) + ","
                + isDeleted + ","
                + version + ")");

            int eligible = eligibleDonor(d.getLastDonatedOn(), updatedAt) ? 1 : 0;
            int donorCount = isDeleted == 1 ? 0 : 1;

            sql("INSERT INTO blood_ops.fact_donor_snapshot "
                + "(donor_fact_id, source_id, donor_sk, bank_id, location_id, blood_group_id, event_time_id, event_date_id, age, donor_count, eligible_donor_count, is_deleted, last_donated_date, snapshot_updated_at, version) VALUES ("
                + stableUInt64("fact-donor:" + d.getSource() + ":" + d.getDonorId() + ":" + version) + ","
                + sourceId + ","
                + donorSk + ","
                + bankId + ","
                + locationId + ","
                + bloodGroupId + ","
                + toEventTimeId(updatedAt) + ","
                + toEventDateId(updatedAt) + ","
                + age(d.getAge()) + ","
                + donorCount + ","
                + eligible + ","
                + isDeleted + ","
                + dateOrDefault(d.getLastDonatedOn()) + ","
                + dt(d.getUpdatedAt()) + ","
                + version + ")");

            ingestCounter(d.getUpdatedAt(), d.getSource(), "incremental", "donor", 1);
        }
    }

    public void loadInventoryTransactions(List<InventoryTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        for (InventoryTransaction t : transactions) {
            int sourceId = sourceId(t.getSource());
            if (sourceId == 0) {
                continue;
            }
            seedSource(sourceId, t.getSource());

            long bankId = bankId(t.getSource(), t.getBankId());
            long donorSk = t.getDonorId() == null || t.getDonorId().isBlank() ? 0 : donorSk(t.getSource(), t.getDonorId());
            int unitsDelta = t.getUnitsDelta() == null ? 0 : t.getUnitsDelta();
            int runningBalance = t.getRunningBalanceAfter() == null ? 0 : t.getRunningBalanceAfter();
            long version = TimeUtil.toDateTime(orElse(t.getUpdatedAt(), t.getEventTimestamp())).toInstant(ZoneOffset.UTC).toEpochMilli();
            int isDeleted = isDelete(t.getOp()) ? 1 : 0;

            sql("INSERT INTO blood_ops.fact_inventory_transaction "
                + "(source_transaction_id, source_event_id, source_id, bank_id, donor_sk, blood_group, component, "
                + "transaction_type, units_delta, running_balance_after, expiry_date, event_time, is_deleted, version) VALUES ("
                + q(t.getTransactionId()) + ","
                + q(t.getSourceEventId()) + ","
                + sourceId + ","
                + bankId + ","
                + donorSk + ","
                + q(normalizeBloodGroup(t.getBloodGroup())) + ","
                + q(t.getComponent()) + ","
                + q(t.getTransactionType()) + ","
                + unitsDelta + ","
                + runningBalance + ","
                + nullableDate(t.getExpiryDate()) + ","
                + dt(t.getEventTimestamp()) + ","
                + isDeleted + ","
                + version + ")");

            ingestCounter(orElse(t.getUpdatedAt(), t.getEventTimestamp()), t.getSource(), "incremental", "inventory_transaction", 1);
        }
    }

    public void aggregateInventoryDay(LocalDate businessDate) {
        String day = businessDate.toString();
        long version = System.currentTimeMillis();

        sql("INSERT INTO blood_ops.fact_inventory_day "
            + "(event_date, source_id, bank_id, blood_group, component, opening_balance_units, inflow_units, outflow_units, "
            + "adjustment_units, closing_balance_units, donation_events_count, withdrawal_events_count, version) "
            + "SELECT "
            + "toDate('" + esc(day) + "') AS event_date, "
            + "source_id, "
            + "bank_id, "
            + "blood_group, "
            + "component, "
            + "toInt32(argMin(running_balance_after - units_delta, event_time)) AS opening_balance_units, "
            + "toInt32(sumIf(units_delta, transaction_type = 'INFLOW' AND units_delta > 0)) AS inflow_units, "
            + "toInt32(abs(sumIf(units_delta, transaction_type = 'OUTFLOW' AND units_delta < 0))) AS outflow_units, "
            + "toInt32(sumIf(units_delta, transaction_type = 'ADJUSTMENT')) AS adjustment_units, "
            + "toInt32(argMax(running_balance_after, event_time)) AS closing_balance_units, "
            + "toUInt32(countIf(transaction_type = 'INFLOW')) AS donation_events_count, "
            + "toUInt32(countIf(transaction_type = 'OUTFLOW')) AS withdrawal_events_count, "
            + version + " AS version "
            + "FROM blood_ops.fact_inventory_transaction "
            + "WHERE toDate(event_time) = toDate('" + esc(day) + "') "
            + "AND is_deleted = 0 "
            + "GROUP BY source_id, bank_id, blood_group, component");
    }

    public void aggregateDonorDay(LocalDate businessDate) {
        String day = businessDate.toString();
        long version = System.currentTimeMillis();

        sql("INSERT INTO blood_ops.fact_donor_day "
            + "(event_date, source_id, bank_id, blood_group_id, total_donors, eligible_donors, version) "
            + "SELECT "
            + "toDate('" + esc(day) + "') AS event_date, "
            + "source_id, "
            + "bank_id, "
            + "blood_group_id, "
            + "toUInt32(sum(donor_count)) AS total_donors, "
            + "toUInt32(sum(eligible_donor_count)) AS eligible_donors, "
            + version + " AS version "
            + "FROM blood_ops.fact_donor_snapshot "
            + "WHERE toDate(snapshot_updated_at) = toDate('" + esc(day) + "') "
            + "AND is_deleted = 0 "
            + "GROUP BY source_id, bank_id, blood_group_id");
    }

    private void sql(String query) {
        clickhouse.post()
            .contentType(Objects.requireNonNull(MediaType.TEXT_PLAIN))
            .body(Objects.requireNonNull(query, "query"))
            .retrieve()
            .body(String.class);
    }

    private void seedSource(int sourceId, String sourceCode) {
        String key = sourceId + ":" + sourceCode;
        if (!seededSources.add(key)) {
            return;
        }
        sql("INSERT INTO blood_ops.dim_source (source_id, source_code, source_name, is_active, updated_at) VALUES ("
            + sourceId + ","
            + q(sourceCode) + ","
            + q(sourceCode == null ? "unknown" : sourceCode.toUpperCase(Locale.ROOT)) + ",1,"
            + "now())");
    }

    private void seedBloodGroup(int id, String bloodGroup) {
        String key = id + ":" + bloodGroup;
        if (!seededBloodGroups.add(key)) {
            return;
        }
        sql("INSERT INTO blood_ops.dim_blood_group (blood_group_id, blood_group, updated_at) VALUES ("
            + id + ","
            + q(normalizeBloodGroup(bloodGroup)) + ","
            + "now())");
    }

    private void ingestCounter(String updatedAt, String source, String apiName, String recordType, int count) {
        LocalDateTime at = TimeUtil.toDateTime(updatedAt).truncatedTo(ChronoUnit.HOURS);
        sql("INSERT INTO blood_ops.source_ingestion_hourly_agg "
            + "(event_hour, source, api_name, record_type, record_count) VALUES ("
            + dt(at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) + ","
            + q(source) + ","
            + q(apiName) + ","
            + q(recordType) + ","
            + count + ")");
    }

    private int sourceId(String source) {
        if (Constants.SOURCE_REDCROSS.equalsIgnoreCase(source)) {
            return 1;
        }
        if (Constants.SOURCE_WHO.equalsIgnoreCase(source)) {
            return 2;
        }
        return 0;
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
            default -> 0;
        };
    }

    private String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isBlank()) {
            return "UNKNOWN";
        }
        return bloodGroup.trim().toUpperCase(Locale.ROOT);
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
        return stableUInt64("loc:" + source + ":" + orBlank(pincode) + ":" + orBlank(address));
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
        return last.plusDays(90).isBefore(updatedAt.toLocalDate()) || last.plusDays(90).isEqual(updatedAt.toLocalDate());
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
        return "toDate('" + esc(dateText) + "')";
    }

    private String nullableDate(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return "NULL";
        }
        return "toDate('" + esc(dateText) + "')";
    }

    private String identityHash(String source, String donorId, String phone) {
        return source + ":" + orBlank(donorId) + ":" + orBlank(phone);
    }

    private String dt(String dateTime) {
        return "parseDateTimeBestEffort('" + esc(dateTime) + "')";
    }

    private String q(String text) {
        return "'" + esc(text) + "'";
    }

    private String esc(String text) {
        return orBlank(text).replace("'", "''");
    }

    private String orBlank(String text) {
        return text == null ? "" : text;
    }

    private String orElse(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}

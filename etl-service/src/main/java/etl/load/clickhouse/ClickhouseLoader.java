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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<String> seededSources = ConcurrentHashMap.newKeySet();
    private final Set<String> seededBloodGroups = ConcurrentHashMap.newKeySet();
    private final Set<String> seededComponents = ConcurrentHashMap.newKeySet();

    public ClickhouseLoader(RestClient.Builder builder) {
        this.clickhouse = builder
            .baseUrl(Objects.requireNonNull(Constants.CLICKHOUSE_URL, "CLICKHOUSE_URL"))
            .defaultHeader("X-ClickHouse-User", Constants.CLICKHOUSE_USER)
            .defaultHeader("X-ClickHouse-Key", Constants.CLICKHOUSE_PASSWORD)
            .build();
    }

    public void ensureAnalyticsTables() {
        log.info("api.enter ClickhouseLoader.ensureAnalyticsTables");
        sql("CREATE DATABASE IF NOT EXISTS blood_ops");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_day ("
            + "event_date Date,"
            + "source_id UInt8,"
            + "source_system LowCardinality(String),"
            + "bank_id UInt64,"
            + "blood_group LowCardinality(String),"
            + "component LowCardinality(String),"
            + "opening_balance_units Int32,"
            + "inflow_units Int32,"
            + "outflow_units Int32,"
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
            + "source_system LowCardinality(String),"
            + "bank_id UInt64,"
            + "blood_group_id UInt8,"
            + "total_donors UInt32,"
            + "eligible_donors UInt32,"
            + "updated_at DateTime DEFAULT now(),"
            + "version UInt64"
            + ") ENGINE = ReplacingMergeTree(version) "
            + "PARTITION BY toYYYYMM(event_date) "
            + "ORDER BY (event_date, source_id, bank_id, blood_group_id)");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.meta_dataset ("
            + "dataset_name LowCardinality(String),"
            + "physical_table String,"
            + "source_system LowCardinality(String),"
            + "is_active UInt8 DEFAULT 1,"
            + "created_at DateTime DEFAULT now()"
            + ") ENGINE = ReplacingMergeTree(created_at) "
            + "ORDER BY dataset_name");

        sql("CREATE TABLE IF NOT EXISTS blood_ops.meta_load_audit ("
            + "batch_id String,"
            + "source_system LowCardinality(String),"
            + "target_dataset String,"
            + "started_at DateTime,"
            + "ended_at DateTime,"
            + "rows_read UInt64,"
            + "rows_written UInt64,"
            + "status LowCardinality(String),"
            + "message String,"
            + "created_at DateTime DEFAULT now()"
            + ") ENGINE = MergeTree "
            + "PARTITION BY toYYYYMM(started_at) "
            + "ORDER BY (started_at, source_system, target_dataset, batch_id)");

        sql("ALTER TABLE blood_ops.meta_dataset MODIFY COLUMN created_at DateTime('Asia/Kolkata')");
        sql("ALTER TABLE blood_ops.meta_load_audit MODIFY COLUMN started_at DateTime('Asia/Kolkata')");
        sql("ALTER TABLE blood_ops.meta_load_audit MODIFY COLUMN ended_at DateTime('Asia/Kolkata')");
        sql("ALTER TABLE blood_ops.meta_load_audit MODIFY COLUMN created_at DateTime('Asia/Kolkata')");

        seedMetadataCatalog();
        seedDateAndTimeDimensions();
        backfillComponentDimensionFromFacts();
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
            if (sourceId == 0) {
                continue;
            }
            seedSource(sourceId, b.getSource());

            long locationId = locationId(b.getSource(), b.getPincode(), b.getAddress());
            long bankId = bankId(b.getSource(), b.getBankId());
            long version = versionOf(b.getUpdatedAt());
            int isDeleted = isDelete(b.getOp()) ? 1 : 0;

            locationRows.add("(" + locationId + "," + q(b.getPincode()) + "," + q(b.getCity()) + "," + q(b.getState()) + "," + q(b.getAddress()) + "," + (b.getLat() != null ? b.getLat() : "0.0") + "," + (b.getLon() != null ? b.getLon() : "0.0") + "," + dt(b.getUpdatedAt()) + ")");
            bankRows.add("(" + bankId + "," + sourceId + "," + q(b.getBankId()) + "," + q(b.getBankName()) + "," + q(b.getCategory()) + "," + q(b.getPhone()) + "," + q(b.getEmail()) + "," + locationId + "," + dt(orElse(b.getCreatedAt(), b.getUpdatedAt())) + "," + dt(b.getUpdatedAt()) + "," + isDeleted + "," + version + ")");
            counterRows.add(counterTuple(b.getUpdatedAt(), b.getSource(), "incremental", "bank", 1));
        }

        flushInsertValues("INSERT INTO blood_ops.dim_location "
            + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ", locationRows);
        flushInsertValues("INSERT INTO blood_ops.dim_blood_bank "
            + "(bank_id, source_id, source_bank_id, bank_name, category, phone, email, location_id, created_at, updated_at, is_deleted, version) VALUES ", bankRows);
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

            locationRows.add("(" + locationId + "," + q(d.getPincodeCurrent()) + "," + q(d.getCityCurrent()) + "," + q(d.getStateCurrent()) + "," + q(d.getAddressCurrent()) + "," + (d.getLat() != null ? d.getLat() : "0.0") + "," + (d.getLon() != null ? d.getLon() : "0.0") + "," + dt(d.getUpdatedAt()) + ")");
            donorRows.add("(" + donorSk + "," + sourceId + "," + q(d.getDonorId()) + "," + bankId + "," + q(d.getName()) + "," + q(identityHash(d.getSource(), d.getDonorId(), d.getPhone())) + "," + q(d.getPhone()) + "," + locationId + "," + bloodGroupId + "," + age(d.getAge()) + "," + dateOrDefault(d.getLastDonatedOn()) + "," + dt(d.getUpdatedAt()) + "," + dt(d.getUpdatedAt()) + "," + isDeleted + "," + version + ")");

            int eligible = eligibleDonor(d.getLastDonatedOn(), updatedAt) ? 1 : 0;
            int donorCount = isDeleted == 1 ? 0 : 1;
            snapshotRows.add("(" + stableUInt64("fact-donor:" + d.getSource() + ":" + d.getDonorId() + ":" + version) + "," + sourceId + "," + q(d.getSource()) + "," + donorSk + "," + bankId + "," + locationId + "," + bloodGroupId + "," + toEventTimeId(updatedAt) + "," + toEventDateId(updatedAt) + "," + age(d.getAge()) + "," + donorCount + "," + eligible + "," + isDeleted + "," + dateOrDefault(d.getLastDonatedOn()) + "," + dt(d.getUpdatedAt()) + "," + version + ")");
            counterRows.add(counterTuple(d.getUpdatedAt(), d.getSource(), "incremental", "donor", 1));
        }

        flushInsertValues("INSERT INTO blood_ops.dim_location "
            + "(location_id, pincode, city, state, street_or_address, latitude, longitude, updated_at) VALUES ", locationRows);
        flushInsertValues("INSERT INTO blood_ops.dim_donor "
            + "(donor_sk, source_id, source_donor_id, bank_id, donor_name, donor_identity_hash, phone, location_id, blood_group_id, age, last_donated_date, created_at, updated_at, is_deleted, version) VALUES ", donorRows);
        flushInsertValues("INSERT INTO blood_ops.fact_donor_snapshot "
            + "(donor_fact_id, source_id, source_system, donor_sk, bank_id, location_id, blood_group_id, event_time_id, event_date_id, age, donor_count, eligible_donor_count, is_deleted, last_donated_date, snapshot_updated_at, version) VALUES ", snapshotRows);
        flushInsertValues("INSERT INTO blood_ops.source_ingestion_hourly_agg "
            + "(event_hour, source, api_name, record_type, record_count) VALUES ", counterRows);

        log.info("api.exit ClickhouseLoader.loadDonors");
    }

    public void loadInventoryDay(LocalDate businessDate, List<InventoryTransaction> transactions) {
        log.info("api.enter ClickhouseLoader.loadInventoryDay");
        String day = businessDate.toString();
        sql("ALTER TABLE blood_ops.fact_inventory_day DELETE WHERE event_date = toDate('" + esc(day) + "')");

        if (transactions == null || transactions.isEmpty()) {
            log.info("api.exit ClickhouseLoader.loadInventoryDay");
            return;
        }

        Map<String, List<InventoryTransaction>> grouped = new HashMap<>();
        for (InventoryTransaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            if (transaction.getSource() == null || transaction.getBankId() == null || transaction.getBloodGroup() == null || transaction.getComponent() == null) {
                continue;
            }
            LocalDate eventDay = TimeUtil.toDateTime(transaction.getEventTimestamp()).toLocalDate();
            if (!businessDate.equals(eventDay)) {
                continue;
            }
            String key = transaction.getSource() + ":" + transaction.getBankId() + ":" + transaction.getBloodGroup() + ":" + transaction.getComponent();
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
                String type = txn.getTransactionType() == null ? "" : txn.getTransactionType().trim().toUpperCase(Locale.ROOT);
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
            if (sourceId == 0) {
                continue;
            }
            seedSource(sourceId, first.getSource());
            seedComponent(first.getComponent());
            long bankId = bankId(first.getSource(), first.getBankId());
            factRows.add("(toDate('" + esc(day) + "')," + sourceId + "," + q(first.getSource()) + "," + bankId + "," + q(normalizeBloodGroup(first.getBloodGroup())) + "," + q(first.getComponent()) + "," + openingBalanceUnits + "," + inflowUnits + "," + outflowUnits + "," + lastBalance + "," + donationEvents + "," + withdrawalEvents + "," + version + ")");
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

        sql("ALTER TABLE blood_ops.fact_donor_day DELETE WHERE event_date = toDate('" + esc(day) + "')");

        sql("INSERT INTO blood_ops.fact_donor_day "
            + "(event_date, source_id, source_system, bank_id, blood_group_id, total_donors, eligible_donors, version) "
            + "SELECT "
            + "toDate('" + esc(day) + "') AS event_date, "
            + "source_id, "
            + "source_system, "
            + "bank_id, "
            + "blood_group_id, "
            + "toUInt32(sum(donor_count)) AS total_donors, "
            + "toUInt32(sum(eligible_donor_count)) AS eligible_donors, "
            + version + " AS version "
            + "FROM blood_ops.fact_donor_snapshot "
            + "WHERE toDate(snapshot_updated_at) = toDate('" + esc(day) + "') "
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
            + q(batchId) + ","
            + q(sourceSystem) + ","
            + q(targetDataset) + ","
            + dtFromMillis(startedAtMillis) + ","
            + dtFromMillis(endedAtMillis) + ","
            + rowsRead + ","
            + rowsWritten + ","
            + q(status) + ","
            + q(message) + ")");

        log.info("api.exit ClickhouseLoader.recordLoadAudit");
    }

    private void seedMetadataCatalog() {
        if (scalarLong("SELECT count() FROM blood_ops.meta_dataset") > 0) {
            return;
        }

        seedDatasets();
    }

    private void seedDatasets() {
        sql("INSERT INTO blood_ops.meta_dataset (dataset_name, physical_table, source_system, is_active, created_at) VALUES "
            + "('redcross_blood_bank', 'redcross_db.blood_bank', 'redcross', 1, now()),"
            + "('redcross_blood_donor', 'redcross_db.donor', 'redcross', 1, now()),"
            + "('who_blood_bank', 'who_db.blood_bank', 'who', 1, now()),"
            + "('who_blood_donor', 'who_db.donor', 'who', 1, now()),"
            + "('dim_source', 'blood_ops.dim_source', 'etl', 1, now()),"
            + "('dim_location', 'blood_ops.dim_location', 'etl', 1, now()),"
            + "('dim_blood_bank', 'blood_ops.dim_blood_bank', 'etl', 1, now()),"
            + "('dim_donor', 'blood_ops.dim_donor', 'etl', 1, now()),"
            + "('fact_inventory_day', 'blood_ops.fact_inventory_day', 'etl', 1, now()),"
            + "('fact_donor_snapshot', 'blood_ops.fact_donor_snapshot', 'etl', 1, now()),"
            + "('fact_donor_day', 'blood_ops.fact_donor_day', 'etl', 1, now()),"
            + "('fact_ingestion_event', 'blood_ops.fact_ingestion_event', 'etl', 1, now()),"
            + "('source_ingestion_hourly_agg', 'blood_ops.source_ingestion_hourly_agg', 'etl', 1, now())");
    }

    private long scalarLong(String query) {
        String result = clickhouse.post()
            .contentType(Objects.requireNonNull(MediaType.TEXT_PLAIN))
            .body(Objects.requireNonNull(query, "query"))
            .retrieve()
            .body(String.class);
        if (result == null || result.isBlank()) {
            return 0L;
        }
        String firstLine = result.trim().split("\\R")[0].trim();
        if (firstLine.isBlank()) {
            return 0L;
        }
        return Long.parseLong(firstLine);
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
        String eventHour = dt(at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return "(" + eventHour + "," + q(source) + "," + q(apiName) + "," + q(recordType) + "," + count + ")";
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

    private void seedComponent(String component) {
        String normalized = normalizeComponent(component);
        if (normalized.isBlank()) {
            return;
        }
        if (!seededComponents.add(normalized)) {
            return;
        }
        sql("INSERT INTO blood_ops.dim_component (component_id, component_name, updated_at) VALUES ("
            + componentId(normalized) + ","
            + q(normalized) + ","
            + "now())");
    }

    private void backfillComponentDimensionFromFacts() {
        sql("INSERT INTO blood_ops.dim_component (component_id, component_name, updated_at) "
            + "SELECT "
            + "multiIf("
            + " upperUTF8(component) = 'WHOLE BLOOD', toUInt8(1),"
            + " upperUTF8(component) = 'PACKED RBC', toUInt8(2),"
            + " upperUTF8(component) = 'PLATELETS', toUInt8(3),"
            + " upperUTF8(component) = 'PLASMA', toUInt8(4),"
            + " upperUTF8(component) = 'FRESH FROZEN PLASMA', toUInt8(5),"
            + " upperUTF8(component) = 'CRYOPRECIPITATE', toUInt8(6),"
            + " toUInt8(250)"
            + ") AS component_id,"
            + " component AS component_name,"
            + " now() AS updated_at "
            + "FROM (SELECT DISTINCT component FROM blood_ops.fact_inventory_day WHERE component != '') "
            + "WHERE upperUTF8(component) NOT IN (SELECT upperUTF8(component_name) FROM blood_ops.dim_component)");
    }

    private void seedDateAndTimeDimensions() {
        if (scalarLong("SELECT count() FROM blood_ops.dim_date") == 0) {
            sql("INSERT INTO blood_ops.dim_date (date_id, dt, year, quarter, month, day, iso_week) "
                + "SELECT "
                + "toUInt32(formatDateTime(d, '%Y%m%d')) AS date_id,"
                + "d AS dt,"
                + "toUInt16(toYear(d)) AS year,"
                + "toUInt8(toQuarter(d)) AS quarter,"
                + "toUInt8(toMonth(d)) AS month,"
                + "toUInt8(toDayOfMonth(d)) AS day,"
                + "toUInt8(toWeek(d, 1)) AS iso_week "
                + "FROM ("
                + " SELECT addDays(toDate('2015-01-01'), number) AS d FROM numbers(7670)"
                + ")");
        }

        if (scalarLong("SELECT count() FROM blood_ops.dim_time") == 0) {
            sql("INSERT INTO blood_ops.dim_time (time_id, event_time, event_date_id, hour, minute) "
                + "SELECT "
                + "toUInt32(intDiv(number, 60) * 100 + (number % 60)) AS time_id,"
                + "toDateTime('1970-01-01 00:00:00') + toIntervalMinute(number) AS event_time,"
                + "toUInt32(19700101) AS event_date_id,"
                + "toUInt8(intDiv(number, 60)) AS hour,"
                + "toUInt8(number % 60) AS minute "
                + "FROM numbers(1440)");
        }
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
            default -> 250;
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

    private String identityHash(String source, String donorId, String phone) {
        return source + ":" + orBlank(donorId) + ":" + orBlank(phone);
    }

    private String dt(String dateTime) {
        return "parseDateTimeBestEffort('" + esc(dateTime) + "')";
    }

    private String dtFromMillis(long millis) {
        return dt(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.of("Asia/Kolkata")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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

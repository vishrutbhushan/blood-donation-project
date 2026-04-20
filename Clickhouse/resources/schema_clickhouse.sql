CREATE DATABASE IF NOT EXISTS blood_ops;

/*
Star schema aligned to source-of-truth systems:
- Redcross: blood_bank, blood_inventory_transaction, blood_donor
- WHO: blood_bank, blood_inventory_transaction, blood_donor
*/

DROP VIEW IF EXISTS blood_ops.mv_ingestion_hourly_to_fact;
DROP VIEW IF EXISTS blood_ops.mv_inventory_1w_day;
DROP VIEW IF EXISTS blood_ops.mv_inventory_2w_day_source;
DROP VIEW IF EXISTS blood_ops.mv_inventory_3w_day_source_bank;
DROP VIEW IF EXISTS blood_ops.mv_donor_1w_day;
DROP VIEW IF EXISTS blood_ops.mv_donor_2w_day_source;
DROP VIEW IF EXISTS blood_ops.mv_donor_3w_day_source_bank;

DROP TABLE IF EXISTS blood_ops.fact_ingestion_event;
DROP TABLE IF EXISTS blood_ops.source_ingestion_hourly_agg;
DROP TABLE IF EXISTS blood_ops.agg_inventory_1w_day;
DROP TABLE IF EXISTS blood_ops.agg_inventory_2w_day_source;
DROP TABLE IF EXISTS blood_ops.agg_inventory_3w_day_source_bank;
DROP TABLE IF EXISTS blood_ops.agg_donor_1w_day;
DROP TABLE IF EXISTS blood_ops.agg_donor_2w_day_source;
DROP TABLE IF EXISTS blood_ops.agg_donor_3w_day_source_bank;
DROP TABLE IF EXISTS blood_ops.fact_donor_snapshot;
DROP TABLE IF EXISTS blood_ops.fact_inventory_day;
DROP TABLE IF EXISTS blood_ops.fact_inventory_transaction;
DROP TABLE IF EXISTS blood_ops.fact_donor_day;
DROP TABLE IF EXISTS blood_ops.meta_load_audit;
DROP TABLE IF EXISTS blood_ops.meta_lineage;
DROP TABLE IF EXISTS blood_ops.meta_column;
DROP TABLE IF EXISTS blood_ops.meta_dataset;
DROP TABLE IF EXISTS blood_ops.meta_source_system;

DROP TABLE IF EXISTS blood_ops.dim_time;
DROP TABLE IF EXISTS blood_ops.dim_date;
DROP TABLE IF EXISTS blood_ops.dim_donor;
DROP TABLE IF EXISTS blood_ops.dim_blood_bank;
DROP TABLE IF EXISTS blood_ops.dim_location;
DROP TABLE IF EXISTS blood_ops.dim_component;
DROP TABLE IF EXISTS blood_ops.dim_blood_group;
DROP TABLE IF EXISTS blood_ops.dim_source;

CREATE TABLE IF NOT EXISTS blood_ops.dim_source (
    source_id UInt8,
    source_code LowCardinality(String),
    source_name String,
    is_active UInt8 DEFAULT 1,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY source_id;

CREATE TABLE IF NOT EXISTS blood_ops.dim_blood_group (
    blood_group_id UInt8,
    blood_group LowCardinality(String),
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY blood_group_id;

CREATE TABLE IF NOT EXISTS blood_ops.dim_component (
    component_id UInt8,
    component_name LowCardinality(String),
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY component_id;

CREATE TABLE IF NOT EXISTS blood_ops.dim_location (
    location_id UInt64,
    pincode String,
    city String,
    state String,
    street_or_address String,
    latitude Float64,
    longitude Float64,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY location_id;

CREATE TABLE IF NOT EXISTS blood_ops.dim_blood_bank (
    bank_id UInt64,
    source_id UInt8,
    source_bank_id String,
    bank_name String,
    category LowCardinality(String),
    phone String,
    email String,
    location_id UInt64,
    created_at DateTime,
    updated_at DateTime,
    is_deleted UInt8 DEFAULT 0,
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
ORDER BY (source_id, source_bank_id);

CREATE TABLE IF NOT EXISTS blood_ops.dim_donor (
    donor_sk UInt64,
    source_id UInt8,
    source_donor_id String,
    bank_id UInt64,
    donor_name String,
    donor_identity_hash String,
    phone String,
    location_id UInt64,
    blood_group_id UInt8,
    age UInt8,
    last_donated_date Date,
    created_at DateTime,
    updated_at DateTime,
    is_deleted UInt8 DEFAULT 0,
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
ORDER BY (source_id, source_donor_id);

CREATE TABLE IF NOT EXISTS blood_ops.dim_date (
    date_id UInt32,
    dt Date,
    year UInt16,
    quarter UInt8,
    month UInt8,
    day UInt8,
    iso_week UInt8
)
ENGINE = MergeTree
ORDER BY date_id;

CREATE TABLE IF NOT EXISTS blood_ops.dim_time (
    time_id UInt32,
    event_time DateTime,
    event_date_id UInt32,
    hour UInt8,
    minute UInt8
)
ENGINE = MergeTree
ORDER BY time_id;

CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_transaction (
    source_transaction_id String,
    source_event_id String,
    source_id UInt8,
    bank_id UInt64,
    donor_sk UInt64,
    blood_group LowCardinality(String),
    component LowCardinality(String),
    transaction_type LowCardinality(String),
    units_delta Int32,
    running_balance_after Int32,
    expiry_date Nullable(Date),
    event_time DateTime,
    is_deleted UInt8 DEFAULT 0,
    ingested_at DateTime DEFAULT now(),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
PARTITION BY toYYYYMM(toDate(event_time))
ORDER BY (source_id, source_transaction_id);

CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_day (
    event_date Date,
    source_id UInt8,
    bank_id UInt64,
    blood_group LowCardinality(String),
    component LowCardinality(String),
    opening_balance_units Int32,
    inflow_units Int32,
    outflow_units Int32,
    adjustment_units Int32,
    closing_balance_units Int32,
    donation_events_count UInt32,
    withdrawal_events_count UInt32,
    updated_at DateTime DEFAULT now(),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id, bank_id, blood_group, component);

CREATE TABLE IF NOT EXISTS blood_ops.fact_donor_snapshot (
    donor_fact_id UInt64,
    source_id UInt8,
    donor_sk UInt64,
    bank_id UInt64,
    location_id UInt64,
    blood_group_id UInt8,
    event_time_id UInt32,
    event_date_id UInt32,
    age UInt8,
    donor_count UInt8 DEFAULT 1,
    eligible_donor_count UInt8,
    is_deleted UInt8 DEFAULT 0,
    last_donated_date Date,
    snapshot_updated_at DateTime,
    ingested_at DateTime DEFAULT now(),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
PARTITION BY toYYYYMM(toDate(snapshot_updated_at))
ORDER BY (event_date_id, source_id, bank_id, blood_group_id, donor_sk, donor_fact_id);

CREATE TABLE IF NOT EXISTS blood_ops.fact_donor_day (
    event_date Date,
    source_id UInt8,
    bank_id UInt64,
    blood_group_id UInt8,
    total_donors UInt32,
    eligible_donors UInt32,
    updated_at DateTime DEFAULT now(),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id, bank_id, blood_group_id);

CREATE TABLE IF NOT EXISTS blood_ops.fact_ingestion_event (
    event_time_id UInt32,
    event_date_id UInt32,
    source_id UInt8,
    api_name LowCardinality(String),
    record_type LowCardinality(String),
    success_count UInt64,
    error_count UInt64,
    total_count UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(toDateTime(event_time_id))
ORDER BY (event_date_id, source_id, api_name, record_type);

-- Hourly aggregation table for ingestion events (source for materialized view)
CREATE TABLE IF NOT EXISTS blood_ops.source_ingestion_hourly_agg (
    event_hour DateTime,
    source LowCardinality(String),
    api_name LowCardinality(String),
    record_type LowCardinality(String),
    record_count UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(toDate(event_hour))
ORDER BY (event_hour, source, api_name, record_type);

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_ingestion_hourly_to_fact
TO blood_ops.fact_ingestion_event
AS
SELECT
    toUInt32(toUnixTimestamp(event_hour)) AS event_time_id,
    toUInt32(toYYYYMMDD(toDate(event_hour))) AS event_date_id,
    multiIf(source = 'redcross', 1, source = 'who', 2, 0) AS source_id,
    api_name,
    record_type,
    sum(record_count) AS success_count,
    toUInt64(0) AS error_count,
    sum(record_count) AS total_count
FROM blood_ops.source_ingestion_hourly_agg
GROUP BY event_time_id, event_date_id, source_id, api_name, record_type;

-- 1-way, 2-way, 3-way inventory aggregates
CREATE TABLE IF NOT EXISTS blood_ops.agg_inventory_1w_day (
    event_date Date,
    inflow_units Int64,
    outflow_units Int64,
    adjustment_units Int64,
    closing_units Int64,
    donation_events UInt64,
    withdrawal_events UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY event_date;

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_inventory_1w_day
TO blood_ops.agg_inventory_1w_day
AS
SELECT
    event_date,
    sum(toInt64(inflow_units)) AS inflow_units,
    sum(toInt64(outflow_units)) AS outflow_units,
    sum(toInt64(adjustment_units)) AS adjustment_units,
    sum(toInt64(closing_balance_units)) AS closing_units,
    sum(toUInt64(donation_events_count)) AS donation_events,
    sum(toUInt64(withdrawal_events_count)) AS withdrawal_events
FROM blood_ops.fact_inventory_day
GROUP BY event_date;

CREATE TABLE IF NOT EXISTS blood_ops.agg_inventory_2w_day_source (
    event_date Date,
    source_id UInt8,
    inflow_units Int64,
    outflow_units Int64,
    adjustment_units Int64,
    closing_units Int64,
    donation_events UInt64,
    withdrawal_events UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_inventory_2w_day_source
TO blood_ops.agg_inventory_2w_day_source
AS
SELECT
    event_date,
    source_id,
    sum(toInt64(inflow_units)) AS inflow_units,
    sum(toInt64(outflow_units)) AS outflow_units,
    sum(toInt64(adjustment_units)) AS adjustment_units,
    sum(toInt64(closing_balance_units)) AS closing_units,
    sum(toUInt64(donation_events_count)) AS donation_events,
    sum(toUInt64(withdrawal_events_count)) AS withdrawal_events
FROM blood_ops.fact_inventory_day
GROUP BY event_date, source_id;

CREATE TABLE IF NOT EXISTS blood_ops.agg_inventory_3w_day_source_bank (
    event_date Date,
    source_id UInt8,
    bank_id UInt64,
    inflow_units Int64,
    outflow_units Int64,
    adjustment_units Int64,
    closing_units Int64,
    donation_events UInt64,
    withdrawal_events UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id, bank_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_inventory_3w_day_source_bank
TO blood_ops.agg_inventory_3w_day_source_bank
AS
SELECT
    event_date,
    source_id,
    bank_id,
    sum(toInt64(inflow_units)) AS inflow_units,
    sum(toInt64(outflow_units)) AS outflow_units,
    sum(toInt64(adjustment_units)) AS adjustment_units,
    sum(toInt64(closing_balance_units)) AS closing_units,
    sum(toUInt64(donation_events_count)) AS donation_events,
    sum(toUInt64(withdrawal_events_count)) AS withdrawal_events
FROM blood_ops.fact_inventory_day
GROUP BY event_date, source_id, bank_id;

-- 1-way, 2-way, 3-way donor aggregates
CREATE TABLE IF NOT EXISTS blood_ops.agg_donor_1w_day (
    event_date Date,
    total_donors UInt64,
    eligible_donors UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY event_date;

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_donor_1w_day
TO blood_ops.agg_donor_1w_day
AS
SELECT
    event_date,
    sum(toUInt64(total_donors)) AS total_donors,
    sum(toUInt64(eligible_donors)) AS eligible_donors
FROM blood_ops.fact_donor_day
GROUP BY event_date;

CREATE TABLE IF NOT EXISTS blood_ops.agg_donor_2w_day_source (
    event_date Date,
    source_id UInt8,
    total_donors UInt64,
    eligible_donors UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_donor_2w_day_source
TO blood_ops.agg_donor_2w_day_source
AS
SELECT
    event_date,
    source_id,
    sum(toUInt64(total_donors)) AS total_donors,
    sum(toUInt64(eligible_donors)) AS eligible_donors
FROM blood_ops.fact_donor_day
GROUP BY event_date, source_id;

CREATE TABLE IF NOT EXISTS blood_ops.agg_donor_3w_day_source_bank (
    event_date Date,
    source_id UInt8,
    bank_id UInt64,
    total_donors UInt64,
    eligible_donors UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, source_id, bank_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS blood_ops.mv_donor_3w_day_source_bank
TO blood_ops.agg_donor_3w_day_source_bank
AS
SELECT
    event_date,
    source_id,
    bank_id,
    sum(toUInt64(total_donors)) AS total_donors,
    sum(toUInt64(eligible_donors)) AS eligible_donors
FROM blood_ops.fact_donor_day
GROUP BY event_date, source_id, bank_id;

-- Metadata catalog tables
CREATE TABLE IF NOT EXISTS blood_ops.meta_source_system (
    source_code LowCardinality(String),
    source_name String,
    owner String,
    is_active UInt8 DEFAULT 1,
    created_at DateTime('Asia/Kolkata') DEFAULT now(),
    updated_at DateTime('Asia/Kolkata') DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY source_code;

CREATE TABLE IF NOT EXISTS blood_ops.meta_dataset (
    dataset_name LowCardinality(String),
    physical_table String,
    dataset_type LowCardinality(String),
    refresh_mode LowCardinality(String),
    description String,
    is_active UInt8 DEFAULT 1,
    created_at DateTime('Asia/Kolkata') DEFAULT now(),
    updated_at DateTime('Asia/Kolkata') DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY dataset_name;

CREATE TABLE IF NOT EXISTS blood_ops.meta_column (
    dataset_name LowCardinality(String),
    column_name String,
    data_type String,
    is_nullable UInt8,
    business_definition String,
    source_system LowCardinality(String),
    created_at DateTime('Asia/Kolkata') DEFAULT now(),
    updated_at DateTime('Asia/Kolkata') DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (dataset_name, column_name);

CREATE TABLE IF NOT EXISTS blood_ops.meta_lineage (
    target_dataset LowCardinality(String),
    target_column String,
    source_system LowCardinality(String),
    source_dataset String,
    source_column String,
    transform_rule String,
    is_active UInt8 DEFAULT 1,
    created_at DateTime('Asia/Kolkata') DEFAULT now(),
    updated_at DateTime('Asia/Kolkata') DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (target_dataset, target_column, source_system, source_dataset, source_column);

CREATE TABLE IF NOT EXISTS blood_ops.meta_load_audit (
    batch_id String,
    source_system LowCardinality(String),
    target_system LowCardinality(String),
    target_dataset String,
    started_at DateTime,
    ended_at DateTime,
    duration_ms UInt64,
    rows_read UInt64,
    rows_written UInt64,
    status LowCardinality(String),
    message String,
    created_at DateTime('Asia/Kolkata') DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(started_at)
ORDER BY (started_at, source_system, target_system, target_dataset, batch_id);

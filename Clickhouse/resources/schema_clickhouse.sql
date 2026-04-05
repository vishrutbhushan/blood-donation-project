CREATE DATABASE IF NOT EXISTS blood_ops;

/*
Star schema aligned to source-of-truth systems:
- Redcross: blood_bank, blood_inventory, blood_donor
- WHO: blood_bank, blood_inventory, blood_donor
*/

DROP VIEW IF EXISTS blood_ops.mv_ingestion_hourly_to_fact;

DROP TABLE IF EXISTS blood_ops.fact_ingestion_event;
DROP TABLE IF EXISTS blood_ops.source_ingestion_hourly_agg;
DROP TABLE IF EXISTS blood_ops.fact_donor_snapshot;
DROP TABLE IF EXISTS blood_ops.fact_inventory_snapshot;

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

CREATE TABLE IF NOT EXISTS blood_ops.fact_inventory_snapshot (
    inventory_fact_id UInt64,
    source_id UInt8,
    bank_id UInt64,
    location_id UInt64,
    blood_group_id UInt8,
    component_id UInt8,
    event_time_id UInt32,
    event_date_id UInt32,
    units_available Int32,
    record_count UInt8 DEFAULT 1,
    is_deleted UInt8 DEFAULT 0,
    snapshot_updated_at DateTime,
    ingested_at DateTime DEFAULT now(),
    version UInt64
)
ENGINE = ReplacingMergeTree(version)
PARTITION BY toYYYYMM(toDate(snapshot_updated_at))
ORDER BY (event_date_id, source_id, bank_id, blood_group_id, component_id, inventory_fact_id);

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
PARTITION BY toYYYYMM(toDate(event_time_id))
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

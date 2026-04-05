# etl-service

A Spring Boot ETL (Extract-Transform-Load) service that ingests blood donation data from multiple upstream sources, normalises it into a canonical schema, and writes it to ClickHouse (analytics) and Elasticsearch (full-text search).

---

## Table of Contents

1. [Purpose](#purpose)
2. [Architecture Overview](#architecture-overview)
3. [Folder Structure](#folder-structure)
4. [Configuration & Constants](#configuration--constants)
5. [Data Models](#data-models)
6. [Extract](#extract)
7. [Transform](#transform)
8. [Load](#load)
9. [Source Handler Abstraction](#source-handler-abstraction)
10. [Utility Classes](#utility-classes)
11. [Orchestration (App.java)](#orchestration-appjava)
12. [ETL Flow Diagram](#etl-flow-diagram)
13. [State Management](#state-management)
14. [External Service Endpoints](#external-service-endpoints)
15. [ClickHouse Schema](#clickhouse-schema)
16. [Build & Run](#build--run)

---

## Purpose

This service continuously synchronises blood bank and donor records from two independent data providers into a unified data store:

| Provider | Source Name | API Port |
|---|---|---|
| World Health Organization (WHO) | `who` | `8001` |
| Red Cross | `redcross` | `8002` |

On startup it performs a **full backfill** (from `2024-01-01`) for any source that has never been synced, then runs **incremental updates every 4 hours**, fetching only records that changed since the last run.

---

## Architecture Overview

```
┌──────────────┐     HTTP GET /incremental     ┌──────────────────────┐
│  WHO API     │ ◄─────────────────────────── │                      │
│  :8001       │ ──────────────────────────── │                      │
└──────────────┘   JSON (blood_banks, donors)  │                      │
                                               │   etl-service        │
┌──────────────┐     HTTP GET /incremental     │   (Spring Boot)      │
│  Redcross API│ ◄─────────────────────────── │                      │
│  :8002       │ ──────────────────────────── │                      │
└──────────────┘   JSON (centres, people)      └───────┬──────────────┘
                                                       │
                         ┌─────────────────────────────┤
                         │                             │
                         ▼                             ▼
               ┌──────────────────┐        ┌──────────────────────┐
               │  ClickHouse      │        │  Elasticsearch       │
               │  :8123           │        │  :9200               │
               │  blood_bank_dim  │        │  blood-banks index   │
               │  donor_dim       │        │  donors index        │
               └──────────────────┘        └──────────────────────┘
```

All upstream and downstream hosts are accessed as `host.docker.internal` from within the container.

---

## Folder Structure

```
etl-service/
├── Dockerfile
├── pom.xml
└── src/main/java/etl/
    ├── App.java                                   ← Main orchestrator
    ├── constants/
    │   ├── Constants.java                         ← All hardcoded config values
    │   └── QueryConstants.java                    ← ClickHouse SQL templates
    ├── extract/
    │   ├── who/
    │   │   └── WhoExtractor.java                  ← Fetches WHO API data
    │   └── redcross/
    │       └── RedcrossExtractor.java             ← Fetches Red Cross API data
    ├── transform/
    │   ├── who/
    │   │   └── WhoTransformPipeline.java          ← Maps WHO JSON → canonical models
    │   └── redcross/
    │       └── RedcrossTransformPipeline.java     ← Maps Red Cross JSON → canonical models
    ├── load/
    │   ├── clickhouse/
    │   │   └── ClickhouseLoader.java              ← Writes to ClickHouse via HTTP
    │   └── elasticsearch/
    │       └── ElasticsearchLoader.java           ← Indexes in Elasticsearch via HTTP
    ├── model/
    │   ├── BloodBank.java                         ← Canonical blood bank record
    │   ├── Donor.java                             ← Canonical donor record
    │   └── EtlBatch.java                          ← Container for a batch of records
    ├── source/
    │   ├── SourceHandler.java                     ← Strategy interface
    │   ├── WhoSourceHandler.java                  ← WHO implementation
    │   └── RedcrossSourceHandler.java             ← Red Cross implementation
    └── util/
        ├── TimeUtil.java                          ← Timestamp normalisation
        ├── JsonUtil.java                          ← JSON read/write helpers
        └── PincodeGeoMap.java                     ← Pincode → lat/lon lookup
```

---

## Configuration & Constants

All configuration is hardcoded in `etl/constants/Constants.java`. There are no external configuration files.

### `Constants.java`

| Constant | Value | Description |
|---|---|---|
| `SOURCE_WHO` | `"who"` | Key for the WHO data source |
| `SOURCE_REDCROSS` | `"redcross"` | Key for the Red Cross data source |
| `INITIAL_PULL_START_TS` | `1704067200000` | Backfill start time (2024-01-01 UTC, epoch ms) |
| `INCREMENT_WINDOW_MS` | `21600000` (6 h) | Maximum time window per API request |
| `SCHEDULE_MS` | `14400000` (4 h) | Delay between scheduled incremental runs |
| `DATA_DIR` | `"/data"` | Directory for persistent state file |
| `KEY_LAST_SYNC` | `"last_sync"` | JSON key storing the last-sync timestamp |
| `OP_DELETE` | `"delete"` | Operation type for record deletion |
| `OP_UPSERT` | `"upsert"` | Operation type for insert/update |
| `WHO_BASE_URL` | `http://host.docker.internal:8001` | WHO API base URL |
| `WHO_INCREMENTAL_ENDPOINT` | `"/incremental"` | WHO incremental endpoint path |
| `WHO_SINCE_PARAM` | `"since"` | Query param: start timestamp |
| `WHO_UNTIL_PARAM` | `"until"` | Query param: end timestamp |
| `REDCROSS_BASE_URL` | `http://host.docker.internal:8002` | Red Cross API base URL |
| `REDCROSS_INCREMENTAL_ENDPOINT` | `"/incremental"` | Red Cross incremental endpoint path |
| `REDCROSS_SINCE_PARAM` | `"since"` | Query param: start timestamp |
| `REDCROSS_UNTIL_PARAM` | `"until"` | Query param: end timestamp |
| `CLICKHOUSE_URL` | `http://host.docker.internal:8123` | ClickHouse HTTP interface |
| `ELASTIC_URL` | `http://host.docker.internal:9200` | Elasticsearch HTTP interface |
| `ELASTIC_INDEX_BANKS` | `"blood-banks"` | Elasticsearch index for blood banks |
| `ELASTIC_INDEX_DONORS` | `"donors"` | Elasticsearch index for donors |

### `QueryConstants.java`

Parameterised SQL templates sent to ClickHouse via HTTP POST:

```sql
-- Upsert a blood bank
INSERT INTO blood_bank_dim
  (bank_id, bank_name, address, city, state, pincode, phone, lat, lon, updated_at)
VALUES
  ('%s','%s','%s','%s','%s','%s','%s',%s,%s,parseDateTimeBestEffort('%s'))

-- Upsert a donor
INSERT INTO donor_dim
  (donor_id, name, blood_group, phone, email,
   address_current, city_current, state_current, pincode_current,
   lat, lon, bank_id, last_donated_on, updated_at)
VALUES
  ('%s','%s','%s','%s','%s','%s','%s','%s','%s',%s,%s,'%s','%s',parseDateTimeBestEffort('%s'))

-- Delete a blood bank
ALTER TABLE blood_bank_dim DELETE WHERE bank_id = '%s'

-- Delete a donor
ALTER TABLE donor_dim DELETE WHERE donor_id = '%s'
```

String values are escaped by doubling any single-quotes (`'` → `''`). Numeric values (`lat`, `lon`) are substituted as bare numbers; if `null` the literal `null` is inserted.

---

## Data Models

All model classes use Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

### `BloodBank`

| Field | Java Type | Description |
|---|---|---|
| `bankId` | `String` | Primary key |
| `bankName` | `String` | Display name |
| `address` | `String` | Street address (optional) |
| `city` | `String` | City (optional) |
| `state` | `String` | State (optional) |
| `pincode` | `String` | 6-digit Indian pincode (required for geo-lookup) |
| `phone` | `String` | Contact phone (optional) |
| `lat` | `Double` | Latitude derived from pincode |
| `lon` | `Double` | Longitude derived from pincode |
| `updatedAt` | `String` | Normalised timestamp: `"yyyy-MM-dd HH:mm:ss"` |
| `op` | `String` | `"upsert"` or `"delete"` |

### `Donor`

| Field | Java Type | Description |
|---|---|---|
| `donorId` | `String` | Primary key |
| `name` | `String` | Full name |
| `bloodGroup` | `String` | Blood group string, e.g. `"A+"` |
| `phone` | `String` | Contact phone (optional) |
| `email` | `String` | Email address (optional) |
| `addressCurrent` | `String` | Current street address (optional) |
| `cityCurrent` | `String` | Current city (optional) |
| `stateCurrent` | `String` | Current state (optional) |
| `pincodeCurrent` | `String` | Current pincode (required for geo-lookup) |
| `lat` | `Double` | Latitude derived from `pincodeCurrent` |
| `lon` | `Double` | Longitude derived from `pincodeCurrent` |
| `bankId` | `String` | FK to `BloodBank.bankId`; resolved during flush |
| `lastDonatedOn` | `String` | Date of last donation (optional) |
| `lastDonatedBloodBank` | `String` | Name of bank where last donated; used to resolve `bankId` |
| `updatedAt` | `String` | Normalised timestamp: `"yyyy-MM-dd HH:mm:ss"` |
| `op` | `String` | `"upsert"` or `"delete"` |

### `EtlBatch`

```java
List<BloodBank> banks;   // initialised as new ArrayList<>()
List<Donor>    donors;   // initialised as new ArrayList<>()
```

A lightweight container carrying all records produced by one transform call.

---

## Extract

Both extractors use Spring's `RestClient` to call `GET /<endpoint>?since=<fromTs>&until=<toTs>`.

### `WhoExtractor` (`@Component`)

- **Endpoint:** `http://host.docker.internal:8001/incremental`
- **Parameters:** `since` (epoch ms), `until` (epoch ms)
- **Returns:** parsed `Object` (Jackson `Map<String,Object>` for a JSON object, or `List` for a JSON array)
- **Throws:** `RuntimeException` if the response body is `null`
- Maintains an in-memory `List<Object> inMemoryPayloads` of every payload fetched in the current run.

### `RedcrossExtractor` (`@Component`)

- Identical structure to `WhoExtractor`.
- **Endpoint:** `http://host.docker.internal:8002/incremental`
- **Parameters:** `since`, `until`

---

## Transform

Both transform pipelines map source-specific JSON field names to the canonical model. They are pure functions: given an `Object payload` and a `Map<String,Object> geoMap`, they return an `EtlBatch`.

### WHO JSON Schema → Canonical Mapping

**Blood banks** are in `payload["blood_banks"]`:

| Source field | Canonical field | Required? |
|---|---|---|
| `bank_id` | `bankId` | ✅ |
| `bank_name` | `bankName` | ✅ |
| `pincode` | `pincode` / geo | ✅ |
| `address` | `address` | optional |
| `city` | `city` | optional |
| `state` | `state` | optional |
| `phone` | `phone` | optional |
| `update_time` | `updatedAt` | ✅ |
| `deleted` / `is_deleted` | `op` (delete/upsert) | — |

**Donors** are in `payload["donors"]`:

| Source field | Canonical field | Required? |
|---|---|---|
| `donor_id` | `donorId` | ✅ |
| `name` | `name` | ✅ |
| `blood_group` | `bloodGroup` | ✅ |
| `pincode_current` | `pincodeCurrent` / geo | ✅ |
| `phone` | `phone` | optional |
| `email` | `email` | optional |
| `address_current` | `addressCurrent` | optional |
| `city_current` | `cityCurrent` | optional |
| `state_current` | `stateCurrent` | optional |
| `bank_id` | `bankId` | optional |
| `last_donated_on` | `lastDonatedOn` | optional |
| `last_donated_blood_bank` | `lastDonatedBloodBank` | optional |
| `update_time` | `updatedAt` | ✅ |
| `deleted` / `is_deleted` | `op` (delete/upsert) | — |

### Red Cross JSON Schema → Canonical Mapping

Identical field names **except**:

| Difference | WHO | Red Cross |
|---|---|---|
| Blood bank array key | `blood_banks` | `centres` |
| Donor array key | `donors` | `people` |

All internal field names (`bank_id`, `bank_name`, `update_time`, etc.) are the same between both sources.

### Geo-Enrichment

Both pipelines call `geo(pincode, geoMap)` to set `lat`/`lon`. The `geoMap` is provided by `PincodeGeoMap` (see [Utility Classes](#utility-classes)). If a pincode is not found in the map, a `RuntimeException` is thrown.

### Timestamp Normalisation

The `update_time` field from the source (either epoch-millisecond number or already-formatted string) is converted to `"yyyy-MM-dd HH:mm:ss"` via `TimeUtil.formatStore()`.

### Operation Detection

```
if (raw["deleted"] is truthy OR raw["is_deleted"] is truthy) → op = "delete"
else → op = "upsert"
```

Truthy values: Java `Boolean true`, or strings `"true"`, `"1"`, `"yes"` (case-insensitive).

---

## Load

### `ClickhouseLoader` (`@Component`)

Uses `RestClient` to POST raw SQL to ClickHouse's HTTP interface (`/?` endpoint accepts plain-text SQL in the request body).

| Method | Iterates | Operation |
|---|---|---|
| `loadBanks(List<BloodBank>)` | each `BloodBank` | INSERT or ALTER TABLE DELETE on `blood_bank_dim` |
| `loadDonors(List<Donor>)` | each `Donor` | INSERT or ALTER TABLE DELETE on `donor_dim` |

Helper methods:
- `esc(String s)` – escapes single-quotes for SQL safety
- `num(Object n)` – renders numeric values or `null`
- `str(Object v)` – null-safe `toString`

### `ElasticsearchLoader` (`@Component`)

Uses `RestClient` against the Elasticsearch REST API.

| Method | UPSERT operation | DELETE operation |
|---|---|---|
| `loadBanks(List<BloodBank>)` | `POST /blood-banks/_doc/{bankId}` with JSON body | `DELETE /blood-banks/_doc/{bankId}` |
| `loadDonors(List<Donor>)` | `POST /donors/_doc/{donorId}` with JSON body | `DELETE /donors/_doc/{donorId}` |

JSON serialisation is handled by `JsonUtil.toJson()` (Jackson `ObjectMapper`).

---

## Source Handler Abstraction

`SourceHandler` is a strategy interface that decouples the orchestrator from specific source implementations.

```java
public interface SourceHandler {
    String sourceName();                                           // "who" or "redcross"
    Object fetchIncremental(long fromTs, long toTs);               // calls extractor
    EtlBatch transform(Object payload, Map<String, Object> geoMap); // calls transform pipeline
    List<Object> inMemoryPayloads();                               // all payloads fetched this run
}
```

Implementations:

| Class | `sourceName()` | Delegates to |
|---|---|---|
| `WhoSourceHandler` | `"who"` | `WhoExtractor` + `WhoTransformPipeline` |
| `RedcrossSourceHandler` | `"redcross"` | `RedcrossExtractor` + `RedcrossTransformPipeline` |

Spring auto-wires all `SourceHandler` beans into a `List<SourceHandler>` in `App`, making it trivial to add new sources.

---

## Utility Classes

### `TimeUtil`

Stateless utility class with two static methods:

| Method | Input | Output |
|---|---|---|
| `toDateTime(Object value)` | epoch-ms `Number` or `"yyyy-MM-dd HH:mm:ss"` `String` | `LocalDateTime` (UTC) |
| `formatStore(Object value)` | same as above | `String` in `"yyyy-MM-dd HH:mm:ss"` format |

Throws `IllegalArgumentException` for `null`, blank strings, or unsupported types.

### `JsonUtil` (`@Component`)

Wraps Jackson `ObjectMapper`:

| Method | Description |
|---|---|
| `readFileMap(String path)` | Reads JSON file → `Map<String,Object>`; returns empty map if file does not exist |
| `writeFile(String path, Object data)` | Serialises object to JSON file; creates parent directories |
| `toJson(Object value)` | Serialises object to JSON string |
| `parse(String json)` | Deserialises JSON string to `Object` (Map or List) |

### `PincodeGeoMap` (`@Component`)

Hardcoded lookup table mapping Indian pincodes to geographic coordinates:

| Pincode | City | Latitude | Longitude |
|---|---|---|---|
| `110001` | Delhi | 28.6304 | 77.2177 |
| `400001` | Mumbai | 18.9388 | 72.8354 |
| `700001` | Kolkata | 22.5726 | 88.3639 |
| `560001` | Bangalore | 12.9762 | 77.6033 |

`asMap()` returns the underlying `Map<String, Object>` where each value is a `Map` with keys `"lat"` and `"lon"`.

> **Note:** Any pincode not present in this map will cause the transform pipeline to throw a `RuntimeException`. To support additional cities, add entries in the constructor.

---

## Orchestration (App.java)

`App` is the Spring Boot entry point. It is annotated with `@SpringBootApplication` and `@EnableScheduling`.

### In-Memory State

```java
Map<String, Object> state                             // loaded from /data/state.json
Map<String, Map<String, BloodBank>> banksBySource     // keyed: source → bankId → BloodBank
Map<String, Map<String, Donor>>    donorsBySource     // keyed: source → donorId → Donor
```

### Lifecycle

#### `@PostConstruct onStart()`

Runs once at startup:
1. Load `state` from `/data/state.json` (empty map if file does not exist).
2. Call `runInitialBackfillIfNeeded()` — for each source not yet present in `state`, pull all data from `INITIAL_PULL_START_TS` to now.
3. Call `runIncremental()` — pull incremental data up to now for all sources.

#### `@Scheduled(fixedDelay = 14400000) runIncremental()`

Runs immediately after `onStart()` completes, then every 4 hours:
1. For each `SourceHandler`: call `pullAndProcess(handler, lastSyncTs, now)`.
2. Call `flushToTargets()`.
3. Update `last_sync` in `state` for each source.
4. Persist `state` to `/data/state.json`.

### Key Methods

#### `pullAndProcess(SourceHandler handler, long fromTs, long toTs)`

Iterates over `[fromTs, toTs)` in `INCREMENT_WINDOW_MS`-sized (6-hour) chunks:
```
cursor = fromTs
while cursor < toTs:
    end = min(cursor + 6h, toTs)
    payload = handler.fetchIncremental(cursor, end)
    batch   = handler.transform(payload, geoMap)
    mergeSourceBatch(source, batch)
    cursor = end
```

#### `mergeSourceBatch(String source, EtlBatch batch)`

Upserts records into the per-source in-memory maps (`banksBySource`, `donorsBySource`). Records with a `null` or blank ID are silently skipped.

#### `flushToTargets()`

1. Collect all banks from `banksBySource` into a flat list; deduplicate by keeping the record with the latest `updatedAt` per `bankId` (`mergeLatestBanks`).
2. Collect all donors from `donorsBySource` similarly (`mergeLatestDonors`).
3. Resolve foreign keys: for each donor, look up `lastDonatedBloodBank` in a bank-name→bankId index and populate `bankId` (`resolveFk`).
4. Write to ClickHouse (`clickhouseLoader.loadBanks`, `loadDonors`).
5. Write to Elasticsearch (`elasticsearchLoader.loadBanks`, `loadDonors`).

#### `resolveFk(List<Donor> donors, List<BloodBank> banks)`

Builds a `Map<bankName, bankId>` from the bank list, then for each donor whose `lastDonatedBloodBank` matches a known bank name, sets `donor.bankId`.

#### State helpers

| Method | Description |
|---|---|
| `getLastSyncTs(String source)` | Returns epoch-ms from `state[source][last_sync]`; falls back to `INITIAL_PULL_START_TS` |
| `setLastSyncTs(String source, long ts)` | Writes `state[source] = { last_sync: "<ts>" }` |
| `saveState()` | Persists `state` to `/data/state.json` via `JsonUtil` |

---

## ETL Flow Diagram

```
APPLICATION STARTUP
        │
        ▼
  Load /data/state.json
        │
        ▼
  Initial backfill needed?  ──Yes──►  pullAndProcess(source, 2024-01-01, now)
  (per source not in state)            │
        │                              │
        │◄──────────────────────────────┘
        ▼
  runIncremental()
        │
        ├── For each source:
        │       └── pullAndProcess(lastSync → now)
        │               └── per 6-hour window:
        │                       1. fetchIncremental(from, to)  → HTTP GET /incremental
        │                       2. transform(payload, geoMap)  → EtlBatch
        │                       3. mergeSourceBatch()          → update in-memory maps
        │
        ├── flushToTargets()
        │       1. Aggregate banks  across sources
        │       2. Deduplicate      (keep latest updatedAt)
        │       3. Aggregate donors across sources
        │       4. Deduplicate      (keep latest updatedAt)
        │       5. Resolve FK       (bank name → bank ID)
        │       6. ClickhouseLoader.loadBanks()  / loadDonors()
        │       7. ElasticsearchLoader.loadBanks() / loadDonors()
        │
        └── Save updated state.json
                │
                └──► Wait 4 hours → repeat runIncremental()
```

---

## State Management

Persistent state is stored as JSON at `/data/state.json`. Example:

```json
{
  "who": {
    "last_sync": "1712345678000"
  },
  "redcross": {
    "last_sync": "1712345678000"
  }
}
```

- If the file does not exist, all sources are treated as never-synced and a full backfill is triggered.
- The `/data` directory is created automatically by `JsonUtil.writeFile` if it does not exist.
- In Docker, mount a persistent volume at `/data` to survive container restarts.

---

## External Service Endpoints

### Upstream APIs

Both APIs share the same query interface:

```
GET /incremental?since=<epoch_ms>&until=<epoch_ms>
```

**WHO response shape:**
```json
{
  "blood_banks": [
    {
      "bank_id": "...", "bank_name": "...", "pincode": "...",
      "address": "...", "city": "...", "state": "...", "phone": "...",
      "update_time": 1712345678000,
      "deleted": false
    }
  ],
  "donors": [
    {
      "donor_id": "...", "name": "...", "blood_group": "A+",
      "pincode_current": "...", "phone": "...", "email": "...",
      "address_current": "...", "city_current": "...", "state_current": "...",
      "bank_id": "...", "last_donated_on": "...", "last_donated_blood_bank": "...",
      "update_time": 1712345678000,
      "deleted": false
    }
  ]
}
```

**Red Cross response shape:** identical structure but `blood_banks` → `centres`, `donors` → `people`.

### Downstream: ClickHouse

- **Interface:** HTTP POST to `http://host.docker.internal:8123`
- **Body:** plain-text SQL
- **Tables:** `blood_bank_dim`, `donor_dim`

### Downstream: Elasticsearch

- **Upsert:** `POST http://host.docker.internal:9200/{index}/_doc/{id}` — `Content-Type: application/json`
- **Delete:** `DELETE http://host.docker.internal:9200/{index}/_doc/{id}`
- **Indices:** `blood-banks`, `donors`

---

## ClickHouse Schema

Expected table definitions (must exist before the service runs):

```sql
CREATE TABLE blood_bank_dim (
    bank_id     String,
    bank_name   String,
    address     String,
    city        String,
    state       String,
    pincode     String,
    phone       String,
    lat         Nullable(Float64),
    lon         Nullable(Float64),
    updated_at  DateTime
) ENGINE = MergeTree()
ORDER BY bank_id;

CREATE TABLE donor_dim (
    donor_id        String,
    name            String,
    blood_group     String,
    phone           String,
    email           String,
    address_current String,
    city_current    String,
    state_current   String,
    pincode_current String,
    lat             Nullable(Float64),
    lon             Nullable(Float64),
    bank_id         String,
    last_donated_on String,
    updated_at      DateTime
) ENGINE = MergeTree()
ORDER BY donor_id;
```

---

## Build & Run

### Prerequisites

- Java 17
- Maven 3.9+
- Docker (for containerised deployment)

### Build locally

```bash
cd etl-service
mvn package
java -jar target/etl-service.jar
```

### Build Docker image

```bash
docker build -t etl-service .
```

The Dockerfile uses a two-stage build:
1. **Build stage** — `maven:3.9.7-eclipse-temurin-17`: copies `pom.xml` and `src/`, runs `mvn -q -DskipTests package`.
2. **Runtime stage** — `eclipse-temurin:17-jre`: copies `target/etl-service.jar` and runs it as `app.jar`.

### Run with Docker

```bash
docker run --add-host=host.docker.internal:host-gateway \
           -v /path/to/data:/data \
           etl-service
```

- `--add-host` exposes the host machine's services (WHO API, Red Cross API, ClickHouse, Elasticsearch) as `host.docker.internal`.
- `-v /path/to/data:/data` mounts a persistent volume so `state.json` survives restarts.

### Run with Docker Compose

The service is referenced in the project-level `docker-compose.yml`. Start the full stack from the repository root:

```bash
docker-compose up --build
```

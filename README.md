# Blood Donation Project Stack

This repository contains a Docker Compose based blood donation platform with four functional zones:

- Source systems: WHO and Redcross services with their own PostgreSQL databases
- Core app: backend API + frontend web app
- ETL: source-to-search-and-analytics data movement
- Analytics: Elasticsearch, ClickHouse, and Grafana

## Current Design

- WHO and Redcross are external-style source providers for ETL consumption only.
- ETL bulk load is manual and async via `GET /admin/etl/bulk-load` on port `8083`.
- ETL incremental is scheduler-driven and gated by bulk completion state.
- Elasticsearch is the operational query store for blood-bank and donor search.
- ClickHouse is the analytics store (daily facts + ingestion facts).
- Backend remains the API boundary for frontend business workflows.

## Runtime Topology

Core services and local ports:

- Backend: `http://localhost:8080`
- Redcross source: `http://localhost:8081`
- WHO source: `http://localhost:8082`
- ETL admin: `http://localhost:8083`
- Grafana: `http://localhost:3000`
- Frontend: `http://localhost:3001`

Databases and engines:

- Primary PostgreSQL: `localhost:5432`
- Read replica PostgreSQL (full profile): `localhost:5433`
- Redcross PostgreSQL: `localhost:5434`
- WHO PostgreSQL: `localhost:5435`
- Elasticsearch: internal container network
- ClickHouse: internal container network

## Startup Modes

- Single mode: `SINGLE_MODE=true`
- Full mode: `SINGLE_MODE=false` with `--profile full`

PowerShell examples:

```powershell
# Single mode
$env:SINGLE_MODE = "true"
docker compose up -d --build

# Full mode
$env:SINGLE_MODE = "false"
docker compose --profile full up -d --build

# Clean reset (volumes included)
docker compose --profile full down -v --remove-orphans
```

Cmd examples:

```cmd
set SINGLE_MODE=true && docker compose up -d --build
set SINGLE_MODE=false && docker compose --profile full up -d --build
docker compose --profile full down -v --remove-orphans
```

## API Routes

Frontend-facing API routes (through Nginx proxy):

- `/api/backend/reference-data`
- `/api/backend/auth/send-otp`
- `/api/backend/auth/verify-otp`
- `/api/backend/users/get-or-create`
- `/api/backend/blood-banks/search`
- `/api/backend/donors/search`
- `/api/backend/searches/{userId}`
- `/api/backend/requests/{searchId}`
- `/api/backend/requests/{requestId}/re-request`
- `/api/backend/requests/{requestId}/re-request-preview`
- `/api/backend/requests/user/{userId}`
- `/api/backend/requests/user/{userId}/responses`
- `/api/backend/requests/user/{userId}/active`

Direct backend controller base routes:

- `/reference-data`
- `/auth/*`
- `/users/*`
- `/blood-banks/*`
- `/donors/*`
- `/searches/*`
- `/requests/*`

Donor WhatsApp webhook:

- `POST /api/donor/respond`
- This endpoint must be exposed on the backend service, not the frontend.
- Twilio Sandbox should point its inbound WhatsApp webhook to `https://<ngrok-host>/api/donor/respond`.

Source-system routes (ETL-facing):

- Redcross: `/api/redcross/centres`, `/api/redcross/centres/incremental`, `/api/redcross/people`, `/api/redcross/people/incremental`, `/incremental`
- WHO: `/api/who/blood-banks`, `/api/who/blood-banks/incremental`, `/api/who/donors`, `/api/who/donors/incremental`, `/incremental`

ETL admin route:

- `GET /admin/etl/bulk-load` (returns accepted response body with status text)

## ETL Schedule

- Bulk load is manual trigger only.
- Incremental scheduler is cron-based:
	- ES incremental env key: `ETL_ES_CRON`
	- ES incremental default: `0 */5 * * * *`
	- ClickHouse incremental env key: `ETL_CH_CRON`
	- ClickHouse incremental default: `0 0 0 * * *`
	- Time zone env key: `ETL_ZONE` (default `Asia/Kolkata`)

## Data and Schemas

Schema source files:

- `backend/src/main/resources/schema.sql`
- `Redcross/src/main/resources/sourcetables.sql`
- `who/src/main/resources/source_coder.sql`
- `Clickhouse/resources/schema_clickhouse.sql`

Table inventory summary:

- Primary PostgreSQL: `users`, `searches`, `requests`, `responses`, `blood_group_lookup`, `blood_component_lookup`
- Redcross PostgreSQL: `blood_bank`, `inventory_transaction`, `donor`
- WHO PostgreSQL: `blood_bank`, `inventory_transaction`, `donor`
- ClickHouse (`blood_ops`): `dim_source`, `dim_blood_group`, `dim_component`, `dim_location`, `dim_blood_bank`, `dim_donor`, `dim_date`, `dim_time`, `fact_inventory_day`, `fact_donor_snapshot`, `fact_donor_day`, `fact_ingestion_event`, `source_ingestion_hourly_agg`

Full table/column and dataflow mapping list is maintained in `SOURCE_OF_TRUTH.txt` (Sections E and H).

## E2E Runbook

1. Start stack: `docker compose up -d --build`
2. Confirm health: `docker compose ps`
3. Seed source data: `python db-seeder/seed.py`
4. Trigger ETL bulk: `GET http://localhost:8083/admin/etl/bulk-load`
5. Validate backend APIs (`reference-data`, blood-bank search, donor search)
6. Validate frontend flow at `http://localhost:3001`
7. Validate Grafana login and datasource query execution at `http://localhost:3000`

## Proxy Behavior

- Frontend serves static assets on port `3001`.
- Nginx maps `/api/backend/*` to backend service `http://backend:8080/*`.
- ETL talks directly to WHO/Redcross by container DNS names.

## Notes

- Keep controller logging in `api.enter` and `api.exit` format.
- Keep API DTO contracts typed (no map-based public payloads).
- Keep ETL orchestration in service classes; keep application entrypoint minimal.

## Donor Reply Setup

1. Start the backend on `8080`.
2. Start ngrok against the backend port, for example: `ngrok http 8080`.
3. Copy the public ngrok URL.
4. In Twilio Sandbox, set the inbound WhatsApp webhook to `https://<ngrok-host>/api/donor/respond`.
5. Send a donor message from WhatsApp with `YES` or `NO`.
6. In demo mode, the WhatsApp send targets are driven by the env values `APP_DEMO_DONOR_PHONE_1`, `APP_DEMO_DONOR_PHONE_2`, `APP_DEMO_DONOR_PHONE_3`, `APP_FIXED_REQUESTOR_PHONE`, and `APP_FIXED_OTP_RECEIVER_PHONE`; the rest of the 20-contact batch is logged only.
7. A pending response row is created with `response_status = NULL`; when `YES` arrives, that row becomes visible to the requestor through `/requests/user/{userId}/responses`.
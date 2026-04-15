# Blood Donation Project Stack

This repository contains a Docker Compose-based stack for the blood donation project. The stack is split into application services, relational databases, search/analytics services, and a static frontend.

## Scaling Model

The stack uses service-level horizontal scaling where it makes sense:

- In single-node mode, Elasticsearch and ClickHouse run as single instances.
- In full mode, Elasticsearch runs as a 3-node cluster for search and index availability.
- In full mode, ClickHouse runs as a 3-node cluster for analytics storage and distributed reads.
- In full mode, PostgreSQL adds a streaming read replica (`postgres-replica`) to the primary.
- The Redcross and WHO databases run as separate PostgreSQL instances.
- Backend services, Grafana, and the frontend remain single instances in the current setup.

This is infrastructure scaling, not application-level sharding. No business logic is embedded in the stack definition.

## Startup Modes

One Docker Compose file drives both modes:

- `docker-compose.yml` is the only stack definition.
- `SINGLE_MODE=true` in [`.env`](.env) starts the lightweight 1-node mode.
- `SINGLE_MODE=false` plus the `full` profile starts the 3-node Elasticsearch and ClickHouse setup with the PostgreSQL read replica.

Run either mode directly from the terminal:

```powershell

# Build once (sufficient for both modes)
docker compose build

$env:SINGLE_MODE="true"; 

# Single-node mode: build
docker compose build

# Single-node mode: start
docker compose up -d

# Single-node mode: remove containers and network
docker compose down --remove-orphans


$env:SINGLE_MODE="false"; 

# Full cluster mode: build
docker compose --profile full build

# Full cluster mode: start
docker compose --profile full up -d

# Full cluster mode: remove containers and network
docker compose --profile full down --remove-orphans

# Remove stack and ALL project volumes (use when you want a clean reset)
docker compose --profile full down -v --remove-orphans
```

Windows cmd equivalents:

```cmd
set SINGLE_MODE=true && docker compose up -d
set SINGLE_MODE=false && docker compose --profile full up -d
docker compose --profile full down -v --remove-orphans
```

Important when switching between single mode and full mode:

- Run `docker compose --profile full down -v --remove-orphans` before starting in the other mode.
- Elasticsearch persists cluster voting metadata in volumes; reusing old volumes across modes can prevent startup.

## Communication Flow

Services communicate directly over the Docker Compose network using service names:

- The backend connects to `postgres-primary` and `elasticsearch`.
- The Redcross service connects to `redcross-db`.
- The WHO service connects to `who-db`.
- Grafana connects to Elasticsearch and ClickHouse through provisioned datasources.
- The ETL service, when run, talks to the source services and pushes data to ClickHouse and Elasticsearch.
- The ETL service does not start bulk loading automatically. Call `GET http://localhost:8083/admin/etl/bulk-load` to trigger the bulk load manually, then incremental runs become eligible.
- Set `ETL_INCREMENTAL_INTERVAL_MS` in [`.env`](.env) to change how often incremental ETL runs. The default is `300000` ms, which is 5 minutes.

The Spring Boot services resolve database and search settings from environment variables through their application property files. The ETL service reads its runtime URLs and credentials from environment variables directly, which are still provided by the shared `.env` file.

For local host access, the application and PostgreSQL services are published on ports such as `8080`, `8081`, `8082`, `3000`, `3001`, `5432`, `5433`, `5434`, and `5435`. Elasticsearch and ClickHouse are internal-only.

## Runbook

Use this order when you want to exercise the full flow end to end:

1. Start the stack with `docker compose up -d --build`.
2. Generate dummy source data and refresh the CSV preview with `python db-seeder/seed.py`.
3. Trigger the ETL bulk load manually with `GET http://localhost:8083/admin/etl/bulk-load`.
4. Open the frontend at `http://localhost:3001`.
5. Use the blood-bank tab for anonymous search.
6. Use the donor tab for the authenticated donor flow.
7. Open Grafana at `http://localhost:3000`.

Trigger endpoints used by the UI and services:

- `GET /api/backend/reference-data` - blood group and component options for the UI.
- `POST /api/backend/auth/send-otp` - start donor login.
- `POST /api/backend/auth/verify-otp` - verify donor login.
- `POST /api/backend/users/get-or-create` - create or resolve the donor user.
- `GET /api/backend/blood-banks/search?pincode={pincode}` - blood bank list served by backend from Elasticsearch data.
- `GET /api/backend/donors/search` - donor search backed by Elasticsearch.
- `POST /api/backend/searches/{userId}` - create a search record.
- `POST /api/backend/requests/{searchId}` - create a request from a search.
- `POST /api/backend/requests/{requestId}/re-request` - create a follow-up request.
- `POST /api/backend/requests/{requestId}/dispatch-next` - notify the next 20 donors.
- `GET /api/backend/requests/user/{userId}` - fetch a user’s requests.
- `GET /api/backend/requests/user/{userId}/responses` - fetch donor responses.
- `GET /api/backend/requests/user/{userId}/active` - check for an active request.

## Proxies

The frontend container uses Nginx as a lightweight reverse proxy for the API routes.

- The frontend is published on port `3001` and serves the compiled UI from `frontend/`.
- Nginx forwards `/api/backend` to the backend service by Docker DNS name.
- WHO and Redcross endpoints are used by ETL as simulated external source systems, not by frontend or backend request flows.
- Containers still talk to each other directly by service name inside the Compose network.
- The ETL service should use Docker service DNS names when it runs in the stack network.

## Where Data Lives

- `postgres-primary`: main blood bank application data, initialized from `backend/src/main/resources/schema.sql`.
- `postgres-replica`: streaming replica of `postgres-primary` for read-scaling and replication topology.
- `redcross-db`: Redcross source data, initialized from `Redcross/src/main/resources/sourcetables.sql`.
- `who-db`: WHO source data, initialized from `who/src/main/resources/source_coder.sql`.
- `elasticsearch`: indexed search documents for blood banks and donors.
- `clickhouse`: analytics tables in the `blood_ops` database.
- `grafana`: dashboards and local state in the Grafana named volume.
- `frontend`: static assets baked into the image, with no persistent application data.

## Notes

- Elasticsearch template JSON files remain in `elastic search/resources/` for later alignment with index naming.
- The Elasticsearch init container was removed from Compose because template loading is not required for the cluster to start.
- The current stack is intentionally minimal and focused on infrastructure wiring rather than feature logic.
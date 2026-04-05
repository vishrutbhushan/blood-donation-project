# Blood Donation Project Stack

This repository contains a Docker Compose-based stack for the blood donation project. The stack is split into application services, relational databases, search/analytics services, and a static frontend.

## Scaling Model

The stack uses service-level horizontal scaling where it makes sense:

- Elasticsearch runs as a 3-node cluster for search and index availability.
- ClickHouse runs as a 3-node cluster for analytics storage and distributed reads.
- PostgreSQL uses a primary-replica topology with streaming replication for read scaling and failover-oriented architecture.
- The Redcross and WHO databases run as separate PostgreSQL instances.
- Backend services, Grafana, and the frontend remain single instances in the current setup.

This is infrastructure scaling, not application-level sharding. No business logic is embedded in the stack definition.

## Communication Flow

Services communicate directly over the Docker Compose network using service names:

- The backend connects to `postgres-primary` and `elasticsearch`.
- The Redcross service connects to `redcross-db`.
- The WHO service connects to `who-db`.
- Grafana connects to Elasticsearch and ClickHouse through provisioned datasources.
- The ETL service, when run, talks to the source services and pushes data to ClickHouse and Elasticsearch.

For local host access, the application and PostgreSQL services are published on ports such as `8080`, `8081`, `8082`, `3000`, `3001`, `5432`, `5433`, `5434`, and `5435`. Elasticsearch and ClickHouse are internal-only.

## Proxies

There is no reverse proxy layer in the current stack.

- No Nginx, Traefik, or API gateway sits in front of the services.
- Containers talk to each other directly by service name.
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
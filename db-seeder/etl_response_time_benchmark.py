import json
import math
import os
import subprocess
import time
from copy import deepcopy
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse, urlunparse

import matplotlib.pyplot as plt
import psycopg2
import requests

WHO_GEO_SQL = """
WITH latest_inventory AS (
    SELECT DISTINCT ON (it.bb_id, it.blood_group, UPPER(it.component))
        it.bb_id,
        it.blood_group,
        UPPER(it.component) AS component,
        it.running_balance_after,
        it.event_timestamp,
        it.transaction_id
    FROM inventory_transaction it
    WHERE it.blood_group = %s
      AND UPPER(it.component) = %s
    ORDER BY it.bb_id, it.blood_group, UPPER(it.component), it.event_timestamp DESC, it.transaction_id DESC
)
SELECT
    b.bb_id::text AS source_bank_id,
    b.name AS bank_name,
    b.phone AS contact_number,
    b.street AS address,
    b.city,
    b.state,
    b.pincode,
    li.running_balance_after AS units_available
FROM latest_inventory li
JOIN blood_bank b ON b.bb_id = li.bb_id
WHERE li.running_balance_after > 0;
"""

REDCROSS_GEO_SQL = """
WITH latest_inventory AS (
    SELECT DISTINCT ON (it.bb_id, it.blood_group, UPPER(it.component))
        it.bb_id,
        it.blood_group,
        UPPER(it.component) AS component,
        it.running_balance_after,
        it.event_timestamp,
        it.transaction_id
    FROM inventory_transaction it
    WHERE it.blood_group = %s
      AND UPPER(it.component) = %s
    ORDER BY it.bb_id, it.blood_group, UPPER(it.component), it.event_timestamp DESC, it.transaction_id DESC
)
SELECT
    b.bb_id::text AS source_bank_id,
    b.name AS bank_name,
    b.contact_number,
    b.full_address AS address,
    NULL::text AS city,
    NULL::text AS state,
    b.postal_code AS pincode,
    li.running_balance_after AS units_available
FROM latest_inventory li
JOIN blood_bank b ON b.bb_id = li.bb_id
WHERE li.running_balance_after > 0;
"""

WHO_AGG_SQL = """
SELECT
    COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'INFLOW' THEN GREATEST(units_delta, 0) ELSE 0 END), 0) AS inflow_units,
    COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'OUTFLOW' THEN ABS(LEAST(units_delta, 0)) ELSE 0 END), 0) AS outflow_units
FROM inventory_transaction
WHERE event_timestamp >= %s
  AND event_timestamp < %s;
"""

REDCROSS_AGG_SQL = """
SELECT
    COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'INFLOW' THEN GREATEST(units_delta, 0) ELSE 0 END), 0) AS inflow_units,
    COALESCE(SUM(CASE WHEN UPPER(transaction_type) = 'OUTFLOW' THEN ABS(LEAST(units_delta, 0)) ELSE 0 END), 0) AS outflow_units
FROM inventory_transaction
WHERE event_timestamp >= %s
  AND event_timestamp < %s;
"""

CLICKHOUSE_AGG_SQL = """
SELECT
    COALESCE(SUM(inflow_units), 0) AS inflow_units,
    COALESCE(SUM(outflow_units), 0) AS outflow_units
FROM blood_ops.fact_inventory_day
WHERE event_date >= toDate('{start_date}')
  AND event_date < toDate('{end_date}')
FORMAT JSON
"""

ELASTIC_GEO_QUERY_TEMPLATE = {
    "size": 20,
    "_source": [
        "source",
        "blood_bank_id",
        "blood_bank_name",
        "contact_number",
        "address",
        "city",
        "state",
        "pincode",
        "blood_group",
        "component",
        "units_available",
        "location"
    ],
    "query": {
        "bool": {
            "filter": [
                {"term": {"blood_group": "AB-"}},
                {"term": {"component": "WHOLE BLOOD"}},
                {"range": {"units_available": {"gt": 0}}}
            ]
        }
    },
    "sort": [
        {
            "_geo_distance": {
                "location": {"lat": 0.0, "lon": 0.0},
                "order": "asc",
                "unit": "km",
                "distance_type": "arc"
            }
        }
    ]
}

TARGET_BLOOD_GROUP = "AB-"
TARGET_COMPONENT = "WHOLE BLOOD"
TARGET_PINCODE = "110001"
GEO_RESULT_LIMIT = 20
AGG_START_DATE = "2025-01-01"
AGG_END_DATE = "2026-01-01"
ELASTIC_INDEX_BANKS = "bb_inventory_current"
ELASTIC_CONTAINER_NAME = "hemo-elasticsearch"
CLICKHOUSE_CONTAINER_NAME = "hemo-clickhouse"
OUTPUT_DIR = Path("db-seeder") / "benchmark_output"
PINCODE_MAP_PATH = Path("etl-service") / "src" / "main" / "resources" / "pincode-map.json"


@dataclass
class PostgresConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str


def load_environment() -> None:
    env_path = Path(".env")
    if not env_path.exists():
        return
    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if key and key not in os.environ:
            os.environ[key] = value


def postgres_config(prefix: str, default_port: int, default_db: str, default_user: str, default_password: str) -> PostgresConfig:
    return PostgresConfig(
        host=os.getenv(f"{prefix}_HOST", "localhost"),
        port=int(os.getenv(f"{prefix}_PORT", str(default_port))),
        dbname=os.getenv(f"{prefix}_DB_NAME", default_db),
        user=os.getenv(f"{prefix}_DB_USER", default_user),
        password=os.getenv(f"{prefix}_DB_PASSWORD", default_password),
    )


def connect_postgres(cfg: PostgresConfig):
    return psycopg2.connect(
        host=cfg.host,
        port=cfg.port,
        dbname=cfg.dbname,
        user=cfg.user,
        password=cfg.password,
    )


def normalize_url_for_host_runtime(raw_url: str) -> str:
    parsed = urlparse(raw_url)
    if parsed.hostname not in {"elasticsearch", "clickhouse"}:
        return raw_url
    new_netloc = "localhost"
    if parsed.port is not None:
        new_netloc = f"localhost:{parsed.port}"
    return urlunparse((parsed.scheme, new_netloc, parsed.path, parsed.params, parsed.query, parsed.fragment))


def with_host(url: str, host: str, fallback_port: int) -> str:
    parsed = urlparse(url)
    scheme = parsed.scheme or "http"
    port = parsed.port if parsed.port is not None else fallback_port
    netloc = f"{host}:{port}"
    return urlunparse((scheme, netloc, parsed.path, parsed.params, parsed.query, parsed.fragment))


def docker_container_ip(container_name: str) -> str | None:
    try:
        ip = subprocess.check_output(
            [
                "docker",
                "inspect",
                "-f",
                "{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
                container_name,
            ],
            text=True,
        ).strip()
    except Exception:
        return None
    return ip or None


def resolve_elastic_url() -> str:
    raw = os.getenv("ELASTIC_URL", "http://localhost:9200")
    candidates = []
    for item in [normalize_url_for_host_runtime(raw), raw]:
        if item and item not in candidates:
            candidates.append(item)
    ip = docker_container_ip(ELASTIC_CONTAINER_NAME)
    if ip is not None:
        ip_url = with_host(raw, ip, 9200)
        if ip_url not in candidates:
            candidates.append(ip_url)
    last_error = None
    for base in candidates:
        try:
            probe = requests.get(f"{base.rstrip('/')}/_cluster/health", timeout=5)
            probe.raise_for_status()
            return base.rstrip("/")
        except requests.RequestException as e:
            last_error = e
    if last_error is not None:
        raise last_error
    raise RuntimeError("no Elasticsearch endpoint resolved")


def resolve_clickhouse_url(clickhouse_user: str, clickhouse_password: str) -> str:
    raw = os.getenv("CLICKHOUSE_URL", "http://localhost:8123")
    candidates = []
    for item in [normalize_url_for_host_runtime(raw), raw]:
        if item and item not in candidates:
            candidates.append(item)
    ip = docker_container_ip(CLICKHOUSE_CONTAINER_NAME)
    if ip is not None:
        ip_url = with_host(raw, ip, 8123)
        if ip_url not in candidates:
            candidates.append(ip_url)
    headers = {
        "X-ClickHouse-User": clickhouse_user,
        "X-ClickHouse-Key": clickhouse_password,
    }
    last_error = None
    for base in candidates:
        try:
            probe = requests.post(base.rstrip("/"), data="SELECT 1 FORMAT JSON", headers=headers, timeout=5)
            probe.raise_for_status()
            return base.rstrip("/")
        except requests.RequestException as e:
            last_error = e
    if last_error is not None:
        raise last_error
    raise RuntimeError("no ClickHouse endpoint resolved")


def docker_exec_etl_curl_json(url: str, body: str, headers: list[str]) -> dict:
    cmd = ["docker", "exec", "etl-service", "curl", "-sS"]
    for header in headers:
        cmd.extend(["-H", header])
    cmd.extend(["--data-binary", body, url])
    out = subprocess.check_output(cmd, text=True)
    return json.loads(out)


def read_pincode_map() -> dict:
    with PINCODE_MAP_PATH.open("r", encoding="utf-8") as f:
        return json.load(f)


def pincode_to_latlon(pincode_map: dict, pincode: str) -> tuple[float, float]:
    row = pincode_map.get(str(pincode).strip())
    if row is None:
        raise ValueError(f"pincode {pincode} not found in map")
    return float(row["lat"]), float(row["long"])


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    p1 = math.radians(lat1)
    p2 = math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return r * c


def fetch_geo_candidates(conn, sql: str, blood_group: str, component: str, source_name: str) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(sql, (blood_group, component.upper()))
        rows = cur.fetchall()
    out = []
    for r in rows:
        out.append(
            {
                "source": source_name,
                "source_bank_id": str(r[0]),
                "blood_bank_name": r[1],
                "contact_number": r[2],
                "address": r[3],
                "city": r[4],
                "state": r[5],
                "pincode": str(r[6]) if r[6] is not None else "",
                "units_available": int(r[7]),
            }
        )
    return out


def nearest_candidates(candidates: list[dict], pincode_map: dict, center_lat: float, center_lon: float, limit: int) -> list[dict]:
    ranked = []
    for row in candidates:
        pc = row.get("pincode", "")
        if pc not in pincode_map:
            continue
        lat, lon = pincode_to_latlon(pincode_map, pc)
        distance = haversine_km(center_lat, center_lon, lat, lon)
        item = dict(row)
        item["distance_km"] = distance
        ranked.append(item)
    ranked.sort(key=lambda x: x["distance_km"])
    return ranked[:limit]


def run_sql_geo_benchmark(who_conn, redcross_conn, pincode_map: dict, center_lat: float, center_lon: float) -> tuple[float, list[dict]]:
    t0 = time.perf_counter()
    who_rows = fetch_geo_candidates(who_conn, WHO_GEO_SQL, TARGET_BLOOD_GROUP, TARGET_COMPONENT, "who")
    redcross_rows = fetch_geo_candidates(redcross_conn, REDCROSS_GEO_SQL, TARGET_BLOOD_GROUP, TARGET_COMPONENT, "redcross")
    ranked = nearest_candidates(who_rows + redcross_rows, pincode_map, center_lat, center_lon, GEO_RESULT_LIMIT)
    elapsed_ms = (time.perf_counter() - t0) * 1000.0
    return elapsed_ms, ranked


def run_elastic_geo_benchmark(center_lat: float, center_lon: float) -> tuple[float, list[dict]]:
    query = deepcopy(ELASTIC_GEO_QUERY_TEMPLATE)
    query["size"] = GEO_RESULT_LIMIT
    query["query"]["bool"]["filter"][0]["term"]["blood_group"] = TARGET_BLOOD_GROUP
    query["query"]["bool"]["filter"][1]["term"]["component"] = TARGET_COMPONENT
    query["sort"][0]["_geo_distance"]["location"] = {"lat": center_lat, "lon": center_lon}
    try:
        elastic_url = resolve_elastic_url()
        t0 = time.perf_counter()
        response = requests.post(
            f"{elastic_url}/{ELASTIC_INDEX_BANKS}/_search",
            json=query,
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
    except Exception:
        t0 = time.perf_counter()
        data = docker_exec_etl_curl_json(
            f"http://elasticsearch:9200/{ELASTIC_INDEX_BANKS}/_search",
            json.dumps(query),
            ["Content-Type: application/json"],
        )
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
    hits = [h.get("_source", {}) for h in data.get("hits", {}).get("hits", [])]
    return elapsed_ms, hits


def fetch_aggregate(conn, sql: str, start_date: str, end_date: str) -> tuple[float, float]:
    with conn.cursor() as cur:
        cur.execute(sql, (start_date, end_date))
        row = cur.fetchone()
    return float(row[0] or 0), float(row[1] or 0)


def run_sql_aggregate_benchmark(who_conn, redcross_conn) -> tuple[float, dict]:
    t0 = time.perf_counter()
    who_inflow, who_outflow = fetch_aggregate(who_conn, WHO_AGG_SQL, AGG_START_DATE, AGG_END_DATE)
    rc_inflow, rc_outflow = fetch_aggregate(redcross_conn, REDCROSS_AGG_SQL, AGG_START_DATE, AGG_END_DATE)
    elapsed_ms = (time.perf_counter() - t0) * 1000.0
    total = {
        "inflow_units": who_inflow + rc_inflow,
        "outflow_units": who_outflow + rc_outflow,
    }
    return elapsed_ms, total


def run_clickhouse_aggregate_benchmark() -> tuple[float, dict]:
    clickhouse_user = os.getenv("CLICKHOUSE_USER", "default")
    clickhouse_password = os.getenv("CLICKHOUSE_PASSWORD", "clickhouse")
    query = CLICKHOUSE_AGG_SQL.format(start_date=AGG_START_DATE, end_date=AGG_END_DATE)
    try:
        clickhouse_url = resolve_clickhouse_url(clickhouse_user, clickhouse_password)
        headers = {
            "X-ClickHouse-User": clickhouse_user,
            "X-ClickHouse-Key": clickhouse_password,
        }
        t0 = time.perf_counter()
        response = requests.post(clickhouse_url, data=query, headers=headers, timeout=30)
        response.raise_for_status()
        payload = response.json()
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
    except Exception:
        t0 = time.perf_counter()
        payload = docker_exec_etl_curl_json(
            "http://clickhouse:8123",
            query,
            [f"X-ClickHouse-User: {clickhouse_user}", f"X-ClickHouse-Key: {clickhouse_password}"],
        )
        elapsed_ms = (time.perf_counter() - t0) * 1000.0

    data_rows = payload.get("data", [])
    row = data_rows[0] if data_rows else {"inflow_units": 0, "outflow_units": 0}
    result = {
        "inflow_units": float(row.get("inflow_units", 0)),
        "outflow_units": float(row.get("outflow_units", 0)),
    }
    return elapsed_ms, result


def save_bar_chart(title: str, labels: list[str], values: list[float], output_file: Path) -> None:
    output_file.parent.mkdir(parents=True, exist_ok=True)
    plt.figure(figsize=(8, 5))
    bars = plt.bar(labels, values, color=["#1f77b4", "#ff7f0e"])
    plt.title(title)
    plt.ylabel("Response Time (ms)")
    plt.grid(axis="y", linestyle="--", alpha=0.3)
    for bar, value in zip(bars, values):
        plt.text(bar.get_x() + bar.get_width() / 2, value, f"{value:.2f}", ha="center", va="bottom")
    plt.tight_layout()
    plt.savefig(output_file, dpi=200)
    plt.close()


def main() -> None:
    load_environment()

    who_cfg = postgres_config("WHO", 5435, "who_db", "who_user", "who_pass")
    redcross_cfg = postgres_config("REDCROSS", 5434, "redcross_db", "redcross_user", "redcross_pass")

    pincode_map = read_pincode_map()
    center_lat, center_lon = pincode_to_latlon(pincode_map, TARGET_PINCODE)

    with connect_postgres(who_cfg) as who_conn, connect_postgres(redcross_cfg) as redcross_conn:
        sql_geo_ms, _ = run_sql_geo_benchmark(who_conn, redcross_conn, pincode_map, center_lat, center_lon)
        elastic_geo_ms, _ = run_elastic_geo_benchmark(center_lat, center_lon)
        sql_agg_ms, _ = run_sql_aggregate_benchmark(who_conn, redcross_conn)

    clickhouse_agg_ms, _ = run_clickhouse_aggregate_benchmark()

    save_bar_chart(
        "Geo Filter Response Time: SQL vs Elasticsearch",
        ["Normal SQL", "Elasticsearch"],
        [sql_geo_ms, elastic_geo_ms],
        OUTPUT_DIR / "geo_sql_vs_elasticsearch.png",
    )

    save_bar_chart(
        "Aggregate Response Time: SQL vs ClickHouse",
        ["Normal SQL", "ClickHouse"],
        [sql_agg_ms, clickhouse_agg_ms],
        OUTPUT_DIR / "aggregate_sql_vs_clickhouse.png",
    )


if __name__ == "__main__":
    main()

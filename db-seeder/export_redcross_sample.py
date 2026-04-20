"""
Export 500 random rows from each Red Cross table into a single CSV.
Output: redcross_sample_500.csv  (500 blood_bank + 500 donor + 500 inventory_transaction)
"""
import csv, psycopg2, random
from pathlib import Path

OUT = Path(__file__).parent / "redcross_sample_500.csv"

conn = psycopg2.connect(
    host="localhost", port=5434,
    dbname="redcross_db", user="redcross_user", password="redcross_pass"
)
cur = conn.cursor()

rows = []

# ---------------------------------------------------------------------------
# blood_bank — 500 random rows
# ---------------------------------------------------------------------------
cur.execute("""
    SELECT bb_id, name, category, contact_number, email,
           full_address, postal_code, created_at, updated_at
    FROM blood_bank
    ORDER BY RANDOM()
    LIMIT 500
""")
for r in cur.fetchall():
    rows.append({
        "table":               "blood_bank",
        "bb_id":               r[0],
        "name":                r[1],
        "category":            r[2],
        "contact_number":      r[3],
        "email":               r[4],
        "full_address":        r[5],
        "postal_code":         r[6],
        "created_at":          r[7],
        "updated_at":          r[8],
        # donor-only fields
        "donor_id":            "",
        "national_id":         "",
        "blood_type":          "",
        "age":                 "",
        "last_donation_date":  "",
        # inventory-only fields
        "transaction_id":      "",
        "source_event_id":     "",
        "donor_id_ref":        "",
        "blood_group":         "",
        "component":           "",
        "transaction_type":    "",
        "units_delta":         "",
        "running_balance_after": "",
        "expiry_date":         "",
        "event_timestamp":     "",
    })

# ---------------------------------------------------------------------------
# donor — 500 random rows
# ---------------------------------------------------------------------------
cur.execute("""
    SELECT donor_id, bb_id, full_name, national_id, contact_number,
           address, blood_type, age, last_donation_date, created_at, updated_at
    FROM donor
    ORDER BY RANDOM()
    LIMIT 500
""")
for r in cur.fetchall():
    rows.append({
        "table":               "donor",
        "bb_id":               r[1],
        "name":                r[2],
        "category":            "",
        "contact_number":      r[4],
        "email":               "",
        "full_address":        r[5],
        "postal_code":         "",
        "created_at":          r[9],
        "updated_at":          r[10],
        "donor_id":            r[0],
        "national_id":         r[3],
        "blood_type":          r[6],
        "age":                 r[7],
        "last_donation_date":  r[8],
        "transaction_id":      "",
        "source_event_id":     "",
        "donor_id_ref":        "",
        "blood_group":         "",
        "component":           "",
        "transaction_type":    "",
        "units_delta":         "",
        "running_balance_after": "",
        "expiry_date":         "",
        "event_timestamp":     "",
    })

# ---------------------------------------------------------------------------
# inventory_transaction — 500 random rows
# ---------------------------------------------------------------------------
cur.execute("""
    SELECT transaction_id, source_event_id, bb_id, donor_id,
           blood_group, component, transaction_type, units_delta,
           running_balance_after, expiry_date, event_timestamp,
           created_at, updated_at
    FROM inventory_transaction
    ORDER BY RANDOM()
    LIMIT 500
""")
for r in cur.fetchall():
    rows.append({
        "table":               "inventory_transaction",
        "bb_id":               r[2],
        "name":                "",
        "category":            "",
        "contact_number":      "",
        "email":               "",
        "full_address":        "",
        "postal_code":         "",
        "created_at":          r[11],
        "updated_at":          r[12],
        "donor_id":            "",
        "national_id":         "",
        "blood_type":          "",
        "age":                 "",
        "last_donation_date":  "",
        "transaction_id":      r[0],
        "source_event_id":     r[1],
        "donor_id_ref":        r[3] if r[3] else "",
        "blood_group":         r[4],
        "component":           r[5],
        "transaction_type":    r[6],
        "units_delta":         r[7],
        "running_balance_after": r[8],
        "expiry_date":         r[9] if r[9] else "",
        "event_timestamp":     r[10],
    })

cur.close()
conn.close()

# ---------------------------------------------------------------------------
# Write CSV
# ---------------------------------------------------------------------------
FIELDNAMES = [
    "table",
    # shared
    "bb_id", "name", "category", "contact_number", "email",
    "full_address", "postal_code", "created_at", "updated_at",
    # donor
    "donor_id", "national_id", "blood_type", "age", "last_donation_date",
    # inventory_transaction
    "transaction_id", "source_event_id", "donor_id_ref",
    "blood_group", "component", "transaction_type",
    "units_delta", "running_balance_after", "expiry_date", "event_timestamp",
]

with open(OUT, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=FIELDNAMES)
    writer.writeheader()
    writer.writerows(rows)

counts = {"blood_bank": 0, "donor": 0, "inventory_transaction": 0}
for r in rows:
    counts[r["table"]] += 1

print(f"Written: {OUT}")
print(f"  blood_bank            : {counts['blood_bank']:,} rows")
print(f"  donor                 : {counts['donor']:,} rows")
print(f"  inventory_transaction : {counts['inventory_transaction']:,} rows")
print(f"  Total                 : {sum(counts.values()):,} rows")

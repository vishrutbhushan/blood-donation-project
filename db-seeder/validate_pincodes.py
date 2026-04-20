"""
Validate that all pincodes in both DBs are in pincode.js.

Red Cross DB (port 5434):
  blood_bank.postal_code  — direct column
  donor.address           — pincode embedded at end: "..., State - XXXXXX"

WHO DB (port 5435):
  blood_bank.pincode      — direct column
  donor.pincode           — direct column
"""
import json, re, psycopg2
from pathlib import Path

# ---------------------------------------------------------------------------
# Load valid pincode set from pincode.js
# ---------------------------------------------------------------------------
raw = Path(__file__).resolve().parents[1].joinpath("pincode.js").read_text(encoding="utf-8")
body = re.sub(r"^\s*let\s+pincodelist\s*=\s*", "", raw, flags=re.DOTALL)
body = re.sub(r"\s*;\s*module\.exports\s*=\s*pincodelist\s*;\s*$", "", body, flags=re.DOTALL)
VALID_PINS = set(json.loads(body).keys())
print(f"pincode.js loaded — {len(VALID_PINS):,} valid pincodes\n")


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------
def check_pins(label, pins, row_count_by_pin=None):
    """Print validation summary for a set of pincodes."""
    distinct = len(pins)
    valid   = pins & VALID_PINS
    invalid = pins - VALID_PINS
    pct_v   = len(valid)   / distinct * 100 if distinct else 0
    pct_i   = len(invalid) / distinct * 100 if distinct else 0

    print(f"  {label}:")
    print(f"    Distinct pincodes : {distinct:,}")
    print(f"    Valid   (in pincode.js): {len(valid):,}  ({pct_v:.1f}%)")
    print(f"    Invalid (not in list)  : {len(invalid):,}  ({pct_i:.1f}%)")

    if invalid and row_count_by_pin:
        affected = sum(row_count_by_pin.get(p, 0) for p in invalid)
        total    = sum(row_count_by_pin.values())
        print(f"    Affected rows    : {affected:,} / {total:,}  ({affected/total*100:.2f}%)")
        top = sorted(invalid, key=lambda p: -row_count_by_pin.get(p, 0))[:10]
        print(f"    Top invalid pincodes:")
        for p in top:
            print(f"      {p}: {row_count_by_pin.get(p, 0):,} rows")
    elif not invalid:
        print(f"    ✓ ALL pincodes are valid!")
    return invalid


# ---------------------------------------------------------------------------
# RED CROSS DB
# ---------------------------------------------------------------------------
print("=" * 60)
print("  RED CROSS DATABASE (port 5434)")
print("=" * 60)

rc = psycopg2.connect(host="localhost", port=5434, dbname="redcross_db",
                      user="redcross_user", password="redcross_pass")
cur = rc.cursor()

# blood_bank.postal_code
cur.execute("SELECT postal_code, COUNT(*) FROM blood_bank GROUP BY postal_code")
rc_bank_counts = {r[0]: r[1] for r in cur.fetchall() if r[0]}
rc_bank_pins   = set(rc_bank_counts)
check_pins("blood_bank  (postal_code)", rc_bank_pins, rc_bank_counts)

print()

# donor.address — extract trailing 6-digit pincode after " - "
cur.execute(r"""
    SELECT SUBSTRING(address FROM '- (\d{6})$') AS pin, COUNT(*)
    FROM donor
    WHERE address IS NOT NULL
    GROUP BY pin
""")
rc_donor_counts = {r[0]: r[1] for r in cur.fetchall() if r[0]}
rc_donor_pins   = set(rc_donor_counts)
check_pins("donor       (address → pincode)", rc_donor_pins, rc_donor_counts)

cur.close()
rc.close()

# ---------------------------------------------------------------------------
# WHO DB
# ---------------------------------------------------------------------------
print()
print("=" * 60)
print("  WHO DATABASE (port 5435)")
print("=" * 60)

who = psycopg2.connect(host="localhost", port=5435, dbname="who_db",
                       user="who_user", password="who_pass")
cur = who.cursor()

# blood_bank.pincode
cur.execute("SELECT pincode, COUNT(*) FROM blood_bank GROUP BY pincode")
who_bank_counts = {r[0]: r[1] for r in cur.fetchall() if r[0]}
who_bank_pins   = set(who_bank_counts)
check_pins("blood_bank  (pincode)", who_bank_pins, who_bank_counts)

print()

# donor.pincode
cur.execute("SELECT pincode, COUNT(*) FROM donor GROUP BY pincode")
who_donor_counts = {r[0]: r[1] for r in cur.fetchall() if r[0]}
who_donor_pins   = set(who_donor_counts)
check_pins("donor       (pincode)", who_donor_pins, who_donor_counts)

cur.close()
who.close()

# ---------------------------------------------------------------------------
# Overall summary
# ---------------------------------------------------------------------------
print()
print("=" * 60)
print("  SUMMARY")
print("=" * 60)
all_dbs = [
    ("Red Cross blood_bank", rc_bank_pins),
    ("Red Cross donor",      rc_donor_pins),
    ("WHO blood_bank",       who_bank_pins),
    ("WHO donor",            who_donor_pins),
]
all_ok = True
for label, pins in all_dbs:
    invalid = pins - VALID_PINS
    status  = "✓ OK" if not invalid else f"✗ {len(invalid)} invalid pincodes"
    print(f"  {label:35s}: {status}")
    if invalid:
        all_ok = False
print()
if all_ok:
    print("  ALL pincodes across both databases are valid!")

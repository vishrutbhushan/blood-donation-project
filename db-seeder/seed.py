"""
Realistic DB Seeder — populates redcross-db (port 5434) and who-db (port 5435)
with India-accurate blood bank data.

Usage:
    cd db-seeder
    source .venv/bin/activate
    python3 seed.py

Scale control:
    BANKS_PER_RUN  = 50         ->  50 new blood banks per source per run
    DONORS_PER_RUN = 2500       ->  2500 new donors per source per run
    INVENTORY_TXN_PER_RUN = 5000 -> 5000 new inventory transactions per source per run

Runs APPEND — data is never wiped. Each donor gets a guaranteed-unique
UUID-based identifier so re-runs never produce duplicates.
Run once → 50 banks + 2500 donors + 5000 inventory transactions per source.
"""

import csv
import hashlib
import json
import random
import re
from datetime import date, datetime, timedelta
from pathlib import Path

import psycopg2
from psycopg2.extras import execute_values

# ---------------------------------------------------------------------------
# Tune per run — adjust without restarting Docker
# ---------------------------------------------------------------------------
BANKS_PER_RUN  = 50               # new blood banks to insert for each source per invocation
DONORS_PER_RUN = 2_500            # new donors to insert for each source per invocation
INVENTORY_TXN_PER_RUN = 5_000     # inventory transactions to insert for each source per run
BATCH_SIZE     = 10_000           # rows per executemany batch (keeps memory bounded)
CSV_PREVIEW_ROWS = 100        # overwrite a 100-row preview CSV on each run
CSV_PREVIEW_PATH = Path(__file__).resolve().with_name("generated_seed_preview.csv")
RANDOM_SEED = 42

random.seed(RANDOM_SEED)

CSV_PREVIEW_HEADERS = [
    "source",
    "entity_type",
    "bank_id",
    "record_id",
    "name",
    "blood_group",
    "component",
    "category",
    "contact",
    "email",
    "address",
    "city",
    "state",
    "pincode",
    "age",
    "quantity",
    "units_available",
    "event_time",
    "updated_at",
    "notes",
]

# ---------------------------------------------------------------------------
# DB connections -- ports from docker-compose.yml
# ---------------------------------------------------------------------------
REDCROSS_DSN = dict(
    host="localhost", port=5434,
    dbname="redcross_db", user="redcross_user", password="redcross_pass",
)
WHO_DSN = dict(
    host="localhost", port=5435,
    dbname="who_db", user="who_user", password="who_pass",
)

# ---------------------------------------------------------------------------
# Indian blood group distribution (source: NBTC / published studies)
#   O+  37%  | B+  25%  | A+  22%  | AB+  7%
#   O-   3%  | B-   3%  | A-   2%  | AB-  1%
#   Rare canonical labels used by the platform:
#   BOMBAY (hh) and RH NULL
# ---------------------------------------------------------------------------
BLOOD_GROUP_DIST = {
    "O+": 37, "B+": 25, "A+": 22, "AB+": 7,
    "O-":  3, "B-":  3, "A-":  2, "AB-": 1,
    "BOMBAY": 0.000001,
    "RH NULL": 0.0000001,
}
BLOOD_GROUPS = list(BLOOD_GROUP_DIST.keys())
BG_WEIGHTS   = list(BLOOD_GROUP_DIST.values())

REDCROSS_COMPONENTS = ["Whole Blood", "Packed RBC", "Fresh Frozen Plasma", "Plasma", "Platelets", "Cryoprecipitate"]
WHO_COMPONENTS      = ["Whole Blood", "Packed RBC", "Fresh Frozen Plasma", "Plasma", "Platelets", "Cryoprecipitate"]


def load_pincode_pool():
    file_path = Path(__file__).resolve().parents[1] / "pincode.js"
    if not file_path.exists():
        return set()

    raw = file_path.read_text(encoding="utf-8")
    body = re.sub(r"^\s*let\s+pincodelist\s*=\s*", "", raw, flags=re.DOTALL)
    body = re.sub(r"\s*;\s*module\.exports\s*=\s*pincodelist\s*;\s*$", "", body, flags=re.DOTALL)
    data = json.loads(body)
    return set(data.keys())


PINCODE_POOL = load_pincode_pool()

# ---------------------------------------------------------------------------
# City master -- (city, state, population_weight, [real 6-digit pincodes])
# Weight is proportional to city population so big metros dominate the dataset.
# All pincodes are real India Post DPIN codes (6 digits).
# ---------------------------------------------------------------------------
CITY_DATA = [
    # Uttar Pradesh (most populous state)
    ("Lucknow",       "Uttar Pradesh",  12,
     ["226001","226002","226003","226004","226010","226012","226016","226018","226020","226022"]),
    ("Kanpur",        "Uttar Pradesh",  10,
     ["208001","208002","208003","208004","208005","208006","208007","208010","208012","208014"]),
    ("Varanasi",      "Uttar Pradesh",   8,
     ["221001","221002","221003","221004","221005","221006","221007","221010","221011","221012"]),
    ("Agra",          "Uttar Pradesh",   7,
     ["282001","282002","282003","282004","282005","282006","282007","282008","282009","282010"]),
    ("Prayagraj",     "Uttar Pradesh",   7,
     ["211001","211002","211003","211004","211005","211006","211007","211008","211009","211010"]),
    ("Meerut",        "Uttar Pradesh",   5,
     ["250001","250002","250003","250004","250005","250101","250110","250103","250104","250106"]),
    # Maharashtra
    ("Mumbai",        "Maharashtra",    18,
     ["400001","400002","400003","400004","400005","400006","400007","400008","400010","400011",
      "400012","400013","400014","400016","400018","400019","400020","400021","400022","400023"]),
    ("Pune",          "Maharashtra",    12,
     ["411001","411002","411003","411004","411005","411006","411007","411008","411009","411011",
      "411012","411013","411014","411015","411016"]),
    ("Nagpur",        "Maharashtra",     9,
     ["440001","440002","440003","440004","440005","440006","440007","440008","440009","440010",
      "440012","440013","440014","440015"]),
    ("Nashik",        "Maharashtra",     5,
     ["422001","422002","422003","422004","422005","422006","422007","422008","422009","422010"]),
    # Bihar
    ("Patna",         "Bihar",          10,
     ["800001","800002","800003","800004","800005","800006","800007","800008","800009","800010",
      "800011","800012","800013","800014","800015"]),
    ("Gaya",          "Bihar",           5,
     ["823001","823002","823003","823004","823005","823006","823007","823008"]),
    ("Muzaffarpur",   "Bihar",           4,
     ["842001","842002","842003","842004","842005","842006","842007"]),
    # West Bengal
    ("Kolkata",       "West Bengal",    15,
     ["700001","700002","700003","700004","700005","700006","700007","700008","700009","700010",
      "700012","700013","700014","700015","700016","700017","700018","700019","700020","700026"]),
    ("Howrah",        "West Bengal",     6,
     ["711101","711102","711103","711104","711105","711106","711107","711108","711109","711110"]),
    # Tamil Nadu
    ("Chennai",       "Tamil Nadu",     15,
     ["600001","600002","600003","600004","600005","600006","600007","600008","600009","600010",
      "600011","600012","600013","600014","600015","600016","600017","600018","600019","600020"]),
    ("Coimbatore",    "Tamil Nadu",      8,
     ["641001","641002","641003","641004","641005","641006","641007","641008","641009","641010",
      "641011","641012","641013","641014"]),
    ("Madurai",       "Tamil Nadu",      7,
     ["625001","625002","625003","625004","625005","625006","625007","625008","625009","625010"]),
    ("Salem",         "Tamil Nadu",      5,
     ["636001","636002","636003","636004","636005","636006","636007","636008"]),
    # Karnataka
    ("Bengaluru",     "Karnataka",      18,
     ["560001","560002","560003","560004","560005","560006","560007","560008","560009","560010",
      "560011","560012","560013","560014","560015","560016","560017","560018","560019","560020",
      "560025","560027","560029","560030","560032","560034","560038","560040","560041","560042"]),
    ("Mysuru",        "Karnataka",       6,
     ["570001","570002","570003","570004","570005","570006","570007","570008","570009","570010",
      "570011","570012","570014","570015"]),
    ("Hubballi",      "Karnataka",       5,
     ["580001","580002","580003","580004","580005","580006","580007","580008","580009","580010"]),
    # Gujarat
    ("Ahmedabad",     "Gujarat",        13,
     ["380001","380002","380003","380004","380005","380006","380007","380008","380009","380010",
      "380013","380014","380015","380016","380018","380019","380021","380022","380023","380024"]),
    ("Surat",         "Gujarat",        11,
     ["395001","395002","395003","395004","395005","395006","395007","395008","395009","395010",
      "395011","395012","395013","395014","395015","395017","395019","395021","395022","395023"]),
    ("Vadodara",      "Gujarat",         7,
     ["390001","390002","390003","390004","390005","390006","390007","390008","390009","390010",
      "390011","390012","390013","390014","390015","390016","390017","390018","390019","390020"]),
    # Rajasthan
    ("Jaipur",        "Rajasthan",      11,
     ["302001","302002","302003","302004","302005","302006","302007","302010","302011","302012",
      "302013","302015","302016","302017","302018","302019","302020","302021","302022","302023"]),
    ("Jodhpur",       "Rajasthan",       6,
     ["342001","342002","342003","342004","342005","342006","342007","342008","342009","342010",
      "342011","342012"]),
    ("Kota",          "Rajasthan",       5,
     ["324001","324002","324003","324004","324005","324006","324007","324008","324009","324010"]),
    # Madhya Pradesh
    ("Bhopal",        "Madhya Pradesh",  9,
     ["462001","462002","462003","462004","462007","462010","462011","462016","462020","462023",
      "462026","462027","462030","462031","462033"]),
    ("Indore",        "Madhya Pradesh",  9,
     ["452001","452002","452003","452004","452005","452006","452007","452008","452009","452010",
      "452011","452012","452013","452014","452015"]),
    ("Gwalior",       "Madhya Pradesh",  5,
     ["474001","474002","474003","474004","474005","474006","474007","474008","474009","474010"]),
    # Telangana
    ("Hyderabad",     "Telangana",      16,
     ["500001","500002","500003","500004","500005","500006","500007","500008","500009","500010",
      "500011","500012","500013","500014","500015","500016","500017","500018","500019","500020"]),
    ("Warangal",      "Telangana",       5,
     ["506001","506002","506003","506004","506005","506006","506007","506008","506009","506010"]),
    # Delhi
    ("New Delhi",     "Delhi",          18,
     ["110001","110002","110003","110004","110005","110006","110007","110008","110009","110010",
      "110011","110012","110013","110014","110015","110016","110017","110018","110019","110020",
      "110021","110022","110023","110024","110025","110026","110027","110028","110029","110030"]),
    # Andhra Pradesh
    ("Visakhapatnam", "Andhra Pradesh",  8,
     ["530001","530002","530003","530004","530005","530006","530007","530008","530009","530010",
      "530011","530012","530013","530014","530015","530016"]),
    ("Vijayawada",    "Andhra Pradesh",  7,
     ["520001","520002","520003","520004","520005","520006","520007","520008","520010","520011",
      "520012","520013","520014","520015"]),
    # Punjab / Haryana
    ("Ludhiana",      "Punjab",          7,
     ["141001","141002","141003","141004","141005","141006","141007","141008","141009","141010"]),
    ("Chandigarh",    "Punjab",          5,
     ["160001","160002","160003","160010","160011","160012","160014","160015","160016","160017",
      "160018","160019","160020","160022","160023","160025"]),
    ("Amritsar",      "Punjab",          5,
     ["143001","143002","143003","143004","143005","143006","143007","143008","143009","143010"]),
    ("Gurugram",      "Haryana",         6,
     ["122001","122002","122003","122004","122005","122006","122007","122008","122009","122010",
      "122011","122012","122015","122016","122017","122018"]),
    ("Faridabad",     "Haryana",         5,
     ["121001","121002","121003","121004","121005","121006","121007","121008","121009","121010"]),
    # Kerala
    ("Kochi",         "Kerala",          8,
     ["682001","682002","682003","682004","682005","682006","682007","682008","682009","682010",
      "682011","682012","682013","682014","682015","682016","682017","682018","682019","682020"]),
    ("Thiruvananthapuram", "Kerala",     7,
     ["695001","695002","695003","695004","695005","695006","695007","695008","695009","695010",
      "695011","695012","695013","695014","695015","695016"]),
    ("Kozhikode",     "Kerala",          5,
     ["673001","673002","673003","673004","673005","673006","673007","673008","673009","673010"]),
    # Odisha
    ("Bhubaneswar",   "Odisha",          6,
     ["751001","751002","751003","751004","751005","751006","751007","751008","751009","751010",
      "751011","751012","751013","751014","751015"]),
    ("Cuttack",       "Odisha",          5,
     ["753001","753002","753003","753004","753005","753006","753007","753008","753009","753010"]),
    # Jharkhand
    ("Ranchi",        "Jharkhand",       6,
     ["834001","834002","834003","834004","834005","834006","834007","834008","834009","834010"]),
    # Assam
    ("Guwahati",      "Assam",           6,
     ["781001","781002","781003","781004","781005","781006","781007","781008","781009","781010",
      "781011","781012","781013","781014"]),
    # Chhattisgarh
    ("Raipur",        "Chhattisgarh",    5,
     ["492001","492002","492003","492004","492005","492006","492007","492008","492009","492010"]),
    # Uttarakhand
    ("Dehradun",      "Uttarakhand",     5,
     ["248001","248002","248003","248004","248005","248006","248007","248008","248009","248010"]),
    # Himachal Pradesh
    ("Shimla",        "Himachal Pradesh",3,
     ["171001","171002","171003","171004","171005","171006","171007","171008","171009","171010"]),
    # Goa
    ("Panaji",        "Goa",             2,
     ["403001","403002","403003","403004","403005","403006","403007","403008","403101","403110"]),
    # Jammu & Kashmir
    ("Jammu",         "Jammu and Kashmir", 3,
     ["180001","180002","180003","180004","180005","180006","180007","180008","180009","180010"]),
    ("Srinagar",      "Jammu and Kashmir", 3,
     ["190001","190002","190003","190004","190005","190006","190007","190008","190009","190010"]),
]

# Filter each city's pincodes to only include those in pincode.js
if PINCODE_POOL:
    _pincode_list = sorted(PINCODE_POOL)          # sorted list for fallback searches
    _filtered = []
    _removed_total = 0
    _fallback_cities = []
    for city, state, weight, pincodes in CITY_DATA:
        valid = [p for p in pincodes if p in PINCODE_POOL]
        removed = len(pincodes) - len(valid)
        _removed_total += removed
        if not valid:
            # Fallback 1: try 3-digit prefix match
            prefix3 = pincodes[0][:3]
            valid = [p for p in _pincode_list if p.startswith(prefix3)][:5]
        if not valid:
            # Fallback 2: try 2-digit prefix match
            prefix2 = pincodes[0][:2]
            valid = [p for p in _pincode_list if p.startswith(prefix2)][:5]
        if not valid:
            # Fallback 3: just pick 5 random valid pincodes
            valid = _pincode_list[:5]
        if removed > 0:
            _fallback_cities.append(f"{city} ({removed}/{removed + len(valid) - (len(valid) if removed == len(pincodes) else 0)})")
        _filtered.append((city, state, weight, valid))
    CITY_DATA = _filtered
    # Verify no city has empty pincodes
    for city, state, weight, pincodes in CITY_DATA:
        assert len(pincodes) > 0, f"City {city} has no valid pincodes after filtering!"
    print(f"[Pincode] Filtered city pincodes against pincode.js — removed {_removed_total} invalid codes")

CITY_WEIGHTS = [c[2] for c in CITY_DATA]

# ---------------------------------------------------------------------------
# Blood bank naming templates
# ---------------------------------------------------------------------------
REDCROSS_NAME_TEMPLATES = [
    "Indian Red Cross Society Blood Bank, {city}",
    "{city} Government Blood Bank",
    "{city} District Hospital Blood Bank",
    "{city} Civil Hospital Blood Bank",
    "{city} Medical College Blood Bank",
    "Government General Hospital Blood Bank, {city}",
    "Rotary Blood Bank and Research Centre, {city}",
    "{city} District Blood Transfusion Centre",
    "Sri {deity} Charitable Blood Bank, {city}",
    "{city} People\'s Blood Bank",
    "{trust} Charitable Trust Blood Bank, {city}",
    "{city} Voluntary Blood Donation Centre",
    "Lifeline Blood Bank, {city}",
    "National Blood Services, {city}",
    "{city} Red Cross Donation Centre",
    "Jeevan Blood Bank, {city}",
    "{city} Thalassemia Society Blood Bank",
]

WHO_NAME_TEMPLATES = [
    "{city} Regional Blood Transfusion Centre",
    "National Blood Transfusion Council, {city}",
    "{city} WHO Blood Centre",
    "{city} Thalassemia and Blood Bank Centre",
    "Community Blood Bank, {city}",
    "{city} District Blood Bank",
    "{city} Multi-Speciality Hospital Blood Bank",
    "Apollo Blood and Diagnostics, {city}",
    "Fortis Blood Bank, {city}",
    "AIIMS Regional Blood Centre, {city}",
    "{city} Private Blood Bank",
    "Narayana Health Blood Bank, {city}",
    "{city} Integrated Blood Services",
    "Manipal Blood Bank, {city}",
    "Columbia Asia Blood Bank, {city}",
    "Max Healthcare Blood Centre, {city}",
    "{trust} Foundation Blood Bank, {city}",
]

DEITIES    = ["Sai Baba", "Venkateshwara", "Lakshmi", "Durga", "Ram", "Hanuman", "Tirupati"]
TRUSTS     = ["Seva", "Jeevan", "Asha", "Raksha", "Kalyan", "Prabhat", "Sanjeevani"]
CATEGORIES = ["Hospital", "Blood Bank", "NGO", "Others"]

STREET_TYPES = ["Main Road", "Cross Road", "Ring Road", "Hospital Road",
                "MG Road", "Station Road", "Market Road", "Bypass Road"]
LOCALITIES   = ["Nagar", "Colony", "Layout", "Extension", "Vihar",
                "Enclave", "Sector", "Phase", "Block"]

# ---------------------------------------------------------------------------
# Donor name pools
# ---------------------------------------------------------------------------
FIRST_NAMES_M = [
    "Ravi","Amit","Rahul","Vijay","Suresh","Arjun","Mohan","Rohit","Ajay","Sanjay",
    "Deepak","Anil","Vishal","Rajesh","Nikhil","Siddharth","Aarav","Pranav","Yash",
    "Harsh","Dev","Ankit","Sachin","Vikas","Gaurav","Tarun","Varun","Naveen","Harish",
    "Girish","Ramesh","Dinesh","Prakash","Ashok","Arvind","Vivek","Alok","Sumit","Akash",
    "Rohan","Kunal","Saurabh","Nitin","Piyush","Tushar","Manish","Ritesh","Abhishek","Pradeep",
]
FIRST_NAMES_F = [
    "Priya","Sunita","Kavita","Deepa","Meena","Anjali","Lakshmi","Rekha","Usha","Nisha",
    "Pooja","Divya","Rani","Geeta","Sita","Ananya","Sneha","Shreya","Ritu","Lata",
    "Swati","Jyoti","Mala","Radha","Manisha","Shweta","Kiran","Neha","Kalpana","Asha",
    "Savita","Pushpa","Kamla","Nirmala","Sudha","Padma","Vimala","Indu","Soni","Preeti",
    "Shalini","Vandana","Shobha","Sarita","Reena","Aarti","Amrita","Bhavna","Charu","Dimple",
]
LAST_NAMES = [
    "Sharma","Verma","Patel","Reddy","Kumar","Singh","Gupta","Nair","Pillai","Iyer",
    "Mehta","Joshi","Rao","Das","Bose","Pandey","Mishra","Tiwari","Shukla","Dubey",
    "Yadav","Chauhan","Malhotra","Kapoor","Khanna","Bajaj","Shah","Jain","Agarwal","Bansal",
    "Naidu","Varma","Murthy","Sethi","Chopra","Batra","Srivastava","Tripathi","Chaudhary","Saxena",
    "Choudhary","Patil","Desai","Bhatt","Kulkarni","More","Shinde","Pawar","Jadhav","Sawant",
]

# ---------------------------------------------------------------------------
# Donor count distribution per bank:
# (weight, min_donors, max_donors)
# Small hospitals dominate. A few major hubs have 60-120 donors.
# ---------------------------------------------------------------------------
DONOR_TIERS = [
    (40,  2,   6),   # small
    (35,  8,  22),   # medium
    (20, 25,  55),   # large
    ( 5, 60, 120),   # major city hub
]
DONOR_TIER_WEIGHTS = [d[0] for d in DONOR_TIERS]


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def rnd_mobile():
    """Valid Indian 10-digit mobile: starts with 6-9, stored as +91XXXXXXXXXX (13 chars)."""
    prefix = random.choice(["6", "7", "8", "9"])
    rest   = random.randint(0, 999_999_999)
    return f"+91{prefix}{rest:09d}"


def rnd_email(institution_name):
    slug   = "".join(c for c in institution_name.lower() if c.isalnum())[:10]
    domain = random.choice(["gmail.com", "yahoo.co.in", "rediffmail.com",
                             "hotmail.com", "outlook.com"])
    return f"{slug}{random.randint(1, 99)}@{domain}"


def rnd_address(city, state, pincode):
    num      = random.randint(1, 999)
    street   = random.choice(STREET_TYPES)
    locality = f"{city} {random.choice(LOCALITIES)}"
    return f"{num}, {locality}, {street}, {city}, {state} - {pincode}"


def rnd_street(city):
    return f"{random.randint(1, 999)}, {random.choice(STREET_TYPES)}"


def rnd_date_past(max_days):
    return date.today() - timedelta(days=random.randint(30, max_days))


def rnd_dt_between(start: date, end: date) -> datetime:
    """Uniform random datetime between two dates."""
    delta = (end - start).days
    if delta < 1:
        delta = 1
    d = start + timedelta(days=random.randint(0, delta - 1))
    return datetime(d.year, d.month, d.day,
                    random.randint(0, 23), random.randint(0, 59), random.randint(0, 59))


def rnd_donor_created_at(age: int) -> datetime:
    """
    Registration date is after the person turned 18 (can't donate earlier).
    Older donors skew towards earlier registration; capped at 12 years back.
    """
    today = date.today()
    # earliest they could have registered = when they turned 18
    birth_year   = today.year - age
    turned_18    = date(birth_year + 18, random.randint(1, 12), random.randint(1, 28))
    twelve_ago   = date(today.year - 12, today.month, today.day)
    earliest     = max(turned_18, twelve_ago)
    # leave at least 30 days gap so updated_at has room
    latest       = today - timedelta(days=30)
    if earliest >= latest:
        earliest = latest - timedelta(days=60)
    return rnd_dt_between(earliest, latest)


def rnd_updated_at(created_at: datetime) -> datetime:
    """Random datetime between created_at and today (record could have been updated any time)."""
    return rnd_dt_between(created_at.date(), date.today())


def rnd_bank_created_at() -> datetime:
    """Blood banks were established 2-20 years ago."""
    today    = date.today()
    years    = random.randint(2, 20)
    earliest = date(today.year - years, 1, 1)
    latest   = today - timedelta(days=90)
    return rnd_dt_between(earliest, latest)


def unique_id():
    """Deterministic identifier for fresh-environment test runs."""
    unique_id.counter += 1
    payload = f"seed:{RANDOM_SEED}:{unique_id.counter}:{random.random():.12f}".encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


unique_id.counter = 0


def pick_city():
    return random.choices(CITY_DATA, weights=CITY_WEIGHTS, k=1)[0]


def pick_blood_group():
    """Weighted draw matching Indian population distribution."""
    return random.choices(BLOOD_GROUPS, weights=BG_WEIGHTS, k=1)[0]


def donors_for_bank():
    tier = random.choices(DONOR_TIERS, weights=DONOR_TIER_WEIGHTS, k=1)[0]
    return random.randint(tier[1], tier[2])


def make_bank_name(city, templates):
    tpl = random.choice(templates)
    return tpl.format(
        city  = city,
        deity = random.choice(DEITIES),
        trust = random.choice(TRUSTS),
    )


def inventory_qty(blood_group, component):
    """
    Stock reflects:
      - Blood group rarity  (O+ 37% -> abundant; AB- 1% -> critically scarce)
      - Component shelf life (Platelets/Cryoprecipitate expire fast -> lower stock)
    """
    rarity_scale = BLOOD_GROUP_DIST[blood_group]       # 1-37
    base_max     = max(2, int(rarity_scale * 1.5))     # 3-55 units
    if component in ("Platelets", "Cryoprecipitate"):
        return random.randint(0, max(1, base_max // 3))
    return random.randint(0, base_max)


def expiry_days(component):
    if component == "Platelets":
        return 5
    if component in ("Whole Blood", "Packed RBC"):
        return 42
    if component in ("Fresh Frozen Plasma", "Plasma"):
        return 365
    if component == "Cryoprecipitate":
        return 365
    return 30


def make_source_event_id(source, bb_id, blood_group, component, sequence, event_timestamp, txn_type):
    raw = f"{source}:{bb_id}:{blood_group}:{component}:{sequence}:{event_timestamp.isoformat()}:{txn_type}"
    digest = hashlib.sha256(raw.encode("utf-8")).hexdigest()[:32]
    return f"{source}-{bb_id}-{sequence}-{digest}"


# Inventory transaction date range: Jan 1 2024 → today + 2 days
INVENTORY_DATE_START = date(2024, 1, 1)
INVENTORY_DATE_END   = date.today() + timedelta(days=2)


def rnd_inventory_updated_at() -> datetime:
    """Random datetime uniformly between Jan 1 2024 and today + 2 days."""
    return rnd_dt_between(INVENTORY_DATE_START, INVENTORY_DATE_END)


def generate_inventory_transactions_batch(source, all_bb_ids, components, target_count):
    """
    Generate `target_count` inventory transactions spread across all banks.
    updated_at is uniformly random between Jan 1 2024 and today+2.
    Yields batches of tuples ready for execute_values.
    """
    generated = 0
    sequence = 0
    batch = []

    while generated < target_count:
        bb_id = random.choice(all_bb_ids)
        bg = pick_blood_group()
        component = random.choice(components)
        txn_type = random.choices(["INFLOW", "OUTFLOW"], weights=[50, 50], k=1)[0]

        event_time = rnd_inventory_updated_at()
        updated_at = event_time

        running_balance = max(0, inventory_qty(bg, component))
        donor_id = None
        expiry = None

        if txn_type == "INFLOW":
            delta = random.randint(1, max(2, (running_balance // 2) + 1))
            donor_id = random.randint(1, max(1, DONORS_PER_RUN))
            expiry = event_time.date() + timedelta(days=expiry_days(component))
        elif txn_type == "OUTFLOW":
            if running_balance <= 0:
                txn_type = "INFLOW"
                delta = random.randint(1, 3)
                donor_id = random.randint(1, max(1, DONORS_PER_RUN))
                expiry = event_time.date() + timedelta(days=expiry_days(component))
            else:
                delta = -random.randint(1, min(running_balance, max(1, running_balance // 2)))
        else:
            delta = -random.randint(1, min(running_balance, max(1, running_balance // 2)))

        running_balance = max(0, running_balance + delta)
        sequence += 1
        source_event_id = make_source_event_id(source, bb_id, bg, component, sequence, event_time, txn_type)

        batch.append((
            source_event_id, bb_id, donor_id, bg, component,
            txn_type, delta, running_balance, expiry, event_time, updated_at,
        ))
        generated += 1

        if len(batch) >= BATCH_SIZE:
            yield batch
            batch = []

    if batch:
        yield batch


def full_name():
    pool = FIRST_NAMES_M if random.random() < 0.55 else FIRST_NAMES_F
    return f"{random.choice(pool)} {random.choice(LAST_NAMES)}"


def preview_record(
    preview_rows,
    *,
    source,
    entity_type,
    bank_id="",
    record_id="",
    name="",
    blood_group="",
    component="",
    category="",
    contact="",
    email="",
    address="",
    city="",
    state="",
    pincode="",
    age="",
    quantity="",
    units_available="",
    event_time="",
    updated_at="",
    notes="",
):
    preview_rows.append(
        {
            "source": source,
            "entity_type": entity_type,
            "bank_id": bank_id,
            "record_id": record_id,
            "name": name,
            "blood_group": blood_group,
            "component": component,
            "category": category,
            "contact": contact,
            "email": email,
            "address": address,
            "city": city,
            "state": state,
            "pincode": pincode,
            "age": age,
            "quantity": quantity,
            "units_available": units_available,
            "event_time": event_time,
            "updated_at": updated_at,
            "notes": notes,
        }
    )


def write_preview_csv(preview_rows):
    rows = []

    def take(entity_type, limit):
        taken = 0
        for row in preview_rows:
            if row.get("entity_type") == entity_type:
                rows.append(row)
                taken += 1
                if taken >= limit:
                    break

    take("blood_bank", 20)
    take("inventory_transaction", 40)
    take("donor", 40)

    if len(rows) < CSV_PREVIEW_ROWS:
        for row in preview_rows:
            if row not in rows:
                rows.append(row)
                if len(rows) >= CSV_PREVIEW_ROWS:
                    break

    rows = rows[:CSV_PREVIEW_ROWS]
    with CSV_PREVIEW_PATH.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_PREVIEW_HEADERS)
        writer.writeheader()
        writer.writerows(rows)


# (banks_per_run removed — use BANKS_PER_RUN constant directly)


# ---------------------------------------------------------------------------
# Red Cross seeder
# ---------------------------------------------------------------------------

def seed_redcross(conn, preview_rows):
    cur = conn.cursor()

    print(f"[Red Cross] Inserting {BANKS_PER_RUN} new banks ...")
    for _ in range(BANKS_PER_RUN):
        city, state, _w, pincodes = pick_city()
        postal   = random.choice(pincodes)
        name     = make_bank_name(city, REDCROSS_NAME_TEMPLATES)
        phone    = rnd_mobile()
        email    = rnd_email(name)
        category = random.choice(CATEGORIES)
        address  = rnd_address(city, state, postal)
        bank_cat = rnd_bank_created_at()
        bank_uat = rnd_updated_at(bank_cat)

        cur.execute(
            """
            INSERT INTO blood_bank
                (name, category, contact_number, email, full_address, postal_code,
                 created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING bb_id
            """,
            (name, category, phone, email, address, postal, bank_cat, bank_uat),
        )
        bb_id = cur.fetchone()[0]
        preview_record(
            preview_rows,
            source="RedCross",
            entity_type="blood_bank",
            bank_id=bb_id,
            record_id=bb_id,
            name=name,
            category=category,
            contact=phone,
            email=email,
            address=address,
            city=city,
            state=state,
            pincode=postal,
            event_time=bank_cat,
            updated_at=bank_uat,
        )
    conn.commit()
    print(f"[Red Cross] {BANKS_PER_RUN} banks committed.")

    # Generate 1M inventory transactions across ALL banks
    cur.execute("SELECT bb_id FROM blood_bank")
    all_bb_ids_for_inv = [row[0] for row in cur.fetchall()]
    print(f"[Red Cross] Generating {INVENTORY_TXN_PER_RUN:,} inventory transactions (updated_at: {INVENTORY_DATE_START} → {INVENTORY_DATE_END}) ...")
    inv_inserted = 0
    for batch in generate_inventory_transactions_batch("redcross", all_bb_ids_for_inv, REDCROSS_COMPONENTS, INVENTORY_TXN_PER_RUN):
        execute_values(
            cur,
            """INSERT INTO inventory_transaction
               (source_event_id, bb_id, donor_id, blood_group, component,
                transaction_type, units_delta, running_balance_after,
                expiry_date, event_timestamp, updated_at)
               VALUES %s""",
            batch,
        )
        conn.commit()
        inv_inserted += len(batch)
        print(f"  [Red Cross] inventory txns: {inv_inserted:,} / {INVENTORY_TXN_PER_RUN:,}", end="\r", flush=True)
    print(f"\n[Red Cross] {inv_inserted:,} inventory transactions committed.")

    # Spread donors across ALL banks in the DB (including from prior runs)
    cur.execute("SELECT bb_id FROM blood_bank")
    all_bb_ids = [row[0] for row in cur.fetchall()]
    print(f"[Red Cross] Total banks in DB: {len(all_bb_ids)} — inserting {DONORS_PER_RUN:,} donors ...")

    inserted = 0
    while inserted < DONORS_PER_RUN:
        batch_n = min(BATCH_SIZE, DONORS_PER_RUN - inserted)
        batch = []
        for _ in range(batch_n):
            bb_id    = random.choice(all_bb_ids)
            city, state, _w, pincodes = pick_city()
            postal   = random.choice(pincodes)
            fname    = full_name()
            nat_id   = unique_id()
            b_type   = pick_blood_group()
            age      = random.randint(18, 65)
            last_don = rnd_date_past(3 * 365)
            contact  = rnd_mobile()
            daddr    = rnd_address(city, state, postal)
            d_cat    = rnd_donor_created_at(age)
            d_uat    = rnd_updated_at(d_cat)
            batch.append((bb_id, fname, nat_id, contact, daddr, b_type, age, last_don, d_cat, d_uat))
            preview_record(
                preview_rows,
                source="RedCross",
                entity_type="donor",
                bank_id=bb_id,
                record_id=nat_id,
                name=fname,
                blood_group=b_type,
                contact=contact,
                address=daddr,
                city=city,
                state=state,
                pincode=postal,
                age=age,
                event_time=last_don,
                updated_at=d_uat,
            )

        execute_values(
            cur,
            """INSERT INTO donor
               (bb_id, full_name, national_id, contact_number, address,
                blood_type, age, last_donation_date, created_at, updated_at)
               VALUES %s""",
            batch,
        )
        conn.commit()
        inserted += batch_n
        print(f"  [Red Cross] donors: {inserted:,} / {DONORS_PER_RUN:,}", end="\r", flush=True)

    cur.close()
    print(f"\n[Red Cross] Done — {BANKS_PER_RUN} new banks | {INVENTORY_TXN_PER_RUN:,} inventory transactions | {DONORS_PER_RUN:,} new donors")


# ---------------------------------------------------------------------------
# WHO seeder
# ---------------------------------------------------------------------------

def seed_who(conn, preview_rows):
    cur = conn.cursor()

    print(f"[WHO] Inserting {BANKS_PER_RUN} new banks ...")
    for _ in range(BANKS_PER_RUN):
        city, state, _w, pincodes = pick_city()
        pincode  = random.choice(pincodes)
        name     = make_bank_name(city, WHO_NAME_TEMPLATES)
        phone    = rnd_mobile()
        email    = rnd_email(name)
        category = random.choice(CATEGORIES)
        street   = rnd_street(city)
        bank_cat = rnd_bank_created_at()
        bank_uat = rnd_updated_at(bank_cat)

        cur.execute(
            """
            INSERT INTO blood_bank
                (name, category, phone, email, street, city, state, pincode,
                 created_at, updated_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            RETURNING bb_id
            """,
            (name, category, phone, email, street, city, state, pincode, bank_cat, bank_uat),
        )
        bb_id = cur.fetchone()[0]
        preview_record(
            preview_rows,
            source="WHO",
            entity_type="blood_bank",
            bank_id=bb_id,
            record_id=bb_id,
            name=name,
            category=category,
            contact=phone,
            email=email,
            address=street,
            city=city,
            state=state,
            pincode=pincode,
            event_time=bank_cat,
            updated_at=bank_uat,
        )
    conn.commit()
    print(f"[WHO] {BANKS_PER_RUN} banks committed.")

    # Generate 1M inventory transactions across ALL banks
    cur.execute("SELECT bb_id FROM blood_bank")
    all_bb_ids_for_inv = [row[0] for row in cur.fetchall()]
    print(f"[WHO] Generating {INVENTORY_TXN_PER_RUN:,} inventory transactions (updated_at: {INVENTORY_DATE_START} → {INVENTORY_DATE_END}) ...")
    inv_inserted = 0
    for batch in generate_inventory_transactions_batch("who", all_bb_ids_for_inv, WHO_COMPONENTS, INVENTORY_TXN_PER_RUN):
        execute_values(
            cur,
            """INSERT INTO inventory_transaction
               (source_event_id, bb_id, donor_id, blood_group, component,
                transaction_type, units_delta, running_balance_after,
                expiry_date, event_timestamp, updated_at)
               VALUES %s""",
            batch,
        )
        conn.commit()
        inv_inserted += len(batch)
        print(f"  [WHO] inventory txns: {inv_inserted:,} / {INVENTORY_TXN_PER_RUN:,}", end="\r", flush=True)
    print(f"\n[WHO] {inv_inserted:,} inventory transactions committed.")

    # Spread donors across ALL banks in the DB (including from prior runs)
    cur.execute("SELECT bb_id FROM blood_bank")
    all_bb_ids = [row[0] for row in cur.fetchall()]
    print(f"[WHO] Total banks in DB: {len(all_bb_ids)} — inserting {DONORS_PER_RUN:,} donors ...")

    inserted = 0
    while inserted < DONORS_PER_RUN:
        batch_n = min(BATCH_SIZE, DONORS_PER_RUN - inserted)
        batch = []
        for _ in range(batch_n):
            bb_id    = random.choice(all_bb_ids)
            city, state, _w, pincodes = pick_city()
            pincode  = random.choice(pincodes)
            fname    = full_name()
            a_hash   = unique_id()
            bg       = pick_blood_group()
            age      = random.randint(18, 65)
            last_don = rnd_date_past(3 * 365)
            phone_d  = rnd_mobile()
            d_cat    = rnd_donor_created_at(age)
            d_uat    = rnd_updated_at(d_cat)
            batch.append((bb_id, fname, a_hash, phone_d, city, state, pincode, bg, age, last_don, d_cat, d_uat))
            preview_record(
                preview_rows,
                source="WHO",
                entity_type="donor",
                bank_id=bb_id,
                record_id=a_hash,
                name=fname,
                blood_group=bg,
                contact=phone_d,
                city=city,
                state=state,
                pincode=pincode,
                age=age,
                event_time=last_don,
                updated_at=d_uat,
            )

        execute_values(
            cur,
            """INSERT INTO donor
               (bb_id, name, abha_hash, phone, city, state, pincode,
                blood_group, age, last_donated, created_at, updated_at)
               VALUES %s""",
            batch,
        )
        conn.commit()
        inserted += batch_n
        print(f"  [WHO] donors: {inserted:,} / {DONORS_PER_RUN:,}", end="\r", flush=True)

    cur.close()
    print(f"\n[WHO] Done — {BANKS_PER_RUN} new banks | {INVENTORY_TXN_PER_RUN:,} inventory transactions | {DONORS_PER_RUN:,} new donors")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print(f"Realistic DB seeder  (BANKS_PER_RUN={BANKS_PER_RUN}, DONORS_PER_RUN={DONORS_PER_RUN:,}, INVENTORY_TXN_PER_RUN={INVENTORY_TXN_PER_RUN:,})\n")
    preview_rows = []

    try:
        rc_conn = psycopg2.connect(**REDCROSS_DSN)
        print("[Red Cross] Connected -- port 5434")
        seed_redcross(rc_conn, preview_rows)
        rc_conn.close()
    except Exception as exc:
        print(f"[Red Cross] ERROR: {exc}")

    print()

    try:
        who_conn = psycopg2.connect(**WHO_DSN)
        print("[WHO] Connected -- port 5435")
        seed_who(who_conn, preview_rows)
        who_conn.close()
    except Exception as exc:
        print(f"[WHO] ERROR: {exc}")

    try:
        write_preview_csv(preview_rows)
        print(f"[CSV] Preview written -- {CSV_PREVIEW_PATH} ({min(len(preview_rows), CSV_PREVIEW_ROWS)} rows)")
    except Exception as exc:
        print(f"[CSV] ERROR: {exc}")

    print("\nSeeding complete.")


if __name__ == "__main__":
    main()

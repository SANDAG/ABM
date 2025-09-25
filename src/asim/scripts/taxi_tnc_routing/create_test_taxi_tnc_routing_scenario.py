import numpy as np
import pandas as pd
import openmatrix as omx
import os

# Target folder (adjust if you want elsewhere)
OUT_DIR = r"C:\Users\david.hensle\OneDrive - Resource Systems Group, Inc\Documents\projects\sandag\AV_TNC_models\tnc_data\test"
os.makedirs(OUT_DIR, exist_ok=True)

np.random.seed(42)

# 1. Create a 10-zone skim (minutes) – simple linear distances
#    Time = 3 * |i-j| + 2 (off-diagonal), 0 on diagonal
ZONES = np.arange(1, 11)  # 1..10
n = len(ZONES)
skim = np.zeros((n, n), dtype=np.float32)
for i in range(n):
    for j in range(n):
        if i != j:
            skim[i, j] = 3 * abs(i - j) + 2  # ensures small clusters within buffer 10

print(f"skim:\n{skim}")

# 2. Write OpenMatrix file with mapping
omx_path = os.path.join(OUT_DIR, "traffic_skims_AM.omx")
if os.path.exists(omx_path):
    os.remove(omx_path)

with omx.open_file(omx_path, "w") as f:
    f["SOV_TR_H_TIME__AM"] = skim
    # Mapping: zone id -> index
    f.create_mapping("ZONE_MAP", ZONES)

print(f"Wrote skim to {omx_path}")

# 3. Land use MAZ→TAZ (1:1 for this test)
land_use = pd.DataFrame({
    "MAZ": ZONES,
    "taz": ZONES
})
land_use.to_csv(os.path.join(OUT_DIR, "land_use.csv"), index=False)
print("Wrote land_use.csv")

# 4. Create trips (half-hour depart periods, some clustered to allow pooling)
# depart is in half-hour bins (your code multiplies by 30 then adds random 0–29)
trips = pd.DataFrame([
    # group A (zones 1–4) – several close OD pairs
    (1001, "TNC_SINGLE", 0, 1, 3),
    (1002, "TNC_SINGLE", 0, 2, 4),
    (1003, "TNC_SHARED", 0, 1, 4),
    (1004, "TAXI",       0, 3, 2),
    (1005, "TNC_SINGLE", 0, 2, 1),
    (1006, "TNC_SHARED", 0, 4, 1),
    # group B (zones 6–8) – another cluster
    (1010, "TAXI",       0, 6, 8),
    (1011, "TNC_SINGLE", 0, 7, 6),
    (1012, "TNC_SHARED", 0, 8, 6),
    (1013, "TNC_SINGLE", 0, 6, 7),
    # mixed / farther (some won’t pool)
    (1020, "TAXI",       0, 1, 9),
    (1021, "TNC_SINGLE", 0, 9, 2),
    (1022, "TNC_SINGLE", 0, 4, 8),
    (1023, "TNC_SHARED", 0, 8, 4),
], columns=["trip_id", "trip_mode", "depart", "origin", "destination"])

trips.to_csv(os.path.join(OUT_DIR, "final_tnc_trips.csv"), index=False)
print("Wrote final_tnc_trips.csv")

print("Test dataset ready.")
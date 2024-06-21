import pandas as pd
import sys
from shutil import copy

est_file = sys.argv[1]
lu_file = sys.argv[2]

print("Creating backup of synthetic establishments file")
copy(est_file, est_file.replace(".csv", "_backup.csv"))

print("Reading Data")
est = pd.read_csv(est_file)
lu = pd.read_csv(lu_file)

print("Recoding MGRAs")
taz2mgra = lu.groupby("taz").first()["mgra"]
est["MGRA"] = est["MGRA"].map(taz2mgra)

print("Writing Data")
est.to_csv(est_file, index = False)
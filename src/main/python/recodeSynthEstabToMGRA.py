import pandas as pd
import numpy as np
import sys
from shutil import copy

est_file = sys.argv[1]
lu_file = sys.argv[2]

# Create backup file just in case something goes wrong
copy(est_file, est_file.replace(".csv", "_backup.csv"))

# Read in input files
est = pd.read_csv(est_file)
lu = pd.read_csv(lu_file)

# Create dictionary mapping TAZ to the share of the TAZ's employtment by its MGRA
taz_map = {}
for taz in est["MGRA"].value_counts().index:
    taz_lu = lu.query("taz == @taz")
    taz_map[taz] = pd.Series(
        (taz_lu["emp_total"] / taz_lu["emp_total"].sum()).values,
        index = taz_lu["mgra"].values
    )

def select_mgra(taz):
    """
    Randomly selects an MGRA belonging to the input TAZ based on the employment in each of the TAZ's MGRAs.

    Parameters
    ----------
    taz (int):
        TAZ ID

    Returns
    -------
    mgra (int):
        Selected MGRA ID
    """
    global taz_map
    return np.random.choice(
        taz_map[taz].index,
        p = taz_map[taz]
    )

# Select MGRA for each establishment (the MGRA field actually has the TAZ ID in the input file)
est["MGRA"] = est["MGRA"].apply(select_mgra)

# Write output
est.to_csv(est_file, index = False)
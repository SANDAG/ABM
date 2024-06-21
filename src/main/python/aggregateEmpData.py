import pandas as pd
import numpy as np
import sys

infile = sys.argv[1]
outfile = sys.argv[2]

emp_fields = [
    "emp_gov",
    "emp_mil",
    "emp_ag_min",
    "emp_bus_svcs",
    "emp_fin_res_mgm",
    "emp_educ",
    "emp_hlth",
    "emp_ret",
    "emp_trn_wrh",
    "emp_con",
    "emp_utl",
    "emp_mnf",
    "emp_whl",
    "emp_ent",
    "emp_accm",
    "emp_food",
    "emp_oth",
    "emp_non_ws_wfh",
    "emp_non_ws_oth",
]

print("Reading Data")
mgra_data = pd.read_csv(infile)

print("Creating Maps")
maz2taz = pd.get_dummies(mgra_data.set_index("mgra")["taz"]) # Matrix to aggregate MAZ-level data by TAZ
taz2luz = mgra_data.groupby("taz").first()['luz_id'] # Series for mapping TAZ to LUZ

print("Aggregating Data")
emp_by_taz = pd.DataFrame(
    maz2taz.T.values.dot(mgra_data[emp_fields].values),
    index = maz2taz.columns,
    columns = emp_fields
    )
emp_by_taz["emp_total"] = emp_by_taz.sum(1)
emp_by_taz.index.name = "taz"
emp_by_taz = emp_by_taz.reset_index()
emp_by_taz["mgra"] = emp_by_taz["taz"].copy()
emp_by_taz["luz_id"] = emp_by_taz["taz"].map(taz2luz)

print("Writing Results")
emp_by_taz[["mgra", "taz", "luz_id"] + emp_fields + ["emp_total"]].to_csv(outfile, index = False)
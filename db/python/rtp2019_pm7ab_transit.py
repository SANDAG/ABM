import csv
import pandas as pd
import pyodbc
import sys

usage = "Correct Usage: pm_7ab_transit.py <scenario_id> <UATS: 0 | 1>"

# if too few/many arguments passed raise error
if len(sys.argv) != 3:
    print(usage)
    sys.exit(-1)

# user inputs the abm database scenario_id
# for their scenario_id of choice
scenario_id = sys.argv[1]

# user inputs whether to restrict results to uats districts only
uats = sys.argv[2]
if uats not in ["0", "1"]:
    print(usage)
    sys.exit(-1)

# set sql server connection attributes
server = "${database_server}"
database = "${database_name}"

# build sql server connection string
conn = pyodbc.connect("DRIVER={SQL Server Native Client 11.0};SERVER="
                      + server + ";DATABASE=" + database +
                      ";Trusted_Connection=yes;")

# get the file system path from sql server
# for the given scenario_id
scenario_path = pd.read_sql(
    sql="SELECT RTRIM([path]) AS [path] FROM " +
        "[dimension].[scenario] WHERE [scenario_id] = " +
        scenario_id,
    con=conn)["path"][0]

# create list of population column names from SQL stored procedure
# [rtp_2019].[sp_pm_7ab_population]
population_columns = ["pop",
                      "pop_senior",
                      "pop_non_senior",
                      "pop_minority",
                      "pop_non_minority",
                      "pop_low_income",
                      "pop_non_low_income"]

# get population input from sql server stored procedure
# for pm 7a, this passes the @age_18_plus parameter as 1
# to restrict the population to 18+
population_7a = pd.read_sql(
    sql="EXECUTE [rtp_2019].[sp_pm_7ab_population] " +
        scenario_id + ",@age_18_plus=1,@uats=" + uats,
    con=conn,
    index_col="mgra_13")

# get population input from sql server stored procedure
# for pm 7b, this passes the @age_18_plus parameter as 0
population_7b = pd.read_sql(
    sql="EXECUTE [rtp_2019].[sp_pm_7ab_population] " +
        scenario_id + ",@age_18_plus=0,@uats=" + uats,
    con=conn,
    index_col="mgra_13")

# get destinations input from sql server stored procedure
destinations = pd.read_sql(
    sql="EXECUTE [rtp_2019].[sp_pm_7ab_destinations] " +
        scenario_id + "," + uats,
    con=conn)

# calculate beachactive and parkactive indicators
destinations.loc[destinations.beachactive >= .5, "beachactive"] = 1
destinations.loc[destinations.beachactive < .5, "beachactive"] = 0
destinations.loc[destinations.parkactive >= .5, "parkactive"] = 1
destinations.loc[destinations.parkactive < .5, "parkactive"] = 0


# Performance Measure 7a
# read in scenario output folder files for
# MGRAs within 30 minutes via transit drive/walk access
# in am peak period
mgra_access_30 = list()
with open(scenario_path + "/output/driveMgrasWithin30Min_am.csv", "r") as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=",")
    for row in csv_reader:
        mgra_access_30.append((row[0], (row[1:])))
with open(scenario_path + "/output/walkMgrasWithin30Min_am.csv", "r") as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=",")
    for row in csv_reader:
        mgra_access_30.append((row[0], (row[1:])))

results_7a = list()
# for each record in the population DataFrame
for index, row in population_7a.iterrows():
    # get list of MGRAs accessible within 30 minutes
    mgra_accessible = list()
    for access_row in mgra_access_30:
        if int(index) == int(access_row[0]):
            for mgra in access_row[1]:
                mgra_accessible.append(int(mgra))

    # sum the destinations with MGRA indices accessible within 30 minutes
    access = destinations[destinations["mgra_13"].astype("int").isin(mgra_accessible)]["emp_educ"].sum()

    # append to results list
    # todo could use star unpacking instead in Python 3 with list of column names
    results_7a.append((row["pop"],
                       row["pop_senior"],
                       row["pop_non_senior"],
                       row["pop_minority"],
                       row["pop_non_minority"],
                       row["pop_low_income"],
                       row["pop_non_low_income"],
                       access))

# convert results list to a DataFrame
# todo could use star unpacking instead in Python 3 with list of column names
results_7a = pd.DataFrame(results_7a,
                          columns=["pop",
                                   "pop_senior",
                                   "pop_non_senior",
                                   "pop_minority",
                                   "pop_non_minority",
                                   "pop_low_income",
                                   "pop_non_low_income",
                                   "emp_educ"])

# combine all final metrics into output DataFrame
# average number of jobs/enrollment accessible to a person
# percentage of total jobs/enrollment accessible to a person
output_7a = pd.DataFrame(data=[(
    "Performance Measure 7a - Transit",
    scenario_id,
    (results_7a["pop"] * results_7a["emp_educ"]).sum() / results_7a["pop"].sum(),
    (results_7a["pop_senior"] * results_7a["emp_educ"]).sum() / results_7a["pop_senior"].sum(),
    (results_7a["pop_non_senior"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_senior"].sum(),
    (results_7a["pop_minority"] * results_7a["emp_educ"]).sum() / results_7a["pop_minority"].sum(),
    (results_7a["pop_non_minority"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_minority"].sum(),
    (results_7a["pop_low_income"] * results_7a["emp_educ"]).sum() / results_7a["pop_low_income"].sum(),
    (results_7a["pop_non_low_income"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_low_income"].sum(),
    100 * (results_7a["pop"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop"].sum(),
    100 * (results_7a["pop_senior"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_senior"].sum(),
    100 * (results_7a["pop_non_senior"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_senior"].sum(),
    100 * (results_7a["pop_minority"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_minority"].sum(),
    100 * (results_7a["pop_non_minority"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_minority"].sum(),
    100 * (results_7a["pop_low_income"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_low_income"].sum(),
    100 * (results_7a["pop_non_low_income"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_low_income"].sum()
    )],
    columns=["measure",
             "scenario_id",
             "pop_job_enroll",
             "pop_senior_job_enroll",
             "pop_non_senior_job_enroll",
             "pop_minority_job_enroll",
             "pop_non_minority_job_enroll",
             "pop_low_income_job_enroll",
             "pop_non_low_income_job_enroll",
             "pop_pct_job_enroll",
             "pop_senior_pct_job_enroll",
             "pop_non_senior_pct_job_enroll",
             "pop_minority_pct_job_enroll",
             "pop_non_minority_pct_job_enroll",
             "pop_low_income_pct_job_enroll",
             "pop_non_low_income_pct_job_enroll"])


# Performance Measure 7b
# read in scenario output folder files for
# MGRAs within 15 minutes via transit drive/walk access
# using mid-day skims
mgra_access_15 = list()
with open(scenario_path + "/output/driveMgrasWithin15Min_md.csv", "r") as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=",")
    for row in csv_reader:
        mgra_access_15.append((row[0], (row[1:])))
with open(scenario_path + "/output/walkMgrasWithin15Min_md.csv", "r") as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=",")
    for row in csv_reader:
        mgra_access_15.append((row[0], (row[1:])))

# initialize empty list to store results
results_7b = list()
# for each record in the population DataFrame
for index, row in population_7b.iterrows():
    # get list of MGRAs accessible within 15 minutes
    mgra_accessible = list()
    for access_row in mgra_access_15:
        if int(index) == int(access_row[0]):
            for mgra in access_row[1]:
                mgra_accessible.append(int(mgra))

    # get the maximum of the destinations
    # with MGRA indices accessible within 15 minutes
    access_beachactive = destinations[destinations["mgra_13"].astype("int").isin(mgra_accessible)]["beachactive"].max()
    access_parkactive = destinations[destinations["mgra_13"].astype("int").isin(mgra_accessible)]["parkactive"].max()

    # sum the destinations with MGRA indices accessible within 15 minutes
    access_emp_health = destinations[destinations["mgra_13"].astype("int").isin(mgra_accessible)]["emp_health"].sum()
    access_emp_retail = destinations[destinations["mgra_13"].astype("int").isin(mgra_accessible)]["emp_retail"].sum()

    # append to results list
    # todo could use star unpacking instead in Python 3 with list of column names
    results_7b.append((row["pop"],
                       row["pop_senior"],
                       row["pop_non_senior"],
                       row["pop_minority"],
                       row["pop_non_minority"],
                       row["pop_low_income"],
                       row["pop_non_low_income"],
                       access_beachactive,
                       access_emp_health,
                       access_parkactive,
                       access_emp_retail))

# convert results list to a DataFrame
# todo could use star unpacking instead in Python 3 with list of column names
results_7b = pd.DataFrame(results_7b,
                          columns=["pop",
                                   "pop_senior",
                                   "pop_non_senior",
                                   "pop_minority",
                                   "pop_non_minority",
                                   "pop_low_income",
                                   "pop_non_low_income",
                                   "beachactive",
                                   "emp_health",
                                   "parkactive",
                                   "emp_retail"])

# combine all final metrics into output DataFrame
# percentage of population with access to an active beach or park
# average number of jobs accessible to a person for health and retail
# percentage of total jobs accessible to a person for health and retail
output_7b = pd.DataFrame(data=[(
    "Performance Measure 7b - Transit",
    scenario_id,
    100 * (results_7b["pop"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop"].sum()),
    100 * (results_7b["pop_senior"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_senior"].sum()),
    100 * (results_7b["pop_non_senior"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_senior"].sum()),
    100 * (results_7b["pop_minority"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_minority"].sum()),
    100 * (results_7b["pop_non_minority"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_minority"].sum()),
    100 * (results_7b["pop_low_income"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_low_income"].sum()),
    100 * (results_7b["pop_non_low_income"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_low_income"].sum()),
    100 * (results_7b["pop"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop"].sum()),
    100 * (results_7b["pop_senior"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_senior"].sum()),
    100 * (results_7b["pop_non_senior"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_senior"].sum()),
    100 * (results_7b["pop_minority"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_minority"].sum()),
    100 * (results_7b["pop_non_minority"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_minority"].sum()),
    100 * (results_7b["pop_low_income"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_low_income"].sum()),
    (results_7b["pop_non_low_income"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_low_income"].sum()),
    (results_7b["pop"] * results_7b["emp_health"]).sum() / results_7b["pop"].sum(),
    (results_7b["pop_senior"] * results_7b["emp_health"]).sum() / results_7b["pop_senior"].sum(),
    (results_7b["pop_non_senior"] * results_7b["emp_health"]).sum() / results_7b["pop_non_senior"].sum(),
    (results_7b["pop_minority"] * results_7b["emp_health"]).sum() / results_7b["pop_minority"].sum(),
    (results_7b["pop_non_minority"] * results_7b["emp_health"]).sum() / results_7b["pop_non_minority"].sum(),
    (results_7b["pop_low_income"] * results_7b["emp_health"]).sum() / results_7b["pop_low_income"].sum(),
    (results_7b["pop_non_low_income"] * results_7b["emp_health"]).sum() / results_7b["pop_non_low_income"].sum(),
    (results_7b["pop"] * results_7b["emp_retail"]).sum() / results_7b["pop"].sum(),
    (results_7b["pop_senior"] * results_7b["emp_retail"]).sum() / results_7b["pop_senior"].sum(),
    (results_7b["pop_non_senior"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_senior"].sum(),
    (results_7b["pop_minority"] * results_7b["emp_retail"]).sum() / results_7b["pop_minority"].sum(),
    (results_7b["pop_non_minority"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_minority"].sum(),
    (results_7b["pop_low_income"] * results_7b["emp_retail"]).sum() / results_7b["pop_low_income"].sum(),
    (results_7b["pop_non_low_income"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_low_income"].sum(),
    100 * (results_7b["pop"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop"].sum(),
    100 * (results_7b["pop_senior"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_senior"].sum(),
    100 * (results_7b["pop_non_senior"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_senior"].sum(),
    100 * (results_7b["pop_minority"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_minority"].sum(),
    100 * (results_7b["pop_non_minority"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_minority"].sum(),
    100 * (results_7b["pop_low_income"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_low_income"].sum(),
    100 * (results_7b["pop_non_low_income"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_low_income"].sum(),
    100 * (results_7b["pop"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop"].sum(),
    100 * (results_7b["pop_senior"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_senior"].sum(),
    100 * (results_7b["pop_non_senior"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_senior"].sum(),
    100 * (results_7b["pop_minority"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_minority"].sum(),
    100 * (results_7b["pop_non_minority"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_minority"].sum(),
    100 * (results_7b["pop_low_income"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_low_income"].sum(),
    100 * (results_7b["pop_non_low_income"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_low_income"].sum()
    )],
    columns=["measure",
             "scenario_id",
             "pct_pop_beachactive",
             "pct_pop_senior_beachactive",
             "pct_pop_non_senior_beachactive",
             "pct_pop_minority_beachactive",
             "pct_pop_non_minority_beachactive",
             "pct_pop_low_income_beachactive",
             "pct_pop_non_low_income_beachactive",
             "pct_pop_parkactive",
             "pct_pop_senior_parkactive",
             "pct_pop_non_senior_parkactive",
             "pct_pop_minority_parkactive",
             "pct_pop_non_minority_parkactive",
             "pct_pop_low_income_parkactive",
             "pct_pop_non_low_income_parkactive",
             "pop_health",
             "pop_senior_health",
             "pop_non_senior_health",
             "pop_minority_health",
             "pop_non_minority_health",
             "pop_low_income_health",
             "pop_non_low_income_health",
             "pop_retail",
             "pop_senior_retail",
             "pop_non_senior_retail",
             "pop_minority_retail",
             "pop_non_minority_retail",
             "pop_low_income_retail",
             "pop_non_low_income_retail",
             "pop_pct_health",
             "pop_senior_pct_health",
             "pop_non_senior_pct_health",
             "pop_minority_pct_health",
             "pop_non_minority_pct_health",
             "pop_low_income_pct_health",
             "pop_non_low_income_pct_health",
             "pop_pct_retail",
             "pop_senior_pct_retail",
             "pop_non_senior_pct_retail",
             "pop_minority_pct_retail",
             "pop_non_minority_pct_retail",
             "pop_low_income_pct_retail",
             "pop_non_low_income_pct_retail"])


# write Performance Measures 7a/b - Transit out to csv
# in the scenario report folder
if uats == "0":
    output_path = scenario_path + "/report/pm_7ab_transit_region.csv"
if uats == "1":
    output_path = scenario_path + "/report/pm_7ab_transit_uats.csv"

output_7a.melt().to_csv(output_path,
                        index=False,
                        encoding="utf-8",
                        header=False)

output_7b.melt().to_csv(output_path,
                        index=False,
                        encoding="utf-8",
                        mode="a",
                        header=False)

import numpy as np
import openmatrix as omx
import pandas as pd
import pyodbc
import sys

usage = "Correct Usage: pm_7ab_auto.py <scenario_id> <UATS: 0 | 1>"

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
    con=conn)

# aggregate pm 7a population to the taz level
population_7a = population_7a.groupby(["taz_13"])[population_columns].sum()

# get population input from sql server stored procedure
# for pm 7b, this passes the @age_18_plus parameter as 0
population_7b = pd.read_sql(
    sql="EXECUTE [rtp_2019].[sp_pm_7ab_population] " +
        scenario_id + ",@age_18_plus=0,@uats=" + uats,
    con=conn)

# aggregate pm 7b population to the taz level
population_7b = population_7b.groupby(["taz_13"])[population_columns].sum()

# get destinations input from sql server stored procedure
destinations = pd.read_sql(
    sql="EXECUTE [rtp_2019].[sp_pm_7ab_destinations] " +
        scenario_id + "," + uats,
    con=conn)

# aggregate destinations input to the taz level
# do not use taz_13 as the index to allow for explicit reference
destinations = destinations.groupby(["taz_13"], as_index=False).agg(
    {"emp_educ": sum,  # sum of employment and enrollment used in pm_7a
     "beachactive": lambda x: 1 if sum(x) > .5 else 0,  # active beach indicator used in pm_7b
     "emp_health": sum,  # sum of health-care employment used in pm_7b
     "parkactive": lambda x: 1 if sum(x) > .5 else 0,  # active park indicator used in pm_7b
     "emp_retail": sum})  # sum of retail employment used in pm_7b

# get the highway network am peak and mid-day period omx skim matrix
# for single occupancy vehicle general purpose
# low value of time highway time skim
# matrix is 0-indexed to TAZs (0:1, 1:2, ..., 4995:4996)
# todo create mapping instead? requires opening in append or write mode
am_sovgpl_time = omx.open_file(
    scenario_path + "/output/traffic_skims_AM.omx")["AM_SOVGPL_TIME"]

md_sovgpl_time = omx.open_file(
    scenario_path + "/output/traffic_skims_MD.omx")["MD_SOVGPL_TIME"]

# Performance Measure 7a
# initialize empty list to store results
results_7a = list()
# for each record in the population DataFrame
for index, row in population_7a.iterrows():
    # get the indices of the TAZs accessible within 30 minutes
    # from the population TAZ using am peak period skims
    # note the 0-index in the skim matrix
    # the population row index taz_13 must be converted to integer
    # it is converted to string earlier in the groupby function
    index = int(index)
    taz_accessible = list(np.where(am_sovgpl_time[index - 1] <= 30)[0] + 1)

    # sum the destinations with TAZ indices accessible within 30 minutes
    # note the destinations taz_13 column must be converted to integer
    # it is converted to string earlier in the groupby function
    access = destinations[destinations["taz_13"].astype("int").isin(taz_accessible)]["emp_educ"].sum()

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
    "Performance Measure 7a - Auto",
    scenario_id,
    (results_7a["pop"] * results_7a["emp_educ"]).sum() / results_7a["pop"].sum(),
    (results_7a["pop_low_income"] * results_7a["emp_educ"]).sum() / results_7a["pop_low_income"].sum(),
    (results_7a["pop_non_low_income"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_low_income"].sum(),
    (results_7a["pop_minority"] * results_7a["emp_educ"]).sum() / results_7a["pop_minority"].sum(),
    (results_7a["pop_non_minority"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_minority"].sum(),
    (results_7a["pop_senior"] * results_7a["emp_educ"]).sum() / results_7a["pop_senior"].sum(),
    (results_7a["pop_non_senior"] * results_7a["emp_educ"]).sum() / results_7a["pop_non_senior"].sum(),
    100 * (results_7a["pop"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop"].sum(),
    100 * (results_7a["pop_low_income"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_low_income"].sum(),
    100 * (results_7a["pop_non_low_income"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_low_income"].sum(),
    100 * (results_7a["pop_minority"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_minority"].sum(),
    100 * (results_7a["pop_non_minority"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_minority"].sum(),
    100 * (results_7a["pop_senior"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_senior"].sum(),
    100 * (results_7a["pop_non_senior"] * results_7a["emp_educ"] / destinations["emp_educ"].sum()).sum() / results_7a["pop_non_senior"].sum()
         )],
    columns=["measure",
             "scenario_id",
             "pop_job_enroll",
             "pop_low_income_job_enroll",
             "pop_non_low_income_job_enroll",
             "pop_minority_job_enroll",
             "pop_non_minority_job_enroll",
             "pop_senior_job_enroll",
             "pop_non_senior_job_enroll",
             "pop_pct_job_enroll",
             "pop_low_income_pct_job_enroll",
             "pop_non_low_income_pct_job_enroll",
             "pop_minority_pct_job_enroll",
             "pop_non_minority_pct_job_enroll",
             "pop_senior_pct_job_enroll",
             "pop_non_senior_pct_job_enroll"
             ])


# Performance Measure 7b
# initialize empty list to store results
results_7b = list()
# for each record in the population DataFrame
for index, row in population_7b.iterrows():
    # get the indices of the TAZs accessible within 15 minutes
    # from the population TAZ using mid-day skim
    # note the 0-index in the skim matrix
    # the population row index taz_13 must be converted to integer
    # it is converted to string earlier in the groupby function
    index = int(index)
    taz_accessible = list(np.where(md_sovgpl_time[index - 1] <= 15)[0] + 1)

    # get the maximum of the destinations
    # with TAZ indices accessible within 15 minutes
    # note the destinations taz_13 column must be converted to integer
    # it is converted to string earlier in the groupby function
    access_beachactive = destinations[destinations["taz_13"].astype("int").isin(taz_accessible)]["beachactive"].max()
    access_parkactive = destinations[destinations["taz_13"].astype("int").isin(taz_accessible)]["parkactive"].max()

    # sum the destinations with TAZ indices accessible within 15 minutes
    # note the destinations taz_13 column must be converted to integer
    # it is converted to string earlier in the groupby function
    access_emp_health = destinations[destinations["taz_13"].astype("int").isin(taz_accessible)]["emp_health"].sum()
    access_emp_retail = destinations[destinations["taz_13"].astype("int").isin(taz_accessible)]["emp_retail"].sum()

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
    "Performance Measure 7b - Auto",
    scenario_id,
    (results_7b["pop"] * results_7b["emp_retail"]).sum() / results_7b["pop"].sum(),
    (results_7b["pop_low_income"] * results_7b["emp_retail"]).sum() / results_7b["pop_low_income"].sum(),
    (results_7b["pop_non_low_income"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_low_income"].sum(),
    (results_7b["pop_minority"] * results_7b["emp_retail"]).sum() / results_7b["pop_minority"].sum(),
    (results_7b["pop_non_minority"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_minority"].sum(),
    (results_7b["pop_senior"] * results_7b["emp_retail"]).sum() / results_7b["pop_senior"].sum(),
    (results_7b["pop_non_senior"] * results_7b["emp_retail"]).sum() / results_7b["pop_non_senior"].sum(),
    (results_7b["pop"] * results_7b["emp_health"]).sum() / results_7b["pop"].sum(),
    (results_7b["pop_low_income"] * results_7b["emp_health"]).sum() / results_7b["pop_low_income"].sum(),
    (results_7b["pop_non_low_income"] * results_7b["emp_health"]).sum() / results_7b["pop_non_low_income"].sum(),
    (results_7b["pop_minority"] * results_7b["emp_health"]).sum() / results_7b["pop_minority"].sum(),
    (results_7b["pop_non_minority"] * results_7b["emp_health"]).sum() / results_7b["pop_non_minority"].sum(),
    (results_7b["pop_senior"] * results_7b["emp_health"]).sum() / results_7b["pop_senior"].sum(),
    (results_7b["pop_non_senior"] * results_7b["emp_health"]).sum() / results_7b["pop_non_senior"].sum(),
    100 * (results_7b["pop"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop"].sum(),
    100 * (results_7b["pop_low_income"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_low_income"].sum(),
    100 * (results_7b["pop_non_low_income"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_low_income"].sum(),
    100 * (results_7b["pop_minority"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_minority"].sum(),
    100 * (results_7b["pop_non_minority"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_minority"].sum(),
    100 * (results_7b["pop_senior"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_senior"].sum(),
    100 * (results_7b["pop_non_senior"] * results_7b["emp_retail"] / destinations["emp_retail"].sum()).sum() / results_7b["pop_non_senior"].sum(),
    100 * (results_7b["pop"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop"].sum(),
    100 * (results_7b["pop_low_income"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_low_income"].sum(),
    100 * (results_7b["pop_non_low_income"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_low_income"].sum(),
    100 * (results_7b["pop_minority"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_minority"].sum(),
    100 * (results_7b["pop_non_minority"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_minority"].sum(),
    100 * (results_7b["pop_senior"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_senior"].sum(),
    100 * (results_7b["pop_non_senior"] * results_7b["emp_health"] / destinations["emp_health"].sum()).sum() / results_7b["pop_non_senior"].sum(),
    100 * (results_7b["pop"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop"].sum()),
    100 * (results_7b["pop_low_income"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_low_income"].sum()),
    100 * (results_7b["pop_non_low_income"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_low_income"].sum()),
    100 * (results_7b["pop_minority"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_minority"].sum()),
    100 * (results_7b["pop_non_minority"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_minority"].sum()),
    100 * (results_7b["pop_senior"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_senior"].sum()),
    100 * (results_7b["pop_non_senior"].where(results_7b["parkactive"] == 1, 0).sum() / results_7b["pop_non_senior"].sum()),
    100 * (results_7b["pop"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop"].sum()),
    100 * (results_7b["pop_low_income"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_low_income"].sum()),
    100 * (results_7b["pop_non_low_income"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_low_income"].sum()),
    100 * (results_7b["pop_minority"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_minority"].sum()),
    100 * (results_7b["pop_non_minority"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_minority"].sum()),
    100 * (results_7b["pop_senior"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_senior"].sum()),
    100 * (results_7b["pop_non_senior"].where(results_7b["beachactive"] == 1, 0).sum() / results_7b["pop_non_senior"].sum())
    )],
    columns=["measure",
             "scenario_id",
             "pop_retail",
             "pop_low_income_retail",
             "pop_non_low_income_retail",
             "pop_minority_retail",
             "pop_non_minority_retail",
             "pop_senior_retail",
             "pop_non_senior_retail",
             "pop_health",
             "pop_low_income_health",
             "pop_non_low_income_health",
             "pop_minority_health",
             "pop_non_minority_health",
             "pop_senior_health",
             "pop_non_senior_health",
             "pop_pct_retail",
             "pop_low_income_pct_retail",
             "pop_non_low_income_pct_retail",
             "pop_minority_pct_retail",
             "pop_non_minority_pct_retail",
             "pop_senior_pct_retail",
             "pop_non_senior_pct_retail",
             "pop_pct_health",
             "pop_low_income_pct_health",
             "pop_non_low_income_pct_health",
             "pop_minority_pct_health",
             "pop_non_minority_pct_health",
             "pop_senior_pct_health",
             "pop_non_senior_pct_health",
             "pct_pop_parkactive",
             "pct_pop_low_income_parkactive",
             "pct_pop_non_low_income_parkactive",
             "pct_pop_minority_parkactive",
             "pct_pop_non_minority_parkactive",
             "pct_pop_senior_parkactive",
             "pct_pop_non_senior_parkactive",
             "pct_pop_beachactive",
             "pct_pop_low_income_beachactive",
             "pct_pop_non_low_income_beachactive",
             "pct_pop_minority_beachactive",
             "pct_pop_non_minority_beachactive",
             "pct_pop_senior_beachactive",
             "pct_pop_non_senior_beachactive"
            ])



# write Performance Measures 7a/b - Auto out to csv
# in the scenario report folder
if uats == "0":
    output_path = scenario_path + "/report/pm_7ab_auto_region.csv"
if uats == "1":
    output_path = scenario_path + "/report/pm_7ab_auto_uats.csv"

output_7a.melt().to_csv(output_path,
                        index=False,
                        encoding="utf-8",
                        header=False)

output_7b.melt().to_csv(output_path,
                        index=False,
                        encoding="utf-8",
                        mode="a",
                        header=False)

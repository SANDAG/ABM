from __future__ import print_function
from datetime import datetime
import pandas as pd
import openpyxl  # necessary for pandas ExcelWriter
import pyodbc
import sys

# error string to print if user inputs invalid parameters
usage = ("Correct Usage: emfac_2014.py <EMFAC version: 2014 | 2017> "
         "<scenario_id> <Season: Annual | Summer | Winter> "
         "<SB 375: On | Off> <Output Folder>")

# if too few/many arguments passed raise error
if len(sys.argv) != 6:
    print(usage)
    sys.exit(-1)

# user inputs
# raise error if emfac version argument passed incorrectly
emfac_version = sys.argv[1]
if emfac_version not in ["2014", "2017"]:
    print(usage)
    sys.exit(-1)
scenario_id = int(sys.argv[2])
season = sys.argv[3]
# raise error if season argument passed incorrectly
if season not in ["Annual", "Summer", "Winter"]:
    print(usage)
    sys.exit(-1)
sb375 = sys.argv[4]
# raise error if sb375 argument passed incorrectly
if sb375 not in ["On", "Off"]:
    print(usage)
    sys.exit(-1)
output_folder = sys.argv[5]

# create emfac settings data frame
emfac_settings = {"Parameter": ["Date", "Season/Month", "SB375 Run"],
                  "Value": [datetime.now().strftime("%x %X"), season, sb375]}
emfac_settings = pd.DataFrame(data=emfac_settings)

# set sql server connection string
# noinspection PyArgumentList
sql_con = pyodbc.connect(driver='{SQL Server}',
                         server='${database_server}',
                         database='${database_name}',
                         trusted_connection='yes')

# get scenario information for the given scenario_id
scenario = pd.read_sql_query(
    sql=("SELECT RTRIM([name]) AS [name], [year] "
         "FROM [dimension].[scenario] WHERE [scenario_id] = ?"),
    con=sql_con,
    params=[scenario_id]
)

# build the output xlsx save location file path
output_path = output_folder + "\EMFAC" + emfac_version + "-SANDAG-" + \
              str(scenario.at[0, "name"]) + "-" + str(scenario_id) + "-" + \
              season + "-" + str(scenario.at[0, "year"])
if sb375 == "On":
    output_path = output_path + "-sb375"
output_path = output_path + ".xlsx"

# get emfac vmt results for the given scenario_id
vmt = pd.read_sql_query(
    sql="EXECUTE [emfac].[sp_emfac_{0}_vmt] @scenario_id = ?".
        format(emfac_version),
    con=sql_con,
    params=[scenario_id]
)

# get emfac vmt speed results for the given scenario_id
vmt_speed = pd.read_sql_query(
    sql="EXECUTE [emfac].[sp_emfac_{0}_vmt_speed] @scenario_id = ?".
        format(emfac_version),
    con=sql_con,
    params=[scenario_id]
)

# create ExcelWriter for pandas write to excel workbook
writer = pd.ExcelWriter(output_path,
                        engine="openpyxl")

# write emfac settings to workbook
emfac_settings.to_excel(excel_writer=writer,
                        sheet_name="Settings",
                        index=False)

# write emfac vmt results to workbook
vmt.to_excel(excel_writer=writer,
             sheet_name="Daily_VMT_By_Veh_Tech",
             index=False)

# write emfac vmt speed results to workbook
vmt_speed.to_excel(excel_writer=writer,
                   sheet_name="Hourly_Fraction_Veh_Tech_Speed",
                   index=False)

# save output workbook
writer.save()

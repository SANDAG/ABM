# Rick.Curry@sandag.org
# May 23, 2017
# Reads regional EE, EI, IE excel files and writes out individual csv files for each forecast year

import pandas as pd
import os
import time

# Start Script Timer
start = time.time()

# Set paths for filenames
fileOutPath = "E:/apps/ABM_develop/input_truck/"

##########################################
#EE
#########################################
# Set paths for filenames
fileEEtrips = "E:/apps/ABM_develop/input_truck/regionalEEtripsTotal.xlsx"
fileEEOutNameBase = "regionalEEtrips"

# Read in Excel Trip Sheet into a Panda DataFrame
df_EETrips = pd.read_excel(io=fileEEtrips, sheetname="regionalEEtripsTotal", header=0, skiprows=0)

# Loop through forecast columns and write out to csv
for column in df_EETrips:
    if column not in ("EE_ID", "fromZone", "toZone"):
        # Set filename
        fileEEOutName = str(column).join([fileEEOutNameBase,".csv"])

        # Delete Existing Output Files
        fileEEOut = fileOutPath + fileEEOutName
        if os.path.exists(fileEEOut):
            os.remove(fileEEOut)

        colNames = ["fromZone", "toZone", column]
        headerName = ["fromZone", "toZone", "EETrucks"]
        print fileEEOut, colNames, headerName
        df_EETrips.to_csv(fileEEOut, index=False, columns=colNames, header=headerName)

#########################################
#EI
#########################################
# Set paths for filenames
fileEItrips = "E:/apps/ABM_develop/input_truck/regionalEItripsTotal.xlsx"
fileEIOutNameBase = "regionalEItrips"

# Read in Excel Trip Sheet into a Panda DataFrame
df_EITrips = pd.read_excel(io=fileEItrips, sheetname="regionalEItripsTotal", header=0, skiprows=0)

# Loop through forecast columns and write out to csv
for column in df_EITrips:
    if column not in ("EI_ID", "fromZone"):
        # Set filename
        fileEIOutName = str(column).join([fileEIOutNameBase, ".csv"])

        # Delete Existing Output Files
        fileEIOut = fileOutPath + fileEIOutName
        if os.path.exists(fileEIOut):
            os.remove(fileEIOut)

        colNames = ["fromZone", column]
        headerName = ["fromZone", "EITrucks"]
        print fileEIOut, colNames, headerName
        df_EITrips.to_csv(fileEIOut, index=False, columns=colNames, header=headerName)

#########################################
# IE
#########################################
# Set paths for filenames
fileIEtrips = "E:/apps/ABM_develop/input_truck/regionalIEtripsTotal.xlsx"
fileIEOutNameBase = "regionalIEtrips"

# Read in Excel Trip Sheet into a Panda DataFrame
df_IETrips = pd.read_excel(io=fileIEtrips, sheetname="regionalIEtripsTotal", header=0, skiprows=0)

# Loop through forecast columns and write out to csv
for column in df_IETrips:
    if column not in ("IE_ID", "toZone"):
        # Set filename
        fileIEOutName = str(column).join([fileIEOutNameBase, ".csv"])

        # Delete Existing Output Files
        fileIEOut = fileOutPath + fileIEOutName
        if os.path.exists(fileIEOut):
            os.remove(fileIEOut)

        colNames = ["toZone", column]
        headerName = ["toZone", "IETrucks"]
        print fileIEOut, colNames, headerName
        df_IETrips.to_csv(fileIEOut, index=False, columns=colNames, header=headerName)

#########################################

# Print Script Execution Time
print "Finished in %5.2f mins" % ((time.time() - start)/60.0)

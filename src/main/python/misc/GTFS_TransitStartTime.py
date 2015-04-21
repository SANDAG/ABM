########################################################################################################
# Main Program Area
########################################################################################################
import csv
import pandas as pd
import os

# Set paths for filenames
stopTimeFilepath = "M:/RES/TransModel/DevelopmentProjects/2013_DTA/Task4-Networks/Transit/MTS/stop_times.txt"
tripsFilepath = "M:/RES/TransModel/DevelopmentProjects/2013_DTA/Task4-Networks/Transit/MTS/trips.txt"
routeStart_File = "M:/RES/TransModel/DevelopmentProjects/2013_DTA/Task4-Networks/Transit/MTS/route_starttime.csv"

# Initialize dictionary and remove existing Route Output File
dict_TripStart = {}
if os.path.exists(routeStart_File):
    os.remove(routeStart_File)

# Read Stop Time File for Trip Starts
with open(stopTimeFilepath) as stopTimeFP:
    reader = csv.DictReader(stopTimeFP)
    # Remove intermediate stops and save just first Trip ID start
    for row in reader:
        if dict_TripStart.setdefault(row['trip_id'],) is None or row['departure_time'] < dict_TripStart[row['trip_id']]:
            dict_TripStart[row['trip_id']] = row['departure_time']

# Put trips starts into a Panda DataFrame, sort on Trip ID and reassign the Trip ID datatype for later join
df_TripStart = pd.DataFrame(dict_TripStart.items(), columns=['trip_id', 'departure_time'])
df_TripStart.sort(columns='trip_id', inplace=True)
df_TripStart.index = xrange(0, len(df_TripStart))
df_TripStart[['trip_id']] = df_TripStart[['trip_id']].astype(long)

# Read in Trip Info file from GTFS into a Panda DataFrame
df_TripNameFull = pd.read_csv(tripsFilepath)
df_TripNameFull.sort(columns='trip_id', inplace=True)
df_TripNameFull.index = xrange(0, len(df_TripNameFull))

# Merge Trip Starts with Trip Info for output to csv
df_RouteInfo = pd.merge(df_TripStart, df_TripNameFull, how='outer', on='trip_id')
df_RouteInfo.to_csv(routeStart_File)
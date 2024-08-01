# -*- coding: utf-8 -*-
import sys
import pandas as pd
import openmatrix as omx

model_dir = sys.argv[1]
output_dir = sys.argv[2]
scenario_year_with_suffix = str(sys.argv[3])

# File Path
cvm_trip_path = f"{output_dir}\\final_cv_trips.csv"
cvm_route_path = f"{output_dir}\\final_routes.csv"
full_skim_path = f"{model_dir}\\Output\\skims\\traffic_skims_MD.omx"
MGRA_TAZ_ref_path = f"{model_dir}\\input\\land_use.csv"
auto_path = f"{model_dir}\\input\parametersByYears.csv"

def read_omx(skimtype):
   
    traffic_skims = []
    with omx.open_file(full_skim_path) as omx_file:
        for name in [f"SOV_NT_M_{skimtype}__MD"]:
            matrix = omx_file[name]
            df_skim = pd.DataFrame(matrix[:])
            
            # Adjust the index and columns to start from 1
            df_skim.index = range(1, df_skim.shape[0] + 1)
            df_skim.columns = range(1, df_skim.shape[1] + 1)
            
            stacked_df = df_skim.stack().reset_index()  # Reset the index to make it separate columns
            stacked_df.columns = ["origin", "destination", name]  # Rename the columns
            traffic_skims.append(stacked_df)
    traffic_skims = pd.concat(traffic_skims, axis=1)
    
    return traffic_skims

traffic_skims_Dist = read_omx('DIST')
#traffic_skims_Time = read_omx('TIME')
traffic_skims_TollCost = read_omx('TOLLCOST')


# Read CSV Files
trip = pd.read_csv(cvm_trip_path)
route = pd.read_csv(cvm_route_path)
ref = pd.read_csv(MGRA_TAZ_ref_path)
df_auto_cost = pd.read_csv(auto_path)


# CVM CV Trip File
trip = trip.merge(ref[['mgra','taz']], how='left', right_on=['mgra'], left_on=['trip_origin']).drop('mgra', axis=1).rename(columns={'taz': 'taz_origin'})
trip = trip.merge(ref[['mgra','taz']], how='left', right_on=['mgra'], left_on=['trip_destination']).drop('mgra', axis=1).rename(columns={'taz': 'taz_destination'})


df = trip.merge(route[['route_id','vehicle_type_abm3']],left_on='route_id',right_on='route_id',how='left')

# df = df[['route_id','cv_trip_id','taz_origin','taz_destination', 'vehicle_type_abm3', 'trip_start_time']]

tod_mapping = {
    **{i: 'EA' for i in range(1, 7)},
    **{i: 'AM' for i in range(7, 13)},
    **{i: 'MD' for i in range(13, 26)},
    **{i: 'PM' for i in range(26, 33)},
    **{i: 'EV' for i in range(33, 48)}}

def map_value_to_range(value):
    return tod_mapping.get(value, None)

df['tod'] = df['trip_start_time'].apply(map_value_to_range)


###
# dfnew = df.groupby(['taz_origin', 'taz_destination', 'vehicle_type_abm3','tod']).agg({'route_id': 'nunique', 'cv_trip_id': 'count'}).reset_index()
df = df.merge(traffic_skims_Dist, how='left', left_on=['taz_destination','taz_origin'], right_on=['destination','origin']).drop(['origin', 'destination'], axis=1).rename(columns={'SOV_NT_M_DIST__MD': 'distanceDrive'})
df = df.merge(traffic_skims_TollCost, how='left', left_on=['taz_destination','taz_origin'], right_on=['destination','origin']).drop(['origin', 'destination'], axis=1).rename(columns={'SOV_NT_M_TOLLCOST__MD': 'costTollDrive'})

df.rename(columns={'vehicle_type_abm3': 'vehicle_type'}, inplace=True)

###

df['costOperatingDrive'] = None

# Mapping dictionary for conditions
mapping = {'passenger_car': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.fuel'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.maintenance'].iloc[0],
            'LHDT': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.light'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.light'].iloc[0],
            'MHDT': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.medium'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.medium'].iloc[0],
            'HHDT': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.high'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.high'].iloc[0]}


def calculate_cost(row):
    return row['distanceDrive'] * mapping[row['vehicle_type']]

df['costOperatingDrive'] = df.apply(calculate_cost, axis=1)

 
df['vehicle_type'] = df['vehicle_type'].replace('passenger_car', 'DRIVEALONE')

###

df.to_csv(f'{output_dir}\\final_trips.csv',index=False)

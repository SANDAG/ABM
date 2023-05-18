# %%
import pandas as pd
pd.set_option("display.max_columns",250)
import os
import numpy as np
import geopandas
import matplotlib.pyplot as plt

# %% [markdown]
# ## Read in SPA Input and Output Tables
# SPA input is needed in order to merge information from survey that was not written by the SPA tool

# %%
estimation_path = r"C:\ABM3_dev\estimation"

configs_dir = os.path.join(estimation_path, r"configs")

landuse_file = r"C:\ABM3_dev\run_data\data_2z_series15\land_use.csv"
landuse = pd.read_csv(landuse_file)

# contains shape file
data_dir = os.path.join(estimation_path, r"data\series15\mgra15")
taz_dir = os.path.join(estimation_path, r"data\series15\taz15")

final_output_path = r"{dir}\survey_data".format(dir=estimation_path)

infer_py_location = r"{dir}\scripts\infer.py".format(dir=estimation_path)
infer_run_command = "python " + infer_py_location + " " + estimation_path + "\data " + configs_dir

# reading in 2016 survey
data_16_folder = os.path.join(estimation_path, r"data\sandag_2016_survey\output")
# reading in raw survey to be able to geocode home, school, and work locations
raw_16_folder = os.path.join(estimation_path, r"data\sandag_2016_survey\data")

# reading in 2022 survey
data_22_folder = os.path.join(estimation_path, r"data\sandag_2022_survey")
# reading in raw survey to be able to geocode home, school, and work locations
raw_22_folder = os.path.join(estimation_path, r"data\sandag_2022_survey\sandag_hts")

# number of periods in activitysim (48 30-minute periods)
num_periods = 48

# %%
maz_taz_xwalk = landuse[['MAZ', 'TAZ']]

# %%
taz15 = geopandas.read_file(os.path.join(taz_dir, 'taz15.shp'))
mgra15 = geopandas.read_file(os.path.join(data_dir, 'mgra15.shp'))
# CA zone 6 to lat long
taz15 = taz15.to_crs(epsg=4326)
mgra15 = mgra15.to_crs(epsg=4326)

# %%
# cropped_zones = mgra15[mgra15.ZIP.isin([92101, 92103])]
# cropped_zones.MGRA.to_csv('downtown_zones.csv', index=False)
# cropped_zones.explore()

# %%
trips_16 = {}
tours_16 = {}
jtours_16 = {}
persons_16 = {}
households_16 = {}

for day in range(1,8):
    print("day", day)
    data_folder = os.path.join(data_16_folder, f"day{day}")
    
    households_16[day] = pd.read_csv(os.path.join(data_folder, "households.csv"))
    persons_16[day] = pd.read_csv(os.path.join(data_folder, "persons.csv"))
    tours_16[day] = pd.read_csv(os.path.join(data_folder, "tours.csv"))
    jtours_16[day] = pd.read_csv(os.path.join(data_folder, "unique_joint_tours.csv"))
    trips_16[day] = pd.read_csv(os.path.join(data_folder, "trips.csv"))

    households_16[day]['day'] = day
    persons_16[day]['day'] = day
    tours_16[day]['day'] = day
    jtours_16[day]['day'] = day
    trips_16[day]['day'] = day
   
# dropping duplicate household and person records
households_16 = pd.concat(households_16.values(), ignore_index=True)
persons_16 = pd.concat(persons_16.values(), ignore_index=True)
tours_16 = pd.concat(tours_16.values(), ignore_index=True)
jtours_16 = pd.concat(jtours_16.values(), ignore_index=True)
trips_16 = pd.concat(trips_16.values(), ignore_index=True)

households_16['survey_year'] = 2016
persons_16['survey_year'] = 2016
tours_16['survey_year'] = 2016
jtours_16['survey_year'] = 2016
trips_16['survey_year'] = 2016

print("Number of Households: ", len(households_16))
print("Number of Persons: ", len(persons_16))
print("Number of Joint Tours: ", len(jtours_16))
print("Number of Tours: ", len(tours_16))
print("Number of Trips: ", len(trips_16))

raw_hh_16 = pd.read_csv(os.path.join(raw_16_folder, 'SDRTS_Household_Data_20170731.csv'))
raw_person_16 = pd.read_csv(os.path.join(raw_16_folder, 'SDRTS_Person_Data_20170731.csv'))
raw_vehicle_16 = pd.read_csv(os.path.join(raw_16_folder, 'SDRTS_Vehicle_Data_20170731.csv'))

# %%
trips_22 = {}
tours_22 = {}
jtours_22 = {}
persons_22 = {}
households_22 = {}

for day in range(1,5):
    print("day", day)
    data_folder = os.path.join(data_22_folder, "SPA_Processed", f"day{day}")
    
    households_22[day] = pd.read_csv(os.path.join(data_folder, "households.csv"))
    persons_22[day] = pd.read_csv(os.path.join(data_folder, "persons.csv"))
    tours_22[day] = pd.read_csv(os.path.join(data_folder, "tours.csv"))
    jtours_22[day] = pd.read_csv(os.path.join(data_folder, "unique_joint_tours.csv"))
    trips_22[day] = pd.read_csv(os.path.join(data_folder, "trips.csv"))

    households_22[day]['day'] = day
    persons_22[day]['day'] = day
    tours_22[day]['day'] = day
    jtours_22[day]['day'] = day
    trips_22[day]['day'] = day
   
households_22 = pd.concat(households_22.values(), ignore_index=True)
persons_22 = pd.concat(persons_22.values(), ignore_index=True)
tours_22 = pd.concat(tours_22.values(), ignore_index=True)
jtours_22 = pd.concat(jtours_22.values(), ignore_index=True)
trips_22 = pd.concat(trips_22.values(), ignore_index=True)

households_22['survey_year'] = 2022
persons_22['survey_year'] = 2022
tours_22['survey_year'] = 2022
jtours_22['survey_year'] = 2022
trips_22['survey_year'] = 2022

spa_input_hhs = os.path.join(data_22_folder, "SPA_Processed", f"day{day}")

print("Number of Households: ", len(households_22))
print("Number of Persons: ", len(persons_22))
print("Number of Joint Tours: ", len(jtours_22))
print("Number of Tours: ", len(tours_22))
print("Number of Trips: ", len(trips_22))

raw_hh_22 = pd.read_csv(os.path.join(raw_22_folder, 'hh.csv'))
raw_person_22 = pd.read_csv(os.path.join(raw_22_folder, 'person.csv'))
raw_vehicle_22 = pd.read_csv(os.path.join(raw_22_folder, 'ex_vehicle.csv'))

# %%
spa_out_hh_df = pd.concat([households_16, households_22]).reset_index(drop=True)
spa_out_per_df = pd.concat([persons_16, persons_22]).reset_index(drop=True)
spa_out_tours_df = pd.concat([tours_16, tours_22]).reset_index(drop=True)
spa_out_ujtours_df = pd.concat([jtours_16, jtours_22]).reset_index(drop=True)
spa_out_trips_df = pd.concat([trips_16, trips_22]).reset_index(drop=True)

# %%
spa_out_tours_df

# %%
spa_out_trips_df

# %% [markdown]
# ## Geocoding

# %%
### Process taz shp to seperate external stations
ext_gdf = taz15[taz15['TAZ_new']<=12].copy()
ext_gdf = ext_gdf.to_crs(4326)

ext_gdf = ext_gdf.rename({'TAZ_new': 'TAZ'}, axis=1)

ext_gdf.reset_index(drop=True, inplace=True)
ext_gdf = pd.merge(ext_gdf, 
                   maz_taz_xwalk[['TAZ', 'MAZ']], 
                   on='TAZ', 
                   how='left')
print("External Stations: ")
ext_gdf

# %%
# don't need to perfrom the tranformation every time below function is called.
mgra15_CA = mgra15.to_crs(epsg=2230)
ext_gdf_CA = ext_gdf.to_crs(epsg=2230)


def geocode_to_mgra15(df, cols_to_keep, x_col, y_col):
    # spatial join to mgra file
    cols_to_keep.append(x_col)
    cols_to_keep.append(y_col)
    df_geocode = geopandas.GeoDataFrame(df[cols_to_keep], geometry=geopandas.points_from_xy(x=df[x_col], y=df[y_col]))
    print(f"{len(df_geocode)} entries to geocode")

    df_geocode.set_crs(epsg=4326, inplace=True)
    assert mgra15.crs == df_geocode.crs, "Mis-matching CRS!"
    df_geocode = geopandas.sjoin(mgra15[['MGRA', 'TAZ', 'geometry']], df_geocode, how='right', predicate='contains')

    print(f"\t{df_geocode['MGRA'].isna().sum()} zones are not within the mgra15 area.")
    missing_xy = (df_geocode[x_col].isna() | df_geocode[y_col].isna())
    print(f"\t{missing_xy.sum()} are missing x or y coordinates.")

    # assigning coastal zones
    df_geocode['coast_geocode'] = 0
    coastal_zones = df_geocode[
        (df_geocode['MGRA'].isna())
        & ~missing_xy
        & (df_geocode.geometry.x >= -117.389) & (df_geocode.geometry.x <= -117.136)
        & (df_geocode.geometry.y >= 32.579)& (df_geocode.geometry.y <= 33.195)
    ]
    # converting to geometric crs needed for nearest join, CA state plane 6
    # need to perform after above filter
    coastal_zones = coastal_zones.to_crs(epsg=2230)

    if len(coastal_zones) > 0:
        coastal_zones = geopandas.sjoin_nearest(mgra15_CA[['MGRA', 'TAZ', 'geometry']], coastal_zones[['geometry']], how='right')
        df_geocode.loc[coastal_zones.index, 'MGRA'] = coastal_zones.MGRA
        df_geocode.loc[coastal_zones.index, 'TAZ'] = coastal_zones.TAZ
        df_geocode.loc[coastal_zones.index, 'coast_geocode'] = 1
    print(f"\t{len(coastal_zones)} are coastal zones.")

    # assigning external zones
    missing_ext_zones = df_geocode[df_geocode['MGRA'].isna() & ~missing_xy]
    missing_ext_zones = missing_ext_zones.to_crs(epsg=2230)
    print(f"\t{len(missing_ext_zones)} are assumed to be external zones.")

    df_geocode['dist_to_ext_station'] = 0
    if len(missing_ext_zones) > 0:
        ext_zones = geopandas.sjoin_nearest(
            ext_gdf_CA[['TAZ', 'MAZ', 'geometry']],
            missing_ext_zones[['geometry']],
            how='right',
            max_distance=360 * 5280, # within 360 miles
            distance_col='dist_to_ext_station')

        df_geocode.loc[ext_zones.index, 'MGRA'] = ext_zones.MAZ
        df_geocode.loc[ext_zones.index, 'TAZ'] = ext_zones.TAZ
        df_geocode.loc[ext_zones.index, 'dist_to_ext_station'] = ext_zones.dist_to_ext_station
    
    print(f"\t{(df_geocode['MGRA'].isna() & ~missing_xy).sum()} entries are outside of 360 mile buffer.")  
    
    assert (df_geocode.index == df.index).all(), "Bad Merge!"
    return df_geocode

# %% [markdown]
# #### home zone id

# %%
print("geocoding home location")
hh_22_geocode = geocode_to_mgra15(raw_hh_22.set_index('hh_id'), cols_to_keep=['hh_weight'], x_col='home_lon', y_col='home_lat')
hh_16_geocode = geocode_to_mgra15(raw_hh_16.set_index('hhid'), cols_to_keep=['hh_final_weight_456x'], x_col='home_lng', y_col='home_lat')

hhid_to_home_zone_dict = hh_22_geocode['MGRA'].to_dict()
hhid_to_home_zone_dict.update(hh_16_geocode['MGRA'].to_dict())

# %%
assert spa_out_hh_df.HH_ID.isin(hhid_to_home_zone_dict.keys()).all()
spa_out_hh_df['home_zone_id'] = spa_out_hh_df['HH_ID'].map(hhid_to_home_zone_dict)

# %% [markdown]
# #### school and work zone ID's

# %%
print(f"geocoding school location:")
sch_22_geocode = geocode_to_mgra15(raw_person_22, cols_to_keep=['hh_id', 'person_num'], x_col='school_lon', y_col='school_lat')
sch_22_geocode.rename(columns={'MGRA': 'school_zone_id', 'hh_id': 'HH_ID', 'person_num': 'PER_ID'}, inplace=True)
sch_16_geocode = geocode_to_mgra15(raw_person_16, cols_to_keep=['hhid', 'pernum'], x_col='mainschool_lng', y_col='mainschool_lat')
sch_16_geocode.rename(columns={'MGRA': 'school_zone_id', 'hhid': 'HH_ID', 'pernum': 'PER_ID'}, inplace=True)
sch_geocode = pd.concat([sch_22_geocode, sch_16_geocode])
sch_geocode

# %%
print(f"geocoding workplace location:")
work_22_geocode = geocode_to_mgra15(raw_person_22, cols_to_keep=['hh_id', 'person_num'], x_col='work_lon', y_col='work_lat')
work_22_rename_dict = {'MGRA': 'work_zone_id', 'hh_id': 'HH_ID', 'person_num': 'PER_ID', 'work_lon': 'work_x', 'work_lat': 'work_y'}
work_22_geocode.rename(columns=work_22_rename_dict, inplace=True)
work_16_geocode = geocode_to_mgra15(raw_person_16, cols_to_keep=['hhid', 'pernum'], x_col='work_lng', y_col='work_lat')
work_22_rename_dict = {'MGRA': 'work_zone_id', 'hh_id': 'HH_ID', 'person_num': 'PER_ID', 'work_lon': 'work_x', 'work_lat': 'work_y'}
work_16_geocode.rename(columns={'MGRA': 'work_zone_id', 'hhid': 'HH_ID', 'pernum': 'PER_ID'}, inplace=True)
work_geocode = pd.concat([work_22_geocode, work_16_geocode])
work_geocode

# %%
orig_index = spa_out_per_df.index
school_work_geocode = pd.concat([sch_geocode[['HH_ID', 'PER_ID', 'school_zone_id']], work_geocode[['work_zone_id']]], axis=1)
spa_out_per_merge_df = pd.merge(spa_out_per_df, school_work_geocode, how='left', on=['HH_ID', 'PER_ID'], suffixes=('', ''))
pd.testing.assert_index_equal(spa_out_per_merge_df.index, orig_index)
spa_out_per_df[['school_zone_id', 'work_zone_id']] = spa_out_per_merge_df[['school_zone_id', 'work_zone_id']].fillna(-1)

# %% [markdown]
# Tours

# %%
for end in ['ORIG', 'DEST']:
    print(f"geocoding tour {end}:")
    tour_geocode_df = geocode_to_mgra15(spa_out_tours_df, cols_to_keep=['HH_ID', 'PER_ID', 'TOUR_ID'], x_col=end + '_X', y_col=end + '_Y')
    assert (tour_geocode_df.index == spa_out_tours_df.index).all(), "Bad Merge!"
    spa_out_tours_df[end + '_MAZ'] = tour_geocode_df['MGRA']
    spa_out_tours_df[end + '_TAZ'] = tour_geocode_df['TAZ']

# %% [markdown]
# Trips

# %%
for end in ['ORIG', 'DEST']:
    trip_geocode_df = geocode_to_mgra15(spa_out_trips_df, cols_to_keep=['HH_ID', 'PER_ID', 'TOUR_ID', 'TRIP_ID'], x_col=end + '_X', y_col=end + '_Y')
    assert (trip_geocode_df.index == spa_out_trips_df.index).all(), "Bad Merge!"
    spa_out_trips_df[end + '_MAZ'] = trip_geocode_df['MGRA']
    spa_out_trips_df[end + '_TAZ'] = trip_geocode_df['TAZ']

# %%
# import folium
# from folium import plugins

# heat_data = [[point.xy[1][0], point.xy[0][0]] for point in trip_geocode_df.geometry]

# map = folium.Map(location=[32.95, -116.897], tiles="OpenStreetMap", zoom_start=9, height='100%', width='100%')

# plugins.HeatMap(heat_data, min_opacity=0.2, radius=12).add_to(map)

# map

# %%
def reindex(series1, series2):
    result = series1.reindex(series2)
    try:
        result.index = series2.index
    except AttributeError:
        pass
    return result

# %% [markdown]
# ## Processing tours and trips involving coast

# %%
## Defining functions used to identify and assign nearest MAZ/TAZ
###
def get_dist(a,b,c,d):
    R = 3958.8

    if type(c) != float:
        a = np.repeat(a, len(c))
        b = np.repeat(b, len(c))
    
    lat1 = np.deg2rad(a)
    lon1 = np.deg2rad(b)
    lat2 = np.deg2rad(c)
    lon2 = np.deg2rad(d)

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = (np.sin(dlat/2))**2 + np.cos(lat1) * np.cos(lat2) * (np.sin(dlon/2))**2
    c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1-a))
    distance = R * c
    return distance


# %% [markdown]
# ## Processing tours and trips with external stops

# %%
### Identify trips whose destination is not tour's primary destination but external 
spa_out_trips_df['external_dest_is_stop'] = 0
spa_out_trips_df.loc[spa_out_trips_df['DEST_TAZ'].isin(ext_gdf['TAZ'])&
                     (spa_out_trips_df['DEST_IS_TOUR_DEST']==0), 'external_dest_is_stop'] = 1

# %%
### Get the group of tours having an external stop
temp_ext_df = spa_out_trips_df[spa_out_trips_df['external_dest_is_stop']==1]
temp_ext_df = temp_ext_df.groupby(['HH_ID', 'PER_ID', 'TOUR_ID', 'day']).size().reset_index().rename(columns={0: 'has_external_stop'})
temp_ext_df['has_external_stop'] = 1
temp_ext_df

# %%
### Add the column identifying if a tour has external stop or not
spa_out_tours_df = pd.merge(spa_out_tours_df, 
                            temp_ext_df,
                            how='left',
                            on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'], 
                            suffixes=('','_x'))
spa_out_tours_df['has_external_stop'].fillna(0, inplace=True)

# %%
### Add tour origin coordinates to each trip
spa_out_trips_merge_df = pd.merge(spa_out_trips_df, 
                            spa_out_tours_df[['HH_ID', 'PER_ID', 'TOUR_ID', 'day', 'ORIG_X', 'ORIG_Y']],
                            how='left',
                            on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'], 
                            suffixes=('','_TOUR'))

### Calculate distance b/w tour origin and trip destinations
spa_out_trips_df['dest_dist'] = spa_out_trips_merge_df.apply(lambda x: 
    (get_dist(a=x.DEST_Y, b=x.DEST_X, c=x.ORIG_Y_TOUR, d=x.ORIG_X_TOUR)), axis=1)

### Trips that are further than 360 miles from tour origin are open jaw tours
spa_out_trips_df['IE_open_jaw'] = 0
spa_out_trips_df.loc[spa_out_trips_df['dest_dist']>360, 'IE_open_jaw'] = 1

# %%
### Get the group of tours having an external stop
temp_open_jaw_df = spa_out_trips_df[spa_out_trips_df['IE_open_jaw']==1]
temp_open_jaw_df = temp_open_jaw_df.groupby(['HH_ID', 'PER_ID', 'TOUR_ID', 'day']).size().reset_index().rename(columns={0: 'IE_open_jaw'})
temp_open_jaw_df['IE_open_jaw'] = 1
temp_open_jaw_df

# %%
### Add the column identifying if a tour is open jawed or not
spa_out_tours_df = pd.merge(spa_out_tours_df,
                            temp_open_jaw_df,
                            how='left',
                            on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'], 
                            suffixes=('','_x'))

# %%
### Coding external tour origins
spa_out_tours_df['OTAZ_ext'] = 0
spa_out_tours_df.loc[spa_out_tours_df['ORIG_TAZ'].isin(ext_gdf['TAZ']), 'OTAZ_ext'] = 1

### Coding external tour destinations
spa_out_tours_df['DTAZ_ext'] = 0
spa_out_tours_df.loc[spa_out_tours_df['DEST_TAZ'].isin(ext_gdf['TAZ']), 'DTAZ_ext'] = 1

'''
External tour types:
    II - Fully internal tour
    II-Ext - Starts and ends internally but whose primary destination is external (these could also have external stops)
    II-Ext(internal_dest) - Tours that start and end internally with an internal primary destination but at least one external stop
    EE - Fully external tour
'''
spa_out_tours_df['external_type'] = np.nan
spa_out_tours_df.loc[(spa_out_tours_df['OTAZ_ext']==0), 'external_type'] = 'II'
spa_out_tours_df.loc[(spa_out_tours_df['OTAZ_ext']==0)&
                     (spa_out_tours_df['DTAZ_ext']==1), 'external_type'] = 'II-Ext' 
spa_out_tours_df.loc[(spa_out_tours_df['OTAZ_ext']==0)&
                     (spa_out_tours_df['DTAZ_ext']==0)&
                     (spa_out_tours_df['has_external_stop']==1), 'external_type'] = 'II-Ext(internal_dest)'
spa_out_tours_df.loc[(spa_out_tours_df['IE_open_jaw']==1), 'external_type'] = 'IE-Open_Jaw'
spa_out_tours_df.loc[(spa_out_tours_df['OTAZ_ext']==1), 'external_type'] = 'EE'
spa_out_tours_df['external_type'].value_counts(dropna=False)

# %% [markdown]
# ## Processing Household File

# %% [markdown]
# The spa output household file just selects SAMPN and HH_SIZE and renames to HH_ID and NUM_PERS.  AREA is not used.  So, processing this file requires just changing the variables for the input hh file.

# %%
hh_inc_cat_dict_22 = {
    1: [0,14999],
    2: [15000,24999],
    3: [25000,34999],
    4: [35000,49999],
    5: [50000,74999],
    6: [75000,99999],
    7: [100000,149999],
    8: [150000,199999],
    9: [200000,249999],
    10: [249999,300000], # 250k and up
    999: [80000, 85000], # median income in san diego is about $83,000
}

hh_inc_cat_dict_16 = {
    1: [0,14999],
    2: [15000,29999],
    3: [30000,44999],
    4: [45000,59999],
    5: [60000,74999],
    6: [75000,99999],
    7: [100000,129999],
    8: [125000,149999],
    9: [150000,199999],
    10: [200000,249999],
    11: [249999,300000], # 250k and up
    99: [80000, 85000], # median income in san diego is about $83,000
}

def interpolate_hh_income(row):
    inc_cat = row['HH_INC_CAT']
    if inc_cat < 0:
        return pd.NA
    else:
        if row['survey_year'] == 2022:
            return np.random.randint(hh_inc_cat_dict_22[inc_cat][0], hh_inc_cat_dict_22[inc_cat][1])
        else:
            # converting 2016 dollars to 2022
            # $1 in 2016 is worth $1.25 in 2022 (https://www.bls.gov/data/inflation_calculator.htm)
            return np.random.randint(hh_inc_cat_dict_22[inc_cat][0], hh_inc_cat_dict_22[inc_cat][1]) * 1.25

# %%
asim_hh_df = pd.DataFrame()

asim_hh_df['HH_ID'] = spa_out_hh_df['HH_ID']
asim_hh_df['home_zone_id'] = spa_out_hh_df['home_zone_id']
asim_hh_df['survey_year'] = spa_out_hh_df['survey_year']
asim_hh_df['hhsize'] = spa_out_per_df[['HH_ID', 'PER_ID']].drop_duplicates().groupby('HH_ID')['PER_ID'].count().reindex(asim_hh_df.HH_ID, fill_value=0).values
asim_hh_df['day'] = spa_out_hh_df['day']
asim_hh_df['num_workers'] = spa_out_per_df[spa_out_per_df['PERSONTYPE'].isin([1,2])][['HH_ID', 'PER_ID']].drop_duplicates().groupby('HH_ID')['PER_ID'].count().reindex(asim_hh_df.HH_ID, fill_value=0).values
asim_hh_df['auto_ownership'] = pd.concat([raw_hh_22.set_index('hh_id')['num_vehicles'], raw_hh_16.set_index('hhid')['vehicle_count']]).reindex(asim_hh_df.HH_ID).values
asim_hh_df['auto_ownership'].clip(upper=4, inplace=True)
asim_hh_df['HH_INC_CAT'] = pd.concat([raw_hh_22.set_index('hh_id')['income_detailed'], raw_hh_16.set_index('hhid')['hhincome_imputed']]).reindex(asim_hh_df.HH_ID).fillna(999).values
assert ~(asim_hh_df[['num_workers', 'auto_ownership', 'HH_INC_CAT']].isna()).any().any()

res_type_coding_dict = { # mapping 2022 survey codes to 2016 codes
    1: 1, # single family detatched --> single family detatched
    2: 2, # single family attached --> single family attached
    3: 3, # 2-4 units --> 3 or fewer units
    4: 4, # 5-49 units --> 4 or more units
    5: 4, # 50+ units  --> 4 or more units
    6: 6, # senior / age restricted --> dorm / barracks / institutional housing
    7: 5, # mobile home  --> mobile home
    8: 6, # dorm / GQ / institutional housing --> dorm / barracks / institutional housing
    9: 97, # other --> other
    995: 97, # missing --> other
}
asim_hh_df['res_type'] = pd.concat(
    [raw_hh_22.set_index('hh_id')['res_type'].map(res_type_coding_dict),
     raw_hh_16.set_index('hhid')['res_type']]
).reindex(asim_hh_df.HH_ID).fillna(999).values

asim_hh_df['bldgsz'] = np.select(
    # mobile home, single family detached, single family attached, 20-49 apartments, 50+ apartments
    [asim_hh_df['res_type'] == 5, asim_hh_df['res_type'] == 1,  asim_hh_df['res_type'] == 2, asim_hh_df['res_type'].isin([3,4]), asim_hh_df['res_type'].isin([6,97])],
    [1, 2, 3, 8, 9]
)
# HHT set below after creation of persons table

# Linear interpolation of income categories, and sample from distribution for missing income values
asim_hh_df['income'] = asim_hh_df.apply(lambda row: interpolate_hh_income(row), axis=1)

# %%
asim_hh_df.bldgsz.value_counts(dropna=False)

# %%
# grabbing toll transponder ownership from vehicles table
# household is said to own a transponder if at least one vehicle has a transponder
raw_vehicle_16['has_toll_transponder'] = np.where(raw_vehicle_16['tolltransp'] == 2, 1, 0)
tr_own_16 = raw_vehicle_16.groupby('hhid')['has_toll_transponder'].sum().clip(upper=1)
tr_own_22 = raw_vehicle_22.groupby('hh_id')['toll_transponder'].sum().clip(upper=1)
tr_own = pd.concat([tr_own_16, tr_own_22]).reindex(asim_hh_df.HH_ID).fillna(0)
asim_hh_df['transponder_ownership'] = tr_own.astype(bool).values

# %%
asim_hh_df.transponder_ownership.value_counts()

# %%
asim_hh_df['income'].hist(bins=100)

# %%
asim_hh_df['auto_ownership'].clip(upper=4).value_counts(dropna=False, normalize=True).loc[[0,1,2,3,4]].plot(kind='bar')

# %%
asim_hh_df.loc[asim_hh_df['HH_ID'].drop_duplicates().index, 'auto_ownership'].clip(upper=4).value_counts(dropna=False, normalize=True).loc[[0,1,2,3,4]].plot(kind='bar')

# %% [markdown]
# ## Processing Person File

# %%
# 2022 & 2016 surveys had different age categories
age_cat_dict_22 = {
    1: [0, 5],
    2: [5, 15],
    3: [16, 17],
    4: [18, 24],
    5: [25, 34],
    6: [35, 44],
    7: [45, 54],
    8: [55, 64],
    9: [65, 74],
    10: [75, 84],
    11: [85, 90], # 85 and up
}

age_cat_dict_16 = {
    1: [0, 4],
    2: [5, 15],
    3: [16, 17],
    4: [18, 24],
    5: [25, 34],
    6: [35, 44],
    7: [45, 49],
    8: [50, 54],
    9: [55, 59],
    10: [60, 64],
    11: [65, 74],
    12: [75, 79],
    13: [80, 84],
    14: [85, 90], # 85 and up
}

def interpolate_age(row):
    age_cat = row['AGE_CAT']
    if (age_cat > 0) & (age_cat < 14):
        if row['survey_year'] == 2016:
            return np.random.randint(age_cat_dict_16[row['AGE_CAT']][0], age_cat_dict_16[row['AGE_CAT']][1] + 1)  # [low,high)
        else:
            return np.random.randint(age_cat_dict_22[row['AGE_CAT']][0], age_cat_dict_22[row['AGE_CAT']][1] + 1)
    
    # impute age based on Student and employment category
    if row['pstudent'] == 1: # school
        return 13 # non-driving age student
    elif row['pstudent'] == 2:  # university
        return 20
    else:
        return 45 # generic adult


# %%
asim_per_df = pd.DataFrame()

keep_cols = ['HH_ID', 'PER_ID', 'day', 'survey_year']

asim_per_df[keep_cols] = spa_out_per_df[keep_cols]
asim_per_df['ptype'] = spa_out_per_df['PERSONTYPE']
asim_per_df['pstudent'] = spa_out_per_df['STU_CAT']
asim_per_df['is_student'] = asim_per_df['pstudent'].isin([1, 2]) # school or university
asim_per_df['pemploy'] = spa_out_per_df['EMP_CAT']
asim_per_df['AGE_CAT'] = spa_out_per_df['AGE_CAT'].where(~spa_out_per_df['AGE_CAT'].isna(), spa_out_per_df['AGE'], axis=0).fillna(999)
asim_per_df['age'] = asim_per_df.apply(lambda row: interpolate_age(row), axis=1)
asim_per_df['PNUM'] = spa_out_per_df['PER_ID']

# looks like some persontypes weren't coded correctly if age is missing
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 1), 'ptype'] = 7  # school, assumed non-driving age
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 2), 'ptype'] = 3  # univ student
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 3) & (asim_per_df['pemploy'] == 4), 'ptype'] = 4  # non-worker
asim_per_df['ptype'] = asim_per_df['ptype'].astype(int)

asim_per_df['school_zone_id'] = spa_out_per_df['school_zone_id']
asim_per_df['workplace_zone_id'] = spa_out_per_df['work_zone_id']

# %% [markdown]
# #### merging other data from raw table that was not available in SPA output

# %%
# sex = 1 for male and 2 for female in ActivitySim configs & synthetic population
raw_person_22['sex'] = np.where(raw_person_22['gender'] == 2, 1, 2) # if gender is 2: Male, then Male, else Female (2: Female, 3: transgender, 4: non-binary, 999: other)
raw_person_16['sex'] = np.where(raw_person_16['gender'] == 1, 1, 2) # if gender is 1: Male, then Male, else Female (2: Female, other)

raw_person_22['work_from_home'] = (raw_person_22['job_type'] == 3) # work only from home or remotely
raw_person_16['work_from_home'] = (raw_person_16['job_type'] == 3) # work only from home or remotely

raw_person_22['transit_pass_subsidy'] = np.where(raw_person_22['commute_subsidy_3'] == 1, 1, 0) # Commute Benefits Provided by Employer: Free/discount transit fare
raw_person_16['transit_pass_subsidy'] = np.where(raw_person_16['commute_subsidy_transit'] == 1, 1, 0) # Commute Subsidy: Free/subsidized transit fare (yes/no)

raw_person_22['transit_pass_ownership'] = np.where(raw_person_22['transit_pass_ownership'] == 1, 1, 0) # Has a PRONTO card
raw_person_16['transit_pass_ownership'] = np.where(raw_person_16['transitpass'].isin(range(1,14)), 1, 0) # 1-13 is transit pass variant, 14 is no, 98 is don't know

# commute_subsidy_1 asks whether Employer provides free partking at work, 1: yes, 0: no, other: N/A
raw_person_22['free_parking_at_work'] = np.where(raw_person_22['commute_subsidy_1'] == 1, True, False)
# 1: no cost, 2: Employer pays all parking, other includes discounts, paying for pass, full cost, and N/A
raw_person_16['free_parking_at_work'] = np.where(raw_person_16['work_park_pay'].isin([1,2]), True, False) 

telecommute_freq_dict_22 = {
    996: 'No_Telecommute', # never
    995: 'No_Telecommute', # missing
    1: '4_days_week', # 6-7 days a week
    2: '2_3_days_week', # 5 days a week
    3: '2_3_days_week', # 4 days a week
    4: '4_days_week', # 2-3 days a week
    5: '1_day_week', # 1 day a week
    6: '1_day_week', # 1-3 days a month
    7: 'No_Telecommute', # less than monthly
}
raw_person_22['telecommute_frequency'] = raw_person_22['telework_freq'].map(telecommute_freq_dict_22)
telecommute_freq_dict_16 = {
    1: '4_days_week', # 6-7 days a week
    2: '4_days_week', # 5 days a week
    3: '2_3_days_week', # 4 days a week
    4: '2_3_days_week', # 2-3 days a week
    5: '1_day_week', # 1 day a week
    6: '1_day_week', # 9 days every 2 weeks
    7: '1_day_week', # 1-3 days a month
    8: 'No_Telecommute', # less than monthly
    9: 'No_Telecommute', # never
}
raw_person_16['telecommute_frequency'] = raw_person_16['telecommute_freq'].fillna(9).map(telecommute_freq_dict_16)

# merge into asim_per_df table
raw_person_16['HH_ID'] = raw_person_16['hhid']
raw_person_16['PER_ID'] = raw_person_16['pernum']
raw_person_22['HH_ID'] = raw_person_22['hh_id']
raw_person_22['PER_ID'] = raw_person_22['person_num']



# %%
# Industry Coding
# new ABM coding (source: Grace email to Joel on 30Mar23 and forwarded to David on 4Apr23)
#[Govn't, utility/manufacturing/wholesale, military, agriculture/mining, business services, Finacnce/insurance/real estate/mgmt enterprises,
# Education, Healthcare, Retail Trade, Entertainment, Accommodation, Food Services, Construction/transporation/warehousing, other services,
# non-wage/salary WFH, non-wage / salary non-wfh]
# putting in pd.NA for no response and N/A

# FIXME: hard to separate food and accomodation, where to put fitness?
industry_coding_dict_2022 = {
    1: 'entertainment', # Arts and entertainment
    2: 'other', # Childcare (e.g., nanny, babysitter)
    3: 'construction', # Construction or landscaping
    4: 'education', # Education (public or private)
    5: 'government', # Government
    6: 'mgmt_srv', # Financial services
    7: 'healthcare', # Health care
    8: 'accomodation', # Hospitality (e.g., restaurant, accommodation)
    9: 'manufacturing', # Manufacturing (e.g., aerospace & defense, electrical, machinery)
    10: 'entertainment', # Media
    11: 'military', # Military
    12: 'agriculture', # Natural resources (e.g., forestry, fisher, energy)
    13: 'business_srv', # Professional and business services (e.g., consulting, legal, marketing)
    14: 'retail', # Personal services (e.g., hair styling, personal assistance, pet sitting)
    15: 'mgmt_srv', # Real estate
    16: 'retail', # Retail
    17: 'other', # Social assistance
    18: 'other', # Sports and fitness
    19: 'business_srv', # Technology and telecommunications
    20: 'construction', # Transportation and utilities
    997: 'other', # Other
    995: np.nan, # Missing Response
}
raw_person_22['industry_coded'] = raw_person_22['industry'].map(industry_coding_dict_2022)

# FIXME: no government category
industry_coding_dict_2016 = {
    1: 'accomodation', # Accommodation (e.g., hotels/motels)
    2: 'mgmt_srv', # Administrative, Support, & Waste Management Services
    3: 'agriculture', # Agriculture, Forestry, Fishing, & Hunting
    4: 'entertainment', # Arts, Entertainment, & Recreation
    5: 'construction', # Construction
    6: 'education', # Education Services
    7: 'food_srv', # Food Services & Drinking Places
    8: 'mgmt_srv', # Finance & Insurance
    9: 'healthcare', # Health Care & Social Assistance
    10: 'business_srv', # Information
    11: 'mgmt_srv', # Management of Companies & Enterprises
    12: 'manufacturing', # Manufacturing
    13: 'military', # Military
    14: 'agriculture', # Mining, Quarrying, & Oil/Gas Extraction
    15: 'other', # Other Services
    16: 'business_srv', # Professional, Scientific, & Technical Services
    17: 'mgmt_srv', # Public Administration
    18: 'mgmt_srv', # Real Estate, Rental, & Leasing
    19: 'retail', # Retail Trade
    20: 'construction', # Transportation & Warehousing
    21: 'construction', # Utilities
    22: 'construction', # Wholesale Trade
    97: 'other', # Other
    98: np.nan, # Don't Know
}
raw_person_16['industry_coded'] = raw_person_16['industry'].map(industry_coding_dict_2016)

merge_cols = ['HH_ID', 'PER_ID', 'work_from_home', 'telecommute_frequency', 'sex',
              'free_parking_at_work', 'transit_pass_subsidy', 'transit_pass_ownership', 'industry_coded', 'relationship']
raw_persons = pd.concat([raw_person_22[merge_cols], raw_person_16[merge_cols]])

asim_per_df = asim_per_df.merge(raw_persons, how='left', on=['HH_ID', 'PER_ID'])
asim_per_df.rename(columns={'industry_coded': 'industry'}, inplace=True)

# %%
# relationship coding: 
# 0: Self
# 1: Spouse/partner
# 2: Son/daughter/in-law
# 3: Father/mother/in-law
# 4: Brother/sister/in-law
# 5: Other relative (e.g., grandchild, cousin)
# 6: Roommate/friend
# 7: Household help
# 997 or 97: Other

# HHT coding logic
# Person 1 | HH size | Anyone identifies as spouse of p1? | Anyone identifies as roommate/help of person 1 | HHT | Description
# Male | 1 | NA | NA | 4 | 4 .Nonfamily household: Male householder: Living alone
# Female | 1 | NA | NA | 6 | 6 .Nonfamily household: Female householder: Living alone
# Male | 2 | yes | NA | 1 | 1 .Married couple household
# Female | 2 | yes | NA | 1 | 1 .Married couple household
# Male | >1 | no | No | 2 | 2 .Other family household: Male householder, no spouse present
# Female | >1 | no | No | 3 | 3 .Other family household: Female householder, no spouse present
# Male | >1 | no | yes | 5  | 5 .Nonfamily household: Male householder: Not living alone
# Female | >1 | no | yes | 7 | 7 .Nonfamily household: Female householder: Not living alone

def determine_hht(grp):
    per_1_sex = grp.loc[grp['PNUM'] == 1, 'sex'].values[0]
    male_head = (per_1_sex == 1)
    hh_id = grp.HH_ID.values[0]
    # hhsize = asim_hh_df.loc[asim_hh_df['HH_ID'] == hh_id, 'hhsize'].values[0]
    hhsize = grp.PER_ID.nunique()

    # display(grp)
    if grp.relationship.isin([7]).any():
        if ~grp.relationship.isin([1]).any():
            print("here")

    if male_head & (hhsize == 1):
        hht = 4 # non-family household, male householder, living alone
    elif ~male_head & (hhsize == 1):
        hht = 6 # non-family household, female householder, living alone
    elif (hhsize >= 2) & grp.relationship.isin([1]).any():
        hht = 1 # married couple household
    elif male_head & ~(grp.relationship.isin([1,7]).any()):
        hht = 2 # other family household, male householder, no spouse
    elif ~male_head & ~(grp.relationship.isin([1,7]).any()):
        hht = 3 # other family household, female householder, no spouse
    elif male_head & ~grp.relationship.isin([1]).any() & grp.relationship.isin([7]).any():
        hht = 5 # non-family household, male householder, not living alone
    elif ~male_head & ~grp.relationship.isin([1]).any() & grp.relationship.isin([7]).any():
        hht = 7 # non-family household, female householder, not living alone
    else:
        raise RuntimeError("Bad HHT coding for group: ", grp[['HH_ID', 'sex', 'PNUM', 'relationship']], hhsize, male_head)
    # print(f"HHT: {hht}, hhsize: {hhsize}, hhid: {hh_id}, male head?: {male_head}, 1 in rel? {grp.relationship.isin([1]).any()}")
    return hht

hht_df = asim_per_df.groupby('HH_ID').apply(lambda grp: determine_hht(grp))
asim_hh_df['HHT'] = asim_hh_df.HH_ID.map(hht_df.to_dict())
asim_hh_df['HHT'].value_counts(dropna=False)

# %%
pd.crosstab(asim_per_df['telecommute_frequency'], asim_per_df['survey_year'], dropna=False)

# %%
pd.crosstab(asim_per_df['work_from_home'], asim_per_df['survey_year'], dropna=False)

# %%
pd.crosstab(asim_per_df['sex'], asim_per_df['survey_year'], dropna=False)

# %%
pd.crosstab(asim_per_df['industry'], asim_per_df['survey_year'], dropna=False, margins=True)

# %% [markdown]
# ## Processing Tours

# %% [markdown]
# Tour mode coding in SPA tool:
#     * 'SOV': 1,
#     * 'HOV2': 2,
#     * 'HOV3': 3,
#     * 'WALK': 4,
#     * 'BIKE': 5,
#     * 'WALK-TRANSIT': 6,
#     * 'PNR-TRANSIT': 7,
#     * 'KNR-TRANSIT': 8,
#     * 'TNC-TRANSIT': 9,
#     * 'TAXI': 10,
#     * 'TNC-REG': 11,
#     * 'TNC-POOL': 12,
#     * 'SCHOOLBUS': 13,
#     * 'OTHER': 14
#     
# Tour purpose coding in SPA tool:
#     * 'HOME':         0,
#     * 'WORK':         1,
#     * 'UNIVERSITY':   2,
#     * 'SCHOOL':       3,
#     * 'ESCORTING':    4,
#     * 'SHOPPING':     5,
#     * 'MAINTENANCE':  6,
#     * 'EAT OUT':      7,
#     * 'SOCIAL/VISIT': 8,
#     * 'DISCRETIONARY':9,
#     * 'WORK-RELATED': 10,
#     * 'LOOP':         11,
#     * 'CHANGE MODE':  12,
#     * 'OTHER':        13
# 
# JOINT_STATUS coding in spa tool:
# * (1) independent tours:        all trips.JOINT==NOT-JOINT
# * (2) partially-joint tours:    all trips.JOINT<>JOINT; some are FULLY-JOINT, some are NOT-JOINT
# * (3) fully-joint tours:        tour.get_is_fully_joint()==True
# * (4) partially-joint problematic tours: some trips are NOT-JOINT, some are JOINT (not grouped)
# * (5) jointness-unclear tours :    no NOT-JOINT, some are JOINT
# 
# In ActivitySim, JOINT_STATUS == 3 are considered joint tours. Only fully-joint tours have joint_tour_participants.

# %%
# 2016 survey was also recoded to use these modes
tour_mode_spa_to_asim_dict_22 = {
    1: 'SOV',
    2: 'HOV2',
    3: 'HOV3',
    4: 'WALK',
    5: 'BIKE',
    6: 'WALK_LOC',
    7: 'WALK_PRM',
    8: 'WALK_MIX',
    9: 'PNR_LOC',
    10: 'PNR_PRM',
    11: 'PNR_MIX',
    12: 'KNR_LOC',
    13: 'KNR_PRM',
    14: 'KNR_MIX',
    15: 'TNC_LOC',
    16: 'TNC_PRM',
    17: 'TNC_MIX',
    18: 'TNC_SINGLE',
    19: 'TNC_SHARED',
    20: 'TAXI',
    21: 'SCH_BUS',
    22: 'SOV', # other to SOV
}

tour_purpose_spa_to_asim_dict = {
    0: 'home', # used for trips
    1: 'work',
    2: 'univ',
    3: 'school',
    4: 'escort',
    5: 'shopping',
    6: 'othmaint',
    7: 'eatout',
    8: 'social',
    9: 'othdiscr',
    10: 'othmaint',    # work-related, no counts in this category
    11: 'othdiscr',    # Loop
    12: 'othdiscr',    # Change mode 
    13: 'othdiscr',    # other
}


# %%
# determining parent tour purpose for subtours
spa_out_tours_df = pd.merge(
    spa_out_tours_df,
    spa_out_tours_df[['HH_ID','PER_ID','TOUR_ID', 'TOURPURP', 'day']],
    how='left',
    left_on=['HH_ID', 'PER_ID', 'PARENT_TOUR_ID', 'day'],
    right_on=['HH_ID','PER_ID','TOUR_ID', 'day'],
    suffixes=('','_y')
)
spa_out_tours_df.drop(columns='TOUR_ID_y', inplace=True)
spa_out_tours_df.rename(columns={'TOURPURP_y':'PARENT_TOURPURP'}, inplace=True)
spa_out_tours_df.head()

# %%
asim_tour_df = spa_out_tours_df[
    ['HH_ID', 'PER_ID','TOUR_ID', 'PARENT_TOUR_ID', 'JTOUR_ID', 'day', 'external_type',
     'OUT_ESCORT_TYPE', 'OUT_CHAUFFUER_ID', 'OUT_CHAUFFUER_PURP',
     'INB_ESCORT_TYPE', 'INB_CHAUFFUER_ID', 'INB_CHAUFFUER_PURP',
     'OUT_ESCORTING_TYPE', 'INB_ESCORTING_TYPE', 'OUT_ESCORTEE_TOUR_PURP', 'INB_ESCORTEE_TOUR_PURP']].copy()

asim_tour_df['origin'] = spa_out_tours_df['ORIG_MAZ']
asim_tour_df['destination'] = spa_out_tours_df['DEST_MAZ']
asim_tour_df['survey_year'] = spa_out_tours_df['survey_year']

asim_tour_df['start'] = spa_out_tours_df['ANCHOR_DEPART_BIN']
asim_tour_df['end'] = spa_out_tours_df['ANCHOR_ARRIVE_BIN']
asim_tour_df['duration'] = asim_tour_df['end'] - asim_tour_df['start']

# treating joint tours as only those that have a JTOUR_ID
asim_tour_df['is_joint'] = spa_out_tours_df['JTOUR_ID'].apply(lambda x: 1 if pd.notna(x) else 0)
asim_tour_df['is_subtour'] = spa_out_tours_df['IS_SUBTOUR']

asim_tour_df.loc[asim_tour_df['start'] > num_periods, 'start'] = asim_tour_df.loc[asim_tour_df['start'] > num_periods, 'start'] - num_periods
asim_tour_df.loc[asim_tour_df['end'] > num_periods, 'end'] = asim_tour_df.loc[asim_tour_df['end'] > num_periods, 'end'] - num_periods

asim_tour_df.loc[asim_tour_df['survey_year'] == 2016, 'tour_mode'] = spa_out_tours_df.loc[
    asim_tour_df['survey_year'] == 2016, 'TOURMODE'].map(tour_mode_spa_to_asim_dict_22)
asim_tour_df.loc[asim_tour_df['survey_year'] == 2022, 'tour_mode'] = spa_out_tours_df.loc[
    asim_tour_df['survey_year'] == 2022, 'TOURMODE'].map(tour_mode_spa_to_asim_dict_22)

asim_tour_df['tour_purpose'] = spa_out_tours_df['TOURPURP'].map(tour_purpose_spa_to_asim_dict)
asim_tour_df.loc[asim_tour_df['is_subtour'] == 1, 'parent_tour_purpose'] = spa_out_tours_df.loc[
    asim_tour_df['is_subtour'] == 1, 'PARENT_TOURPURP'].map(tour_purpose_spa_to_asim_dict)

assert (~asim_tour_df['tour_mode'].isna()).all(), "Missing tour modes!"
assert (~asim_tour_df['tour_purpose'].isna()).all(), "Missing tour purpose!"


# %%
asim_tour_df.loc[asim_tour_df['is_subtour'] == 1, 'parent_tour_purpose'].value_counts(dropna=False)

# %%
def determine_tour_category(row):
    if (row['is_subtour'] == 1) & (row['parent_tour_purpose'] == 'work'):
        return 'atwork'
    elif row['tour_purpose'] in ['work', 'univ', 'school']:
        return 'mandatory'
    elif row['is_joint'] == 1:
        return 'joint'
    else:
        return 'non_mandatory'

def determine_tour_type(row):
    if row['tour_category'] == 'atwork':
        if row['tour_purpose'] == 'work':
            return 'business'
        elif row['tour_purpose'] == 'eatout':
            return 'eat'
        else:
            return 'maint'
    else:
        return row['tour_purpose']
    
asim_tour_df['tour_category'] = asim_tour_df.apply(lambda row: determine_tour_category(row), axis=1)
asim_tour_df['tour_type'] = asim_tour_df.apply(lambda row: determine_tour_type(row), axis=1)
# asim_tour_df.loc[asim_tour_df['tour_category'] == 'atwork', 'tour_purpose'] = 'atwork'  # has to be after tour_type calculation

# %%
asim_tour_df['tour_category'].value_counts(dropna=False)

# %%
pd.crosstab(asim_tour_df['tour_type'], asim_tour_df['tour_category'], margins=True)

# %%
asim_tour_df['duration'].hist(bins=np.linspace(0,48,48))

# %% [markdown]
# ### Joint Tour Participants

# %%
spa_out_ujtours_df.head()

# %%
asim_jtour_participants_df = pd.melt(spa_out_ujtours_df,
       id_vars=['HH_ID', 'JTOUR_ID', 'day'],
       value_vars=['PERSON_1','PERSON_2','PERSON_3','PERSON_4','PERSON_5','PERSON_6','PERSON_7','PERSON_8','PERSON_9'])

# %%
asim_jtour_participants_df = asim_jtour_participants_df[pd.notna(asim_jtour_participants_df['value'])]
asim_jtour_participants_df.head()

# %%
asim_jtour_participants_df[asim_jtour_participants_df['HH_ID'] == 70004828]

# %%
asim_jtour_participants_df['participant_num'] = asim_jtour_participants_df['variable'].apply(lambda x: int(x.strip('PERSON_')))
asim_jtour_participants_df['PER_ID'] = asim_jtour_participants_df['value'].astype(int)

# %%
asim_jtour_participants_df

# %% [markdown]
# ## Re-Indexing
# Need unique household_id, per_id, tour_id, etc. for ActivitySim
# 

# %% [markdown]
# #### Household

# %%
# household ID should be unique already, but we want to be sure
asim_hh_df['household_id'] = asim_hh_df.reset_index().index + 1
asim_hh_df.head()

# %% [markdown]
# #### Person

# %%
asim_per_df = pd.merge(
    asim_per_df,
    asim_hh_df[['HH_ID', 'household_id', 'day']],
    how='left',
    on=['HH_ID', 'day'],
)

# %%
asim_per_df['person_id'] = asim_per_df.reset_index().index + 1
asim_per_df.head()

# %%
# need to re-number PNUM
# not every person is listed for every day in spa output.  See HH_ID == 22008078 for an example.
# There is a cut in infer looking for PNUM == 1, which every household needs.
asim_per_df['PNUM'] = asim_per_df.groupby('household_id')['person_id'].cumcount() + 1

# %% [markdown]
# Determining number of children in each household

# %%
hh_children = asim_per_df[asim_per_df['age'] < 18].groupby('household_id')['person_id'].count().to_frame()
hh_children.columns = ['children']

# %%
asim_hh_df.set_index('household_id', inplace=True)
asim_hh_df.loc[hh_children.index, 'children'] = hh_children['children']
asim_hh_df['children'] = asim_hh_df['children'].fillna(0).astype(int)
asim_hh_df.reset_index(inplace=True)

# %% [markdown]
# #### Tour

# %%
asim_tour_df.head()

# %%
asim_tour_df = pd.merge(
    asim_tour_df,
    asim_per_df[['day', 'HH_ID', 'PER_ID', 'household_id', 'person_id']],
    how='left',
    on=['HH_ID', 'PER_ID', 'day']
)

# %% [markdown]
# Joint tours are replicated in SPA output accross all members of the tour.  asim_tour_df will keep just the first instance of joint tours.  Members of the joint tours are tracked in the asim_jtour_participants_df table.
# 
# Not removing duplicated joint tours will cause different tour_id's to be assigned to the same joint tour.

# %%
asim_tour_df.sort_values(by=['day', 'HH_ID', 'JTOUR_ID', 'PER_ID', 'TOUR_ID',], inplace = True)
asim_tour_df['prev_JTOUR_ID'] = asim_tour_df['JTOUR_ID'].shift(1)
asim_tour_df['prev_HH_ID'] = asim_tour_df['HH_ID'].shift(1)
same_household = (asim_tour_df['HH_ID'] == asim_tour_df['prev_HH_ID'])
same_jtour = ((asim_tour_df['prev_JTOUR_ID'] == asim_tour_df['JTOUR_ID']) & asim_tour_df['JTOUR_ID'].notna())
asim_tour_df['is_duplicated_jtour'] = 0
asim_tour_df.loc[(same_household & same_jtour), 'is_duplicated_jtour'] = 1
asim_tour_df['hh_duplicated_jtours'] = asim_tour_df.groupby(['HH_ID'])['is_duplicated_jtour'].transform('sum')
asim_tour_df.sort_values(by=['day', 'HH_ID', 'PER_ID', 'TOUR_ID'], inplace = True)
all_asim_tour_df = asim_tour_df.copy()
asim_tour_df = asim_tour_df[asim_tour_df['is_duplicated_jtour'] == 0]

# %%
#asim_tour_df['survey_tour_id'] = asim_tour_df['TOUR_ID']
#asim_tour_df['survey_person_id'] = asim_tour_df['PER_ID']
asim_tour_df['tour_id'] = asim_tour_df.reset_index().index + 1

# merge parent_tour_id
asim_tour_df = pd.merge(
    asim_tour_df,
    asim_tour_df[['HH_ID', 'PER_ID', 'TOUR_ID', 'tour_id', 'day']],
    how='left',
    left_on=['HH_ID', 'PER_ID', 'PARENT_TOUR_ID', 'day'],
    right_on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'],
    suffixes=('','_y')
)
asim_tour_df.drop(columns='TOUR_ID_y', inplace=True)
asim_tour_df.rename(columns={'tour_id_y':'parent_tour_id'}, inplace=True)

# %%
# do not allow subtours that are not joint from joint tours
asim_tour_df = asim_tour_df[~(asim_tour_df['PARENT_TOUR_ID'].notna() & asim_tour_df['parent_tour_id'].isna())]

# %% [markdown]
# #### Joint Tour

# %%
asim_jtour_participants_df.head()

# %%
# merging person_id's separately since not every person may be listed in tour_file
asim_jtour_participants_df = pd.merge(
    asim_jtour_participants_df,
    asim_per_df[['HH_ID', 'PER_ID', 'day', 'household_id', 'person_id']],
    how='left',
    on=['HH_ID', 'PER_ID', 'day']
)

# merging tour_id 
asim_jtour_participants_df = pd.merge(
    asim_jtour_participants_df,
    asim_tour_df[['HH_ID', 'JTOUR_ID', 'tour_id', 'day']],
    how='left',
    on=['HH_ID', 'JTOUR_ID', 'day'],
)

# %%
asim_jtour_participants_df.sort_values(by=['household_id', 'tour_id', 'participant_num'], inplace=True)
asim_jtour_participants_df['participant_id'] = asim_jtour_participants_df.reset_index().index + 1
asim_jtour_participants_df.head()

# %%
all(asim_jtour_participants_df['tour_id'].isin(asim_tour_df['tour_id']))

# %% [markdown]
# ## Additional changes to make infer.py work

# %% [markdown]
# #### "univ" is not a trip option
# converting all univ trips to school

# %%
num_univ_tours = len(asim_tour_df[asim_tour_df['tour_type'] == 'univ'])
print("Number of univ tours: ", num_univ_tours)

# %%
asim_tour_df.loc[asim_tour_df['tour_type'] == 'univ', 'tour_type'] = "school"

# %% [markdown]
# #### No joint escorting mode
# error: Unable to parse string "j_escort1"
# 
# solution: recategorizing joint escort tours to non_mandatory escort tours

# %%
asim_tour_df[asim_tour_df['tour_type'] == 'escort']['tour_category'].value_counts()

# %%
asim_tour_df.loc[asim_tour_df['tour_type'] == 'escort', 'tour_category'] = 'non_mandatory'

# %%
asim_tour_df[asim_tour_df['tour_type'] == 'escort']['tour_category'].value_counts()

# %% [markdown]
# ## Removing tours

# %% [markdown]
# #### Tours with invalid start or end MAZ's are removed

# %%
orig_num_tours = len(asim_tour_df)
asim_tour_df = asim_tour_df[asim_tour_df['origin'].isin(landuse.mgra)]
asim_tour_df = asim_tour_df[asim_tour_df['destination'].isin(landuse.mgra)]
valid_maz_tours = len(asim_tour_df)
print("Removed", orig_num_tours - valid_maz_tours, "tours due to invalid tour start or end maz")
print(valid_maz_tours, " tours remain")

# removing these tours from the joint_tour_participants file
asim_jtour_participants_df = asim_jtour_participants_df[asim_jtour_participants_df['tour_id'].isin(asim_tour_df['tour_id'])]

# %%
asim_jtour_participants_df.head()

# %%
asim_jtour_participants_df['tot_num_participants'] = asim_jtour_participants_df.groupby('tour_id')['participant_num'].transform('max')
asim_jtour_participants_df['tot_num_participants'].value_counts(dropna=False)

# %% [markdown]
# ### Using configs files to determine allowed tour frequencies
# For example, each person can only have 2 mandatory tours

# %%
mand_tour_freq_alts_df = pd.read_csv(
    os.path.join(configs_dir, 'mandatory_tour_frequency_alternatives.csv'),
    comment="#")
non_mand_tour_freq_alts_df = pd.read_csv(
    os.path.join(configs_dir, 'non_mandatory_tour_frequency_alternatives.csv'),
    comment="#")
non_mand_tour_freq_alts_df['alt'] = non_mand_tour_freq_alts_df.index
joint_tour_freq_alts_df = pd.read_csv(
    os.path.join(configs_dir, 'joint_tour_frequency_alternatives.csv'),
    comment="#")
atwork_subtour_freq_alts_df = pd.read_csv(
    os.path.join(configs_dir, 'atwork_subtour_frequency_alternatives.csv'),
    comment="#")

# %%
def count_tours_by_category(df, category, count_by, tour_types):
    for tour_type in tour_types:
        count_name = category + "_" + tour_type
        print(tour_type)
        df[count_name] = \
            df[(df['tour_category'] == category) & (df['tour_type'] == tour_type)] \
            .groupby(count_by).cumcount() + 1
        df.loc[(df[count_by] != df.shift(1)[count_by]) & pd.isna(df[count_name]), count_name] = 0
        df[count_name].ffill(inplace=True)
    return df

# %%
# - checking mandatory tour frequency
mand_tour_types = list(mand_tour_freq_alts_df.columns.drop('alt'))
asim_tour_df = count_tours_by_category(
    df=asim_tour_df,
    category='mandatory',
    count_by='person_id',
    tour_types=mand_tour_types
)

asim_tour_df = pd.merge(
    asim_tour_df,
    mand_tour_freq_alts_df,
    how='left',
    right_on=mand_tour_types,
    left_on=['mandatory_' + tour_type for tour_type in mand_tour_types]
)
asim_tour_df.drop(labels=mand_tour_types, axis='columns', inplace=True)
asim_tour_df.rename(columns={'alt': 'mandatory_alt'}, inplace=True)
asim_tour_df[asim_tour_df['tour_category'] == 'mandatory']['mandatory_alt'].value_counts(dropna=False)

# %%
asim_tour_df['mandatory_school'].value_counts()

# %%
# - checking non_mandatory tour frequency
non_mand_tour_types = list(non_mand_tour_freq_alts_df.columns.drop('alt'))
asim_tour_df = count_tours_by_category(
    df=asim_tour_df,
    category='non_mandatory',
    count_by='person_id',
    tour_types=non_mand_tour_types
)

asim_tour_df = pd.merge(
    asim_tour_df,
    non_mand_tour_freq_alts_df,
    how='left',
    right_on=non_mand_tour_types,
    left_on=['non_mandatory_' + tour_type for tour_type in non_mand_tour_types]
)
asim_tour_df.drop(labels=non_mand_tour_types, axis='columns', inplace=True)
asim_tour_df.rename(columns={'alt': 'non_mandatory_alt'}, inplace=True)
asim_tour_df[asim_tour_df['tour_category'] == 'non_mandatory']['non_mandatory_alt'].value_counts(dropna=False)

# %%
# - checking atwork tour frequency
atwork_subtour_types = list(atwork_subtour_freq_alts_df.columns.drop('alt'))
asim_tour_df = count_tours_by_category(
    df=asim_tour_df,
    category='atwork',
    count_by='parent_tour_id',
    tour_types=atwork_subtour_types
)

asim_tour_df = pd.merge(
    asim_tour_df,
    atwork_subtour_freq_alts_df,
    how='left',
    right_on=atwork_subtour_types,
    left_on=['atwork_' + tour_type for tour_type in atwork_subtour_types]
)
asim_tour_df.drop(labels=atwork_subtour_types, axis='columns', inplace=True)
asim_tour_df.rename(columns={'alt': 'atwork_alt'}, inplace=True)
asim_tour_df[asim_tour_df['tour_category'] == 'atwork']['atwork_alt'].value_counts(dropna=False)

# %%
asim_tour_df[asim_tour_df['tour_category'] == 'atwork']['tour_type'].value_counts()

# %%
# - checking joint tour frequency
# FIXME need to modify to match new joint_tour_frequency_composition model
joint_tour_types = list(joint_tour_freq_alts_df.columns.drop('alt'))
asim_tour_df = count_tours_by_category(
    df=asim_tour_df,
    category='joint',
    count_by='household_id',
    tour_types=joint_tour_types
)

# merging joint tour alternatives
asim_tour_df = pd.merge(
    asim_tour_df,
    joint_tour_freq_alts_df,
    how='left',
    right_on=joint_tour_types,
    left_on=['joint_' + tour_type for tour_type in joint_tour_types]
)
asim_tour_df.drop(labels=joint_tour_types, axis='columns', inplace=True)
asim_tour_df.rename(columns={'alt': 'joint_alt'}, inplace=True)
asim_tour_df[asim_tour_df['tour_category'] == 'joint']['joint_alt'].value_counts(dropna=False)

# %%
asim_tour_df['keep_tour'] = 1
original_num_tours = len(asim_tour_df)

asim_tour_df.loc[(asim_tour_df['tour_category'] == 'mandatory') & pd.isna(asim_tour_df['mandatory_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'non_mandatory') & pd.isna(asim_tour_df['non_mandatory_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'atwork') & pd.isna(asim_tour_df['atwork_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'joint') & pd.isna(asim_tour_df['joint_alt']), 'keep_tour'] = 0

after_removed_tours = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])
print("Removed ", original_num_tours - after_removed_tours, "tours that did not match in the tour frequency configs files")

# %%
# do not allow 
original_num_tours = asim_tour_df['keep_tour'].sum()

asim_tour_df.loc[(asim_tour_df['is_subtour'] == 1) & (asim_tour_df['parent_tour_purpose'] != 'work'), 'keep_tour'] = 0

after_removed_tours = asim_tour_df['keep_tour'].sum()
print("Removed ", original_num_tours - after_removed_tours, "subtours without a parent work purpose")

# %%
### Marking tours that could be part of extension model
# FIXME: should be grabbing this from configs
asim_tour_df['included_by_extension_model'] = 0
asim_tour_df.loc[(asim_tour_df['keep_tour']==0)&
                 (asim_tour_df['tour_category']=='non_mandatory')&
                 (asim_tour_df['non_mandatory_escort']<=4)&
                 (asim_tour_df['non_mandatory_shopping']<=4)&
                 (asim_tour_df['non_mandatory_othmaint']<=4)&
                 (asim_tour_df['non_mandatory_othdiscr']<=4)&
                 (asim_tour_df['non_mandatory_eatout']<=3)&
                 (asim_tour_df['non_mandatory_social']<=3), 'included_by_extension_model'] = 1

# %%
asim_tour_df

# %% [markdown]
# ### If person takes a work tour, they can't work from home

# %%
people_taking_work_tours = asim_tour_df.loc[asim_tour_df['tour_purpose'] == 'work', 'person_id']
wfh_and_work_tour = asim_per_df.loc[asim_per_df['work_from_home'] & asim_per_df['person_id'].isin(people_taking_work_tours), 'person_id']
print(f"Changing work from home from True to False for {len(wfh_and_work_tour)} people that take a work tour")
asim_per_df.loc[asim_per_df['person_id'].isin(wfh_and_work_tour), 'work_from_home'] = False

# %% [markdown]
# #### Tour Start and End times must be acceptable
# Checking the tour_departure_and_duration_alternatives.csv configs file for allowable times

# %%
tdd_df = pd.read_csv(os.path.join(configs_dir, "tour_departure_and_duration_alternatives.csv"))
min_start_allowed = tdd_df['start'].min()
max_start_allowed = tdd_df['start'].max()
min_end_allowed = tdd_df['end'].min()
max_end_allowed = tdd_df['end'].max()

# %%
count_before_tdd = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])
asim_tour_df.loc[
    (asim_tour_df['start'] < min_start_allowed)
    | (asim_tour_df['start'] > max_start_allowed)
    | (asim_tour_df['end'] < min_end_allowed)
    | (asim_tour_df['end'] > max_end_allowed)
    | pd.isna(asim_tour_df['start'])
    | pd.isna(asim_tour_df['end'])
    | (asim_tour_df['start'] > asim_tour_df['end']), 'keep_tour'] = 0

count_after_tdd = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])

print("Removed an additional", count_before_tdd - count_after_tdd, "tours due to bad start/end times")

# %% [markdown]
# ### Reassigning tours from persons that make work or school trip but have invalid work or school MAZ

# %%
asim_per_df.loc[(asim_per_df['school_zone_id'].isin([0,-9999])) | (asim_per_df['school_zone_id'].isna()), 'school_zone_id'] = -1
asim_per_df.loc[(asim_per_df['workplace_zone_id'].isin([0,-9999])) | (asim_per_df['workplace_zone_id'].isna()), 'workplace_zone_id'] = -1

# %%
asim_per_df

# %%
univ_students = (asim_per_df['pstudent'] == 2)
gradeschool_students = ((asim_per_df['pstudent'] == 1) & (asim_per_df['age'] < 14))
highschool_students = ((asim_per_df['pstudent'] == 1) & (asim_per_df['age'] >= 14))

# %%
landuse['tot_college_enroll'] = landuse['collegeenroll'] + landuse['othercollegeenroll'] + landuse['adultschenrl']
univ_mazs = landuse[landuse['tot_college_enroll'] > 0]['mgra']
k_8_mazs = landuse[landuse['enrollgradekto8'] > 0]['mgra']
G9_12_mazs = landuse[landuse['enrollgrade9to12'] > 0]['mgra']
print(len(univ_mazs), 'MAZs with university enrollment')
print(len(k_8_mazs), 'MAZs with K-8 enrollment')
print(len(G9_12_mazs), 'MAZs with 9-12 enrollment')

# %%
def make_school_co(asim_per_df):
    school_co = asim_per_df[univ_students | gradeschool_students | highschool_students].copy()
    school_co['school_segment_named'] = 'university'
    school_co.loc[highschool_students, 'school_segment_named'] = 'highschool'
    school_co.loc[gradeschool_students, 'school_segment_named'] = 'gradeschool'
    school_co['has_landuse_maz'] = 0
    school_co.loc[(univ_students & (school_co['school_zone_id'].isin(univ_mazs)))
                  | (highschool_students & (school_co['school_zone_id'].isin(G9_12_mazs)))
                  | (gradeschool_students & (school_co['school_zone_id'].isin(k_8_mazs))),
                'has_landuse_maz'] = 1
    return school_co
school_co = make_school_co(asim_per_df)

# %%
def make_missing_school_maz_df(school_co):
    missing_maz_df = pd.DataFrame({
        'survey_total': school_co['school_segment_named'].value_counts(),
        'survey_missing_maz': school_co[school_co['school_zone_id'] == -1]['school_segment_named'].value_counts(),
        'has_valid_maz': school_co[school_co['has_landuse_maz'] == 1]['school_segment_named'].value_counts()
    })
    missing_maz_df.loc['total'] = missing_maz_df.sum()
    missing_maz_df['percent_missing'] = missing_maz_df['survey_missing_maz'] / missing_maz_df['survey_total'] * 100
    missing_maz_df['percent_valid'] = missing_maz_df['has_valid_maz'] / missing_maz_df['survey_total'] * 100
    return missing_maz_df

make_missing_school_maz_df(school_co)

# %%
bad_school_co = school_co[school_co['has_landuse_maz'] == 0]
bad_school_co_by_maz = bad_school_co.groupby(['school_zone_id', 'school_segment_named']).count().reset_index()
bad_school_co_by_maz = bad_school_co_by_maz.pivot_table(index='school_zone_id', columns='school_segment_named', values='person_id')
bad_school_co_by_maz = bad_school_co_by_maz.reset_index()
bad_school_co_by_maz = bad_school_co_by_maz.rename(columns={'school_zone_id': 'MAZ'}).fillna(0)
bad_school_co_by_maz = bad_school_co_by_maz[bad_school_co_by_maz['MAZ'] > 0]
bad_school_co_by_maz
# bad_school_co_by_maz.astype(int).to_csv('reported_school_locations_without_enrollment.csv', index=False)

# %%
maz_shp_landuse = pd.merge(mgra15, landuse, how='left', left_on='MGRA', right_on='mgra')
# maz_shp_landuse = geopandas.GeoDataFrame(maz_shp_landuse, geometry='geometry', crs=mgra15.crs)
maz_shp_landuse = maz_shp_landuse.to_crs(epsg=2230)  # CA state plane 6 (feet)

# %%
bad_school_shp = pd.merge(bad_school_co_by_maz, mgra15, how='left', left_on='MAZ', right_on='MGRA')
bad_school_shp = geopandas.GeoDataFrame(bad_school_shp, geometry='geometry', crs=mgra15.crs)
# ignore external zones
bad_school_shp = bad_school_shp[~bad_school_shp['geometry'].isna()]
bad_school_shp = bad_school_shp.to_crs(epsg=2230)  # CA state plane 6 (feet)

# %%
def find_closest_valid_maz(row, maz_shp_landuse):
    if (row.MAZ < 0) | pd.isna(row.MAZ):
        return row
    centroid = row['geometry']
    
    if row.gradeschool > 0:
        gradeschool_mazs = maz_shp_landuse[maz_shp_landuse['enrollgradekto8'] > 0].reset_index(drop=True)
        distances = [centroid.distance(geom) for geom in gradeschool_mazs['geometry']]
        row['closest_gradeschool_distance'] = np.amin(distances) / 5280 # ft to miles
        row['closest_gradeschool_maz'] = gradeschool_mazs.loc[np.argmin(distances), 'mgra']
        
    if row.highschool > 0:
        highschool_mazs = maz_shp_landuse[maz_shp_landuse['enrollgrade9to12'] > 0].reset_index(drop=True)
        distances = [centroid.distance(geom) for geom in highschool_mazs['geometry']]
        row['closest_highschool_distance'] = np.amin(distances) / 5280 # ft to miles
        row['closest_highschool_maz'] = highschool_mazs.loc[np.argmin(distances), 'mgra']
    
    if row.university > 0:
        univ_mazs = maz_shp_landuse[maz_shp_landuse['tot_college_enroll'] > 0].reset_index(drop=True)
        distances = [centroid.distance(geom) for geom in univ_mazs['geometry']]
        row['closest_university_distance'] = np.amin(distances) / 5280 # ft to miles
        row['closest_university_maz'] = univ_mazs.loc[np.argmin(distances), 'mgra']
    return row
    
school_reassign = bad_school_shp.apply(lambda row: find_closest_valid_maz(row, maz_shp_landuse), axis=1)

# %%
school_reassign['closest_gradeschool_distance'].hist(bins=20)
plt.xlabel("Distance [miles]")
plt.title("Center of Invalid MAZ to closest valid MAZ")
plt.ylabel("Gradeschool Counts")
plt.show()

# %%
school_reassign['closest_highschool_distance'].hist(bins=20)
plt.xlabel("Distance [miles]")
plt.title("Center of Invalid MAZ to closest valid MAZ")
plt.ylabel("Highschool Counts")
plt.show()

# %%
# univ_reassign_mazs = (school_reassign['closest_univ_maz'].notna())
asim_per_with_school_df = pd.merge(asim_per_df, school_reassign, how='left', left_on='school_zone_id', right_on='MAZ')
# asim_per_with_school_df.head()
asim_per_with_school_df.loc[univ_students, 'school_zone_id'] = np.where(
    asim_per_with_school_df.loc[univ_students, 'closest_university_maz'].isna(),
    asim_per_with_school_df.loc[univ_students, 'school_zone_id'],
    asim_per_with_school_df.loc[univ_students, 'closest_university_maz'])
asim_per_with_school_df.loc[gradeschool_students, 'school_zone_id'] = np.where(
    asim_per_with_school_df.loc[gradeschool_students, 'closest_gradeschool_maz'].isna(),
    asim_per_with_school_df.loc[gradeschool_students, 'school_zone_id'],
    asim_per_with_school_df.loc[gradeschool_students, 'closest_gradeschool_maz'])
asim_per_with_school_df.loc[highschool_students, 'school_zone_id'] = np.where(
    asim_per_with_school_df.loc[highschool_students, 'closest_highschool_maz'].isna(),
    asim_per_with_school_df.loc[highschool_students, 'school_zone_id'],
    asim_per_with_school_df.loc[highschool_students, 'closest_highschool_maz'])
num_reassigned = (asim_per_with_school_df['school_zone_id'] != asim_per_df['school_zone_id']).sum()
asim_per_df['school_zone_id'] = asim_per_with_school_df['school_zone_id']
print("Number of invalid school MAZ's reassigned:", num_reassigned)

# %%
new_school_co = make_school_co(asim_per_df)
make_missing_school_maz_df(new_school_co)

# %%
people_with_invalid_school_maz = asim_per_df.loc[asim_per_df['school_zone_id'] <= 0, 'person_id']
asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'school') 
    & asim_tour_df['person_id'].isin(people_with_invalid_school_maz),
    'keep_tour'] = 0

# %% [markdown]
# ### If person makes a work tour, but has an invalid (missing) workplace maz, use the first work tour destination as workplace maz

# %%
people_with_invalid_work_maz = asim_per_df.loc[asim_per_df['workplace_zone_id'] <= 0, 'person_id']

# %%
inferred_workplace_mazs = asim_tour_df[(asim_tour_df['tour_type'] == 'work') & asim_tour_df['person_id'].isin(people_with_invalid_work_maz)][['destination', 'person_id']]
inferred_workplace_mazs = inferred_workplace_mazs.drop_duplicates('person_id', keep='first')
inferred_workplace_mazs.set_index('person_id', inplace=True)
print("Inferred workplace maz for people that have a workplace zone_id missing but make a work tour: ")
inferred_workplace_mazs

# %%
asim_per_df.set_index('person_id', inplace=True)
asim_per_df.loc[inferred_workplace_mazs.index, 'workplace_zone_id'] = inferred_workplace_mazs['destination']
asim_per_df.reset_index(inplace=True)

# %%
# distinguishing between internal and external workers:
asim_per_df['external_worker_identification'] = np.where(asim_per_df['workplace_zone_id'].isin(ext_gdf['MAZ']), 0, 1)
asim_per_df['external_workplace_zone_id'] = np.where(asim_per_df['external_worker_identification'] == 0, asim_per_df['workplace_zone_id'], -1)

# %% [markdown]
# #### Change people that go to school and not work, but say they are workers
# Had a problem where an individual says they are a full time worker and a university student, but do not provide a work location and do not take a work tour, but do take a school tour. school_zone_id was changed to -1 after initialize households (full time workers do not get a school location), but cdap has 1 M school tour.  Since school_zone_id changed to -1, caused 0 probs error in mandatory tour frequency.

# %%
people_making_work_tours = asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'work')
    & (asim_tour_df['keep_tour'] == 1), 'person_id'].unique()
people_making_school_tours = asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'school') 
    & (asim_tour_df['keep_tour'] == 1), 'person_id'].unique()
workers_who_are_actually_students = (asim_per_df['person_id'].isin(people_making_school_tours) 
                                    & (asim_per_df['ptype'] == 1) # ft worker
                                    & ~asim_per_df['person_id'].isin(people_making_work_tours)
                                    & (asim_per_df['pstudent'] < 3)  # school or university, not non-student
                                    & (asim_per_df['school_zone_id'] > 0))
# FIXME are we allowing ft workers to go to school?
# if so, I think this is fine.  If not, need to change pytpe to part-time worker or univ students
# asim_per_df.loc[workers_who_are_actually_students, 'ptype'] = ..

# %%
print("number of workers who are actually students: ", workers_who_are_actually_students.sum())

# %% [markdown]
# #### Change people that go to work and not school, but say they are students
# Same problem as above -- peoply say they are students, but only go to work.  This makes for zero probs in mandatory tour frequency because they aren't assigned a workplace maz because they are not listed as workers.

# %%
asim_per_df.pemploy.value_counts()

# %%
people_making_work_tours = asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'work')
    & (asim_tour_df['keep_tour'] == 1), 'person_id'].unique()
people_making_school_tours = asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'school') 
    & (asim_tour_df['keep_tour'] == 1), 'person_id'].unique()
students_who_are_actually_workers = (asim_per_df['person_id'].isin(people_making_work_tours) 
                                    & (asim_per_df['pstudent'] < 3) # is a student
                                    & ~asim_per_df['person_id'].isin(people_making_school_tours)
                                    & (asim_per_df['school_zone_id'] < 0)
                                    & (asim_per_df['workplace_zone_id'] > 0))

asim_per_df.loc[students_who_are_actually_workers, 'ptype'] = 2 # part time worker
asim_per_df.loc[students_who_are_actually_workers, 'pemploy'] = 2 # part time worker

# # ptype and pstudent and pemploy are calculated on these fields
# asim_per_df.loc[students_who_are_actually_workers, 'WKW'] = 1
# asim_per_df.loc[students_who_are_actually_workers, 'WKHP'] = 40
# asim_per_df.loc[students_who_are_actually_workers, 'SCHG'] = -9
# asim_per_df.loc[students_who_are_actually_workers, 'ESR'] = 1


# %%
print("number of students who are actually fulltime workers: ", students_who_are_actually_workers.sum())

# %% [markdown]
# Also want to count people who are making school and work tours but didn't list themselves as a worker. annotate_persons.csv determines pemploy and it just checks for ESR != [3,6] to determine partime status, so just need to ensure ESR = 1 for all people who make a work tour.

# %%
# setting them to part time workers
asim_per_df.loc[(asim_per_df['person_id'].isin(people_making_work_tours) 
                & (asim_per_df['workplace_zone_id'] > 0)), 'ptype'] = 2
asim_per_df.loc[(asim_per_df['person_id'].isin(people_making_work_tours) 
                & (asim_per_df['workplace_zone_id'] > 0)), 'pemploy'] = 2


# %% [markdown]
# #### Activitysim does not allow full-time workers to go to school.  Turning all full time workers going to school into part-time instead.

# %%
full_time_workers_going_to_school = (asim_per_df['person_id'].isin(people_making_work_tours) 
                & asim_per_df['person_id'].isin(people_making_school_tours)
                & (asim_per_df['pemploy'] == 1))
# FIXME allow FT workers to go to school?
# asim_per_df.loc[full_time_workers_going_to_school, 'WKW'] = 5  # 14-26 number of weeks worked
# asim_per_df.loc[full_time_workers_going_to_school, 'WKHP'] = 35  # 35 hrs per week

# %%
print("Number of full time workers also going to school: ", full_time_workers_going_to_school.sum())

# %% [markdown]
# ### Further person type checking

# %%
asim_per_df['is_worker'] = asim_per_df['pemploy'].isin([1,2])  # full-time or part-time worker
asim_per_df['is_student'] = asim_per_df['pstudent'].isin([1,2])  # grade/highschool or university

# %%
assert (asim_per_df['is_worker'] & asim_per_df.workplace_zone_id.isna()).sum() == 0, "not all workers have valid workplace zone id"
assert (asim_per_df['is_student'] & asim_per_df.school_zone_id.isna()).sum() == 0, "not all students have valid school zone id"
assert (asim_per_df['is_worker'] & ~asim_per_df.ptype.isin([1,2,3,6])).sum() == 0, "worker is not in the allowed person types"
assert (asim_per_df['is_student'] & ~asim_per_df.ptype.isin([1,2,3,6,7,8])).sum() == 0, "student is not in the allowed person types"

# %% [markdown]
# #### Remove external starting and open jaw tours

# %%
### Remove external starting and open jaw tours
count_before_tdd = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])
asim_tour_df.loc[~(asim_tour_df['external_type'].isin(['II', 'II-Ext', 'II-Ext(internal_dest)'])), 'keep_tour'] = 0
# also removing external atwork subtours
asim_tour_df.loc[~(asim_tour_df['external_type'].isin(['II']))&(asim_tour_df['tour_category']=='atwork'), 'keep_tour'] = 0

count_after_tdd = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])

print("Removed an additional", count_before_tdd - count_after_tdd, "tours not starting and ending internally")

# %% [markdown]
# #### Coding external tour identification

# %%
asim_tour_df['external_joint_tour_identification'] = np.where((asim_tour_df.external_type != "II") & (asim_tour_df.tour_category == 'joint'), 0, 1)
asim_tour_df['external_non_mandatory_identification'] = np.where((asim_tour_df.external_type != "II") & (asim_tour_df.tour_category == 'non_mandatory'), 0, 1)

# %% [markdown]
# #### Setting school escorting variables
# Need to code escort type (ride_hail vs pure_escort) and chauffeur person ids here.  The rest is handled in infer.py.

# %%
chauf_type_dict = {
    1: 'ride_share',
    2: 'pure_escort',
    3: pd.NA, # none
}
asim_tour_df['out_escort_type'] = asim_tour_df['OUT_ESCORT_TYPE'].map(chauf_type_dict)
asim_tour_df['inb_escort_type'] = asim_tour_df['INB_ESCORT_TYPE'].map(chauf_type_dict)
# asim_tour_df['out_escortee_purp'] = asim_tour_df['OUT_ESCORTEE_TOUR_PURP'].map(tour_purpose_spa_to_asim_dict)
# asim_tour_df['inb_escortee_purp'] = asim_tour_df['INB_ESCORTEE_TOUR_PURP'].map(tour_purpose_spa_to_asim_dict)
# FIXME Below flags are only for chauffeur tours, not escortee tours!  But we are instead looking at escortee trips in infer.py
# asim_tour_df['school_esc_outbound'] = np.where(asim_tour_df['out_escortee_purp'] == 'school', asim_tour_df['out_escortee_type'], pd.NA)
# asim_tour_df['school_esc_inbound'] = np.where(asim_tour_df['inb_escortee_purp'] == 'school', asim_tour_df['inb_escortee_type'], pd.NA)


# %%
chauf_id_map = asim_per_df.set_index(['household_id', 'PER_ID'])['person_id'].to_dict()

def map_chauf_id(row, col='OUT_CHAUFFUER_ID'):
    if (row[col] == 'nan') | pd.isna(row[col]) | (row[col] == 'None'):
        return pd.NA
    try:
        return chauf_id_map[(row['household_id'], int(row[col]))]
    except ValueError:
        print(row['household_id'], row[col])
        return pd.NA


asim_tour_df['out_chauf_person_id'] = asim_tour_df.apply(lambda row: map_chauf_id(row, 'OUT_CHAUFFUER_ID'), axis=1)
asim_tour_df['inb_chauf_person_id'] = asim_tour_df.apply(lambda row: map_chauf_id(row, 'INB_CHAUFFUER_ID'), axis=1)
assert len(asim_tour_df[~asim_tour_df['out_chauf_person_id'].isna()]) == len(asim_tour_df[(asim_tour_df['OUT_CHAUFFUER_ID'].fillna(-1).replace('None', -1).astype(int) > 0)]), "Missing outbound chauffeur id"
assert len(asim_tour_df[~asim_tour_df['inb_chauf_person_id'].isna()]) == len(asim_tour_df[(asim_tour_df['INB_CHAUFFUER_ID'].fillna(-1).replace('None', -1).astype(int) > 0)]), "Missing inbound chauffeur id"

# %% [markdown]
# #### If person or parent tour is removed, also need to remove it's subtours

# %%
asim_tour_df.loc[
      pd.notna(asim_tour_df['parent_tour_id'])
      & ~(asim_tour_df['parent_tour_id'].isin(asim_tour_df.loc[asim_tour_df['keep_tour'] == 1, 'tour_id'])),
    'keep_tour'] = 0

# %%
print("Total number of Tours: ", len(asim_tour_df))
print("Total number of tours removed: ", len(asim_tour_df[asim_tour_df['keep_tour'] == 0]))
trimmed_asim_tour_df = asim_tour_df[asim_tour_df['keep_tour'] == 1].copy()
print("Final number of tours: ", len(trimmed_asim_tour_df))


# %% [markdown]
# Also need to remove the tours from the joint tour participants file

# %%
asim_jtour_participants_df

# %%
print("Initial number of joint tour participants: ", len(asim_jtour_participants_df))
trimmed_asim_jtour_participants_df = asim_jtour_participants_df[asim_jtour_participants_df['tour_id'].isin(trimmed_asim_tour_df['tour_id'])]
print("Final number of joint tour participants: ", len(trimmed_asim_jtour_participants_df))

# %% [markdown]
# ### Not all joint tours have a matching entry in joint_tour_participants
# error found in infer.py 'assert (tour_has_adults | tour_has_children).all()' when trying to assign joint tour composition.  If tours are classified as joint, but there are no adults or children on the tour, an error is thrown.
# 
# Only fully joint tours in the SPA tool have joint tour participants listed.
# 
# In this script, setting the asim_tour_df["is_joint"] = 1 for fully joint tours only solves this problem. The following code is a re-implementation of the code in infer.py to check that all joint tours have an adult or child

# %%
joint_tours = trimmed_asim_tour_df[trimmed_asim_tour_df['is_joint'] == 1]

# %%
joint_tour_participants = pd.merge(
    trimmed_asim_jtour_participants_df,
    asim_per_df,
    how='left',
    on='person_id'
)
joint_tour_participants['adult'] = (joint_tour_participants.age >= 18)

# %%
tour_has_adults = joint_tour_participants[joint_tour_participants.adult]\
        .groupby('tour_id').size().reindex(joint_tours['tour_id']).fillna(0) > 0
tour_has_adults

# %%
tour_has_children = joint_tour_participants[~joint_tour_participants.adult]\
        .groupby('tour_id').size().reindex(joint_tours['tour_id']).fillna(0) > 0
tour_has_children

# %%
tour_has_children.sum()

# %%
tour_has_adults.sum()

# %%
good_tours = (tour_has_children | tour_has_adults)
good_tours.sum()

# %%
assert good_tours.sum() == len(joint_tours)

# %% [markdown]
# ## Trips Processing

# %%
spa_out_trips_df = pd.merge(
    spa_out_trips_df,
    asim_tour_df[['HH_ID', 'PER_ID', 'TOUR_ID', 'household_id', 'person_id', 'tour_id', 'day', 'tour_purpose', 'external_type']],
    on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'],
    how='left',
    suffixes=('', '_x')
)

# %%
spa_out_trips_df

# %%
asim_trip_df = pd.DataFrame()

keep_cols = ['household_id', 'person_id', 'tour_id', 'day', 'survey_year', 'HH_ID', 
'PER_ID', 'TOUR_ID', 'TRIP_ID', 'ORIG_X', 'ORIG_Y', 'DEST_X', 'DEST_Y', 'external_type',
'ESCORTED', 'ESCORTING', 'NUM_PERSONS_ESCORTED', 'ESCORT_PERS_1', 'ESCORT_PERS_2', 
'ESCORT_PERS_3', 'ESCORT_PERS_4', 'ESCORT_PERS_5', 'DEST_ESCORTING']

asim_trip_df[keep_cols] = spa_out_trips_df[keep_cols]
asim_trip_df['origin'] = spa_out_trips_df['ORIG_MAZ']
asim_trip_df['destination'] = spa_out_trips_df['DEST_MAZ']
asim_trip_df['depart'] = spa_out_trips_df['ORIG_DEP_BIN']
asim_trip_df['primary_purpose'] = spa_out_trips_df['tour_purpose']
asim_trip_df['purpose'] = spa_out_trips_df['DEST_PURP'].map(tour_purpose_spa_to_asim_dict)
# asim_trip_df['trip_mode'] = spa_out_trips_df['TRIPMODE'].map(tour_mode_spa_to_asim_dict)
asim_trip_df['trip_num'] = spa_out_trips_df['TRIP_ID']
asim_trip_df['outbound'] = np.where(spa_out_trips_df['IS_INBOUND'] == 0, True, False)
asim_trip_df['home_zone_id'] = reindex(asim_hh_df.set_index('household_id')['home_zone_id'], asim_trip_df.household_id)
asim_trip_df['workplace_zone_id'] = reindex(asim_per_df.set_index('person_id')['workplace_zone_id'], asim_trip_df.person_id)
asim_trip_df['school_zone_id'] = reindex(asim_per_df.set_index('person_id')['workplace_zone_id'], asim_trip_df.person_id)
asim_trip_df['is_subtour'] = reindex(asim_tour_df.set_index('tour_id')['is_subtour'], asim_trip_df.tour_id)
asim_trip_df['parent_tour_id'] = reindex(asim_tour_df.set_index('tour_id')['parent_tour_id'], asim_trip_df.tour_id)
asim_trip_df['tour_category'] = reindex(asim_tour_df.set_index('tour_id')['tour_category'], asim_trip_df.tour_id)
# calculating trip_num and trip_count
grouped = asim_trip_df.groupby(["tour_id", "outbound"])
asim_trip_df["trip_num"] = grouped.cumcount() + 1
asim_trip_df["trip_count"] = asim_trip_df["trip_num"] + grouped.cumcount(ascending=False)
asim_trip_df['trip_id'] = asim_trip_df.reset_index().index + 1
asim_trip_df['keep_trip'] = 1

# %%
# 2022 survey has same trip & tour modes
asim_trip_df.loc[asim_trip_df['survey_year'] == 2022, 'trip_mode'] = spa_out_trips_df.loc[
    spa_out_trips_df['survey_year'] == 2022, 'TRIPMODE'].map(tour_mode_spa_to_asim_dict_22)

# 2016 survey was re-processed to 2022 survey modes
asim_trip_df.loc[asim_trip_df['survey_year'] == 2016, 'trip_mode'] = spa_out_trips_df.loc[
    spa_out_trips_df['survey_year'] == 2016, 'TRIPMODE'].map(tour_mode_spa_to_asim_dict_22)

# %% [markdown]
# Perform additional checks on the survey data.
#  1. Do all trips have a tour?
#  2. Are there four or fewer trips per tour and direction?
#  3. Do all tours have a trip?
#  4. Do all trips have a person and household?
#  5. Do all tours (except subtours) start and end at home?
#  6. Do mandatory (work and school) tours end at same zone as person work/school zone ids?
#  7. Do all tours have persons and households?
#  8. Do all persons have a household?
#  9. Do trip purposes match destination_choice_size_terms 
# 10. Do the joint tours match (tour, person, hh)

# %%
#  Do all trips still belong to a valid tour?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
asim_trip_df.loc[~asim_trip_df['tour_id'].isin(trimmed_asim_tour_df.tour_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed", count_before_removed_trips - count_after_removed_trips, "trips because their tour was removed")

# %%
#  Do all trips go to a valid zone?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
asim_trip_df.loc[(asim_trip_df['origin'] <= 0) | (asim_trip_df['destination'] <= 0), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed", count_before_removed_trips - count_after_removed_trips, "trips because of bad origin / destination")

# %%
#  Are there four or fewer trips per tour and direction?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

# do not remove primary tour destination trip or home trip
unallowed_trips = (
    (asim_trip_df['trip_num'] > 3)
    & (asim_trip_df['trip_num'] != asim_trip_df['trip_count'])
)

asim_trip_df.loc[unallowed_trips, 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips because their trip number is > 4")
print(f"Happens on {len(asim_trip_df.loc[unallowed_trips, 'tour_id'].unique())} tours")

# %%
#  Do all trips have a person & household?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

asim_trip_df.loc[~asim_trip_df['household_id'].isin(asim_hh_df.household_id) | ~asim_trip_df['person_id'].isin(asim_per_df.person_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips did not belong to a person or hh")

# %%
# ActivitySim only allows subtours on work tours -- removing all other subtours
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

# should remove the whole tour since these are tour-level variables
unallowed_trips = (
    (asim_trip_df['is_subtour'] == 1)
    & (asim_trip_df['tour_category'] != 'atwork')
)

asim_trip_df.loc[unallowed_trips, 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips because they are on non-atwork subtours")
print(f"Happens on {len(asim_trip_df.loc[unallowed_trips, 'tour_id'].unique())} tours")

# %% [markdown]
# ### Explore tours not starting at home

# %%
raw_hh_16['year'] = 2016
raw_hh_22['year'] = 2022

def get_xy_in_feet(df, lat_col='home_lat', lon_col='home_lng', prefix='home'):
    gdf = geopandas.GeoDataFrame(df, geometry=geopandas.points_from_xy(x=df[lon_col], y=df[lat_col]))
    # WGS84 to CA Stateplane 
    gdf.set_crs(epsg=4326, inplace=True, allow_override=True)
    gdf.to_crs(epsg=2230, inplace=True)
    df[prefix + '_x'] = gdf.geometry.x
    df[prefix + '_y'] = gdf.geometry.y
    return df

raw_hh_16 = get_xy_in_feet(raw_hh_16, lat_col='home_lat', lon_col='home_lng', prefix='home')
raw_hh_22 = get_xy_in_feet(raw_hh_22, lat_col='home_lat', lon_col='home_lon', prefix='home')
raw_hh = pd.concat([raw_hh_16[['hhid', 'year', 'home_x', 'home_y']].rename(columns={'hhid': 'hh_id'}), 
                    raw_hh_22[['hh_id', 'year', 'home_x', 'home_y']]])
raw_hh

# %%
### Function to calculate distance between two points
def calc_dist(x1, y1, x2, y2):
    distance = np.sqrt((x2 - x1)**2 + (y2 - y1)**2)
    return distance

# %%
### Calculate the mismatching 
# trips not starting at home
bad_starts = asim_trip_df[(asim_trip_df['trip_num'] == 1) & 
                          (asim_trip_df['outbound']) & 
                          (asim_trip_df['is_subtour'] == 0) & 
                          (asim_trip_df['home_zone_id'] != asim_trip_df['origin'])].copy()

# converting X and Y starts to California state plane CRS
bad_starts = get_xy_in_feet(bad_starts, lat_col='ORIG_Y', lon_col='ORIG_X', prefix='origin')

# merging in home x & y
bad_starts = pd.merge(bad_starts, 
                      raw_hh[['hh_id', 'year', 'home_x', 'home_y']], 
                      left_on=['HH_ID', 'survey_year'],
                      right_on=['hh_id', 'year'],
                      how='left',
                      suffixes=('', '_x'))

bad_starts['origin_home_dist'] = bad_starts.apply(lambda row: calc_dist(x1=row.origin_x, 
                                                                        y1=row.origin_y,
                                                                        x2=row.home_x,
                                                                        y2=row.home_y), axis=1)

bad_starts['acceptable_origin_home_dist'] = 0
bad_starts.loc[bad_starts['origin_home_dist']<=550, 'acceptable_origin_home_dist'] = 1

print(f"{len(bad_starts)} trips do not start at home, but {bad_starts['acceptable_origin_home_dist'].sum()} are within 550 feet")

### Merge the acceptable dist column to asim trips file
asim_trip_df.loc[bad_starts.index, 'acceptable_origin_home_dist'] = bad_starts['acceptable_origin_home_dist']
asim_trip_df['acceptable_origin_home_dist'].fillna(0, inplace=True)

# %%
###  Does first trip on tour start at home (or) the difference b/w mismatching MAZ is < 550 ft
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
bad_starts = asim_trip_df[(asim_trip_df['trip_num'] == 1) & 
                          (asim_trip_df['outbound']) & 
                          (asim_trip_df['is_subtour'] == 0) & 
                          (asim_trip_df['home_zone_id'] != asim_trip_df['origin']) & 
                          (asim_trip_df['acceptable_origin_home_dist'] != 1)]

asim_trip_df.loc[asim_trip_df['tour_id'].isin(bad_starts.tour_id), 'keep_trip'] = 0
# need to also change origin to home zone if within threshold
asim_trip_df.loc[asim_trip_df['acceptable_origin_home_dist'] == 1, 'origin'] = asim_trip_df.loc[asim_trip_df['acceptable_origin_home_dist'] == 1, 'home_zone_id']

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips that belong to tours that do not start at home")
print(f"Happens for {len(bad_starts)} tours")

# %% [markdown]
# ### Exploring tours not ending at home

# %%
bad_ends = asim_trip_df[
    (asim_trip_df['trip_num'] == asim_trip_df['trip_count']) 
    & (~asim_trip_df['outbound']) & (asim_trip_df['is_subtour'] == 0) 
    & ((asim_trip_df['home_zone_id'] != asim_trip_df['destination']) | (asim_trip_df['purpose'] != 'home'))
    ].copy()

# converting X and Y starts to California state plane CRS
bad_ends = get_xy_in_feet(bad_ends, lat_col='DEST_Y', lon_col='DEST_X', prefix='destination')

# merging home x & y
bad_ends = pd.merge(bad_ends, 
                    raw_hh[['hh_id', 'year', 'home_x', 'home_y']], 
                    left_on=['HH_ID', 'survey_year'],
                    right_on=['hh_id', 'year'],
                    how='left', 
                    suffixes=('', '_x'))

bad_ends['dest_home_dist'] = bad_ends.apply(lambda row: calc_dist(x1=row.destination_x, 
                                                                  y1=row.destination_y,
                                                                  x2=row.home_x,
                                                                  y2=row.home_y), axis=1)

bad_ends['acceptable_dest_home_dist'] = 0
bad_ends.loc[bad_ends['dest_home_dist']<=550, 'acceptable_dest_home_dist'] = 1

print(f"{len(bad_starts)} trips do not end at home, but {bad_starts['acceptable_origin_home_dist'].sum()} are within 550 feet")

### Merge the acceptable dist column to asim trips file
asim_trip_df.loc[bad_ends.index, 'acceptable_dest_home_dist'] = bad_ends['acceptable_dest_home_dist']
asim_trip_df['acceptable_dest_home_dist'].fillna(0, inplace=True)

# %%
###  Does last trip on tour end at home (or) the difference b/w mismatching MAZ is < 550 ft
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
bad_ends = asim_trip_df[
    (asim_trip_df['trip_num'] == asim_trip_df['trip_count']) 
    & (~asim_trip_df['outbound']) & (asim_trip_df['is_subtour'] == 0) 
    & ((asim_trip_df['home_zone_id'] != asim_trip_df['destination']) | (asim_trip_df['purpose'] != 'home'))
    & (asim_trip_df['acceptable_dest_home_dist'] != 1)
    ]

asim_trip_df.loc[asim_trip_df['tour_id'].isin(bad_ends.tour_id), 'keep_trip'] = 0
# need to also change destination to home zone if within threshold
asim_trip_df.loc[asim_trip_df['acceptable_dest_home_dist'] == 1, 'destination'] = asim_trip_df.loc[asim_trip_df['acceptable_dest_home_dist'] == 1, 'home_zone_id']
count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips that belong to tours that do not end at home")
print(f"Happens for {len(bad_ends)} tours")

# %%
# Do school and work tours go to the school or workplace zone?
# just changing primary destination instead of removing them.  Adding flag to filter if needed
asim_trip_df['mand_dest_changed'] = 0

bad_school_trips = (
    (asim_trip_df['purpose'] == 'school') 
    & (asim_trip_df['destination'] != asim_trip_df['school_zone_id'])
    & (asim_trip_df['school_zone_id'] > 0)
)
asim_trip_df.loc[bad_school_trips, 'destination'] = asim_trip_df.loc[bad_school_trips, 'school_zone_id']
asim_trip_df.loc[bad_school_trips, 'mand_dest_changed'] = 1
print(f"School trip does not go to school zone for {bad_school_trips.sum()} trips")

bad_work_trips = (
    (asim_trip_df['purpose'] == 'work') 
    & (asim_trip_df['destination'] != asim_trip_df['workplace_zone_id'])
    & (asim_trip_df['workplace_zone_id'] > 0)
)
asim_trip_df.loc[bad_work_trips, 'destination'] = asim_trip_df.loc[bad_work_trips, 'school_zone_id']
asim_trip_df.loc[bad_work_trips, 'mand_dest_changed'] = 1
print(f"Work trip does not go to workplace zone for {bad_work_trips.sum()} trips")

# %%
#  Atwork subtours need to start at the workplace location

bad_atwork_origins = (
    (asim_trip_df['is_subtour'] == 1)
    & (asim_trip_df['tour_category'] == 'atwork')
    & (asim_trip_df['trip_num'] == 1)
    & (asim_trip_df['outbound'] == True)
    & (asim_trip_df['origin'] != asim_trip_df['workplace_zone_id'])
)

asim_trip_df.loc[bad_atwork_origins, 'origin'] = asim_trip_df.loc[bad_atwork_origins, 'workplace_zone_id']

print("Changed origins for ", bad_atwork_origins.sum(), "trips on atwork subtours who didn't start at work")

# %%
#  Are there at least two trips in the tour?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

trip_counts = asim_trip_df.groupby(['tour_id'])['keep_trip'].sum()
tours_with_lt_2_trips = trip_counts[trip_counts < 2].index

asim_trip_df.loc[asim_trip_df['tour_id'].isin(tours_with_lt_2_trips), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips because they belonged to tours that didn't have at least 2 trips")
print(f"Happens for {len(tours_with_lt_2_trips.unique())} tours")


# %%
print("Total Number of trips kept after filtering:")
asim_trip_df['keep_trip'].value_counts()

# %%
print("Percentage of trip that are kept after filtering:")
asim_trip_df['keep_trip'].value_counts(normalize=True) * 100

# %% [markdown]
# ### Coding all trips to/from external TAZ 11 to external TAZ 12:
# Extenal TAZ 11 is a dummy external station in the northwest corner of the region and has no counts in the intenal-external counts file and therefore no size-term in ActivitySim.  External TAZ 12 is the north I-5 external station.  Because TAZ 11 is actually closer to LA than TAZ 11, almost all external trips/tours are being geo-coded to TAZ 11 instead of TAZ 12.  Adding logic here to change any origin or destination that has the dummy TAZ 11 to the I-5 external station TAZ 12.

# %%
taz_11_maz = landuse.loc[landuse.taz == 11, 'MAZ'].values[0]
taz_12_maz = landuse.loc[landuse.taz == 12, 'MAZ'].values[0]

num_trips_to_taz_11 = ((asim_trip_df.origin == taz_11_maz) | (asim_trip_df.destination == taz_11_maz)).sum()
num_tours_to_taz_11 = ((asim_tour_df.origin == taz_11_maz) | (asim_tour_df.destination == taz_11_maz)).sum()
print("number of tours to or from taz 11: ", num_tours_to_taz_11)
print("number of trips to or from taz 11: ", num_trips_to_taz_11)

asim_trip_df.loc[asim_trip_df.origin == taz_11_maz, 'origin'] = taz_12_maz
asim_trip_df.loc[asim_trip_df.destination == taz_11_maz, 'destination'] = taz_12_maz
asim_tour_df.loc[asim_tour_df.origin == taz_11_maz, 'origin'] = taz_12_maz
asim_tour_df.loc[asim_tour_df.destination == taz_11_maz, 'destination'] = taz_12_maz

# %% [markdown]
# ### Need to check tour table for consistency again
#  Do all tours have trips?

# %%
trimmed_asim_trip_df = asim_trip_df[asim_trip_df['keep_trip'] == 1]

print(f"Tours before trip cleaning: {len(trimmed_asim_tour_df)}")
# removing tours that have trips removed
trimmed_asim_tour_df = trimmed_asim_tour_df[trimmed_asim_tour_df['tour_id'].isin(trimmed_asim_trip_df.tour_id)]
# removing subtours if their parent tour was removed
trimmed_asim_tour_df = trimmed_asim_tour_df[trimmed_asim_tour_df['parent_tour_id'].isin(trimmed_asim_trip_df.tour_id) | (trimmed_asim_tour_df['is_subtour'] == 0)]
print(f"Tours after trip cleaning: {len(trimmed_asim_tour_df)}")

print("Joint tour participants before trip cleaning: ", len(trimmed_asim_jtour_participants_df))
trimmed_asim_jtour_participants_df = trimmed_asim_jtour_participants_df[trimmed_asim_jtour_participants_df['tour_id'].isin(trimmed_asim_trip_df['tour_id'])]
print("Joint tour participants after trip cleaning: ", len(trimmed_asim_jtour_participants_df))

print(f"Trips before tour re-cleaning: {len(trimmed_asim_trip_df)}")
trimmed_asim_trip_df = trimmed_asim_trip_df[trimmed_asim_trip_df['tour_id'].isin(trimmed_asim_tour_df.tour_id)]
print(f"Trips after tour re-cleaning: {len(trimmed_asim_trip_df)}")

# %%
# re-calculating trip variables after trimming:

# origins and destinations need to be consistent.
# in previous trip cleaning procedures, origins were kept consistent when filtering out extra stops
# but the destinations were changed for trips to mandatory tour locations
# sooo, resetting origin when the previous trip was mandatory and then making all destinations consistent with origin
trimmed_asim_trip_df['prev_trip_purpose'] = trimmed_asim_trip_df.groupby('tour_id')['purpose'].shift(1)
trimmed_asim_trip_df['prev_trip_dest'] = trimmed_asim_trip_df.groupby('tour_id')['destination'].shift(1)
prev_mand_trips = trimmed_asim_trip_df['prev_trip_purpose'].isin(['work', 'school', 'univ'])
trimmed_asim_trip_df.loc[prev_mand_trips, 'origin'] = trimmed_asim_trip_df.loc[prev_mand_trips, 'prev_trip_dest']

# destination is the next trips origin
new_dest = trimmed_asim_trip_df.groupby('tour_id')['origin'].shift(-1)
new_dest = new_dest[~new_dest.isna()] # ignoring trips to home

# re-numbering and counting trips
grouped = trimmed_asim_trip_df.groupby(["tour_id", "outbound"])
trimmed_asim_trip_df["trip_num"] = grouped.cumcount() + 1
trimmed_asim_trip_df["trip_count"] = trimmed_asim_trip_df["trip_num"] + grouped.cumcount(ascending=False)
trimmed_asim_trip_df['trip_id'] = trimmed_asim_trip_df.reset_index().index + 1

# %%
trimmed_asim_trip_df[trimmed_asim_trip_df.tour_id == 4727]

# %%
trimmed_asim_trip_df[trimmed_asim_trip_df.tour_id == 4725]

# %% [markdown]
# ## Adding Additional Required Columns

# %%
# FIXME: making these up right now!
asim_per_df['educ'] = np.where(asim_per_df.age >= 18, 9, 0)
asim_per_df['educ'] = np.where(asim_per_df.age >= 22, 13, asim_per_df.educ)
# asim_hh_df['school_escorting_outbound'] = 1  # no escorting
# asim_hh_df['school_escorting_inbound'] = 1  # no escorting
# asim_hh_df['school_escorting_outbound_cond'] = asim_hh_df['school_escorting_outbound']  # only one real decision
# asim_hh_df['bldgsz'] = np.where(asim_hh_df.HHT.isin([1,2,3]), 2, -1) # detacted single family home

# %% [markdown]
# ## Writing Output and Running Infer.py

# %%
hh_output_cols = [
    'household_id',
    'home_zone_id',
    'income',
    'hhsize',
    'HHT',
    'auto_ownership',
    'num_workers',
    'children',
    'day',
    'bldgsz',
    'res_type',
    'transponder_ownership',
    'survey_year',
    'day',
    'HH_ID',
]
output_asim_hh_df = asim_hh_df[hh_output_cols].copy()
per_output_cols = [
    'person_id',
    'household_id',
    'PNUM',
    'age',
    'sex',
    'pemploy',
    'pstudent',
    'is_student',
    'ptype',
    'school_zone_id',
    'workplace_zone_id',
    'free_parking_at_work',
    'work_from_home',
    'telecommute_frequency',
    'day',
    'educ',
    'external_worker_identification',
    'external_workplace_zone_id',
    'transit_pass_subsidy',
    'transit_pass_ownership',
    'industry',
]
output_asim_per_df = asim_per_df[per_output_cols].copy()
tour_output_cols = [
    'tour_id',
    'person_id',
    'household_id',
    'tour_type',
    'tour_category',
    'origin',
    'destination',
    'start',
    'end',
    'tour_mode',
    'parent_tour_id',
    'day',
    'external_joint_tour_identification',
    'external_non_mandatory_identification',
    'out_escort_type', # escort type of the person being escorted
    'inb_escort_type',
    'out_chauf_person_id',
    'inb_chauf_person_id',
]
output_asim_tour_df = trimmed_asim_tour_df[tour_output_cols].copy()
jtour_output_cols = [
    'participant_id',
    'tour_id',
    'household_id',
    'person_id',
    'participant_num',
    'day'
]
output_asim_jtour_df = trimmed_asim_jtour_participants_df[jtour_output_cols].copy()

trip_output_cols = [
    'household_id',
    'tour_id',
    'person_id',
    'trip_id',
    'day',
    'origin',
    'destination',
    'depart',
    'primary_purpose',
    'purpose',
    'trip_mode',
    'trip_num',
    'outbound',
    'trip_count'
]
output_asim_trip_df = trimmed_asim_trip_df[trip_output_cols].copy()

# %%
output_asim_hh_df.to_csv(os.path.join(final_output_path, "survey_households.csv"), index=False)
output_asim_per_df.to_csv(os.path.join(final_output_path, "survey_persons.csv"), index=False)
output_asim_tour_df.to_csv(os.path.join(final_output_path, "survey_tours.csv"), index=False)
output_asim_jtour_df.to_csv(os.path.join(final_output_path, "survey_joint_tour_participants.csv"), index=False)
output_asim_trip_df.to_csv(os.path.join(final_output_path, "survey_trips.csv"), index=False)

# %%
cur_dir = os.getcwd()
os.chdir(estimation_path)
infer_py_location = r"{dir}\scripts\infer.py".format(dir=estimation_path)
infer_run_command = "python " + infer_py_location + " " + estimation_path + " " + configs_dir
infer_result = os.system(infer_run_command)
if infer_result == 0:
    print("infer script successfully completed")
else:
    print("Error in infer script!")
os.chdir(cur_dir)

# %% [markdown]
# ## Creating External Targets Table

# %%
# If a tour leaves and enters the region through multiple external stations, assign it the most crossed station
# If they are equal, it takes the first instance

### Identifying most frequented external station for non-mandatory (or) joint II-Ext(internal_dest) tours
dest_change_tour_df = asim_tour_df[(asim_tour_df['keep_tour']==1)&
                                   (asim_tour_df['external_type']=='II-Ext(internal_dest)')&
                                   (asim_tour_df['tour_category'].isin(['non_mandatory', 'joint']))]

dest_change_trip_df = asim_trip_df[asim_trip_df['tour_id'].isin(dest_change_tour_df['tour_id'])&
                                   asim_trip_df['destination'].isin(ext_gdf['MAZ'])]

most_freq_dest_df = dest_change_trip_df.groupby(['tour_id'])['destination'].agg(lambda x: pd.Series.mode(x)[0]).to_frame()

### Updating the destination for non-mandatory II-Ext(internal_dest) tours
asim_tour_df.set_index('tour_id', inplace=True)
asim_tour_df.loc[most_freq_dest_df.index, 'destination'] = most_freq_dest_df['destination']
asim_tour_df.reset_index(inplace=True)

### Summary table of external type and tour category
asim_tour_df[asim_tour_df['keep_tour']==1].groupby(['external_type', 'tour_category']).size().reset_index().rename(columns={0: 'count'})
## Creating targets for external station
### Filter out invalid tours and tours that are fully internal or starting externally
target_tours_df = asim_tour_df[(asim_tour_df['keep_tour']==1)&(~asim_tour_df['external_type'].isin(['II']))].copy()

### Create a summary table counting tours at each external station by tour_category
target_tours_df['tour_category'].replace({'joint': 'non_mandatory'}, inplace=True)
external_targets = pd.pivot_table(target_tours_df, 
                                  columns='tour_category', 
                                  index='destination', 
                                  values='tour_id', 
                                  aggfunc='count', 
                                  margins=True, 
                                  margins_name='Total', 
                                  fill_value=0).reset_index().rename(columns={'destination': 'external_station'})

### Add TAZ number and sort the targets by it
external_targets['external_station'] = external_targets['external_station'].replace(ext_gdf.set_index('MAZ')['TAZ'])
external_targets = external_targets[external_targets['external_station'].isin(ext_gdf['TAZ'])]
external_targets.sort_values('external_station', inplace=True)

external_targets.loc[len(external_targets.index)] = ['Total', 
                                                     external_targets['mandatory'].sum(), 
                                                     external_targets['non_mandatory'].sum(), 
                                                     external_targets['Total'].sum()]

external_targets.set_index('external_station', inplace=True)
external_targets

# %%
# external_targets.to_csv(os.path.join(final_output_path, "external_station_targets.csv"), index=True)

# %% [markdown]
# #### Code to make cut and create set of zones to test estimation mode in a cropped zone system

# %%
or_households = pd.read_csv(os.path.join(final_output_path, "override_households.csv"))
or_persons = pd.read_csv(os.path.join(final_output_path, "override_persons.csv"))
or_tours = pd.read_csv(os.path.join(final_output_path, "override_tours.csv"))
or_jtp = pd.read_csv(os.path.join(final_output_path, "override_joint_tour_participants.csv"))
or_trips = pd.read_csv(os.path.join(final_output_path, "override_trips.csv"))

# %%
def return_relevant_zones(household_ids, crop_output_path=None):
    zones = []
    cut_households = or_households[or_households.household_id.isin(household_ids)]
    # home zone id's
    zones.append(cut_households.home_zone_id.unique())

    # person school and work locations
    cut_persons = or_persons[or_persons.household_id.isin(household_ids)]
    zones.append(cut_persons.school_zone_id.unique())
    zones.append(cut_persons.workplace_zone_id.unique())

    # tour origins and destinations
    cut_tours = or_tours[or_tours.household_id.isin(household_ids)]
    zones.append(cut_tours.origin.unique())
    zones.append(cut_tours.destination.unique())

    # trip orogins and destinations
    cut_trips = or_trips[or_trips.household_id.isin(household_ids)]
    zones.append(cut_trips.origin.unique())
    zones.append(cut_trips.destination.unique())

    zones = np.unique(np.concatenate(zones))
    zones = zones[zones >= 0].astype(int)
    print(f"Set of households has {len(zones)} relevant zones.")

    if crop_output_path:
        cut_jtp = or_jtp[or_jtp.household_id.isin(household_ids)]

        cut_households.to_csv(os.path.join(crop_output_path, 'override_households.csv'))
        cut_persons.to_csv(os.path.join(crop_output_path, 'override_persons.csv'))
        cut_tours.to_csv(os.path.join(crop_output_path, 'override_tours.csv'))
        cut_trips.to_csv(os.path.join(crop_output_path, 'override_trips.csv'))
        cut_jtp.to_csv(os.path.join(crop_output_path, 'override_joint_tour_participants.csv'))

    return zones

# failing_in_mtf = [622, 4233, 6192, 6821, 7928, 7968, 13020, 17427, 22830, 41427]
# failing_in_mtf_zones = return_relevant_zones(failing_in_mtf)

se_households = or_households[(or_households.school_escorting_inbound > 1) | (or_households.school_escorting_inbound > 1)].household_id.values[:40]
crop_output_path = r'C:\ABM3_dev\run_data\data_2z_series15_crop_debug'
se_zones = return_relevant_zones(se_households, crop_output_path)

# output_zones = pd.DataFrame(np.unique(np.concatenate([failing_in_mtf_zones, se_zones])))
output_zones = pd.DataFrame(np.unique(se_zones))
output_zones.to_csv(os.path.join(final_output_path, 'se_zones.csv'), index=False, header=['MGRA'])

# %%
crop_output_path = r'C:\ABM3_dev\run_data\data_2z_series15_crop_debug'
cut_households = pd.read_csv(os.path.join(crop_output_path, 'override_households.csv'))
cut_persons = pd.read_csv(os.path.join(crop_output_path, 'override_persons.csv'))
cut_tours = pd.read_csv(os.path.join(crop_output_path, 'override_tours.csv'))
cut_trips = pd.read_csv(os.path.join(crop_output_path, 'override_trips.csv'))
cut_jtp = pd.read_csv(os.path.join(crop_output_path, 'override_joint_tour_participants.csv'))
cut_landuse = pd.read_csv(os.path.join(crop_output_path, 'land_use.csv'))
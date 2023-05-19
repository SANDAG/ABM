import pandas as pd
pd.set_option("display.max_columns",250)
import os
import numpy as np
import geopandas
import matplotlib.pyplot as plt

# ## Read in SPA Input and Output Tables
# SPA input is needed in order to merge information from survey that was not written by the SPA tool

configs_dir = r"T:\Survey\HHTS\estimation\configs"

landuse_file = r"T:\Survey\HHTS\estimation\data\2019\mgra15_based_input2019_rev.csv"
landuse = pd.read_csv(landuse_file)

# contains shape file
data_dir = r"T:\Survey\HHTS\estimation\data\series15\mgra15"

estimation_path = r"T:\Survey\HHTS\estimation\asim_combined"
final_output_path = r"{dir}\survey_data".format(dir=estimation_path)

# reading in 2016 survey
data_16_folder = r"T:\Survey\HHTS\estimation\2016"
# reading in raw survey to be able to geocode home, school, and work locations
raw_16_folder = r"T:\Survey\HHTS\estimation\2016\Raw"

# reading in 2022 survey
data_22_folder = r"T:\Survey\HHTS\estimation\2022"
# reading in raw survey to be able to geocode home, school, and work locations
raw_22_folder = r"T:\Survey\HHTS\estimation\2022\Raw"

# number of periods in activitysim (48 30-minute periods)
num_periods = 48

mgra15 = geopandas.read_file(os.path.join(data_dir, 'mgra15.shp'))
mgra15 = mgra15.to_crs(epsg=4326)

trips_16 = {}
tours_16 = {}
jtours_16 = {}
persons_16 = {}
households_16 = {}

for day in range(1,8):
    print("day", day)
    data_folder = os.path.join(data_16_folder, "SPA_Processed", f"day{day}")
    
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

spa_out_hh_df = pd.concat([households_16, households_22]).reset_index(drop=True)
spa_out_per_df = pd.concat([persons_16, persons_22]).reset_index(drop=True)
spa_out_tours_df = pd.concat([tours_16, tours_22]).reset_index(drop=True)
spa_out_ujtours_df = pd.concat([jtours_16, jtours_22]).reset_index(drop=True)
spa_out_trips_df = pd.concat([trips_16, trips_22]).reset_index(drop=True)

# ## Geocoding
def geocode_to_mgra15(df, cols_to_keep, x_col, y_col):
    df_geocode = geopandas.GeoDataFrame(df[cols_to_keep], geometry=geopandas.points_from_xy(x=df[x_col], y=df[y_col]))
    df_geocode.set_crs(epsg=4326, inplace=True)
    df_geocode = geopandas.sjoin(mgra15[['MGRA', 'TAZ', 'geometry']], df_geocode, how='right', predicate='contains')
    assert (df_geocode.index == df.index).all(), "Bad Merge!"
    return df_geocode

# #### home zone id
hh_22_geocode = geocode_to_mgra15(raw_hh_22.set_index('hh_id'), cols_to_keep=['hh_weight'], x_col='home_lon', y_col='home_lat')
hh_16_geocode = geocode_to_mgra15(raw_hh_16.set_index('hhid'), cols_to_keep=['hh_final_weight_456x'], x_col='home_lng', y_col='home_lat')

hhid_to_home_zone_dict = hh_22_geocode['MGRA'].to_dict()
hhid_to_home_zone_dict.update(hh_16_geocode['MGRA'].to_dict())

assert spa_out_hh_df.HH_ID.isin(hhid_to_home_zone_dict.keys()).all()
spa_out_hh_df['home_zone_id'] = spa_out_hh_df['HH_ID'].map(hhid_to_home_zone_dict)

# #### school and work zone ID's
sch_22_geocode = geocode_to_mgra15(raw_person_22, cols_to_keep=['hh_id', 'person_num'], x_col='school_lon', y_col='school_lat')
sch_22_geocode.rename(columns={'MGRA': 'school_zone_id', 'hh_id': 'HH_ID', 'person_num': 'PER_ID'}, inplace=True)
sch_16_geocode = geocode_to_mgra15(raw_person_16, cols_to_keep=['hhid', 'pernum'], x_col='mainschool_lng', y_col='mainschool_lat')
sch_16_geocode.rename(columns={'MGRA': 'school_zone_id', 'hhid': 'HH_ID', 'pernum': 'PER_ID'}, inplace=True)
sch_geocode = pd.concat([sch_22_geocode, sch_16_geocode])

work_22_geocode = geocode_to_mgra15(raw_person_22, cols_to_keep=['hh_id', 'person_num'], x_col='work_lon', y_col='work_lat')
work_22_geocode.rename(columns={'MGRA': 'work_zone_id', 'hh_id': 'HH_ID', 'person_num': 'PER_ID'}, inplace=True)
work_16_geocode = geocode_to_mgra15(raw_person_16, cols_to_keep=['hhid', 'pernum'], x_col='work_lng', y_col='work_lat')
work_16_geocode.rename(columns={'MGRA': 'work_zone_id', 'hhid': 'HH_ID', 'pernum': 'PER_ID'}, inplace=True)
work_geocode = pd.concat([work_22_geocode, work_16_geocode])

school_work_geocode = pd.concat([sch_geocode[['HH_ID', 'PER_ID', 'school_zone_id']], work_geocode[['work_zone_id']]], axis=1)
school_work_geocode

spa_out_per_df = spa_out_per_df.merge(school_work_geocode, how='left', on=['HH_ID', 'PER_ID'], suffixes=('', ''))

# Tours
for end in ['ORIG', 'DEST']:
    tour_geocode_df = geopandas.GeoDataFrame(spa_out_tours_df[['HH_ID', 'PER_ID', 'TOUR_ID']], geometry=geopandas.points_from_xy(x=spa_out_tours_df[end + '_X'], y=spa_out_tours_df[end + '_Y']))
    tour_geocode_df.set_crs(epsg=4326, inplace=True)
    tour_geocode_df = geopandas.sjoin(mgra15[['MGRA', 'TAZ', 'geometry']], tour_geocode_df, how='right', predicate='contains')
    assert (tour_geocode_df.index == spa_out_tours_df.index).all(), "Bad Merge!"
    spa_out_tours_df[end + '_MAZ'] = tour_geocode_df['MGRA']
    spa_out_tours_df[end + '_TAZ'] = tour_geocode_df['TAZ']

# Trips
for end in ['ORIG', 'DEST']:
    trip_geocode_df = geopandas.GeoDataFrame(spa_out_trips_df[['HH_ID', 'PER_ID', 'TOUR_ID', 'TRIP_ID']], geometry=geopandas.points_from_xy(x=spa_out_trips_df[end + '_X'], y=spa_out_trips_df[end + '_Y']))
    trip_geocode_df.set_crs(epsg=4326, inplace=True)
    trip_geocode_df = geopandas.sjoin(mgra15[['MGRA', 'TAZ', 'geometry']], trip_geocode_df, how='right', predicate='contains')
    assert (tour_geocode_df.index == spa_out_tours_df.index).all(), "Bad Merge!"
    spa_out_trips_df[end + '_MAZ'] = trip_geocode_df['MGRA']
    spa_out_trips_df[end + '_TAZ'] = trip_geocode_df['TAZ']

# FIXME need to geocode external tours/trips

def reindex(series1, series2):
    result = series1.reindex(series2)
    try:
        result.index = series2.index
    except AttributeError:
        pass
    return result

# ## Processing Household File

# The spa output household file just selects SAMPN and HH_SIZE and renames to HH_ID and NUM_PERS.  AREA is not used.  So, processing this file requires just changing the variables for the input hh file.

# FIXME: are the income categories the same for the 2016 survey??
hh_inc_cat_dict = {
    1: [0,14999],
    2: [15000,24999],
    3: [25000,29999],
    4: [30000,34999],
    5: [35000,49999],
    6: [50000,59999],
    7: [60000,74999],
    8: [75000,99999],
    9: [100000,149999],
    10: [150000,250000],
    11: [0,29999],
    12: [30000,59999],
    13: [60000,99999],
    14: [100000,149999],
    15: [150000,250000],
    999: [80000, 85000], # median income in san diego is about $83,000
}

def interpolate_hh_income(inc_cat):
    if inc_cat < 0:
        return pd.NA
    else:
        return np.random.randint(hh_inc_cat_dict[inc_cat][0], hh_inc_cat_dict[inc_cat][1])

asim_hh_df = pd.DataFrame()

asim_hh_df['HH_ID'] = spa_out_hh_df['HH_ID']
asim_hh_df['home_zone_id'] = spa_out_hh_df['home_zone_id']
asim_hh_df['survey_year'] = spa_out_hh_df['survey_year']
asim_hh_df['hhsize'] = spa_out_per_df[['HH_ID', 'PER_ID']].drop_duplicates().groupby('HH_ID')['PER_ID'].count().reindex(asim_hh_df.HH_ID, fill_value=0).values
asim_hh_df['day'] = spa_out_hh_df['day']
asim_hh_df['num_workers'] = spa_out_per_df[spa_out_per_df['PERSONTYPE'].isin([1,2])][['HH_ID', 'PER_ID']].drop_duplicates().groupby('HH_ID')['PER_ID'].count().reindex(asim_hh_df.HH_ID, fill_value=0).values
asim_hh_df['auto_ownership'] = pd.concat([raw_hh_22.set_index('hh_id')['num_vehicles'], raw_hh_16.set_index('hhid')['vehicle_count']]).reindex(asim_hh_df.HH_ID).values
asim_hh_df['HH_INC_CAT'] = pd.concat([raw_hh_22.set_index('hh_id')['income_detailed'], raw_hh_16.set_index('hhid')['hhincome_imputed']]).reindex(asim_hh_df.HH_ID).fillna(999).values

assert ~(asim_hh_df[['num_workers', 'auto_ownership', 'HH_INC_CAT']].isna()).any().any()

# FIXME: need to set actual household type
asim_hh_df['HHT'] = 1

# Linear interpolation of income categories, and sample from distribution for missing income values
asim_hh_df['income'] = asim_hh_df['HH_INC_CAT'].apply(lambda inc_cat: interpolate_hh_income(inc_cat))
sampled_incomes = asim_hh_df.loc[pd.notna(asim_hh_df['income']), 'income'].sample(
        n=len(asim_hh_df[pd.isna(asim_hh_df['income'])]),
        replace=True)
asim_hh_df.loc[pd.isna(asim_hh_df['income']), 'income'] = sampled_incomes.values

# converting 2016 dollars to 2022
# $1 in 2016 is worth $1.25 in 2022 (https://www.bls.gov/data/inflation_calculator.htm)
asim_hh_df.loc[asim_hh_df['survey_year'] == 2016, 'income'] = asim_hh_df.loc[asim_hh_df['survey_year'] == 2016, 'income'] * 1.25

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
    11: [85, 90],
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
    14: [85, 90],
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

asim_per_df = pd.DataFrame()

keep_cols = ['HH_ID', 'PER_ID', 'day', 'survey_year']

asim_per_df[keep_cols] = spa_out_per_df[keep_cols]
asim_per_df['ptype'] = spa_out_per_df['PERSONTYPE']
asim_per_df['pstudent'] = spa_out_per_df['STU_CAT']
asim_per_df['pemploy'] = spa_out_per_df['EMP_CAT']
asim_per_df['AGE_CAT'] = spa_out_per_df['AGE_CAT'].where(~spa_out_per_df['AGE_CAT'].isna(), spa_out_per_df['AGE'], axis=0).fillna(999)
asim_per_df['age'] = asim_per_df.apply(lambda row: interpolate_age(row), axis=1)
asim_per_df['PNUM'] = spa_out_per_df['PER_ID']
# FIXME: need to grab gender
# asim_per_df['sex'] = spa_out_per_df['PER_GENDER']  # 1: male, 2: female for both survey and PUMS
asim_per_df['sex'] = 1


# looks like some persontypes weren't coded correctly if age is missing
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 1), 'ptype'] = 7  # school, assumed non-driving age
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 2), 'ptype'] = 3  # univ student
asim_per_df.loc[asim_per_df['ptype'].isna() & (asim_per_df['pstudent'] == 3) & (asim_per_df['pemploy'] == 4), 'ptype'] = 4  # non-worker
asim_per_df['ptype'] = asim_per_df['ptype'].astype(int)

asim_per_df['school_zone_id'] = spa_out_per_df['school_zone_id']
asim_per_df['workplace_zone_id'] = spa_out_per_df['work_zone_id']
# FIXME: need to grab work from home category
asim_per_df['work_from_home'] = False

# FIXME: do we have free parking at work data?
asim_per_df['free_parking_at_work'] = 'FALSE'

spa_TELECOMM_FREQ_dict = {
    0: 'No_Telecommute', 
    1: '1_day_week', 
    2: '2_3_days_week', 
    3: '2_3_days_week', 
    4: '4_days_week', 
    5: '4_days_week', 
    6: '4_days_week', 
    7: '4_days_week', 
    -9: 'No_Telecommute', 
}
#FIXME: Need to merge in telecommute frequency
# asim_per_df['telecommute_frequency'] = spa_input_per_df['TELECOMM_FREQ'].apply(lambda x: spa_TELECOMM_FREQ_dict[x])
asim_per_df['telecommute_frequency'] = 'No_Telecommute'

# ## Processing Tours

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

# FIXME: transit modes need to be converted to 2022 modes using trip mode info
tour_mode_spa_to_asim_dict_16 = {
    1: 'SOV',
    2: 'HOV2',
    3: 'HOV3',
    4: 'WALK',
    5: 'BIKE',
    6: 'WALK_LOC',
    7: 'PNR_LOC',
    8: 'KNR_LOC',
    9: 'SCH_BUS',
    10: 'TAXI',
    11: 'SOV', # other to SOV
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


spa_out_tours_df[spa_out_tours_df['IS_SUBTOUR'] == 1]['TOURPURP'].value_counts(dropna=False)

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

asim_tour_df = spa_out_tours_df[['HH_ID', 'PER_ID','TOUR_ID', 'PARENT_TOUR_ID', 'JTOUR_ID', 'day']].copy()

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
    asim_tour_df['survey_year'] == 2016, 'TOURMODE'].map(tour_mode_spa_to_asim_dict_16)
asim_tour_df.loc[asim_tour_df['survey_year'] == 2022, 'tour_mode'] = spa_out_tours_df.loc[
    asim_tour_df['survey_year'] == 2022, 'TOURMODE'].map(tour_mode_spa_to_asim_dict_22)

asim_tour_df['tour_purpose'] = spa_out_tours_df['TOURPURP'].map(tour_purpose_spa_to_asim_dict)
asim_tour_df.loc[asim_tour_df['is_subtour'] == 1, 'parent_tour_purpose'] = spa_out_tours_df.loc[
    asim_tour_df['is_subtour'] == 1, 'PARENT_TOURPURP'].map(tour_purpose_spa_to_asim_dict)

assert (~asim_tour_df['tour_mode'].isna()).all(), "Missing tour modes!"
assert (~asim_tour_df['tour_purpose'].isna()).all(), "Missing tour purpose!"


spa_out_tours_df.loc[spa_out_tours_df['survey_year'] == 2016, 'TOURMODE'].value_counts(dropna=False)

def determine_tour_category(row):
    if row['tour_purpose'] in ['work', 'univ', 'school']:
        return 'mandatory'
    elif row['is_joint'] == 1:
        return 'joint'
    elif (row['is_subtour'] == 1) & (row['parent_tour_purpose'] == 'work'):
        return 'atwork'
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

asim_tour_df[asim_tour_df['tour_category'] == 'atwork']['tour_purpose'].value_counts(normalize=True)

asim_tour_df[asim_tour_df['tour_category'] == 'atwork']['tour_type'].value_counts(normalize=True)

asim_tour_df['duration'].hist(bins=np.linspace(0,48,48))

asim_tour_df[asim_tour_df['end'] < 0]

# ### Joint Tour Participants
asim_jtour_participants_df = pd.melt(spa_out_ujtours_df,
       id_vars=['HH_ID', 'JTOUR_ID', 'day'],
       value_vars=['PERSON_1','PERSON_2','PERSON_3','PERSON_4','PERSON_5','PERSON_6','PERSON_7','PERSON_8','PERSON_9'])

asim_jtour_participants_df = asim_jtour_participants_df[pd.notna(asim_jtour_participants_df['value'])]
asim_jtour_participants_df['participant_num'] = asim_jtour_participants_df['variable'].apply(lambda x: int(x.strip('PERSON_')))
asim_jtour_participants_df['PER_ID'] = asim_jtour_participants_df['value'].astype(int)

# ## Re-Indexing
# Need unique household_id, per_id, tour_id, etc. for ActivitySim
# 
# #### Household
# household ID should be unique already, but we want to be sure
asim_hh_df['household_id'] = asim_hh_df.reset_index().index + 1

# #### Person
asim_per_df = pd.merge(
    asim_per_df,
    asim_hh_df[['HH_ID', 'household_id', 'day']],
    how='left',
    on=['HH_ID', 'day'],
)
asim_per_df['person_id'] = asim_per_df.reset_index().index + 1

# need to re-number PNUM
# not every person is listed for every day in spa output.  See HH_ID == 22008078 for an example.
# There is a cut in infer looking for PNUM == 1, which every household needs.
asim_per_df['PNUM'] = asim_per_df.groupby('household_id')['person_id'].cumcount() + 1

# Determining number of children in each household
hh_children = asim_per_df[asim_per_df['age'] < 18].groupby('household_id')['person_id'].count().to_frame()
hh_children.columns = ['children']

asim_hh_df.set_index('household_id', inplace=True)
asim_hh_df.loc[hh_children.index, 'children'] = hh_children['children']
asim_hh_df['children'] = asim_hh_df['children'].fillna(0).astype(int)
asim_hh_df.reset_index(inplace=True)

# #### Tour
asim_tour_df = pd.merge(
    asim_tour_df,
    asim_per_df[['day', 'HH_ID', 'PER_ID', 'household_id', 'person_id']],
    how='left',
    on=['HH_ID', 'PER_ID', 'day']
)

# Joint tours are replicated in SPA output accross all members of the tour.  asim_tour_df will keep just the first instance of joint tours.  Members of the joint tours are tracked in the asim_jtour_participants_df table.
# 
# Not removing duplicated joint tours will cause different tour_id's to be assigned to the same joint tour.

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

# do not allow subtours that are not joint from joint tours
asim_tour_df = asim_tour_df[~(asim_tour_df['PARENT_TOUR_ID'].notna() & asim_tour_df['parent_tour_id'].isna())]

# #### Joint Tour
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

asim_jtour_participants_df.sort_values(by=['household_id', 'tour_id', 'participant_num'], inplace=True)
asim_jtour_participants_df['participant_id'] = asim_jtour_participants_df.reset_index().index + 1

all(asim_jtour_participants_df['tour_id'].isin(asim_tour_df['tour_id']))

# ## Additional changes to make infer.py work

# #### "univ" is not a trip option
# converting all univ trips to school

num_univ_tours = len(asim_tour_df[asim_tour_df['tour_type'] == 'univ'])
print("Number of univ tours: ", num_univ_tours)

asim_tour_df.loc[asim_tour_df['tour_type'] == 'univ', 'tour_type'] = "school"

# #### No joint escorting mode
# error: Unable to parse string "j_escort1"
# 
# solution: recategorizing joint escort tours to non_mandatory escort tours
asim_tour_df[asim_tour_df['tour_type'] == 'escort']['tour_category'].value_counts()
asim_tour_df.loc[asim_tour_df['tour_type'] == 'escort', 'tour_category'] = 'non_mandatory'
asim_tour_df[asim_tour_df['tour_type'] == 'escort']['tour_category'].value_counts()

# ## Removing tours
# #### Tours with invalid start or end MAZ's are removed
orig_num_tours = len(asim_tour_df)
asim_tour_df = asim_tour_df[asim_tour_df['origin'].isin(landuse.mgra)]
asim_tour_df = asim_tour_df[asim_tour_df['destination'].isin(landuse.mgra)]
valid_maz_tours = len(asim_tour_df)
print("Removed", orig_num_tours - valid_maz_tours, "tours due to invalid tour start or end maz")
print(valid_maz_tours, " tours remain")

# removing these tours from the joint_tour_participants file
asim_jtour_participants_df = asim_jtour_participants_df[asim_jtour_participants_df['tour_id'].isin(asim_tour_df['tour_id'])]

asim_jtour_participants_df['tot_num_participants'] = asim_jtour_participants_df.groupby('tour_id')['participant_num'].transform('max')
asim_jtour_participants_df['tot_num_participants'].value_counts(dropna=False)

# ### Using configs files to determine allowed tour frequencies
# For example, each person can only have 2 mandatory tours
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

asim_tour_df[asim_tour_df['tour_category'] == 'atwork']['tour_type'].value_counts()

# - checking joint tour frequency
# FIXME need to modify to match new joint_tour_frequency_composition model
joint_tour_types = list(joint_tour_freq_alts_df.columns.drop('alt'))
asim_tour_df = count_tours_by_category(
    df=asim_tour_df,
    category='joint',
    count_by='household_id',
    tour_types=joint_tour_types
)

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

asim_tour_df['keep_tour'] = 1
original_num_tours = len(asim_tour_df)

asim_tour_df.loc[(asim_tour_df['tour_category'] == 'mandatory') & pd.isna(asim_tour_df['mandatory_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'non_mandatory') & pd.isna(asim_tour_df['non_mandatory_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'atwork') & pd.isna(asim_tour_df['atwork_alt']), 'keep_tour'] = 0
asim_tour_df.loc[(asim_tour_df['tour_category'] == 'joint') & pd.isna(asim_tour_df['joint_alt']), 'keep_tour'] = 0

after_removed_tours = len(asim_tour_df[asim_tour_df['keep_tour'] == 1])
print("Removed ", original_num_tours - after_removed_tours, "tours that did not match in the tour frequency configs files")

# #### Tour Start and End times must be acceptable
# Checking the tour_departure_and_duration_alternatives.csv configs file for allowable times
tdd_df = pd.read_csv(os.path.join(configs_dir, "tour_departure_and_duration_alternatives.csv"))
min_start_allowed = tdd_df['start'].min()
max_start_allowed = tdd_df['start'].max()
min_end_allowed = tdd_df['end'].min()
max_end_allowed = tdd_df['end'].max()

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

# ### Reassigning tours from persons that make work or school trip but have invalid work or school MAZ
asim_per_df.loc[(asim_per_df['school_zone_id'].isin([0,-9999])) | (asim_per_df['school_zone_id'].isna()), 'school_zone_id'] = -1
asim_per_df.loc[(asim_per_df['workplace_zone_id'].isin([0,-9999])) | (asim_per_df['workplace_zone_id'].isna()), 'workplace_zone_id'] = -1

univ_students = (asim_per_df['pstudent'] == 2)
gradeschool_students = ((asim_per_df['pstudent'] == 1) & (asim_per_df['age'] < 14))
highschool_students = ((asim_per_df['pstudent'] == 1) & (asim_per_df['age'] >= 14))

landuse['tot_college_enroll'] = landuse['collegeenroll'] + landuse['othercollegeenroll'] + landuse['adultschenrl']
univ_mazs = landuse[landuse['tot_college_enroll'] > 0]['mgra']
k_8_mazs = landuse[landuse['enrollgradekto8'] > 0]['mgra']
G9_12_mazs = landuse[landuse['enrollgrade9to12'] > 0]['mgra']
print(len(univ_mazs), 'MAZs with university enrollment')
print(len(k_8_mazs), 'MAZs with K-8 enrollment')
print(len(G9_12_mazs), 'MAZs with 9-12 enrollment')

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

bad_school_co = school_co[school_co['has_landuse_maz'] == 0]
bad_school_co_by_maz = bad_school_co.groupby(['school_zone_id', 'school_segment_named']).count().reset_index()
bad_school_co_by_maz = bad_school_co_by_maz.pivot_table(index='school_zone_id', columns='school_segment_named', values='person_id')
bad_school_co_by_maz = bad_school_co_by_maz.reset_index()
bad_school_co_by_maz = bad_school_co_by_maz.rename(columns={'school_zone_id': 'MAZ'}).fillna(0)
bad_school_co_by_maz = bad_school_co_by_maz[bad_school_co_by_maz['MAZ'] > 0]
bad_school_co_by_maz

maz_shp_landuse = pd.merge(mgra15, landuse, how='left', left_on='MGRA', right_on='mgra')
# maz_shp_landuse = geopandas.GeoDataFrame(maz_shp_landuse, geometry='geometry', crs=mgra15.crs)
maz_shp_landuse = maz_shp_landuse.to_crs(epsg=2230)  # CA state plane 6 (feet)

bad_school_shp = pd.merge(bad_school_co_by_maz, mgra15, how='left', left_on='MAZ', right_on='MGRA')
bad_school_shp = geopandas.GeoDataFrame(bad_school_shp, geometry='geometry', crs=mgra15.crs)
bad_school_shp = bad_school_shp.to_crs(epsg=2230)  # CA state plane 6 (feet)

def find_closest_valid_maz(row, maz_shp_landuse):
    if row.MAZ < 0:
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

new_school_co = make_school_co(asim_per_df)
make_missing_school_maz_df(new_school_co)

people_with_invalid_school_maz = asim_per_df.loc[asim_per_df['school_zone_id'] <= 0, 'person_id']
asim_tour_df.loc[
    (asim_tour_df['tour_type'] == 'school') 
    & asim_tour_df['person_id'].isin(people_with_invalid_school_maz),
    'keep_tour'] = 0

# ### If person makes a work tour, but has an invalid (missing) workplace maz, use the first work tour destination as workplace maz
people_with_invalid_work_maz = asim_per_df.loc[asim_per_df['workplace_zone_id'] <= 0, 'person_id']

inferred_workplace_mazs = asim_tour_df[(asim_tour_df['tour_type'] == 'work') & asim_tour_df['person_id'].isin(people_with_invalid_work_maz)][['destination', 'person_id']]
inferred_workplace_mazs = inferred_workplace_mazs.drop_duplicates('person_id', keep='first')
inferred_workplace_mazs.set_index('person_id', inplace=True)
print("Inferred workplace maz for people that have a workplace zone_id missing but make a work tour: ")
print(inferred_workplace_mazs)

asim_per_df.set_index('person_id', inplace=True)
asim_per_df.loc[inferred_workplace_mazs.index, 'workplace_zone_id'] = inferred_workplace_mazs['destination']
asim_per_df.reset_index(inplace=True)

# #### Change people that go to school and not work, but say they are workers
# Had a problem where an individual says they are a full time worker and a university student, but do not provide a work location and do not take a work tour, but do take a school tour. 
# school_zone_id was changed to -1 after initialize households (full time workers do not get a school location), but cdap has 1 M school tour.  
# Since school_zone_id changed to -1, caused 0 probs error in mandatory tour frequency.
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

print("number of workers who are actually students: ", workers_who_are_actually_students.sum())

# #### Change people that go to work and not school, but say they are students
# Same problem as above -- peoply say they are students, but only go to work.  
# This makes for zero probs in mandatory tour frequency because they aren't assigned a workplace maz because they are not listed as workers.

asim_per_df.pemploy.value_counts()

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

print("number of students who are actually fulltime workers: ", students_who_are_actually_workers.sum())

# Also want to count people who are making school and work tours but didn't list themselves as a worker. 
# annotate_persons.csv determines pemploy and it just checks for ESR != [3,6] to determine partime status, 
# so just need to ensure ESR = 1 for all people who make a work tour.
# setting them to part time workers
asim_per_df.loc[(asim_per_df['person_id'].isin(people_making_work_tours) 
                & (asim_per_df['workplace_zone_id'] > 0)), 'ptype'] = 2
asim_per_df.loc[(asim_per_df['person_id'].isin(people_making_work_tours) 
                & (asim_per_df['workplace_zone_id'] > 0)), 'pemploy'] = 2

# #### Activitysim does not allow full-time workers to go to school.  Turning all full time workers going to school into part-time instead.
full_time_workers_going_to_school = (asim_per_df['person_id'].isin(people_making_work_tours) 
                & asim_per_df['person_id'].isin(people_making_school_tours)
                & (asim_per_df['pemploy'] == 1))
# FIXME allow FT workers to go to school?
# asim_per_df.loc[full_time_workers_going_to_school, 'WKW'] = 5  # 14-26 number of weeks worked
# asim_per_df.loc[full_time_workers_going_to_school, 'WKHP'] = 35  # 35 hrs per week

print("Number of full time workers also going to school: ", full_time_workers_going_to_school.sum())

# #### If person or parent tour is removed, also need to remove it's subtours
asim_tour_df.loc[
      pd.notna(asim_tour_df['parent_tour_id'])
      & ~(asim_tour_df['parent_tour_id'].isin(asim_tour_df.loc[asim_tour_df['keep_tour'] == 1, 'tour_id'])),
    'keep_tour'] = 0

print("Total number of Tours: ", len(asim_tour_df))
print("Total number of tours removed: ", len(asim_tour_df[asim_tour_df['keep_tour'] == 0]))
trimmed_asim_tour_df = asim_tour_df[asim_tour_df['keep_tour'] == 1].copy()
print("Final number of tours: ", len(trimmed_asim_tour_df))

# Also need to remove the tours from the joint tour participants file
print("Initial number of joint tour participants: ", len(asim_jtour_participants_df))
trimmed_asim_jtour_participants_df = asim_jtour_participants_df[asim_jtour_participants_df['tour_id'].isin(trimmed_asim_tour_df['tour_id'])]
print("Final number of joint tour participants: ", len(trimmed_asim_jtour_participants_df))

# ### Not all joint tours have a matching entry in joint_tour_participants
# error found in infer.py 'assert (tour_has_adults | tour_has_children).all()' when trying to assign joint tour composition.  
# If tours are classified as joint, but there are no adults or children on the tour, an error is thrown.
# Only fully joint tours in the SPA tool have joint tour participants listed.
# 
# In this script, setting the asim_tour_df["is_joint"] = 1 for fully joint tours only solves this problem. 
# The following code is a re-implementation of the code in infer.py to check that all joint tours have an adult or child

joint_tours = trimmed_asim_tour_df[trimmed_asim_tour_df['is_joint'] == 1]

joint_tour_participants = pd.merge(
    trimmed_asim_jtour_participants_df,
    asim_per_df,
    how='left',
    on='person_id'
)
joint_tour_participants['adult'] = (joint_tour_participants.age >= 18)

tour_has_adults = joint_tour_participants[joint_tour_participants.adult]\
        .groupby('tour_id').size().reindex(joint_tours['tour_id']).fillna(0) > 0

tour_has_children = joint_tour_participants[~joint_tour_participants.adult]\
        .groupby('tour_id').size().reindex(joint_tours['tour_id']).fillna(0) > 0

tour_has_children.sum()
tour_has_adults.sum()
good_tours = (tour_has_children | tour_has_adults)
good_tours.sum()

assert good_tours.sum() == len(joint_tours)

# ## Trips Processing
spa_out_trips_df = pd.merge(
    spa_out_trips_df,
    asim_tour_df[['HH_ID', 'PER_ID', 'TOUR_ID', 'household_id', 'person_id', 'tour_id', 'day', 'tour_purpose']],
    how='left',
    on=['HH_ID', 'PER_ID', 'TOUR_ID', 'day'],
)

asim_trip_df = pd.DataFrame()

keep_cols = ['household_id', 'person_id', 'tour_id', 'day', 'survey_year']

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
# calculating trip_num and trip_count
grouped = asim_trip_df.groupby(["tour_id", "outbound"])
asim_trip_df["trip_num"] = grouped.cumcount() + 1
asim_trip_df["trip_count"] = asim_trip_df["trip_num"] + grouped.cumcount(ascending=False)
asim_trip_df['trip_id'] = asim_trip_df.reset_index().index + 1
asim_trip_df['keep_trip'] = 1

# 2022 survey has same trip & tour modes
asim_trip_df.loc[asim_trip_df['survey_year'] == 2022, 'trip_mode'] = spa_out_trips_df.loc[
    spa_out_trips_df['survey_year'] == 2022, 'TRIPMODE'].map(tour_mode_spa_to_asim_dict_22)
    
# FIXME: Can't differentiate between PRM and MIX for 2016 survey
trip_mode_spa_to_asim_dict_16 = {
    1: 'SOV', # 'SOV-FREE' 
    2: 'SOV', # 'SOV-PAY' 
    3: 'HOV2', # 'HOV2-FREE' 
    4: 'HOV2', # 'HOV2-PAY' 
    5: 'HOV3', # 'HOV3-FREE' 
    6: 'HOV3', # 'HOV3-PAY' 
    7: 'WALK', # 'WALK' 
    8: 'BIKE', # 'BIKE' 
    9: 'WALK_LOC', # 'WALK-LB' 
    10: 'WALK_PRM', # 'WALK-EB' 
    11: 'WALK_PRM', # 'WALK-LR' 
    12: 'WALK_PRM', # 'WALK-CR' 
    13: 'PNR_LOC', # 'PNR-LB' 
    14: 'PNR_PRM', # 'PNR-EB' 
    15: 'PNR_PRM', # 'PNR-LR' 
    16: 'PNR_PRM', # 'PNR-CR' 
    17: 'KNR_LOC', # 'KNR-LB' 
    18: 'KNR_PRM', # 'KNR-EB' 
    19: 'KNR_PRM', # 'KNR-LR' 
    20: 'KNR_PRM', # 'KNR-CR' 
    21: 'SCH_BUS', # 'SCHOOLBUS' 
    22: 'TAXI', # 'TAXI' 
    23: 'SOV', # 'OTHER' 
}

asim_trip_df.loc[asim_trip_df['survey_year'] == 2016, 'trip_mode'] = spa_out_trips_df.loc[
    spa_out_trips_df['survey_year'] == 2016, 'TRIPMODE'].map(trip_mode_spa_to_asim_dict_16)

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

#  Do all trips still belong to a valid tour?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
asim_trip_df.loc[~asim_trip_df['tour_id'].isin(trimmed_asim_tour_df.tour_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed", count_before_removed_trips - count_after_removed_trips, "trips because their tour was removed")

#  Are there four or fewer trips per tour and direction?
# FIXME do not filter out entire tour, just the trips
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
tours_with_too_many_trips = asim_trip_df.loc[asim_trip_df['trip_num'] > 4, 'tour_id']
asim_trip_df.loc[asim_trip_df['tour_id'].isin(tours_with_too_many_trips), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips because they belonged to tours that had more than the allowed stops")
print(f"Happens for {len(tours_with_too_many_trips.unique())} tours")

#  Do all trips have a person & household?
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

asim_trip_df.loc[~asim_trip_df['household_id'].isin(asim_hh_df.household_id) | ~asim_trip_df['person_id'].isin(asim_per_df.person_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips did not belong to a person or hh")

#  Does first trip on tour start at home
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
bad_starts = asim_trip_df[(asim_trip_df['trip_num'] == 1) & (asim_trip_df['outbound']) & (asim_trip_df['is_subtour'] == 0) & (asim_trip_df['home_zone_id'] != asim_trip_df['origin'])]

asim_trip_df.loc[asim_trip_df['tour_id'].isin(bad_starts.tour_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips that belong to tours that do not start at home")
print(f"Happens for {len(bad_starts)} tours")

#  Does first trip on tour end at home
count_before_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])
bad_ends = asim_trip_df[
    (asim_trip_df['trip_num'] == asim_trip_df['trip_count']) 
    & (~asim_trip_df['outbound']) & (asim_trip_df['is_subtour'] == 0) 
    & ((asim_trip_df['home_zone_id'] != asim_trip_df['destination']) | (asim_trip_df['purpose'] != 'home'))
    ]

asim_trip_df.loc[asim_trip_df['tour_id'].isin(bad_ends.tour_id), 'keep_trip'] = 0

count_after_removed_trips = len(asim_trip_df[asim_trip_df['keep_trip'] == 1])

print("Removed an additional", count_before_removed_trips - count_after_removed_trips, "trips that belong to tours that do not end at home")
print(f"Happens for {len(bad_ends)} tours")

# Do school and work tours go to the school or workplace zone?
# just changing primary destination instead of removing them.  Adding flag to filter if needed
asim_trip_df['mand_dest_changed'] = 0

bad_school_trips = (
    (asim_trip_df['purpose'] == 'school') 
    & (asim_trip_df['destination'] != asim_trip_df['school_zone_id'])
)
asim_trip_df.loc[bad_school_trips, 'destination'] = asim_trip_df.loc[bad_school_trips, 'school_zone_id']
asim_trip_df.loc[bad_school_trips, 'mand_dest_changed'] = 1
print(f"School trip does not go to school zone for {bad_school_trips.sum()} trips")

bad_work_trips = (
    (asim_trip_df['purpose'] == 'work') 
    & (asim_trip_df['destination'] != asim_trip_df['workplace_zone_id'])
)
asim_trip_df.loc[bad_work_trips, 'destination'] = asim_trip_df.loc[bad_work_trips, 'school_zone_id']
asim_trip_df.loc[bad_work_trips, 'mand_dest_changed'] = 1
print(f"Work trip does not go to workplace zone for {bad_work_trips.sum()} trips")

# FIXME need to re-set origin zone if destination was changed!
print("Total Number of trips kept after filtering:")
asim_trip_df['keep_trip'].value_counts()

print("Percentage of trip that are kept after filtering:")
asim_trip_df['keep_trip'].value_counts(normalize=True) * 100

# ### Need to check tour table for consistency again
#  Do all tours have trips?
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

# ## Writing Output
hh_output_cols = [
    'household_id',
    'home_zone_id',
    'income',
    'hhsize',
    'HHT',
    'auto_ownership',
    'num_workers',
    'children',
    'day'
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
    'ptype',
    'school_zone_id',
    'workplace_zone_id',
    'free_parking_at_work',
    'work_from_home',
    'telecommute_frequency',
    'day'
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
    'day'
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

output_asim_hh_df.to_csv(os.path.join(final_output_path, "survey_households.csv"), index=False)
output_asim_per_df.to_csv(os.path.join(final_output_path, "survey_persons.csv"), index=False)
output_asim_tour_df.to_csv(os.path.join(final_output_path, "survey_tours.csv"), index=False)
output_asim_jtour_df.to_csv(os.path.join(final_output_path, "survey_joint_tour_participants.csv"), index=False)
output_asim_trip_df.to_csv(os.path.join(final_output_path, "survey_trips.csv"), index=False)

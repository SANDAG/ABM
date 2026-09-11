import pandas as pd
import numpy as np
import os
import shutil
import time
import datetime
from IPython.display import display
import matplotlib
import matplotlib.pyplot as plt
import seaborn as sns
import subprocess
sns.set()

# --------------------------------------------------------------------------------------------------
# dictionary mappings
total_keyword = 'All'
output_calibration_trip_purposes = [
    'work',
    'univ',
    'school',
    'atwork',
    'ind_maint',
    'ind_discr',
    'joint',
    total_keyword  # last entry should be for all purposes
]

output_calibration_tour_modes = [
    'DRIVEALONE',
    'SHARED2',
    'SHARED3',
    'WALK',
    'BIKE',
    'ESCOOTER',
    'EBIKE',
    'WALK_TRANSIT',
    'PNR_TRANSIT',
    'KNR_TRANSIT',
    'TNC_TRANSIT',
    'SCHOOLBUS',
    'TAXI',
    'TNC_SINGLE',
    'TNC_SHARED',
    total_keyword]

output_calibration_trip_modes = [
    'DRIVEALONE',
    'SHARED2',
    'SHARED3',
    'WALK',
    'BIKE',
    'ESCOOTER',
    'EBIKE',
    'WALK_LOC',
    'WALK_PRM',
    'WALK_MIX',
    'PNR_LOC',
    'PNR_PRM',
    'PNR_MIX',
    'KNR_LOC',
    'KNR_PRM',
    'KNR_MIX',
    'TNC_LOC',
    'TNC_PRM',
    'TNC_MIX',
    'SCHOOLBUS',
    'TNC_SINGLE',
    'TNC_SHARED',
    'TAXI',    
    total_keyword]

# transit calibraiton modes are not scaled when comparing to model
output_calibration_transit_modes = [
    'WALK_LOC',
    'WALK_PRM',
    'WALK_MIX',
    'PNR_LOC',
    'PNR_PRM',
    'PNR_MIX',
    'KNR_LOC',
    'KNR_PRM',
    'KNR_MIX',
    'TNC_LOC',
    'TNC_PRM',
    'TNC_MIX'
]

# # used to switch drivealone mode to walk transit for people parked at major university
# output_calibration_auto_modes = [
#     'DRIVEALONE',
#     'SHARED2',
#     'SHARED3',
# ]
# parked_at_univ_calib_mode = 'WALK-TRANSIT'

# should match the mode choice coefficient names
output_auto_suff = [
    '0',
    '1',
    '2'
] 

# Mapping calibration mode to trip mode choice coefficient names
output_calibration_tour_mode_to_coef_name_dict = {
    'DRIVEALONE': 'da',
    'SHARED2': 's2',
    'SHARED3': 's3',
    'WALK': 'walk',
    'BIKE': 'bike',
    'ESCOOTER': 'escooter',
    'EBIKE': 'ebike',
    'WALK_TRANSIT': 'wtran',
    'PNR_TRANSIT': 'pnr',
    'KNR_TRANSIT': 'knr',
    'TNC_TRANSIT': 'tnr',
    'SCHOOLBUS': 'schbus',
    'TAXI': 'taxi',
    'TNC_SINGLE': 'tnc_single',
    'TNC_SHARED': 'tnc_shared',
    total_keyword: 'all',
}

output_calibration_trip_mode_to_coef_name_dict = {
    'DRIVEALONE': 'DRIVEALONE',
    'SHARED2': 'SHARED2',
    'SHARED3': 'SHARED3',
    'WALK': 'WALK',
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK_LOC': 'WALK_LOC',
    'WALK_PRM': 'WALK_PRM',
    'WALK_MIX': 'WALK_MIX',
    'PNR_LOC': 'PNR_LOC',
    'PNR_PRM': 'PNR_PRM',
    'PNR_MIX': 'PNR_MIX',
    'KNR_LOC': 'KNR_LOC',
    'KNR_PRM': 'KNR_PRM',
    'KNR_MIX': 'KNR_MIX',
    'TNC_LOC': 'TNC_LOC',
    'TNC_PRM': 'TNC_PRM',
    'TNC_MIX': 'TNC_MIX',
    'SCHOOLBUS': 'SCH_BUS',
    'TNC_SINGLE': 'TNC_SINGLE',
    'TNC_SHARED': 'TNC_SHARED',
    'TAXI': 'TAXI',
    total_keyword: 'all',
}

output_calibration_purposes_to_coef_names_dict = {
    'work': ['work'],
    'univ': ['univ'],
    'school': ['school'],
    'atwork': ['atwork'],
    # 'ind_maint': ['shopping', 'escort', 'othmaint'], ### DELETE
    # 'ind_discr': ['social', 'eatout', 'othdiscr'], ### DELETE
    'ind_maint': ['maint'],
    'ind_discr': ['disc'],
    'joint': ['maint', 'disc'], 
    # 'joint': ['joint'],  # placeholder ### DELETE
    # 'joint': ['joint'],  # placeholder ### DELETE
    total_keyword: ['all']
}

coef_name_to_output_calibration_purpose_dict = {
    'work': 'work',
    'univ': 'univ',
    'school': 'school',
    'atwork': 'atwork',
    'shopping': 'ind_maint',  # individual split to joint in code
    'escort': 'ind_maint',
    'othmaint': 'ind_maint',
    'social': 'ind_discr',
    'eatout': 'ind_discr',
    'othdiscr': 'ind_discr',
    'joint': 'joint',
    'all': total_keyword  # last entry should be for all purposes
}

# ------------------------------------------
# Mapping activitysim to calibration definitions
asim_to_calib_purpose_dict = {
    'work': 'work',
    'univ': 'univ',
    'school': 'school',
    'shopping': 'ind_maint',  # individual split to joint in code
    'escort': 'ind_maint',
    'othmaint': 'ind_maint',
    'social': 'ind_discr',
    'eatout': 'ind_discr',
    'othdiscr': 'ind_discr',
    'atwork': 'atwork',
    'eat': 'atwork',
    'maint': 'atwork',
    'business': 'atwork',
    'all': total_keyword
}

asim_to_calib_trip_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE',
    'SHARED2': 'SHARED2',
    'SHARED3': 'SHARED3',
    'WALK': 'WALK',
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK_LOC': 'WALK_LOC',
    'WALK_PRM': 'WALK_PRM',
    'WALK_MIX': 'WALK_MIX',
    'PNR_LOC': 'PNR_LOC',
    'PNR_PRM': 'PNR_PRM',
    'PNR_MIX': 'PNR_MIX',
    'KNR_LOC': 'KNR_LOC',
    'KNR_PRM': 'KNR_PRM',
    'KNR_MIX': 'KNR_MIX',
    'TNC_LOC': 'TNC_LOC',
    'TNC_PRM': 'TNC_PRM',
    'TNC_MIX': 'TNC_MIX',
    'TAXI': 'TAXI',
    'TNC_SINGLE': 'TNC_SINGLE',
    'TNC_SHARED': 'TNC_SHARED',
    'SCH_BUS': 'SCHOOLBUS'
}

# added for trip tables
asim_to_calib_tour_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE',
    'SHARED2': 'SHARED2',
    'SHARED3': 'SHARED3',
    'WALK': 'WALK',
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK_LOC': 'WALK_TRANSIT',
    'WALK_PRM': 'WALK_TRANSIT',
    'WALK_MIX': 'WALK_TRANSIT',
    'PNR_LOC': 'PNR_TRANSIT',
    'PNR_PRM': 'PNR_TRANSIT',
    'PNR_MIX': 'PNR_TRANSIT',
    'KNR_LOC': 'KNR_TRANSIT',
    'KNR_PRM': 'KNR_TRANSIT',
    'KNR_MIX': 'KNR_TRANSIT',
    'TNC_LOC': 'TNC_TRANSIT',
    'TNC_PRM': 'TNC_TRANSIT',
    'TNC_MIX': 'TNC_TRANSIT',
    'TAXI': 'TAXI',
    'TNC_SINGLE': 'TNC_SINGLE',
    'TNC_SHARED': 'TNC_SHARED',
    'SCH_BUS': 'SCHOOLBUS'
}

asim_auto_suff_dict = {
    0: output_auto_suff[0],
    1: output_auto_suff[1],
    2: output_auto_suff[2],
}

# ------------------------------------------
# Mapping Survey targets to calibration definitions
survey_to_calib_purposes_dict = {
    'Work': 'work',
    'University': 'univ',
    'School': 'school',
    'Work sub-tour': 'atwork',
    'Ind-Maintenance': 'ind_maint',
    'Ind-Discretionary': 'ind_discr',
    'Joint-Maintenance': 'joint',
    'Joint-Discretionary': 'joint',
    'Total': total_keyword
}

survey_to_calib_trip_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE', 
    'SHARED2': 'SHARED2', 
    'SHARED3': 'SHARED3', 
    'WALK': 'WALK', 
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    # Disaggregated transit modes - trip modes
    'Walk_LOC': 'WALK_LOC',
    'Walk_PRM': 'WALK_PRM',
    'Walk_MIX': 'WALK_MIX',
    'PNR_LOC': 'PNR_LOC',
    'PNR_PRM': 'PNR_PRM',
    'PNR_MIX': 'PNR_MIX',
    'KNR_LOC': 'KNR_LOC',
    'KNR_PRM': 'KNR_PRM',
    'KNR_MIX': 'KNR_MIX',
    'TNC_LOC': 'TNC_LOC',
    'TNC_PRM': 'TNC_PRM',
    'TNC_MIX': 'TNC_MIX',
    # # Aggregated transit modes (legacy support)
    # 'WALK-TRANSIT': 'WALK-TRANSIT', 
    # 'PNR-TRANSIT': 'PNR-TRANSIT', 
    # 'KNR-TRANSIT': 'KNR-TRANSIT', 
    # 'TNC-TRANSIT': 'TNC-TRANSIT',
    'TAXI': 'TAXI', 
    'TNC-REG': 'TNC_SINGLE', 
    'TNC-SHARED': 'TNC_SHARED', 
    'TNC_SINGLE': 'TNC_SINGLE',
    'TNC_SHARED': 'TNC_SHARED',
    'SCHOOLBUS': 'SCHOOLBUS',
    'All': total_keyword
}

survey_to_calib_tour_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE', 
    'SHARED2': 'SHARED2', 
    'SHARED3': 'SHARED3', 
    'WALK': 'WALK', 
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    # Tour modes are aggregated in the new target file
    'WALK-TRANSIT': 'WALK_TRANSIT', 
    'WALK_TRANSIT': 'WALK_TRANSIT',
    'PNR-TRANSIT': 'PNR_TRANSIT',
    'PNR_TRANSIT': 'PNR_TRANSIT', 
    'KNR-TRANSIT': 'KNR_TRANSIT',
    'KNR_TRANSIT': 'KNR_TRANSIT', 
    'TNC-TRANSIT': 'TNC_TRANSIT',
    'TNC_TRANSIT': 'TNC_TRANSIT',
    'TAXI': 'TAXI', 
    'TNC-REG': 'TNC_SINGLE', 
    'TNC-SHARED': 'TNC_SHARED', 
    'TNC_SINGLE': 'TNC_SINGLE',
    'TNC_SHARED': 'TNC_SHARED',
    'SCHOOLBUS': 'SCHOOLBUS',
    'All': total_keyword
}

survey_calib_target_auto_suff_dict = {
    '0': output_auto_suff[0],
    '1': output_auto_suff[1],
    '2': output_auto_suff[2],
}

survey_table_purpose_col = 'purpose'
survey_table_trip_mode_col = 'grouped_linked_trip_mode'
# added for trip mc calibration
survey_table_tour_mode_col = 'grouped_tour_mode'
# not needed for trip mc calibration
#survey_table_auto_suff_col = 'auto_suff'
survey_table_trips_col = 'trips'

# HTML visualizer dictionaries.  needs to match HTS summary script.
#  need to be checked only if the scaled calibration targets want to be included in the visualizer
calib_trip_mode_to_vis_dict = { #tripmode vis
    # calibration_trip_mode: visualizer_trip_mode
    'DRIVEALONE': 1,
    'SHARED2': 2,
    'SHARED3': 3,
    'WALK': 4,
    'BIKE': 5,
    'WALK_LOC': 6,
    'WALK_PRM': 7,
    'WALK_MIX': 8,
    'PNR_LOC': 9,
    'PNR_PRM': 10,
    'PNR_MIX': 11,
    'KNR_LOC': 12,
    'KNR_PRM': 13,
    'KNR_MIX': 14,
    'TNC_LOC': 15,
    'TNC_PRM': 16,
    'TNC_MIX': 17,
    'TAXI': 18,
    'TNC_SINGLE': 19,
    'TNC_SHARED': 20,
    'SCHOOLBUS': 21,
    'ESCOOTER': 22,
    'EBIKE': 23,
    total_keyword: 'Total'
}
calib_tourmode_num_to_vis_dict = { #tourmode vis
    # calibration_tour_mode: visualizer_tour_mode
    'DRIVEALONE': 1,
    'SHARED2': 2,
    'SHARED3': 3,
    'WALK': 4,
    'BIKE': 5,
    'WALK_TRANSIT': 6,
    'PNR_TRANSIT': 7,
    'KNR_TRANSIT': 8,
    'TNC_TRANSIT': 9,
    'TAXI': 10,
    'TNC_SINGLE': 11,
    'TNC_SHARED': 12,
    'SCHOOLBUS': 13,
    'ESCOOTER': 14,
    'EBIKE': 15,
    total_keyword: 'Total'
}
calib_tour_mode_to_vis_dict = {  #tourmode vis
    'DRIVEALONE': 'Drivealone',
    'SHARED2': 'Shared2',
    'SHARED3': 'Shared3',
    'WALK': 'Walk',
    'BIKE': 'Bike',
    'ESCOOTER': 'Escooter',
    'EBIKE': 'Ebike',
    'WALK_TRANSIT': 'Walk-Transit',
    'PNR_TRANSIT': 'PNR-Transit',
    'KNR_TRANSIT': 'KNR-Transit',
    'TNC_TRANSIT': 'TNC-Transit',
    'SCHOOLBUS': 'SchoolBus',
    'TAXI': 'Taxi',
    'TNC_SINGLE': 'TNC-Single',
    'TNC_SHARED': 'TNC-Shared',
    total_keyword: 'Total'
}
purpose_vis_dict = {  # same purposes used for both trip and tour
    # calibration purpose: visualizer purpose
    'univ': 'univ',
    'school': 'sch',
    'work': 'work',
    'atwork': 'atwork',
    'ind_discr': 'idisc',
    'ind_maint': 'imain',
    'joint': 'jmain',
    'jdisc': 'jdisc',  # joint is copied to produce jdisc purpose in the code
    total_keyword: 'total'
}


# --------------------------------------------------------------------------------------------------
# Helper functions
def check_input_dictionaries_for_consistency():
    # checking trip purposes
    for purpose in list(asim_to_calib_purpose_dict.values()):
        assert purpose in output_calibration_trip_purposes, "ActivitySim purpose not in calibration"
    for purpose in list(survey_to_calib_purposes_dict.values()):
        assert purpose in output_calibration_trip_purposes, "Survey purpose not in calibration"

    # checking trip mode
    for mode in list(asim_to_calib_trip_mode_dict.values()):
        assert mode in output_calibration_trip_modes, f"ActivitySim trip mode {mode} not in calibration"
    for mode in list(asim_to_calib_tour_mode_dict.values()):
        assert mode in output_calibration_tour_modes, f"ActivitySim trip mode {mode} not in calibration"
    for mode in list(survey_to_calib_trip_mode_dict.values()):
        assert mode in output_calibration_trip_modes, f"Survey trip mode {mode} not in calibration"
    for mode in list(survey_to_calib_tour_mode_dict.values()):
        assert mode in output_calibration_tour_modes, f"Survey tour mode {mode} not in calibration"

    print("No problems found in input dictionaries")


def write_tables_to_excel(dfs,
                          excel_writer,
                          excel_sheet_name,
                          start_row,
                          start_col,
                          title,
                          sep_for_col_title,
                          col_title):

    # have to write first table to initialize sheet before writing title
    dfs[0].to_excel(excel_writer, excel_sheet_name, startrow=start_row+2, startcol=start_col)
    worksheet = excel_writer.sheets[excel_sheet_name]

    # writing title at and first table name
    # openpyxl uses 1-based indexing
    worksheet.cell(row=start_row+1, column=start_col+1, value=title)
    worksheet.cell(row=start_row+2, column=start_col+1, value=dfs[0].name)
    worksheet.cell(row=start_row+2, column=start_col+sep_for_col_title+1, value=col_title)
    start_row += len(dfs[0]) + 6

    for df in dfs[1:]:
        df.to_excel(excel_writer, excel_sheet_name, startrow=start_row, startcol=start_col)
        worksheet.cell(row=start_row, column=start_col+1, value=df.name)
        worksheet.cell(row=start_row, column=start_col+sep_for_col_title+1, value=col_title)
        start_row += len(df) + 4
    return


def map_asim_trip_table_to_calib(trips_df):
    trips_df['calib_tour_purpose'] = trips_df['primary_purpose'].apply(
        lambda x: asim_to_calib_purpose_dict[x])

    # added modification for trips mc calibration
    trips_df.loc[(trips_df['calib_tour_purpose'] == 'ind_maint')
                 & (trips_df['tour_category'] == 'joint'), 'calib_tour_purpose'] = 'joint'
    trips_df.loc[(trips_df['calib_tour_purpose'] == 'ind_discr')
                 & (trips_df['tour_category'] == 'joint'), 'calib_tour_purpose'] = 'joint'

    trips_df['calib_trip_mode'] = trips_df['trip_mode'].apply(
        lambda x: asim_to_calib_trip_mode_dict[x])

    trips_df['calib_tour_mode'] = trips_df['tour_mode'].apply(
        lambda x: asim_to_calib_tour_mode_dict[x])

    ## if parked at university, act as if tour mode is walk-transit
    #trips_df.loc[(trips_df['parked_at_university'] == True)
    #            & trips_df['tour_mode'].isin(output_calibration_auto_modes),
    #            'calib_tour_mode'] = parked_at_univ_calib_mode

    return trips_df


def create_asim_trip_mode_choice_tables(trips_df):
    asim_trip_mode_purpose_cts = []

    columns = list(output_calibration_tour_modes)

    for trip_purpose in output_calibration_trip_purposes[:-1]:
        asim_trip_mode_purpose_ct = pd.crosstab(
            trips_df['calib_trip_mode'],
            trips_df[trips_df['calib_tour_purpose'] == trip_purpose]['calib_tour_mode'],
            margins=True,
            margins_name=total_keyword,
            dropna=False
        )
        asim_trip_mode_purpose_ct = asim_trip_mode_purpose_ct.reindex(
            index=output_calibration_trip_modes, columns=columns, fill_value=0)
        asim_trip_mode_purpose_ct.index.name = 'trip_mode'
        asim_trip_mode_purpose_ct.columns.name = 'tour_mode'
        # not needed for trip mc calibration? will not rename columns?
        #asim_trip_mode_purpose_ct.rename(columns=columns, inplace=True)
        asim_trip_mode_purpose_ct.name = trip_purpose
        asim_trip_mode_purpose_cts.append(asim_trip_mode_purpose_ct)

    asim_trip_mode_all_ct = pd.crosstab(
        trips_df['calib_trip_mode'],
        trips_df['calib_tour_mode'],
        margins=True,
        margins_name=total_keyword,
        dropna=False
    )
    asim_trip_mode_all_ct = asim_trip_mode_all_ct.reindex(
        index=output_calibration_trip_modes, columns=columns, fill_value=0)
    asim_trip_mode_all_ct.index.name = 'trip_mode'
    asim_trip_mode_all_ct.columns.name = 'tour_mode'
    # not needed for trip mc calibration? will not rename columns?
    #asim_trip_mode_all_ct.rename(columns=asim_auto_suff_dict, inplace=True)
    asim_trip_mode_all_ct.name = output_calibration_trip_purposes[-1]
    asim_trip_mode_purpose_cts.append(asim_trip_mode_all_ct)
    return asim_trip_mode_purpose_cts


def process_asim_tables_for_trip_mode_choice(asim_tables_dir):
    households_df = pd.read_csv(
        os.path.join(asim_tables_dir, 'final_households.csv'), engine='pyarrow')
    sample_rate = households_df['sample_rate'].max()
    trips_df = pd.read_csv(os.path.join(asim_tables_dir, 'final_trips.csv'), engine='pyarrow')
    tours_df = pd.read_csv(os.path.join(asim_tables_dir, 'final_tours.csv'), engine='pyarrow')

    trips_df = pd.merge(
        trips_df,
        tours_df[['tour_id', 'tour_mode', 'tour_category', 'number_of_participants']],
        how='left',
        on='tour_id'
    )

    trips_df = map_asim_trip_table_to_calib(trips_df)
    asim_trip_mode_tables = create_asim_trip_mode_choice_tables(trips_df)

    total_model_trips = len(trips_df)
    num_trips_full_model = int(total_model_trips / sample_rate)
    print("Sample rate of ", sample_rate, "results in ", total_model_trips, "out of",
          num_trips_full_model, "tours")

    return asim_trip_mode_tables, num_trips_full_model, trips_df


def read_trip_mode_choice_calibration_file(trip_mode_choice_calib_targets_file):
    trip_mc_calib_df = pd.read_csv(trip_mode_choice_calib_targets_file)
    trip_mc_calib_df['calib_trip_mode'] = trip_mc_calib_df[survey_table_trip_mode_col].apply(
        lambda x: survey_to_calib_trip_mode_dict[x])
    trip_mc_calib_df['calib_tour_mode'] = trip_mc_calib_df[survey_table_tour_mode_col].apply(
        lambda x: survey_to_calib_tour_mode_dict[x])

    columns = list(output_calibration_tour_modes)

    trip_mc_calib_target_tables = []

    for purpose in list(survey_to_calib_purposes_dict.keys()):
        df = trip_mc_calib_df[(trip_mc_calib_df[survey_table_purpose_col] == purpose)
                              & (trip_mc_calib_df[survey_table_trip_mode_col] != total_keyword)
                              & (trip_mc_calib_df[survey_table_tour_mode_col] != total_keyword)]

        df_ct = pd.crosstab(
            df['calib_trip_mode'],
            df['calib_tour_mode'],
            values=df[survey_table_trips_col],
            aggfunc='sum',
            margins=True,
            margins_name=total_keyword,
            dropna=False,
        )
        df_ct = df_ct.reindex(index=output_calibration_trip_modes, columns=columns, fill_value=0)
        df_ct.index.name = 'trip_mode'
        df_ct.columns.name = 'tour_mode'
        df_ct.name = survey_to_calib_purposes_dict[purpose]

        # looking to see if calibration purposes need to be combined
        # e.g. joint = joint_ind + joint_maint
        for i in range(len(trip_mc_calib_target_tables)):
            prev_df_ct = trip_mc_calib_target_tables[i]
            if prev_df_ct.name == df_ct.name:
                prev_df_ct = prev_df_ct + df_ct
                prev_df_ct.name = df_ct.name  # reassignment does not preserve name
                trip_mc_calib_target_tables[i] = prev_df_ct
                break
        else:  # only enters if above for loop breaks
            trip_mc_calib_target_tables.append(df_ct)

    return trip_mc_calib_target_tables


def read_trip_mode_choice_constants(configs_dir, coef_file="trip_mode_choice_coefficients.csv"):
    constants_config_path = os.path.join(configs_dir, coef_file)
    trip_mc_constants_df = pd.read_csv(constants_config_path)
    return trip_mc_constants_df


def write_scaled_targets_to_excel(unscaled_model_tables,
                                  unscaled_calib_tables,
                                  full_model_tables,
                                  full_calib_tables,
                                  scaled_model_tables,
                                  scaled_calib_tables,
                                  output_dir):
    excel_writer = pd.ExcelWriter(os.path.join(output_dir, 'scaled_targets.xlsx'), engine='openpyxl')

    start_col = 0
    # unscaled model
    write_tables_to_excel(
        dfs=unscaled_model_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Unscaled Model Trips',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19
    # unscaled calibration targets
    write_tables_to_excel(
        dfs=unscaled_calib_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Unscaled Calibration Trips',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19
    # full model
    write_tables_to_excel(
        dfs=full_model_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Full Model Trips',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19
    # full calibration targets
    write_tables_to_excel(
        dfs=full_calib_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Calibration Trips Matching Model',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19
    # scaled model
    write_tables_to_excel(
        dfs=scaled_model_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Scaled Model Trips',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19
    # scaled calibration targets
    write_tables_to_excel(
        dfs=scaled_calib_tables,
        excel_writer=excel_writer,
        excel_sheet_name='trip_mode_choice',
        start_row=0,
        start_col=start_col,
        title='Scaled Calibration Trips',
        sep_for_col_title=3,
        col_title='Tour Mode'
    )
    start_col += 19

    excel_writer.close()


def calculate_share_by_tour_mode(trip_mc_calib_target_tables, asim_trip_mc_tables):
    '''
    Calibration target and model counts tables are scaled to represent
        the distribution of trip modes for each tour mode.
    This is done by dividing each column by the total number of trips in that column
        and converting to a percentage.
    '''
    assert asim_trip_mc_tables[-1].name == total_keyword, "Last table is not total!"

    scaled_trip_mc_calib_target_tables = []
    scaled_asim_trip_mc_tables = []
    total_model_df = None
    total_calib_df = None

    # making total tables sum of all purposes
    for i in range(len(trip_mc_calib_target_tables) - 1):
        model_df = asim_trip_mc_tables[i]
        calib_target_df = trip_mc_calib_target_tables[i]
        # total purpose should be the sum of all prev purposes
        if total_model_df is not None:
            total_model_df = total_model_df + model_df
            total_calib_df = total_calib_df + calib_target_df
        else:
            total_model_df = model_df
            total_calib_df = calib_target_df

    # replacing total table as sum of all purposes
    total_model_df.name = total_keyword
    total_calib_df.name = total_keyword
    asim_trip_mc_tables[len(asim_trip_mc_tables) - 1] = total_model_df
    trip_mc_calib_target_tables[len(trip_mc_calib_target_tables) - 1] = total_calib_df

    # iterating over all purposes
    for i in range(len(trip_mc_calib_target_tables)):
        model_df = asim_trip_mc_tables[i]
        calib_target_df = trip_mc_calib_target_tables[i]
        assert model_df.name == calib_target_df.name, "Tables do not match!"

        scaled_calib_target_df = calib_target_df.copy()
        scaled_model_df = model_df.copy()

        # for tour_mode in output_calibration_tour_modes:
        #     scaled_calib_target_df[tour_mode] = scaled_calib_target_df[tour_mode] / scaled_calib_target_df.loc[total_keyword, tour_mode] * 100
        #     scaled_model_df[tour_mode] = scaled_model_df[tour_mode] / scaled_model_df.loc[total_keyword, tour_mode] * 100
        #     scaled_calib_target_df = scaled_calib_target_df.fillna(0)
        #     scaled_model_df = scaled_model_df.fillna(0)

        scaled_calib_target_df.name = calib_target_df.name
        scaled_model_df.name = model_df.name

        scaled_trip_mc_calib_target_tables.append(scaled_calib_target_df)
        scaled_asim_trip_mc_tables.append(scaled_model_df)

    return scaled_trip_mc_calib_target_tables, scaled_asim_trip_mc_tables


def scale_targets_to_match_model_by_tour_mode(trip_mc_calib_target_tables,
                                 asim_trip_mc_tables,
                                 full_model_trips):

    assert asim_trip_mc_tables[-1].name == total_keyword, "Last table is not total!"
    total_model_trips = asim_trip_mc_tables[-1].loc[total_keyword, total_keyword]
    model_scale_factor = full_model_trips / total_model_trips

    scaled_trip_mc_calib_target_tables = []
    scaled_asim_trip_mc_tables = []
    total_model_df = None
    total_calib_df = None

    # iterating over all non-total tables
    for i in range(len(trip_mc_calib_target_tables) - 1):
        model_df = asim_trip_mc_tables[i]
        calib_target_df = trip_mc_calib_target_tables[i]
        assert model_df.name == calib_target_df.name, "Tables do not match!"

        # counts from model run are scaled to match full model run counts
        scaled_model_df = model_df * model_scale_factor

        # calibration counts need to match full model run counts, but leave transit trip modes unscaled
        scaled_calib_target_df = calib_target_df.copy()
        # Transit trip modes (rows) should not be scaled - these are the disaggregated trip modes
        transit_trip_mode_idxs = scaled_calib_target_df.index.isin(output_calibration_transit_modes)
        no_transit_idxs = ~transit_trip_mode_idxs

        # iterating over all non-total tour modes (columns)
        for tour_mode in output_calibration_tour_modes[:-1]:
            tot_calib_trips_for_tour_mode = calib_target_df.loc[total_keyword, tour_mode]
            tot_model_trips_for_tour_mode = scaled_model_df.loc[total_keyword, tour_mode]

            # Get transit trip counts (rows that are transit modes) in this tour mode
            calib_transit_trips_in_tour_mode = calib_target_df.loc[transit_trip_mode_idxs, tour_mode].sum()
            
            # Calculate scaling factor for non-transit trip modes only
            # Transit trip modes remain unscaled
            numerator = tot_model_trips_for_tour_mode - calib_transit_trips_in_tour_mode
            denom = tot_calib_trips_for_tour_mode - calib_transit_trips_in_tour_mode
            
            if denom == 0 or numerator < 0:
                # don't scale if no non-transit trips in calibration targets
                #  or number of calib transit trips is larger than total model trips
                tour_mode_scaling_factor = 1
            else:
                tour_mode_scaling_factor = numerator / denom
            
            # Apply scaling only to non-transit trip modes (rows)
            scaled_calib_target_df.loc[no_transit_idxs, tour_mode] = \
                scaled_calib_target_df.loc[no_transit_idxs, tour_mode] * tour_mode_scaling_factor

        # recomputing margins
        scaled_calib_target_df = scaled_calib_target_df.applymap(lambda x: 0 if x < 0 else x)
        scaled_calib_target_df.fillna(0, inplace=True)
        scaled_calib_target_df.loc[total_keyword] = scaled_calib_target_df.drop(
            labels=total_keyword, axis=0, inplace=False).sum()
        scaled_calib_target_df.loc[:, total_keyword] = scaled_calib_target_df.drop(
            labels=total_keyword, axis=1, inplace=False).sum(axis=1)

        scaled_calib_target_df = round(scaled_calib_target_df)
        scaled_model_df = round(scaled_model_df)
        scaled_calib_target_df.name = calib_target_df.name
        scaled_model_df.name = model_df.name

        scaled_trip_mc_calib_target_tables.append(scaled_calib_target_df)
        scaled_asim_trip_mc_tables.append(scaled_model_df)

        # total purpose should be the sum of all prev purposes
        if total_model_df is not None:
            total_model_df = total_model_df + scaled_model_df
            total_calib_df = total_calib_df + scaled_calib_target_df
        else:
            total_model_df = scaled_model_df
            total_calib_df = scaled_calib_target_df

    # adding total tables
    total_model_df.name = total_keyword
    total_calib_df.name = total_keyword
    scaled_asim_trip_mc_tables.append(total_model_df)
    scaled_trip_mc_calib_target_tables.append(total_calib_df)

    return scaled_trip_mc_calib_target_tables, scaled_asim_trip_mc_tables


def melt_df(df, melt_id_var, value_name):
    melted_df = df.reset_index().melt(id_vars=[melt_id_var])
    melted_df.rename(columns={'value': value_name}, inplace=True)
    melted_df['purpose'] = df.name
    return melted_df


def write_visualizer_calibration_target_table(full_trip_mc_calib_target_tables, asim_trips_df, output_dir):
    # multiplying output visualizer tables to transform from trips to person trips
    avg_tour_participants_df = asim_trips_df.groupby(
        ['calib_tour_purpose', 'calib_tour_mode', 'calib_trip_mode'])['number_of_participants'].agg('mean').to_frame()
    total_df = None
    for df in full_trip_mc_calib_target_tables:
        trip_purpose = df.name
        if trip_purpose == total_keyword:
            df = total_df
            continue
        for tour_mode in output_calibration_tour_modes[:-1]:
            for trip_mode in output_calibration_trip_modes[:-1]:
                try:
                    avg_tour_participants = avg_tour_participants_df.loc[
                        (trip_purpose, tour_mode, trip_mode), 'number_of_participants']
                except KeyError:
                    avg_tour_participants = 1
                # print("Average number of particapants for",
                #       trip_purpose, tour_mode, trip_mode, " = ", avg_tour_participants)
                df.loc[trip_mode, tour_mode] = df.loc[trip_mode, tour_mode] * avg_tour_participants

        # recompute marginals
        df.loc[total_keyword] = df.drop(
            labels=total_keyword, axis=0, inplace=False).sum()
        df.loc[:, total_keyword] = df.drop(
            labels=total_keyword, axis=1, inplace=False).sum(axis=1)

        # total purpose should be the sum of all prev purposes
        if total_df is not None:
            total_df = total_df + df
        else:
            total_df = df

    total_df.name = total_keyword
    full_trip_mc_calib_target_tables[-1] = total_df

    melted_dfs = []
    for df in full_trip_mc_calib_target_tables:
        if df.name == 'joint':
            # create an extra joint Discretionary table for visualizer
            df_copy = df.copy()
            df_copy.name = 'jdisc'
            melted_df_copy = melt_df(df_copy, melt_id_var='trip_mode', value_name='trips')
            melted_dfs.append(melted_df_copy)
        melted_df = melt_df(df, melt_id_var='trip_mode', value_name='trips')
        melted_dfs.append(melted_df)
    trip_mode_choice_calibration_table = pd.concat(melted_dfs)

    trip_mc_vis = trip_mode_choice_calibration_table.copy()
    trip_mc_vis = trip_mc_vis[trip_mc_vis['trip_mode'] != total_keyword]
    trip_mc_vis['tripmode'] = trip_mc_vis['trip_mode'].apply(lambda x: calib_trip_mode_to_vis_dict[x])
    trip_mc_vis['tourmode'] = trip_mc_vis['tour_mode'].apply(lambda x: calib_tour_mode_to_vis_dict[x])
    trip_mc_vis['value'] = trip_mc_vis['trips']
    trip_mc_vis['tourmode_num'] = trip_mc_vis['tour_mode'].apply(lambda x: calib_tourmode_num_to_vis_dict[x])
    trip_mc_vis['purpose'] = trip_mc_vis['purpose'].apply(lambda x: purpose_vis_dict[x])
    trip_mc_vis['grp_var'] = trip_mc_vis.apply(lambda row: \
                                               row['purpose'] + 'tourmode' + str(row['tourmode_num'])
                                               if row['tourmode'] != 'Total' \
                                               else  row['purpose'] + str(row['tourmode_num']), axis=1)
    trip_mc_vis_cols = ['tripmode', 'tourmode', 'purpose', 'value', 'grp_var']
    trip_mc_vis[trip_mc_vis_cols].to_csv(
        os.path.join(output_dir, 'tripModeProfile_vis_calib.csv'), index=False)


def match_model_and_calib_targets_to_coefficients(original_trip_mc_constants_df,
                                                  scaled_asim_trip_mc_tables,
                                                  scaled_trip_mc_calib_target_tables):

    columns = ['coefficient_name', 'purpose', 'tour_mode', 'trip_mode', 'scaled_model_percent', 'scaled_target_percent']
    model_calib_target_match_df = pd.DataFrame(columns=columns)

    for i, purpose in enumerate(output_calibration_trip_purposes):
        asim_trip_mc_table = scaled_asim_trip_mc_tables[i]
        calib_target_mc_table = scaled_trip_mc_calib_target_tables[i]
        assert asim_trip_mc_table.name == purpose, "Purpose doesn't match!"
        assert calib_target_mc_table.name == purpose, "Purpose doesn't match!"

        for mode in output_calibration_trip_modes:
            for tour_mode in output_calibration_tour_modes[:-1]:

                asim_mc_count = round(asim_trip_mc_table.loc[mode, tour_mode], 3)
                calib_target_count = round(calib_target_mc_table.loc[mode, tour_mode], 3)

                # multiple coefficients match a single calibration purpose.
                # e.g. ind_maint = shopping and escort, both are adjusted the same amount
                for coef_purpose in output_calibration_purposes_to_coef_names_dict[purpose]:
                    coef_trip_mode = output_calibration_trip_mode_to_coef_name_dict[mode]
                    coef_tour_mode = output_calibration_tour_mode_to_coef_name_dict[tour_mode]
                    coef_name = 'coef_calib_tour' + coef_tour_mode + '_' + coef_trip_mode + '_' + coef_purpose
                    #target_name = mode + '_' + coef_tour_mode

                    if coef_purpose in ['maint', 'disc']:
                        coef_name = 'coef_calib_tour' + coef_tour_mode + 'jointtour0_' + coef_trip_mode + '_' + coef_purpose
                        if purpose == 'joint':
                            coef_name = 'coef_calib_tour' + coef_tour_mode + 'jointtour1_' + coef_trip_mode + '_' + coef_purpose

                    row = [coef_name, coef_purpose, coef_tour_mode, coef_trip_mode,
                           asim_mc_count, calib_target_count]
                    model_calib_target_match_df.loc[len(model_calib_target_match_df)] = row

    return model_calib_target_match_df


def calculate_new_coefficient(row, damping_factor, max_ASC_adjust, adjust_when_zero_counts):
    row['difference'] = row.scaled_target_percent - row.scaled_model_percent
    row['percent_diff'] = pd.NA
    row['coef_change'] = pd.NA
    row['new_value'] = row.value
    row['converged'] = True

    # added for trip mc calibration. we kept those coefficients (from melted table) that
    # had formulas as values. those are non ASC coefficients though
    # omit rows that have no value or values as a string formula
    if pd.isna(row.value) or isinstance(row.value, str):
        return row

    # do not adjust parameters that should not be adjusted
    # if row.value > 900 or (pd.isna(row.scaled_target_percent) and pd.isna(row.scaled_model_percent)):
    if row.constrain == 'T' or abs(row.value) > 900 or (pd.isna(row.scaled_target_percent) and pd.isna(row.scaled_model_percent)):
        return row

    # have neither target counts or model counts
    if row.scaled_target_percent == 0 and row.scaled_model_percent == 0:
        row.percent_diff = 0
        row.difference = 0
        return row

    # have model counts but not target counts
    if row.scaled_target_percent == 0 and row.scaled_model_percent > 0:
        row.converged = False
        row.coef_change = -adjust_when_zero_counts
        row.new_value = row.value + row.coef_change
        # if row.value + row.coef_change > -10: 
        #     row.new_value = row.value + row.coef_change
        # else:
        #     row.new_value = -10
        return row

    # have target counts but not model counts
    if row.scaled_target_percent > 0 and row.scaled_model_percent == 0:
        row.converged = False
        row.coef_change = adjust_when_zero_counts
        row.new_value = row.value + row.coef_change
        return row

    # normal calculations for counts in both model and target
    row.percent_diff = (abs(row.scaled_target_percent - row.scaled_model_percent)
                        / row.scaled_target_percent) * 100
    row.coef_change = np.log(row.scaled_target_percent / row.scaled_model_percent) * damping_factor
    if ('WALK' in row.coefficient_name) and ('school' not in row.coefficient_name) and ('univ' not in row.coefficient_name): 
        # walk unavailable > 3 mi, increase coef_change for faster convergence
        row.coef_change = np.log(row.scaled_target_percent / row.scaled_model_percent) * damping_factor * 2

    if abs(row.coef_change) > max_ASC_adjust:
        row.coef_change = max_ASC_adjust if row.coef_change > 0 else -max_ASC_adjust
    row.new_value = row.value + row.coef_change

    if (row.percent_diff > 5):
        row.converged = False
    return row


def calculate_coefficient_change(original_trip_mc_constants_df,
                                 model_calib_target_match_df,
                                 damping_factor,
                                 max_ASC_adjust,
                                 adjust_when_zero_counts,
                                 output_dir):
    # coef_update_df = pd.merge(
    #     original_trip_mc_constants_df,
    #     model_calib_target_match_df,
    #     how='left',
    #     on=['coefficient_name', 'purpose']
    # )
    original_trip_mc_constants_df.to_csv(os.path.join(output_dir, 'original_trip_mc_constants_df.csv'), index=False)
    model_calib_target_match_df.to_csv(os.path.join(output_dir, 'model_calib_target_match_df.csv'), index=False)
    coef_update_df = pd.merge(
        original_trip_mc_constants_df,
        model_calib_target_match_df,
        how='left',
        on='coefficient_name',
    )
    assert str(max_ASC_adjust).isdigit(), "max_ASC_adjust is not numeric"
    # assert str(damping_factor).isdigit(), "damping_factor is not numeric"
    assert str(adjust_when_zero_counts).isdigit(), "adjust_when_zero_counts is not numeric"

    coef_update_df = coef_update_df.apply(
        lambda row: calculate_new_coefficient(
            row, damping_factor, max_ASC_adjust, adjust_when_zero_counts), axis=1)

    coef_update_df.to_csv(os.path.join(output_dir, 'coefficient_updates.csv'), index=False)

    new_config_df = coef_update_df[['coefficient_name', 'new_value', 'constrain']].copy()
    new_config_df.rename(columns={'new_value': 'value'}, inplace=True)
    new_config_df.to_csv(os.path.join(output_dir, 'trip_mode_choice_coefficients.csv'), index=False)

    assert (new_config_df['value'].notna()).all(), f"Missing coefficient values:\n {new_config_df[new_config_df['value'].isna()]}"

    return coef_update_df


def make_trip_mode_choice_comparison_plots(viz_df, purpose):
    fig = plt.figure(figsize=(30, 28))

    # not needed for trip mc plots given we need 3x3 plot grid size
    #plot_idx = 221
    # added for trip mc plots to keep track of grid number
    plot_no = 1
    for tour_mode in output_calibration_tour_modes[:-1]:
        plt.subplot(4, 4, plot_no)
        data = viz_df[(viz_df['tour_mode'] == tour_mode)
                      & (viz_df['trip_mode'] != total_keyword)].copy()
        total_trip_per_source_df = data.groupby('source').sum()
        for i in range(len(total_trip_per_source_df)):
            source = total_trip_per_source_df.index[i]
            total_trips = total_trip_per_source_df.trips[i]
            data.loc[data['source'] == source, 'percent'] = \
                data.loc[data['source'] == source, 'trips'] / total_trips * 100

        sns.barplot(data=data, x='trip_mode', y='percent', hue='source')
        plt.title('Trip Purpose: ' + purpose + ', Tour Mode: ' + tour_mode, fontsize=18)
        plt.xticks(rotation=90, fontsize=13)
        plt.yticks(fontsize=16)
        plt.ylabel('Percent', fontsize=16)
        plt.xlabel('Trip Mode', fontsize=16)
        plot_no += 1

    plt.subplot(4, 4, plot_no)
    data = viz_df[(viz_df['tour_mode'] == total_keyword)
                  & (viz_df['trip_mode'] != total_keyword)].copy()
    total_trip_per_source_df = data.groupby('source').sum()
    for i in range(len(total_trip_per_source_df)):
        source = total_trip_per_source_df.index[i]
        total_trips = total_trip_per_source_df.trips[i]
        data.loc[data['source'] == source, 'percent'] = \
            data.loc[data['source'] == source, 'trips'] / total_trips * 100

    sns.barplot(data=data, x='trip_mode', y='percent', hue='source')
    plt.title('Tour Purpose: ' + purpose + ', Tour Mode: All', fontsize=18)
    plt.xticks(rotation=90, fontsize=13)
    plt.yticks(fontsize=16)
    plt.ylabel('Percent', fontsize=16)
    plt.xlabel('Trip Mode', fontsize=16)

    plt.tight_layout()
    plt.close()
    return fig


def visualize_trip_mode_choice(scaled_tables_1, scaled_tables_2, source_1, source_2, output_dir):
    for i in range(len(scaled_tables_1)):
        df_1 = scaled_tables_1[i]
        df_2 = scaled_tables_2[i]
        assert df_1.name == df_2.name, "Table purposes do not match!"

        viz_df_1 = df_1.reset_index().melt(id_vars=['trip_mode']).rename(
            columns={'variable': 'tour_mode', 'value': 'trips'})
        viz_df_1['source'] = source_1

        viz_df_2 = df_2.reset_index().melt(id_vars=['trip_mode']).rename(
            columns={'variable': 'tour_mode', 'value': 'trips'})
        viz_df_2['source'] = 'Model'

        viz_df = pd.concat([viz_df_1, viz_df_2])

        plot_name = df_1.name + '_' + source_1 + '_' + source_2 + '.png'
        fig = make_trip_mode_choice_comparison_plots(viz_df, df_1.name)

        fig.savefig(os.path.join(output_dir, plot_name))
        # fig.show()
    return fig


def evaluate_coefficient_updates(coef_update_df, output_dir):
    print('Coefficient Statistics: ')
    tot_coef = len(coef_update_df)
    print('\t', tot_coef, 'total coefficients')

    # not needed for trip mc calibration given trip mc coefficients do not have a 'constrain' field
    #num_constrained_coef = len(coef_update_df[
    #    (coef_update_df['constrain'] == 'T')
    #    | (coef_update_df['value'] > 900)])
    #print('\t', num_constrained_coef, 'constrained coefficients')

    num_changed_coef = len(coef_update_df[pd.notna(coef_update_df['coef_change'])])
    print('\t', num_changed_coef, 'coefficients adjusted')
    num_converged_coef = len(coef_update_df[coef_update_df['converged'] == True])
    num_unconverged_coef = len(coef_update_df[coef_update_df['converged'] == False])
    print('\t', num_converged_coef, 'coefficients converged')
    print('\t', num_unconverged_coef, 'coefficients not converged')

    fig = plt.figure(figsize=(20, 7))
    plt.subplot(121)
    coef_update_df[pd.notna(coef_update_df['coef_change'])]['coef_change'].plot(
        kind='hist', bins=50)
    plt.xlabel('Coefficient Change [Utiles]', fontsize=14)
    plt.ylabel('Number of Coefficients', fontsize=14)
    plt.title('Adjustment Factors', fontsize=14)

    plt.subplot(122)
    coef_update_df[coef_update_df['new_value'] > -900]['new_value'].plot(kind='hist', bins=50)
    plt.xlabel('Coefficient Value [Utiles]', fontsize=14)
    plt.ylabel('Number of Coefficients', fontsize=14)
    plt.title('Coefficient Values After Adjustment', fontsize=14)
    plt.savefig(os.path.join(output_dir, 'coef_change.png'))
    plt.close()
    return fig


def display_largest_coefficients(coef_update_df, num_to_display=10):
    coef_update_df['value_size'] = abs(coef_update_df['new_value'])
    top_coef_df = coef_update_df[coef_update_df['value_size'] < 900].sort_values(
        'value_size', ascending=False).head(10)
    # added 'purpose' (i.e. tour purpose) given trip mc coefficients includes it as separate column
    cols_to_display = ['coefficient_name', 'purpose', 'value', 'scaled_model_percent',
                       'scaled_target_percent', 'coef_change', 'new_value', 'converged']
    top_coef_df = top_coef_df[cols_to_display]
    print("Top", num_to_display, "largest coefficients:")
    display(top_coef_df)


def copy_directory(dir_to_copy, copy_location):
    if os.path.exists(copy_location):
        shutil.rmtree(copy_location)
    # copy config files from configs_dir to run dir
    shutil.copytree(dir_to_copy, copy_location)


def launch_activitysim(activitysim_run_command):
    start_time = time.time()
    print("ActivitySim run started at: ", datetime.datetime.now())
    print(activitysim_run_command)
    ret_value = os.system(activitysim_run_command)
    end_time = time.time()
    print("ActivitySim ended at", datetime.datetime.now())
    run_time = round(time.time() - start_time, 2)
    print("Run Time: ", run_time, "secs = ", run_time/60, " mins")
    assert ret_value == 0, "ActivitySim run not completed! See ActivitySim log file for details."

def launch_activitysim_new(activitysim_run_command):
    start_time = time.time()
    print("ActivitySim run started at: ", datetime.datetime.now())
    print(activitysim_run_command)
    
    # Use subprocess.run to capture detailed error information
    result = subprocess.run(
        activitysim_run_command,
        shell=True,
        capture_output=True,
        text=True
    )
    
    # Print stdout and stderr for debugging
    if result.stdout:
        print("STDOUT:", result.stdout)
    if result.stderr:
        print("STDERR:", result.stderr)
    
    end_time = time.time()
    print("ActivitySim ended at", datetime.datetime.now())
    run_time = round(time.time() - start_time, 2)
    print("Run Time: ", run_time, "secs = ", run_time/60, " mins")
    
    assert result.returncode == 0, f"ActivitySim run failed with exit code {result.returncode}. Error: {result.stderr}"
    return


def melt_trip_mc_coef_file(trip_mc_coef_file, output_dir):
    # obtain original original column and row orders
    trip_purpose = trip_mc_coef_file.columns[1:]
    trip_mc_coef_col_order = list(trip_mc_coef_file.columns)
    trip_mc_coef_col_order[0] = 'coefficient_name'
    trip_purpose_check = all(col in trip_mc_coef_col_order for col in trip_purpose)
    assert trip_purpose_check, 'Missing trip purpose!'
    trip_mc_coef_row_order = trip_mc_coef_file['Expression'].copy().rename('coefficient_name')

    # melt trip mc coefficient file
    trip_mc_coef_melted_full = pd.melt(trip_mc_coef_file, id_vars = ['Expression'], value_vars = trip_purpose,
                                  var_name = 'purpose')
    num_rows = len(trip_mc_coef_melted_full)
    trip_mc_coef_melted_full.rename(columns = {'Expression':'coefficient_name'}, inplace = True)

    # include only ASC coefficients and drop commented out coefficients
    trip_mc_coef_melted = trip_mc_coef_melted_full[(trip_mc_coef_melted_full.loc[:,'coefficient_name'].str.contains('_ASC_')) &
                                              (~trip_mc_coef_melted_full.loc[:,'coefficient_name'].str.contains('#'))].copy()
    trip_mc_coef_melted.loc[:,'value'] = pd.to_numeric(trip_mc_coef_melted.loc[:,'value'])

    # creates a dataframe (omit) filled with values that do not contain ASC coefficients
    # will add it back when un-melting the dataframe
    trip_mc_coef_melted_omit = trip_mc_coef_melted_full[~trip_mc_coef_melted_full.index.isin(trip_mc_coef_melted.index)]

    assert len(trip_mc_coef_melted) + len(trip_mc_coef_melted_omit) == num_rows, \
        'Missing trip mode choice coefficients!'

    # added purpose_original to remember which coefficient corresponds to which mode
    # added 'joint' as purpose for non-work, -univ, -school, and -atwork purpose records
    trip_mc_coef_melted.loc[:,'purpose_original'] = trip_mc_coef_melted.loc[:,'purpose']
    joint_purposes = ['escort', 'shopping', 'eatout', 'othmaint', 'social', 'othdiscr']
    trip_mc_coef_melted.loc[trip_mc_coef_melted.loc[:,'coefficient_name'].str.contains('joint_') &
                            trip_mc_coef_melted.loc[:,'purpose'].isin(joint_purposes), 'purpose'] = 'joint'

    #trip_mc_coef_melted.to_csv(os.path.join(output_dir, 'trip_mode_choice_coefficients.csv'), index = False)

    # reset indices
    trip_mc_coef_melted.reset_index(inplace = True, drop = True)
    trip_mc_coef_melted_omit.reset_index(inplace = True, drop = True)

    return trip_mc_coef_melted, trip_mc_coef_melted_omit, trip_mc_coef_col_order, trip_mc_coef_row_order


def unmelt_trip_mc_coef(trip_mc_coef_update, trip_mc_coef_omit,
                        trip_mc_coef_col_order, trip_mc_coef_row_order, output_dir):

    num_rows = len(trip_mc_coef_row_order)

    # obtain new trip mc coefficient values
    trip_mc_coef = trip_mc_coef_update[['coefficient_name', 'purpose_original', 'new_value']].copy()
    trip_mc_coef.rename(columns={'new_value': 'value', 'purpose_original': 'purpose'}, inplace = True)

    # re-add previously removed trip mc coefficient records
    trip_mc_coef_new = trip_mc_coef.append(trip_mc_coef_omit)
    trip_mc_coef_new.reset_index(inplace = True, drop = True)

    # unmelt trip mc coefficient dataframe
    trip_mc_coef_new_unmelted = trip_mc_coef_new.pivot_table(index = 'coefficient_name',
                                                             columns = 'purpose',
                                                             dropna = False,
                                                             aggfunc = lambda x: x)
    trip_mc_coef_new_unmelted.columns = trip_mc_coef_new_unmelted.columns.droplevel()
    trip_mc_coef_new_unmelted.reset_index(inplace = True)

    # rearrange columns and rows per original trip mc coefficient file
    trip_mc_coef_new_unmelted = pd.merge(trip_mc_coef_row_order.to_frame(), trip_mc_coef_new_unmelted,
                                how = 'left', on = 'coefficient_name')
    trip_mc_coef_new_unmelted = trip_mc_coef_new_unmelted[trip_mc_coef_col_order]
    trip_mc_coef_new_unmelted.rename(columns={'coefficient_name':'Expression'}, inplace = True)
    assert len(trip_mc_coef_new_unmelted) == num_rows, 'Missing trip mode choice coefficients!'

    # print
    trip_mc_coef_new_unmelted.to_csv(os.path.join(output_dir, 'trip_mode_choice_coefficients.csv'), index=False)

    return trip_mc_coef_new_unmelted

# -------------------------------------------------------------------------------------------------
# Entry Points
def perform_trip_mode_choice_model_calibration(asim_output_dir,
                                               asim_configs_dir,
                                               trip_mode_choice_calib_targets_file,
                                               max_ASC_adjust,
                                               damping_factor,
                                               adjust_when_zero_counts,
                                               output_dir):

    # ActivitySim model output transformed into trip mode choice calibration format
    print("Processing ActivitySim output tables for trip mode choice calibration...")
    asim_trip_mc_tables, num_trips_full_model, asim_trips_df = \
        process_asim_tables_for_trip_mode_choice(asim_output_dir)

    # trip mode choice constants read from config file
    print("Reading trip mode choice coefficient file...")
    original_trip_mc_constants_df = read_trip_mode_choice_constants(asim_configs_dir)

    # melt trip mode choice coefficient file
    # original_trip_mc_constants_df, original_trip_mc_constants_df_omit, \
    #     trip_mc_coef_col_order, trip_mc_coef_row_order = \
    #     melt_trip_mc_coef_file(original_trip_mc_constants_df, output_dir)

    # Calibration targets from survey data
    print("Reading trip mode choice calibration target file...")
    trip_mc_calib_target_tables = read_trip_mode_choice_calibration_file(
        trip_mode_choice_calib_targets_file)

    print("Scaling calibration targets to match model output...")
    full_trip_mc_calib_target_tables, full_asim_trip_mc_tables = scale_targets_to_match_model_by_tour_mode(
        trip_mc_calib_target_tables,
        asim_trip_mc_tables,
        full_model_trips=num_trips_full_model,
    )

    scaled_trip_mc_calib_target_tables, scaled_asim_trip_mc_tables = calculate_share_by_tour_mode(
        full_trip_mc_calib_target_tables,
        full_asim_trip_mc_tables,
    )

    write_scaled_targets_to_excel(
        unscaled_model_tables=asim_trip_mc_tables,
        unscaled_calib_tables=trip_mc_calib_target_tables,
        full_model_tables=full_asim_trip_mc_tables,
        full_calib_tables=full_trip_mc_calib_target_tables,
        scaled_model_tables=scaled_asim_trip_mc_tables,
        scaled_calib_tables=scaled_trip_mc_calib_target_tables,
        output_dir=output_dir)

    # Compare calibration targets to model outputs
    all_purposes_fig = visualize_trip_mode_choice(
        scaled_tables_1=scaled_trip_mc_calib_target_tables,
        scaled_tables_2=scaled_asim_trip_mc_tables,
        source_1='Survey',
        source_2='Model',
        output_dir=output_dir)
    display(all_purposes_fig)

    # Match model counts and calibration targets to model coefficients
    model_calib_target_match_df = match_model_and_calib_targets_to_coefficients(
        original_trip_mc_constants_df,
        scaled_asim_trip_mc_tables,
        scaled_trip_mc_calib_target_tables)

    # Update model coefficients
    coef_update_df = calculate_coefficient_change(
        original_trip_mc_constants_df,
        model_calib_target_match_df,
        damping_factor=damping_factor,
        max_ASC_adjust=max_ASC_adjust,
        adjust_when_zero_counts=adjust_when_zero_counts,
        output_dir=output_dir
    )

    # unmelt new trip mode choice coefficient file
    # unmelt_trip_mc_coef(coef_update_df, original_trip_mc_constants_df_omit,
    #                     trip_mc_coef_col_order, trip_mc_coef_row_order, output_dir)

    coef_hists = evaluate_coefficient_updates(coef_update_df, output_dir)
    display(coef_hists)

    display_largest_coefficients(coef_update_df)

    # write scaled calibration targets for HTML visualizer
    write_visualizer_calibration_target_table(full_trip_mc_calib_target_tables, asim_trips_df, output_dir)

    return coef_update_df


def run_activitysim(data_dir,
                    skims_dir,
                    simpy_dir,
                    configs_resident_dir,
                    configs_common_dir,
                    run_dir,
                    output_dir,
                    settings_file=None,
                    trip_mc_coef_file=None):
    assert os.path.exists(configs_resident_dir), "configs_resident not found!"
    assert os.path.exists(configs_common_dir), "configs_common not found!"
    assert os.path.exists(data_dir), "data_dir not found!"

    if not os.path.exists(output_dir):
        print("creating output_dir at", output_dir)
        os.mkdir(output_dir)

    # creating new config folder(s) in run directory
    run_config_resident_dir = os.path.join(run_dir, 'configs')
    copy_directory(dir_to_copy=configs_resident_dir, copy_location=run_config_resident_dir)
    
    # optional copy of settings and coefficient file
    if settings_file is not None:
        shutil.copyfile(settings_file, os.path.join(run_config_resident_dir, 'settings_mp.yaml'))
    if trip_mc_coef_file is not None:
        shutil.copyfile(trip_mc_coef_file, os.path.join(run_config_resident_dir, 'trip_mode_choice_coefficients.csv'))

    activitysim_run_command = 'python ' + simpy_dir + ' -s ' + settings_file + ' -c ' + run_config_resident_dir \
        + ' -c ' + configs_common_dir + ' -d ' + data_dir + ' -d ' + skims_dir + ' -o ' + run_dir

    launch_activitysim_new(activitysim_run_command)

    activitysim_output_tables = [
        'final_households.csv',
        'final_persons.csv',
        'final_tours.csv',
        'final_trips.csv',
        'final_joint_tour_participants.csv',
        'activitysim.log',
    ]

    for asim_table in activitysim_output_tables:
        if os.path.exists(os.path.join(run_dir, asim_table)):
            shutil.copyfile(os.path.join(run_dir, asim_table), os.path.join(output_dir, asim_table))

    return

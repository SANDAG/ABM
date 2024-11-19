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
sns.set()

# --------------------------------------------------------------------------------------------------
# dictionary mappings
total_keyword = 'All'
output_calibration_tour_purposes = [
    'work',
    'univ',
    'school',
    'atwork',
    'ind_maint',
    'ind_discr',
    'joint',
    total_keyword  # last entry should be for all purposes
]

output_calibration_modes = [
    'DRIVEALONE',
    'SHARED2',
    'SHARED3',
    'WALK',
    'BIKE',
    'ESCOOTER',
    'EBIKE',
    'WALK-TRANSIT',
    'PNR-TRANSIT',
    'KNR-TRANSIT',
    'TNC-TRANSIT',
    'SCHOOLBUS',
    'TNC_SINGLE',
    'TNC_SHARED',
    'TAXI',    
    total_keyword]

# transit calibraiton modes are not scaled when comparing to model
output_calibration_transit_modes = [
    'WALK-TRANSIT',
    'PNR-TRANSIT',
    'KNR-TRANSIT'
]

# should match the mode choice coefficient names
output_auto_suff = [
    'zeroautohh',
    'autodeficienthh',
    'autosufficienthh'
]

# Mapping calibration mode to tour mode choice coefficient names
output_calibration_mode_to_coef_name_dict = {
    'DRIVEALONE': 'DRIVEALONE',
    'SHARED2': 'SHARED2',
    'SHARED3': 'SHARED3',
    'WALK': 'WALK',
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK-TRANSIT': 'WALK_TRANSIT',
    'PNR-TRANSIT': 'PNR_TRANSIT',
    'KNR-TRANSIT': 'KNR_TRANSIT',
    'TNC-TRANSIT': 'TNC_TRANSIT',
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

output_calibration_autosuff_to_coef_names_indiv_dict = {
    'zeroautohh': 'zeroautohhindivtou',
    'autodeficienthh': 'autodeficienthhind',
    'autosufficienthh': 'autosufficienthhin'
}

output_calibration_autosuff_to_coef_names_joint_dict = {
    'zeroautohh': 'zeroautohhjointtou',
    'autodeficienthh': 'autodeficienthhjoi',
    'autosufficienthh': 'autosufficienthhjo'
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

asim_to_calib_tour_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE',
    'SHARED2': 'SHARED2',
    'SHARED3': 'SHARED3',
    'WALK': 'WALK',
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK_LOC': 'WALK-TRANSIT',
    'WALK_PRM': 'WALK-TRANSIT',
    'WALK_MIX': 'WALK-TRANSIT',
    'PNR_LOC': 'PNR-TRANSIT',
    'PNR_PRM': 'PNR-TRANSIT',
    'PNR_MIX': 'PNR-TRANSIT',
    'KNR_LOC': 'KNR-TRANSIT',
    'KNR_PRM': 'KNR-TRANSIT',
    'KNR_MIX': 'KNR-TRANSIT',
    'TNC_LOC': 'TNC-TRANSIT',
    'TNC_PRM': 'TNC-TRANSIT',
    'TNC_MIX': 'TNC-TRANSIT',
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
# keys should match the names of the csv files in the calibration targets directory
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

survey_to_calib_tour_mode_dict = {
    'DRIVEALONE': 'DRIVEALONE', 
    'SHARED2': 'SHARED2', 
    'SHARED3': 'SHARED3', 
    'WALK': 'WALK', 
    'BIKE': 'BIKE',
    'ESCOOTER': 'ESCOOTER',
    'EBIKE': 'EBIKE',
    'WALK-TRANSIT': 'WALK-TRANSIT', 
    'PNR-TRANSIT': 'PNR-TRANSIT', 
    'KNR-TRANSIT': 'KNR-TRANSIT', 
    'TNC-TRANSIT': 'TNC-TRANSIT',
    'TAXI': 'TAXI', 
    'TNC-REG': 'TNC_SINGLE', 
    'TNC-SHARED': 'TNC_SHARED', 
    'SCHOOLBUS': 'SCHOOLBUS',
    'All': total_keyword
}

survey_calib_target_auto_suff_dict = {
    '0': output_auto_suff[0],
    '1': output_auto_suff[1],
    '2': output_auto_suff[2]
}

survey_table_purpose_col = 'purpose'
survey_table_mode_col = 'grouped_tour_mode'
survey_table_auto_suff_col = 'auto_suff'
survey_table_tours_col = 'tours'

# HTML visualizer dictionaries.  needs to match HTS summary script.
#  need to be checked only if the scaled calibration targets want to be included in the visualizer
calib_tour_mode_to_vis_dict = {
    # calibration_tour_mode: visualizer_tour_mode
    'DRIVEALONE': 1,
    'SHARED2': 2,
    'SHARED3': 3,
    'WALK': 4,
    'BIKE': 5,
    'WALK-TRANSIT': 6,
    'PNR-TRANSIT': 7,
    'KNR-TRANSIT': 8,
    'TNC-TRANSIT': 9,
    'TAXI': 10,
    'TNC_SINGLE': 11,
    'TNC_SHARED':12,
    'SCHOOLBUS': 13,
    'ESCOOTER': 14, ### FIX_IT: The number for ESCOOTER and EBIKE are temporary, need to decide and finalize them later
    'EBIKE': 15,
    total_keyword: 'Total'
}
purpose_vis_dict = {
    # calibration purpose: visualizer purpose
    'univ': 'univ',
    'school': 'sch',
    'work': 'work',
    'atwork': 'atwork',
    'ind_discr': 'idisc',
    'ind_maint': 'imain',
    'joint': 'jmain',
    'jdisc': 'jdisc',  # joint is copied to produce jdisc purpose in the code
    total_keyword: 'Total'
}


# --------------------------------------------------------------------------------------------------
# Helper functions
def check_input_dictionaries_for_consistency():
    # checking tour purposes
    for purpose in list(asim_to_calib_purpose_dict.values()):
        assert purpose in output_calibration_tour_purposes, "ActivitySim purpose not in calibration"
    for purpose in list(survey_to_calib_purposes_dict.values()):
        assert purpose in output_calibration_tour_purposes, "Survey purpose not in calibration"

    # checking tour mode
    for mode in list(asim_to_calib_tour_mode_dict.values()):
        assert mode in output_calibration_modes, "ActivitySim mode not in calibration"
    for mode in list(survey_to_calib_tour_mode_dict.values()):
        assert mode in output_calibration_modes, "Survey mode not in calibration"

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
    worksheet.write(start_row, start_col, title)
    worksheet.write(start_row+1, start_col, dfs[0].name)
    worksheet.write(start_row+1, start_col + sep_for_col_title, col_title)
    start_row += len(dfs[0]) + 6

    for df in dfs[1:]:
        df.to_excel(excel_writer, excel_sheet_name, startrow=start_row, startcol=start_col)
        worksheet.write(start_row-1, start_col, df.name)
        worksheet.write(start_row-1, start_col + sep_for_col_title, col_title)
        start_row += len(df) + 4
    return


def map_asim_tour_table_to_calib(tours_df):
    tours_df['calib_tour_purpose'] = tours_df['primary_purpose'].apply(
        lambda x: asim_to_calib_purpose_dict[x])

    tours_df.loc[(tours_df['calib_tour_purpose'] == 'ind_maint')
                 & (tours_df['tour_category'] == 'joint'), 'calib_tour_purpose'] = 'joint'
    tours_df.loc[(tours_df['calib_tour_purpose'] == 'ind_discr')
                 & (tours_df['tour_category'] == 'joint'), 'calib_tour_purpose'] = 'joint'

    tours_df['calib_tour_mode'] = tours_df['tour_mode'].apply(
        lambda x: asim_to_calib_tour_mode_dict[x])

    return tours_df


def create_asim_tour_mode_choice_tables(tours_df):
    asim_tour_mode_purpose_cts = []

    columns = list(asim_auto_suff_dict.keys())
    columns.append(total_keyword)

    for tour_purpose in output_calibration_tour_purposes[:-1]:
        asim_tour_mode_purpose_ct = pd.crosstab(
            tours_df['calib_tour_mode'],
            tours_df[tours_df['calib_tour_purpose'] == tour_purpose]['auto_suff'],
            margins=True,
            margins_name=total_keyword,
            dropna=False
        )
        asim_tour_mode_purpose_ct = asim_tour_mode_purpose_ct.reindex(
            index=output_calibration_modes, columns=columns, fill_value=0)
        asim_tour_mode_purpose_ct.index.name = 'tour_mode'
        asim_tour_mode_purpose_ct.rename(columns=asim_auto_suff_dict, inplace=True)
        asim_tour_mode_purpose_ct.name = tour_purpose
        asim_tour_mode_purpose_cts.append(asim_tour_mode_purpose_ct)

    asim_tour_mode_all_ct = pd.crosstab(
        tours_df['calib_tour_mode'],
        tours_df['auto_suff'],
        margins=True,
        margins_name=total_keyword,
        dropna=False
    )
    asim_tour_mode_all_ct = asim_tour_mode_all_ct.reindex(
        index=output_calibration_modes, columns=columns, fill_value=0)
    asim_tour_mode_all_ct.index.name = 'tour_mode'
    asim_tour_mode_all_ct.rename(columns=asim_auto_suff_dict, inplace=True)
    asim_tour_mode_all_ct.name = output_calibration_tour_purposes[-1]
    asim_tour_mode_purpose_cts.append(asim_tour_mode_all_ct)
    return asim_tour_mode_purpose_cts


def process_asim_tables_for_tour_mode_choice(asim_tables_dir):
    households_df = pd.read_csv(
        os.path.join(asim_tables_dir, 'final_households.csv'), low_memory=False)
    sample_rate = households_df['sample_rate'].max()
    tours_df = pd.read_csv(os.path.join(asim_tables_dir, 'final_tours.csv'), low_memory=False)
    households_df['auto_suff'] = 0
    households_df.loc[(households_df.auto_ownership < households_df.num_adults)
                      & (households_df.auto_ownership > 0), 'auto_suff'] = 1
    households_df.loc[(households_df.auto_ownership >= households_df.num_adults)
                      & (households_df.auto_ownership > 0), 'auto_suff'] = 2

    tours_df = pd.merge(
        tours_df,
        households_df[['household_id', 'auto_suff']],
        how='left',
        on='household_id'
    )

    tours_df = map_asim_tour_table_to_calib(tours_df)
    asim_tour_mode_tables = create_asim_tour_mode_choice_tables(tours_df)

    sample_rate = households_df['sample_rate'].max()
    total_model_tours = len(tours_df)
    num_tours_full_model = int(total_model_tours / sample_rate)
    print("Sample rate of ", sample_rate, "results in ", total_model_tours, "out of",
          num_tours_full_model, "tours")

    return asim_tour_mode_tables, total_model_tours, num_tours_full_model


def read_tour_mode_choice_calibration_file(tour_mode_choice_calib_targets_file):
    tour_mc_calib_df = pd.read_csv(tour_mode_choice_calib_targets_file)
    tour_mc_calib_df['calib_tour_mode'] = tour_mc_calib_df[survey_table_mode_col].apply(
        lambda x: survey_to_calib_tour_mode_dict[x])

    columns = list(survey_calib_target_auto_suff_dict.keys())
    columns.append(total_keyword)

    tour_mc_calib_target_tables = []

    for purpose in list(survey_to_calib_purposes_dict.keys()):
        df = tour_mc_calib_df[(tour_mc_calib_df[survey_table_purpose_col] == purpose)
                              & (tour_mc_calib_df[survey_table_mode_col] != total_keyword)
                              & (tour_mc_calib_df[survey_table_auto_suff_col] != total_keyword)]

        df_ct = pd.crosstab(
            df['calib_tour_mode'],
            df[survey_table_auto_suff_col],
            values=df[survey_table_tours_col],
            aggfunc='sum',
            margins=True,
            margins_name=total_keyword,
            dropna=False,
        )
        df_ct = df_ct.reindex(index=output_calibration_modes, columns=columns, fill_value=0)
        df_ct.index.name = 'tour_mode'
        df_ct.rename(columns=survey_calib_target_auto_suff_dict, inplace=True)
        df_ct.name = survey_to_calib_purposes_dict[purpose]

        # looking to see if calibration purposes need to be combined
        # e.g. joint = joint_ind + joint_maint
        for i in range(len(tour_mc_calib_target_tables)):
            prev_df_ct = tour_mc_calib_target_tables[i]
            if prev_df_ct.name == df_ct.name:
                prev_df_ct = prev_df_ct + df_ct
                prev_df_ct.name = df_ct.name  # reassignment does not preserve name
                tour_mc_calib_target_tables[i] = prev_df_ct
                break
        else:  # only enters if above for loop breaks
            tour_mc_calib_target_tables.append(df_ct)

    return tour_mc_calib_target_tables


def read_tour_mode_choice_constants(configs_dir, coef_file="tour_mode_choice_coefficients.csv"):
    constants_config_path = os.path.join(configs_dir, coef_file)
    tour_mc_constants_df = pd.read_csv(constants_config_path)
    return tour_mc_constants_df


def write_scaled_targets_to_excel(unscaled_model_tables,
                                  unscaled_calib_tables,
                                  scaled_model_tables,
                                  scaled_calib_tables,
                                  output_dir):
    excel_writer = pd.ExcelWriter(os.path.join(output_dir, 'scaled_targets.xlsx'))
    # unscaled model
    write_tables_to_excel(
        dfs=unscaled_model_tables,
        excel_writer=excel_writer,
        excel_sheet_name='tour_mode_choice',
        start_row=0,
        start_col=0,
        title='Unscaled Model Tours',
        sep_for_col_title=3,
        col_title='Auto Sufficiency'
    )
    # unscaled calibration targets
    write_tables_to_excel(
        dfs=unscaled_calib_tables,
        excel_writer=excel_writer,
        excel_sheet_name='tour_mode_choice',
        start_row=0,
        start_col=7,
        title='Unscaled Calibration Tours',
        sep_for_col_title=3,
        col_title='Auto Sufficiency'
    )
    # scaled tar
    write_tables_to_excel(
        dfs=scaled_model_tables,
        excel_writer=excel_writer,
        excel_sheet_name='tour_mode_choice',
        start_row=0,
        start_col=14,
        title='Scaled Model Tours',
        sep_for_col_title=3,
        col_title='Auto Sufficiency'
    )
    # scaled calibration targets
    write_tables_to_excel(
        dfs=scaled_calib_tables,
        excel_writer=excel_writer,
        excel_sheet_name='tour_mode_choice',
        start_row=0,
        start_col=21,
        title='Scaled Calibration Tours',
        sep_for_col_title=3,
        col_title='Auto Sufficiency'
    )
    excel_writer.save()
    excel_writer.close()


def scale_targets_to_match_model(tour_mc_calib_target_tables,
                                 asim_tour_mc_tables,
                                 full_model_tours,
                                 output_dir):

    assert asim_tour_mc_tables[-1].name == total_keyword, "Last table is not total!"
    total_model_tours = asim_tour_mc_tables[-1].loc[total_keyword, total_keyword]
    model_scale_factor = full_model_tours / total_model_tours

    scaled_tour_mc_calib_target_tables = []
    scaled_asim_tour_mc_tables = []
    total_model_df = None
    total_calib_df = None

    # iterating over all non-total tables
    for i in range(len(tour_mc_calib_target_tables) - 1):
        model_df = asim_tour_mc_tables[i]
        calib_target_df = tour_mc_calib_target_tables[i]
        assert model_df.name == calib_target_df.name, "Tables do not match!"

        # counts from model run are scaled to match full model run counts
        scaled_model_df = model_df * model_scale_factor

        # calibration counts need to match full model run counts, but leave transit tours unscaled
        scaled_calib_target_df = calib_target_df.copy()
        no_transit_idxs = ~(scaled_calib_target_df.index.isin(output_calibration_transit_modes))  ### Note: Scaling correction for OBS data

        for auto_suff in output_auto_suff:
            model_tours = scaled_model_df.loc[total_keyword, auto_suff]
            calib_tours = scaled_calib_target_df.loc[total_keyword, auto_suff]
            transit_calib_tours = scaled_calib_target_df.loc[~no_transit_idxs, auto_suff].sum()   ### Note: Scaling correction for OBS data
            scaling_factor = ((model_tours - transit_calib_tours) 
                              / (calib_tours - transit_calib_tours))
            # scaling_factor = ((model_tours) 
            #                   / (calib_tours))

            # only applying scaling factor to non-transit tours
            scaled_calib_target_df.loc[no_transit_idxs, auto_suff] = \
                scaled_calib_target_df.loc[no_transit_idxs, auto_suff] * scaling_factor   ### Note: Scaling correction for OBS data
            # scaled_calib_target_df.loc[:, auto_suff] = scaled_calib_target_df.loc[:, auto_suff] * scaling_factor   

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

        scaled_tour_mc_calib_target_tables.append(scaled_calib_target_df)
        scaled_asim_tour_mc_tables.append(scaled_model_df)

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
    scaled_asim_tour_mc_tables.append(total_model_df)
    scaled_tour_mc_calib_target_tables.append(total_calib_df)

    write_scaled_targets_to_excel(
        unscaled_model_tables=asim_tour_mc_tables,
        unscaled_calib_tables=tour_mc_calib_target_tables,
        scaled_model_tables=scaled_asim_tour_mc_tables,
        scaled_calib_tables=scaled_tour_mc_calib_target_tables,
        output_dir=output_dir)

    return scaled_tour_mc_calib_target_tables, scaled_asim_tour_mc_tables


def melt_df(df, melt_id_var, value_name):
    melted_df = df.reset_index().melt(id_vars=[melt_id_var])
    melted_df.rename(columns={'value': value_name}, inplace=True)
    melted_df['purpose'] = df.name
    return melted_df


def write_visualizer_calibration_target_table(scaled_tour_mc_calib_target_tables, output_dir):
    melted_dfs = []
    for df in scaled_tour_mc_calib_target_tables:
        if df.name == 'joint':
            # create an extra joint Discretionary table for visualizer
            df_copy = df.copy()
            df_copy.name = 'jdisc'
            melted_df_copy = melt_df(df_copy, melt_id_var='tour_mode', value_name='tours')
            melted_dfs.append(melted_df_copy)
        melted_df = melt_df(df, melt_id_var='tour_mode', value_name='tours')
        melted_dfs.append(melted_df)
    tour_mode_choice_calibration_table = pd.concat(melted_dfs)

    tour_mc_vis = tour_mode_choice_calibration_table.copy()
    tour_mc_vis = tour_mc_vis[tour_mc_vis['tour_mode'] != 'All']
    tour_mc_vis['id'] = tour_mc_vis['tour_mode'].apply(lambda x: calib_tour_mode_to_vis_dict[x])
    tour_mc_vis['purpose'] = tour_mc_vis['purpose'].apply(lambda x: purpose_vis_dict[x])
    tour_mc_vis = tour_mc_vis.reset_index(drop=True).pivot_table(
        columns='auto_suff', values='tours', index=['id', 'purpose']).reset_index()
    tour_mc_vis = tour_mc_vis.rename(
        columns={output_auto_suff[0]: 'freq_as0',
                 output_auto_suff[1]: 'freq_as1',
                 output_auto_suff[2]: 'freq_as2',
                 total_keyword: 'freq_all'})
    col_order = ['id', 'purpose', 'freq_as0', 'freq_as1', 'freq_as2', 'freq_all']
    tour_mc_vis[col_order].to_csv(
        os.path.join(output_dir, 'tmodeProfile_vis_calib.csv'), index=False)

    return


def match_model_and_calib_targets_to_coefficients(original_tour_mc_constants_df,
                                                  scaled_asim_tour_mc_tables,
                                                  scaled_tour_mc_calib_target_tables):

    columns = ['coefficient_name', 'purpose', 'mode', 'auto_suff', 'model_counts', 'target_counts']
    model_calib_target_match_df = pd.DataFrame(columns=columns)

    for i, purpose in enumerate(output_calibration_tour_purposes):
        asim_tour_mc_table = scaled_asim_tour_mc_tables[i]
        calib_target_mc_table = scaled_tour_mc_calib_target_tables[i]
        assert asim_tour_mc_table.name == purpose, "Purpose doesn't match!"
        assert calib_target_mc_table.name == purpose, "Purpose doesn't match!"

        for mode in output_calibration_modes:
            for auto_suff in output_auto_suff:

                asim_mc_count = int(asim_tour_mc_table.loc[mode, auto_suff])
                calib_target_count = int(calib_target_mc_table.loc[mode, auto_suff])

                # multiple coefficients match a single calibration purpose.
                # e.g. ind_maint = shopping and escort, both are adjusted the same amount
                for coef_purpose in output_calibration_purposes_to_coef_names_dict[purpose]:
                    coef_mode = output_calibration_mode_to_coef_name_dict[mode]
                    coef_name = 'coef_calib_' + output_calibration_autosuff_to_coef_names_indiv_dict[auto_suff] + '_' + coef_mode + '_' + coef_purpose
                    #target_name = mode + '_' + auto_suff + '_' + purpose ### DELETE

                    if purpose == 'joint':
                        coef_name = 'coef_calib_' + output_calibration_autosuff_to_coef_names_joint_dict[auto_suff] + '_' + coef_mode + '_' + coef_purpose

                    row = [coef_name, coef_purpose, coef_mode, auto_suff,
                           asim_mc_count, calib_target_count]
                    model_calib_target_match_df.loc[len(model_calib_target_match_df)] = row

    return model_calib_target_match_df


def calculate_new_coefficient(row, damping_factor, max_ASC_adjust, adjust_when_zero_counts):
    row['difference'] = row.target_counts - row.model_counts
    row['percent_diff'] = pd.NA
    row['coef_change'] = pd.NA
    row['new_value'] = row.value
    row['converged'] = True

    # do not adjust parameters that should not be adjusted
    if row.constrain == 'T' or row.value > 900 or (pd.isna(row.target_counts) and pd.isna(row.model_counts)):
        return row

    # have neither target counts or model counts
    if row.target_counts == 0 and row.model_counts == 0:
        row.percent_diff = 0
        row.difference = 0
        return row

    # have model counts but not target counts, do not adjust coefficient
    if row.target_counts == 0 and row.model_counts > 0:
        if row.model_counts > 100:
            row.converged = False
            # only adjusting when not converged
            row.coef_change = -adjust_when_zero_counts
            row.new_value = row.value + row.coef_change
        return row

    # have target counts but not model counts, also do not adjust coefficient
    if row.target_counts > 0 and row.model_counts == 0:
        if row.target_counts > 100:
            row.converged = False
            # only adjusting when not converged
            row.coef_change = adjust_when_zero_counts
            row.new_value = row.value + row.coef_change
        return row

    # normal calculations for counts in both model and target
    row.percent_diff = (abs(row.target_counts - row.model_counts)
                        / row.target_counts) * 100
    # row.coef_change = np.log(row.target_counts / row.model_counts) * damping_factor ### DELETE

    # if abs(row.coef_change) > max_ASC_adjust:
    #     if row.coef_change > 0:
    #         row.coef_change = max_ASC_adjust
    #     else:
    #         row.coef_change = -1*max_ASC_adjust
    # row.new_value = row.value + row.coef_change

    if (row.percent_diff > 5) and (abs(row.difference) > 100):
        row.converged = False

        row.coef_change = np.log(row.target_counts / row.model_counts) * damping_factor

        if abs(row.coef_change) > max_ASC_adjust:
            if row.coef_change > 0:
                row.coef_change = max_ASC_adjust
            else:
                row.coef_change = -1*max_ASC_adjust
        row.new_value = row.value + row.coef_change

    return row


def calculate_coefficient_change(original_tour_mc_constants_df,
                                 model_calib_target_match_df,
                                 damping_factor,
                                 max_ASC_adjust,
                                 adjust_when_zero_counts,
                                 output_dir):
    coef_update_df = pd.merge(
        original_tour_mc_constants_df,
        model_calib_target_match_df,
        how='left',
        on='coefficient_name'
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
    new_config_df.to_csv(os.path.join(output_dir, 'tour_mode_choice_coefficients.csv'), index=False)

    assert (new_config_df['value'].notna()).all(), "Missing coefficient values"

    return coef_update_df


def make_tour_mode_choice_comparison_plots(viz_df, purpose):
    fig = plt.figure(figsize=(20, 15))

    plot_idx = 221
    for auto_suff in output_auto_suff:
        plt.subplot(plot_idx)
        data = viz_df[(viz_df['auto_suff'] == auto_suff)
                      & (viz_df['tour_mode'] != total_keyword)].copy()
        total_tour_per_source_df = data.groupby('source').sum()
        for i in range(len(total_tour_per_source_df)):
            source = total_tour_per_source_df.index[i]
            total_tours = total_tour_per_source_df.tours[i]
            data.loc[data['source'] == source, 'percent'] = \
                data.loc[data['source'] == source, 'tours'] / total_tours * 100

        sns.barplot(data=data, x='tour_mode', y='percent', hue='source')
        plt.title('Tour Purpose: ' + purpose + ', Auto Sufficiency: ' + auto_suff, fontsize=18)
        plt.xticks(rotation=90, fontsize=13)
        plt.yticks(fontsize=16)
        plt.ylabel('Percent', fontsize=16)
        plt.xlabel('Tour Mode', fontsize=16)
        plot_idx += 1

    plt.subplot(plot_idx)
    data = viz_df[(viz_df['auto_suff'] == total_keyword)
                  & (viz_df['tour_mode'] != total_keyword)].copy()
    total_tour_per_source_df = data.groupby('source').sum()
    for i in range(len(total_tour_per_source_df)):
        source = total_tour_per_source_df.index[i]
        total_tours = total_tour_per_source_df.tours[i]
        data.loc[data['source'] == source, 'percent'] = \
            data.loc[data['source'] == source, 'tours'] / total_tours * 100

    sns.barplot(data=data, x='tour_mode', y='percent', hue='source')
    plt.title('Tour Purpose: ' + purpose + ', Auto Sufficiency: All', fontsize=18)
    plt.xticks(rotation=90, fontsize=13)
    plt.yticks(fontsize=16)
    plt.ylabel('Percent', fontsize=16)
    plt.xlabel('Tour Mode', fontsize=16)

    plt.tight_layout()
    plt.close()
    return fig


def visualize_tour_mode_choice(scaled_tables_1, scaled_tables_2, source_1, source_2, output_dir):
    for i in range(len(scaled_tables_1)):
        df_1 = scaled_tables_1[i]
        df_2 = scaled_tables_2[i]
        assert df_1.name == df_2.name, "Table purposes do not match!"

        viz_df_1 = df_1.reset_index().melt(id_vars=['tour_mode']).rename(
            columns={'variable': 'auto_suff', 'value': 'tours'})
        viz_df_1['source'] = source_1

        viz_df_2 = df_2.reset_index().melt(id_vars=['tour_mode']).rename(
            columns={'variable': 'auto_suff', 'value': 'tours'})
        viz_df_2['source'] = 'Model'

        viz_df = pd.concat([viz_df_1, viz_df_2])

        plot_name = df_1.name + '_' + source_1 + '_' + source_2 + '.png'
        fig = make_tour_mode_choice_comparison_plots(viz_df, df_1.name)

        fig.savefig(os.path.join(output_dir, plot_name))
        # fig.show()
    return fig


def evaluate_coefficient_updates(coef_update_df, output_dir):
    print('Coefficient Statistics: ')
    tot_coef = len(coef_update_df)
    print('\t', tot_coef, 'total coefficients')
    num_constrained_coef = len(coef_update_df[
        (coef_update_df['constrain'] == 'T')
        | (coef_update_df['value'] > 900)])
    print('\t', num_constrained_coef, 'constrained coefficients')
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
    coef_update_df[abs(coef_update_df['new_value']) < 900]['new_value'].plot(kind='hist', bins=50)
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
    cols_to_display = ['coefficient_name', 'value', 'model_counts',
                       'target_counts', 'coef_change', 'new_value', 'converged']
    top_coef_df = top_coef_df[cols_to_display]
    print("Top", num_to_display, "largest coefficients:")
    display(top_coef_df)
    return


def copy_directory(dir_to_copy, copy_location):
    if os.path.exists(copy_location):
        shutil.rmtree(copy_location)
    # copy config files from configs_dir to run dir
    shutil.copytree(dir_to_copy, copy_location)
    return


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
    return


# -------------------------------------------------------------------------------------------------
# Entry Points
def perform_tour_mode_choice_model_calibration(asim_output_dir,
                                               asim_configs_dir,
                                               tour_mode_choice_calib_targets_file,
                                               max_ASC_adjust,
                                               damping_factor,
                                               adjust_when_zero_counts,
                                               output_dir):
    # ActivitySim model output transformed into tour mode choice calibration format
    asim_tour_mc_tables, asim_model_tours, num_tours_full_model = \
        process_asim_tables_for_tour_mode_choice(asim_output_dir)

    # tour mode choice constants read from config file
    original_tour_mc_constants_df = read_tour_mode_choice_constants(asim_configs_dir)

    # Calibration targets from survey data
    # tour_mc_calib_target_tables = read_tour_mode_choice_calibration_tables(
    #     tour_mc_calib_targets_dir)

    tour_mc_calib_target_tables = read_tour_mode_choice_calibration_file(
        tour_mode_choice_calib_targets_file)

    if num_tours_full_model is None:
        num_tours_full_model = asim_model_tours

    # Scale calibration and model targets to match num_tours_full_model
    scaled_tour_mc_calib_target_tables, scaled_asim_tour_mc_tables = scale_targets_to_match_model(
        tour_mc_calib_target_tables,
        asim_tour_mc_tables,
        full_model_tours=num_tours_full_model,
        output_dir=output_dir
    )

    # write scaled calibration targets for HTML visualizer
    write_visualizer_calibration_target_table(scaled_tour_mc_calib_target_tables, output_dir)

    # Compare calibration targets to model outputs
    all_purposes_fig = visualize_tour_mode_choice(
        scaled_tables_1=scaled_tour_mc_calib_target_tables,
        scaled_tables_2=scaled_asim_tour_mc_tables,
        source_1='Survey',
        source_2='Model',
        output_dir=output_dir)
    display(all_purposes_fig)

    # Match model counts and calibration targets to model coefficients
    model_calib_target_match_df = match_model_and_calib_targets_to_coefficients(
        original_tour_mc_constants_df,
        scaled_asim_tour_mc_tables,
        scaled_tour_mc_calib_target_tables)

    # Update model coefficients
    coef_update_df = calculate_coefficient_change(
        original_tour_mc_constants_df,
        model_calib_target_match_df,
        damping_factor=damping_factor,
        max_ASC_adjust=max_ASC_adjust,
        adjust_when_zero_counts=adjust_when_zero_counts,
        output_dir=output_dir
    )

    coef_hists = evaluate_coefficient_updates(coef_update_df, output_dir)
    display(coef_hists)

    display_largest_coefficients(coef_update_df)

    return coef_update_df


def run_activitysim(data_dir,
                    configs_resident_dir,
                    configs_common_dir,
                    run_dir,
                    output_dir,
                    settings_file=None,
                    tour_mc_coef_file=None):
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
    if tour_mc_coef_file is not None:
        shutil.copyfile(tour_mc_coef_file, os.path.join(run_config_resident_dir, 'tour_mode_choice_coefficients.csv'))

    activitysim_run_command = 'python simulation.py -s ' + settings_file + ' -c ' + run_config_resident_dir \
        + ' -c ' + configs_common_dir + ' -d ' + data_dir + ' -o ' + run_dir

    launch_activitysim(activitysim_run_command)

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

# -*- coding: utf-8 -*-
"""
Heavy Truck Model
FAF Annual Tonnage to Truck Number by TAZ Disaggregation

Author: Maddie Hasani, Fehr & Peers <br/>
Reviewer: Fatemeh Ranaiefar, Fehr & Peers<br/>
Last update: 2/1/2024
"""

## REQUIRED LIBRARIES
import pandas as pd
import numpy as np
import openmatrix as omx
from scipy import io
import openpyxl
from random import sample, choices
# from dbfread import DBF
# import geopandas as gpd
import sys
import shutil
pd.set_option('display.max_columns', None)  # Display all columns

#===================================================================================================================
#===================================================================================================================


## Functions
def load_and_preprocess_data(model_dir, output_dir, input_excel_file_name, faf_file_name, skim_tod, skim_name):

    """
    Parameters:
    - Path: a full path to the input file. For example: 'C:\GitLab\HVM scripts\input_file.xlsx'

    input_file:
    - HTM input excel file

    Returns:
    - df_dict (dict): A dictionary containing DataFrames for each sheet in the input Excel file.
    - traffic_skims (DataFrame): Processed DataFrame containing traffic skims.

    Description:
    This function loads and preprocesses input files and returns DataFrames and processed traffic skims.

    """

    # Initialize a dictionary to hold DataFrames
    df_dict = {}

    # 1. Load in input files
    #input_path = path
    input_excel_name = input_excel_file_name
    input_excel_path = model_dir + "\input\htm"
    full_input_excel_path = f"{input_excel_path}\\{input_excel_name}"
    inputs_sandag_HTM = pd.ExcelFile(full_input_excel_path)
    sheet_names = [sheet_name for sheet_name in inputs_sandag_HTM.sheet_names if sheet_name.lower() not in ['userguide', 'reference']]

    # 1.1 Load all sheets into separate DataFrames with lowercase names
    for sheet_name in sheet_names:
        df_name = sheet_name.lower()  # Convert sheet name to lowercase
        df_dict[df_name] = inputs_sandag_HTM.parse(sheet_name)  # Save DataFrame to the dictionary

    # 2. Load in FAF data
    # faf = df_dict['faf']  # Use the dictionary to get the DataFrame
    faf_name = faf_file_name
    faf_path = model_dir + "\input\htm"
    full_faf_path = f"{faf_path}\\{faf_name}"
    df = pd.read_csv(full_faf_path)

    # 3. Load in MGRA data
    # mgra_loc = df_dict['mgra']  # Use the dictionary to get the DataFrame
    # full_mgra_path = f"{model_dir}\\{mgra_file_name}"
    full_mgra_path = f"{model_dir}\\input\\land_use.csv"
    mgra = pd.read_csv(full_mgra_path)
    
    # mgra_dbf_loc = df_dict['mgra_dbf']  # Use the dictionary to get the DataFrame
    # mgra_dbf_name = mgra_dbf_file_name
    # full_mgra_dbf_path = f"{model_dir}\\{mgra_dbf_name}"
    # full_mgra_dbf_path = f"{model_dir}\input\\{mgra_dbf_name}"
    sra_taz = df_dict['sra_taz']
    
    # 4. Load in Skim file
    def read_omx(skimtype):
        
        #skim = df_dict['skim']  # Use the dictionary to get the DataFrame       
        # skim_name = skim_file_name
        skim_path = output_dir
        # full_skim_path = f"{output_dir}\\skims\\{skim_name}.omx"
        full_skim_path = f"{output_dir}\\skims\\traffic_skims_{skim_tod}.omx"
    
        traffic_skims = []
        with omx.open_file(full_skim_path) as omx_file:
            # for name in ["PM_TRK_H_DIST"]:
            for name in [f"{skim_name}_{skimtype}__{skim_tod}"]:
                matrix = omx_file[name]
                df_skim = pd.DataFrame(matrix[:])
                
                # Adjust the index and columns to start from 1
                df_skim.index = range(1, df_skim.shape[0] + 1)
                df_skim.columns = range(1, df_skim.shape[1] + 1)
                
                stacked_df = df_skim.stack().reset_index()  # Reset the index to make it separate columns
                stacked_df.columns = ["origin", "destination", name]  # Rename the columns
                traffic_skims.append(stacked_df)
        traffic_skims = pd.concat(traffic_skims, axis=1)
        
        # traffic_skims.origin = traffic_skims.origin + 1 # TAZ number is from 1 to 4947
        # traffic_skims.destination = traffic_skims.destination + 1
        
        return traffic_skims
    
    traffic_skims_Dist = read_omx('DIST')
    traffic_skims_Time = read_omx('TIME')
    traffic_skims_TollCost = read_omx('TOLLCOST')
    

    # 5. Filter out OD pairs where at least one end is a gateway
    traffic_skims_Dist_filter = traffic_skims_Dist.loc[(traffic_skims_Dist['origin'] >= 13) & (traffic_skims_Dist['destination'] >= 13)]

    return df, mgra, sra_taz, traffic_skims_Dist_filter, traffic_skims_Dist, traffic_skims_Time, traffic_skims_TollCost, df_dict


#===================================================================================================================
#===================================================================================================================

def clean_faf(df, scenario_year, faz_gateway, faz_county, sd_flows, othermode_truck, commodity_group):
    """
    Process a DataFrame containing transportation data.

    Parameters:
    - df (DataFrame): The input DataFrame containing transportation data.
    - faz_gateway (DataFrame): DataFrame containing FAZ and Gateways/Airports/Ports TAZ.
    - faz_county (DataFrame): DataFrame containing FAZ and County information.
    - sd_flows (DataFrame): DataFrame containing Origin and Destination pair that will generate truck trips to/from SANDAG or Pass through SANDAG region.
    - othermode_truck (DataFrame): DataFrame with mode information and percentages.
    - commodity_group (DataFrame): DataFrame mapping commodities to CG values.

    Returns:
    - processed_df (DataFrame): Processed DataFrame with updated columns.

    Description:
    This function takes FAF data and performs the following steps:
    0. Determine if FAF data is available for the scenario year, if not, interpolate/extrapolate the FAF tonnage for the scenario year
    1. Determine what are the OD pairs that may pass through SANDAG region or start/end in SANDAG
    2. Includes only specified modes and calculates tonnage.
    3. Deletes unnecessary columns.
    4. Assigns SANDAG commodity groups based on SCTG commodity group.
    5. Aggregates tonnage data by OD FAZ and Commodity Group.
    6. Adds columns identifying if OD pairs have at least one end within Orange County or Mexico.
    The processed DataFrame is returned.
    """
    sd_flows = df_dict[sd_flows]
    faz_gateway = df_dict[faz_gateway]
    faz_county = df_dict[faz_county]
    othermode_truck = df_dict[othermode_truck]
    commodity_group = df_dict[commodity_group]

    #--------------------------------------------------------

    # 0. Determine what FAF year data to use based on the model scenario year


    def interpolate_for_missing_year(year, dataframe):
        """
        Interpolate data for a missing year using linear interpolation from the closest years.

        :year: Year to interpolate data for.
        :dataframe: FAF dataframe to perform interpolation on.
        :return: FAF dataframe with the interpolated column added.
        """
        years = [int(col.split('_')[1]) for col in dataframe.columns if col.startswith('distons_')]
        closest_prior_year = max([y for y in years if y < year], default=None)
        closest_following_year = min([y for y in years if y > year], default=None)

        if closest_prior_year is not None and closest_following_year is not None:
            prior_column = f"distons_{closest_prior_year}"
            following_column = f"distons_{closest_following_year}"
            interpolated_column = f"tons_{year}"
            dataframe[interpolated_column] = dataframe[prior_column] + (dataframe[following_column] - dataframe[prior_column]) * ((year - closest_prior_year) / (closest_following_year - closest_prior_year))
            return dataframe
        else:
            return None

    def extrapolate_for_missing_year(year, dataframe):
        """
        Extrapolate data for a missing year using the closest available year.

        : year: Year to extrapolate data for.
        : dataframe: FAF dataframe to perform extrapolation on.
        :return: FAF dataframe with the extrapolated column added.
        """
        years = [int(col.split('_')[1]) for col in dataframe.columns if col.startswith('distons_')]
        years.sort()

        # Find two closest previous or next years
        previous_years = [y for y in years if y < year]
        following_years = [y for y in years if y > year]

        closest_years = []
        if len(previous_years) >= 2:
            closest_years = previous_years[-2:]
        elif len(following_years) >= 2:
            closest_years = following_years[:2]

        if len(closest_years) == 2:
            y1, y2 = closest_years
            col1, col2 = f"distons_{y1}", f"distons_{y2}"
            extrapolated_column = f"tons_{year}"
            # Linear extrapolation
            m = (dataframe[col2] - dataframe[col1]) / (y2 - y1)
            dataframe[extrapolated_column] = dataframe[col2] + m * (year - y2)
            return dataframe
        else:
            # Cannot extrapolate if not enough data
            return None


    # Check if the user input is a year and if the corresponding column exists in the dataframe
    year = int(scenario_year)
    
    if f"distons_{year}" not in df.columns:
        interpolated_df = interpolate_for_missing_year(year, df)
        if interpolated_df is None:
            extrapolated_df = extrapolate_for_missing_year(year, df)
        else:
            extrapolated_df = interpolated_df
    else:
        new_column = f"tons_{year}"
        df[new_column] = df[f"distons_{year}"]
        extrapolated_df = df
    

    df = extrapolated_df
    ton_column = f"tons_{year}"
    #-------------------------------------------------------------

    # 1. Determine what are the OD pairs that may pass through SANDAG region or start/end in SANDAG
    # Mapping columns 'dms_orig' and 'dms_dest' based on the lookup table to determine the area code (check out the reference tab in the input spreadsheet for more info)
    df = df.merge(faz_gateway[['FAZ', 'AreaCode']], how='left', left_on='dms_orig', right_on='FAZ').drop('FAZ', axis=1).rename(columns={'AreaCode': 'code_orig'})
    df = df.merge(faz_gateway[['FAZ', 'AreaCode']], how='left', left_on='dms_dest', right_on='FAZ').drop('FAZ', axis=1).rename(columns={'AreaCode': 'code_dest'})
    # if FAZ is within SANDAG, the areacode should be 8
    faz_sandag = faz_county[faz_county["County"] == "San Diego"]["FAZ"]
    df.loc[df['dms_orig'].isin(faz_sandag), 'code_orig'] = 8
    df.loc[df['dms_dest'].isin(faz_sandag), 'code_dest'] = 8
    # merge the distribution of truck trips between each OD pair
    df = df.merge(sd_flows[['OriginCode', 'DestinationCode', 'Dist']], how='left', left_on=['code_orig', 'code_dest'], right_on=['OriginCode', 'DestinationCode']).drop(['OriginCode', 'DestinationCode'], axis=1)
    # calculate how much of each OD pairs will travel through SANDAG - multiply by the ton
    df[ton_column] = df[ton_column] * df['Dist']
    # remove all OD pairs that do not have any tons traveled through or within SANDAG
    df = df[df[ton_column] > 0]
    df.drop(['Dist'], axis=1, inplace=True)

    # 2. Include some modes
    mode_to_include = othermode_truck.set_index('Mode_Num')['Percentage'].to_dict()
    df = df[df['Mode'].isin(mode_to_include.keys())]
    df['truck_perc'] = df['Mode'].map(mode_to_include)
    df['ton'] = df[ton_column] * df['truck_perc']
    df.drop(['truck_perc', ton_column], axis=1, inplace=True)

    # 3. Delete unnecessary columns
    # delete_col = ['Direction', 'Trade', 'disvalue_2017', 'distons_2017', 'distons_2025', 'disvalue_2025', 'distons_2030', 'disvalue_2030', 'distons_2035', 
    #                 'disvalue_2035', 'distons_2040', 'disvalue_2040', 'distons_2045', 'disvalue_2045', 'distons_2050', 'disvalue_2050'] 
    delete_col = ['Direction', 'disvalue_2017', 'distons_2017', 'distons_2025', 'disvalue_2025', 'distons_2030', 'disvalue_2030', 'distons_2035', 
                    'disvalue_2035', 'distons_2040', 'disvalue_2040', 'distons_2045', 'disvalue_2045', 'distons_2050', 'disvalue_2050'] 
    
    
    df.drop(delete_col, axis=1, inplace=True)

    # 4. Assign SANDAG commodity groups based on SCTG commodity group
    commodity_to_cg = commodity_group.set_index('SCTG')['CG'].to_dict()
    df['CG'] = df['Commodity'].map(commodity_to_cg)
    df.drop('Commodity', axis=1, inplace=True)

    # 5. Aggregate the Tonnage Data by Origin/Destination and Commodity Group
    df['fr_orig'] = df['fr_orig'].fillna(0)
    df['fr_dest'] = df['fr_dest'].fillna(0)
    df = df.groupby(['dms_orig', 'dms_dest', 'fr_orig', 'fr_dest', 'code_orig', 'code_dest', 'CG'], as_index=False).agg({'ton': 'sum'})

    # 6. Create a new column that identifies if at least one end of the OD is within Orange County and mexico
    df['one_end_orange'] = ((df['code_orig'] == 6) | (df['code_dest'] == 6)).astype(int)
    df['one_end_mx'] = ((df['fr_orig'].isin([802])) | (df['fr_dest'].isin([802]))).astype(int)                            

    return df

#===================================================================================================================
#===================================================================================================================

def prepare_taz_data(mgra, faz_county, emp_converter, taz_faz):
    """
    Prepare TAZ data by calculating NAICS employee category percentages within each TAZ.

    Parameters:
    - mgra (DataFrame): DataFrame containing MGRA data.
    - faz_county (DataFrame): DataFrame containing FAZ and County information.
    - emp_converter (DataFrame): DataFrame mapping SANDAG emp category to NAICS emp category.
    - taz_faz (DataFrame): DataFrame mapping TAZ values to FAZ values.

    Returns:
    - taz_long (DataFrame): Processed DataFrame with calculated employee category percentages.

    Description:
    This function calculates the percentage of each employee category (from the NAICS Dataset)
    within each TAZ. It performs the following steps:
    1. Calculate the number of SADNAG employees by TAZ.
    2. Add FAZ to the emp by TAZ table
    3. Reformat the TAZ table to long format using emp_ and pop columns.
    4. Reformat the emp_converter table to long format using FAZ columns
    5. Convert SANDAG emp category to NAICS emp category.
    6. Calculate the number of NAICS employees by TAZ.
    7. Aggregate total number of each NAICS employee category by TAZ.
    6. Create a mapping of TAZ values to FAZ values.
    8. Calculate how many percentage of each emp category is within a TAZ
    The processed DataFrame is returned.
    """

    emp_converter = df_dict[emp_converter]
    taz_faz = df_dict[taz_faz]
    faz_county = df_dict[faz_county]
    
    # 1. Calculate the number of employee by TAZ
    cols_to_sum = ['pop'] + [col for col in mgra.columns if col.startswith('emp_')]
    taz = mgra.groupby(['taz'], as_index=False)[cols_to_sum].sum()

    # 2. Add FAZ to the emp by TAZ table
    taz_to_faz = taz_faz.set_index('TAZ')['FAZ'].to_dict()
    # Use the map function to directly assign TAZ values
    taz['FAZ'] = taz['taz'].map(taz_to_faz)

    # 3. Reformat the taz table to long format using emp_ columns and pop
    taz_long = taz.melt(id_vars=['taz', 'FAZ'], value_vars=cols_to_sum, value_name='sandag_emp_num', var_name='sandag_emp')

    # 4. Reformat the emp_converter table to long format using FAZ columns
    faz_sandag = faz_county[faz_county["County"] == "San Diego"]["FAZ"]
    emp_converter_long = emp_converter.melt(id_vars=['NAICS_Emp', 'SANDAG_Emp'], value_vars=faz_sandag, value_name='Emp_PCT', var_name='FAZ')

    # 5. Convert SANDAG emp category to NAICS emp category
    taz_long = emp_converter_long.merge(taz_long, how='inner', right_on=['sandag_emp', 'FAZ'], left_on=['SANDAG_Emp', 'FAZ']).drop(['sandag_emp'], axis=1)

    # 6. Calculate the number of NAICS employees by TAZ.
    taz_long['naics_emp_num'] = taz_long['sandag_emp_num'] * taz_long['Emp_PCT']

    # 7. Aggregate NAICS employee number by TAZ
    taz_long = taz_long.groupby(['taz', 'FAZ', 'NAICS_Emp'], as_index=False).agg({'naics_emp_num': 'sum'})

    # 8. Calculate how many percentage of each emp category is within a TAZ
    taz_long['emp_naics_perc'] = taz_long['naics_emp_num'] / taz_long.groupby(['NAICS_Emp', 'FAZ'])['naics_emp_num'].transform('sum')
    taz_long = taz_long.loc[taz_long['naics_emp_num'] > 0]

    return taz_long

#===================================================================================================================
#===================================================================================================================


def faf_disaggregate_to_taz(df, faz_gateway, cg_emp_a, cg_emp_p, faz_county, taz):
    """
    Parameters:
    - df (DataFrame): faf data with aggregated commodity
    - faz_gateway (DataFrame): DataFrame mapping FAZ to SADNAG gateways.
    - faz_county (DataFrame): DataFrame containing FAZ and County information.

    Returns:
    - processed_df (DataFrame): Processed DataFrame with annual tonnage by TAZ origin and destination.

    Description:
    This function performs the following steps:
    1. Determine NAICS emp category for production and attraction in FAF.
    2. Bring TAZ numbers and the percentage of each emp category within each TAZ to relatively distribute tonnage to TAZ.
    3. For FAZ outside the SANDAG region, assume that the distribution of tonnage is 1 for both attraction/production.
    4. Calculate annual tonnage for each OD pair.
    5. Reformat the faz_gateway table to long format using emp_ columns and pop.
    6. Assign Gateway TAZ and percentage to the DataFrame.
    7. Calculate final tonnage where one end of trip is outside the SANDAG region (taz_a or taz_p is null) and assign corresponding gateways as taz_a or taz_p.
    8. Group by TAZ attraction and production and sum up the annual tonnage.


    The processed DataFrame is returned.
    """
    faz_gateway = df_dict[faz_gateway]
    cg_emp_a = df_dict[cg_emp_a]
    cg_emp_p = df_dict[cg_emp_p]
    faz_county = df_dict[faz_county]
    


    # 1. Determine NAICS emp category for production and attraction in FAF
    cg_to_emp_a = cg_emp_a.set_index('CG')['Emp_a'].to_dict()
    cg_to_emp_p = cg_emp_p.set_index('CG')['Emp_p'].to_dict()

    # Use the map function to directly assign Emp_a and Emp_p values
    df['Emp_a'] = df['CG'].map(cg_to_emp_a)
    df['Emp_p'] = df['CG'].map(cg_to_emp_p)
    
    # 2. Bring TAZ numbers and the percentage of each emp category within each TAZ to relatively distribute tonnage to TAZ.
    df = df.merge(taz[['taz', 'FAZ', 'NAICS_Emp', 'emp_naics_perc']], how='left', left_on=['Emp_p', 'dms_orig'], right_on=['NAICS_Emp', 'FAZ']).drop(['FAZ', 'NAICS_Emp', 'Emp_p'], axis=1)
    df.rename(columns={'taz': 'taz_p', 'emp_naics_perc': 'emp_naics_perc_p'}, inplace=True)

    df = df.merge(taz[['taz', 'FAZ', 'NAICS_Emp', 'emp_naics_perc']], how='left', left_on=['Emp_a', 'dms_dest'], right_on=['NAICS_Emp', 'FAZ']).drop(['FAZ', 'NAICS_Emp', 'Emp_a'], axis=1)
    df.rename(columns={'taz': 'taz_a', 'emp_naics_perc': 'emp_naics_perc_a'}, inplace=True)

    # 3. For FAZ outside the SANDAG region, assume that the distribution percentage is 1 for both attraction/production
    # create a list of FAZ in San Diego
    faz_sandag = faz_county[faz_county["County"] == "San Diego"]["FAZ"]
    df.loc[(~df['dms_orig'].isin(faz_sandag)) & (df['emp_naics_perc_p'].isnull()), 'emp_naics_perc_p'] = 1
    df.loc[(~df['dms_dest'].isin(faz_sandag)) & (df['emp_naics_perc_a'].isnull()), 'emp_naics_perc_a'] = 1
    
    # 4. Calculate annual tonnage for each OD pair
    df['dist_perc'] = df['emp_naics_perc_a'] * df['emp_naics_perc_p']
    df['ton_annual'] = df['ton'] * df['dist_perc'] * 1000  # FAF data is in thousand tons
    df.drop(['ton', 'emp_naics_perc_p', 'emp_naics_perc_a', 'dist_perc'], axis=1, inplace=True)
    
    # 5. Reformat the faz_gateway table to long format using emp_ columns and pop
    gateway_airport_port = faz_gateway.columns[4:]
    faz_gateway_long = faz_gateway.melt(id_vars=['FAZ'], value_vars=gateway_airport_port, value_name='faz_gtw_perc', var_name='gateways').dropna(subset=['faz_gtw_perc'])

    # 6. Assign Gateway TAZ and percentage to the DataFrame
    df = df.merge(faz_gateway_long, how='left', left_on='dms_orig', right_on='FAZ').drop('FAZ', axis=1)
    df.rename(columns={'gateways': 'gateways_p', 'faz_gtw_perc': 'faz_gtw_perc_p'}, inplace=True)

    df = df.merge(faz_gateway_long, how='left', left_on='dms_dest', right_on='FAZ').drop('FAZ', axis=1)
    df.rename(columns={'gateways': 'gateways_a', 'faz_gtw_perc': 'faz_gtw_perc_a'}, inplace=True)

    # 7. Calculate final tonnage 
    # For the end that is within SANDAG region, gateway distributions should be ignored 
    df.loc[(df['dms_orig'].isin(faz_sandag)) & (df['faz_gtw_perc_p'].isnull()), 'faz_gtw_perc_p'] = 1
    df.loc[(df['dms_dest'].isin(faz_sandag)) & (df['faz_gtw_perc_a'].isnull()), 'faz_gtw_perc_a'] = 1

    df['faz_gtw_perc'] = df['faz_gtw_perc_p'] * df['faz_gtw_perc_a']
    df['ton_tot'] = df['ton_annual'] * df['faz_gtw_perc']

    # df.loc[df['taz_a'].isnull(), 'ton_tot'] = df.loc[df['taz_a'].isnull(), 'ton_annual'] * df.loc[df['taz_a'].isnull(), 'faz_gtw_perc_a']
    df.loc[df['taz_a'].isnull(), 'taz_a'] = df.loc[df['taz_a'].isnull(), 'gateways_a']

    # df.loc[df['taz_p'].isnull(), 'ton_tot'] = df.loc[df['taz_p'].isnull(), 'ton_annual'] * df.loc[df['taz_p'].isnull(), 'faz_gtw_perc_p']
    df.loc[df['taz_p'].isnull(), 'taz_p'] = df.loc[df['taz_p'].isnull(), 'gateways_p']
    
    # 8. Group by TAZ attraction and production and sum up the annual tonnage
    # Note that there is a column that shows if at least one end of the trip is within Orange County. This column will be used in future steps to determine the OD distance.
    processed_df = df.groupby(['dms_orig', 'dms_dest','taz_a', 'taz_p', 'CG', 'one_end_orange', 'one_end_mx'], as_index=False).agg({'ton_tot': 'sum'})


    return processed_df




#===================================================================================================================
#===================================================================================================================

def ton_to_truck_by_type(df, annual_factor, traffic_skims, truck_dist, payload, skim_name, skim_tod):
    """
    Convert annual tonnage to number of trucks by type and time of day.

    Parameters:
    - df (DataFrame): DataFrame containing the daily tonnage by TAZ.
    - annual_factor: annual to daily factor is basically the number of working days within a year
    - traffic_skims (DataFrame): DataFrame containing OD pair distance data.
    - truck_dist (DataFrame): DataFrame containing truck type distribution data.
    - payload (DataFrame): DataFrame containing payload data for truck types.


    Returns:
    - final_df (DataFrame): Processed DataFrame with number of trucks by type and time of day (TOD).

    Description:
    This function performs the following steps:
    1. Convert annual to daily tonnage.
    2. Identify distance between two OD pairs within San Diego using skim.
    3. Categorize the distance into different categories.
        1. less than 50 miles; 
        2. 51 to 100 miles ; 
        3. One end in OC ; 
        4. 201 miles or more; or one end outside of SANDAG and Orange county regions.
    4. Distribute daily tonnage between truck types based on OD distance.
    5. Convert tonnage by truck type to the number of trucks using truck type and commodity.
    The final processed DataFrame is returned.
    """
    
    annual_factor = df_dict[annual_factor]
    truck_dist = df_dict[truck_dist]
    payload = df_dict[payload]
    
    # 1. Convert annual to daily tonnage . the annual to daily factor is basically the number of working days within a year.
    annual_to_daily_factor = annual_factor['Factor'].values
    df['ton_daily'] = df['ton_tot'] / annual_to_daily_factor

    # Drop unnecessary columns
    df.drop(['ton_tot'], axis=1, inplace=True)

    # 2. Identify distance between two OD pairs within San Diego using skim
    df = df.merge(traffic_skims, how='left', left_on=['taz_a', 'taz_p'], right_on=['destination', 'origin']).drop(['origin', 'destination'], axis=1)

    # 3. Categorize the distance
    def categorize_dist(value):
        if value <= 50:
            return 1
        elif value <= 100:
            return 2
        elif value <= 150:
            return 3
        elif value > 200:
            return 4
        else:
            return 0

    df['dist_cat'] = np.where(df['one_end_orange'] == 1, 3, 0)
    df['dist_cat'] = np.where(df['one_end_mx'] == 1, 5, 0)
    df['dist_cat'] = np.where(df['dist_cat'] == 0, df[f'{skim_name}_DIST__{skim_tod}'].apply(categorize_dist), df['dist_cat'])
    df['dist_cat'] = np.where(df['dist_cat'] == 0, 4, df['dist_cat'])
    df.drop(['one_end_orange', f'{skim_name}_DIST__{skim_tod}'], axis=1, inplace=True)

    # 4. Distribute daily tonnage between truck types based on OD distance
    df = df.merge(truck_dist[['Dist_GP', 'Truck_Type', 'Dist']], how='left', left_on='dist_cat', right_on='Dist_GP').drop(['Dist_GP'], axis=1)
    df['ton_daily_bytruck'] = df['ton_daily'] * df['Dist']
    df.drop(['ton_daily', 'dist_cat', 'Dist'], axis=1, inplace=True)
    df = df.groupby([  'taz_a', 'taz_p', 'CG', 'Truck_Type'], as_index=False)['ton_daily_bytruck'].sum()

    # 5. Convert tonnage by truck type to the number of trucks using truck type and commodity
    max_tonnage_dict = payload.set_index(['CG', 'Truck_Type'])['Pounds'].to_dict()
    df['Tonnage'] = df.apply(lambda row: max_tonnage_dict.get((row['CG'], row['Truck_Type']), 0), axis=1)
    df = df.loc[df['Tonnage'] > 0]
    df['tot_truck'] = (df['ton_daily_bytruck'] / df['Tonnage']) * 2000
    df.drop(['ton_daily_bytruck', 'Tonnage'], axis=1, inplace=True)

    return df

#===================================================================================================================
#===================================================================================================================
# The function below help to distribute Non-FAF data according to counts collected at the SD gateways. Only for Light and Meduim Trucks

# Define the distribution to use to allocate trip origins or destinations accross TAZs
def define_dist(
    df_full, #full dataset at a level smaller than TAZ (in this case block group) 
    drop_percentile, # what bottom percentile to drop from the distribution to avoid too many TAZs that round down to 0
    pop_scale = 1, # how much to weight the population
    emp_scale = 1 # how much to weight the employment of a TAZ 
    ):
    # aggregate to TAZ level
    df = taz[~taz['taz'].isin(external)].groupby(['taz', 'SRA'])[['pop','emp_total']].sum().reset_index()

    # Calculate the sum of 'pop' and 'emp_total' for each 'SRA'
    sra_total = df.groupby('SRA', as_index=False)[['emp_total', 'pop']].sum()
    # Merge the totals back into the original DataFrame
    df = df.merge(sra_total, how='left', on='SRA', suffixes=('', '_sratotal'))
    # calculate proportion of trip attraction or production using weights defined in function
    df['proportion'] = (pop_scale * df['pop'] + emp_scale * df['emp_total']) / (pop_scale*df['pop_sratotal'] + emp_scale*df['emp_total_sratotal'])
    # calculate the proportion threshold under which we will drop the TAZ from consideration 
    # q = df['proportion'].quantile(drop_percentile)
    sra_quantile = df.groupby('SRA', as_index=False)[['proportion']].quantile(drop_percentile)
    df = df.merge(sra_quantile, how='left', on='SRA', suffixes=('', '_sraquant'))
    # Drop TAZs under this threshold
    df_sub = df[df['proportion'] > df['proportion_sraquant']]

    # Recalculate the proportion with only the remaining TAZs
    sra_total_re = df_sub.groupby('SRA', as_index=False)[['emp_total', 'pop']].sum()
    df_sub = df_sub.merge(sra_total_re, how='left', on='SRA', suffixes=('', '_sratotal_re'))
    df_sub['proportion_use'] = (pop_scale * df_sub['pop'] + emp_scale * df_sub['emp_total']) / (pop_scale*df_sub['pop_sratotal_re'] + emp_scale*df_sub['emp_total_sratotal_re']) 
    df_sub = df_sub[['taz', 'SRA', 'proportion_use']]
    return df_sub

#===================================================================================================================
#===================================================================================================================
# Distribute a specified number of trips to TAZs based on the distribution of trips to SRA, 
# taking into consideration that certain TAZs are situated within those SRAs. 
# This allocation is applicable exclusively to Light and Medium Trucks.
def allocate_trips(
    n, # The number of trips to distribute
    prop_df, # The dataframe with the share of trips that should go to each TAZ. Output of define_dist
    all_tazs, # A list of all TAZs eligible to receive any of the trips
    gateway, #what gatewat to allocate additional trips to
    sra_dist):
    # Allocate trips to each TAZ and round to nearest whole number
    sra_dist_selected = sra_dist.loc[:, ['SRA', gateway]]
    prop_df = prop_df.merge(sra_dist_selected, on="SRA", how='left')
    prop_df['trips_test'] = prop_df['proportion_use'] * prop_df[gateway] * n
    # Create a dictionary mapping each TAZ to the number of new trips it should get
    dict1 = dict(zip(prop_df[prop_df['trips_test']> 0]['taz'],prop_df[prop_df['trips_test']> 0]['trips_test']))
    # Calculate the delta between the amount of trips we want to allocate and the sum after we round
    n_missing = int(n - prop_df['trips_test'].sum())
    
    if n_missing > 0: 
        # If we have too few trips after rounding sameple from the TAZs that 
        # are eligible but did not get a trip assigned and assign one trip to each sampled TAZ
        print('Allocating %d trips to low volume TAZs' %(n_missing)) 
        dropped_tazs = [t for t in all_tazs if t not in list(prop_df['taz'])]
        dict2 = dict(pd.Series(choices(dropped_tazs, k=n_missing)).value_counts())
        taz_trip_dict = {**dict1, **dict2}
    else: 
        # If we have too many trips after rounding, sample from the TAZs we did allocate to 
        # and remove one trip from each
        print('Removing %d trips from TAZs' %(n_missing))
        remove_trips_tazs = sample(list(dict1.keys()), n_missing*(-1))
        taz_trip_dict = {k:(v-1 if k in remove_trips_tazs else v) for (k,v) in dict1.items()}

    # Returns a dictionary mapping TAZs to the amount of new trips they will get. Not all
    # TAZs are included
    return taz_trip_dict

#===================================================================================================================
#===================================================================================================================
#===================================================================================================================

## Main Script - FAF Data

arguments = sys.argv
# arguments = ['',r'Z:\projects\SANDAG\31000583_CVM\ABM3_CVMtesting',r'Z:\projects\SANDAG\31000583_CVM\ABM3_CVMtesting\output','FAF5_BaseandFutureYears_Oct27_2023.csv','inputs_sandag_HTM_2022.xlsx','mgra15_based_input2035_old.csv','MGRA15.dbf','PM',2035,[0.29,0.55,0.62]]


model_dir = arguments[1]
output_dir = arguments[2]
faf_file_name = arguments[3]
htm_input_file_name = arguments[4]
skim_tod = arguments[5]
scenario_year = arguments[6]
scenario_year_with_suffix = arguments[7]

skim_name = 'TRK_H'

# Mapping dictionary for conditions
#auto_path = f"{model_dir}\input\parametersByYears.csv"
#df_auto_cost = pd.read_csv(auto_path)

# Step 1: Load and preprocess data
df, mgra, sra_taz, traffic_skims_Dist_filter, traffic_skims_Dist, traffic_skims_Time, traffic_skims_TollCost, df_dict = load_and_preprocess_data(model_dir, output_dir, htm_input_file_name, faf_file_name, skim_tod, skim_name)

# Step 2: Prepare TAZ data
taz = prepare_taz_data(mgra, 'faz_county', 'emp_converter', 'taz_faz')

# Step 3: Clean FAF data
df_cleaned = clean_faf(df, scenario_year, 'faz_gateway', 'faz_county', 'sd_flows', 'othermode_truck', 'commodity_group')

# Step 4: FAF disaggregation to TAZ
df_cleaned_disagg = faf_disaggregate_to_taz(df_cleaned, 'faz_gateway', 'cg_emp_a', 'cg_emp_p', 'faz_county', taz)

# Process df in chunks
chunk_size = 2000000
num_chunks = len(df_cleaned_disagg) // chunk_size + 1
final_results = []

for chunk_num in range(num_chunks):
    start_idx = chunk_num * chunk_size
    end_idx = (chunk_num + 1) * chunk_size
    df_chunk = df_cleaned_disagg[start_idx:end_idx]

    # Step 5: Daily tonnage to truck types and time of day
    final_chunk = ton_to_truck_by_type(df_chunk, 'annual_factor', traffic_skims_Dist_filter, 'truck_dist', 'payload', skim_name, skim_tod)
    
    # Append the results of the chunk to the final_results list
    final_results.append(final_chunk)

    # Print progress
    progress = (chunk_num + 1) / num_chunks * 100
    print(f"Processing chunk {chunk_num + 1}/{num_chunks} - {progress:.2f}% done")

# Combine the results of all chunks
htm = pd.concat(final_results, ignore_index=True)

#===================================================================================================================

# combine medium1 and 2 truck type to a medium type
# combine medium1 and 2 truck type to a medium type
htm.loc[htm['Truck_Type'] == "Medium1", 'Truck_Type'] = 'Medium'
htm.loc[htm['Truck_Type'] == "Medium2", 'Truck_Type'] = 'Medium'

htm = htm.groupby(['Truck_Type', 'taz_a', 'taz_p'], as_index=False)[['tot_truck']].sum()

# # Please check
# htm = htm[htm['taz_p'].apply(lambda x: not isinstance(x, str))]
# htm = htm[htm['taz_a'].apply(lambda x: not isinstance(x, str))]


#===================================================================================================================
#===================================================================================================================
#===================================================================================================================
#===================================================================================================================
## Additional Script - Non-FAF Data

# Add the Non-FAF truck trips to gateways and distribute them

# Specify external TAZs and the ones that are gateways
external = [1,2,3,4,5,6,7,8,9,10,11,12, 
    1154, 1294, 1338, 1457,1476,1485,1520, 2086,
    2193,2372,2384,2497,3693,4184]
gateways = [1,2,3,4,5,6,7,8,9,10,11,12]

# Open the MGRA15 DBF file - to get the SRA to TAZ correspondence
sra_taz = sra_taz.groupby(['TAZ'], as_index=False)['SRA'].first()
# Read in data for population / employment stats by TAZ
taz = mgra[['taz', 'pop','emp_total']]
taz = taz.groupby(['taz'], as_index=False)[['pop', 'emp_total']].sum()
# merge SRA to mgra file
taz = taz.merge(sra_taz, left_on='taz', right_on='TAZ', how='left')
taz.drop(['TAZ'], axis=1, inplace = True)


# Create a table in the same format as the one we will read in with the control totals
# Consider only trips from a gateway to an internal location, or vice versa. This makes it
# easier to deal with inflating counts later on. By keeping IE and EI trips mutually exclusive
# we can ensure the totals reach the counts we want, which are classified as IE or EI
od_ie = htm[
    (htm['taz_a'].isin(gateways)) & (~htm['taz_p'].isin(external))
    ].groupby(['taz_a','Truck_Type'])['tot_truck'].sum().unstack().reset_index().rename(columns = {'taz_a':'gateway'})
od_ei = htm[
    (htm['taz_p'].isin(gateways)) & (~htm['taz_a'].isin(external))
    ].groupby(['taz_p','Truck_Type'])['tot_truck'].sum().unstack().reset_index().rename(columns = {'taz_p':'gateway'})

od_comp = od_ie.merge(od_ei, how = 'left', on = 'gateway', suffixes=('_ie','_ei'))
cols = ['gateway','Light_ie','Light_ei','Medium_ie','Medium_ei','Heavy_ie','Heavy_ei']
od_comp = od_comp[cols] # Reorder columns to be in the same order as control table
od_comp.columns.name = None
# Define a dictionary mapping old column names to new column names
column_mapping = {'Light_ie': 'LHDT_IE', 'Light_ei': 'LHDT_EI', 'Medium_ie': 'MHDT_IE', 'Medium_ei': 'MHDT_EI', 'Heavy_ie': 'HHDT_IE', 'Heavy_ei': 'HHDT_EI'}
od_comp.rename(columns=column_mapping, inplace=True)
od_comp = od_comp.set_index('gateway').reindex(np.arange(1,13)).fillna(0).reset_index()

#===================================================================================================================
# Read in control table and SRA distribution. 
controls = df_dict['external_count'][['Gateway'] + list(column_mapping.values())]
sra_dist = df_dict['sra_dist']

# Get the common column names
common_columns = list(set(controls.columns) & set(od_comp.columns))

for o in gateways:
    for d in gateways:
        t = htm[(htm['Truck_Type']=='Heavy') & (htm['taz_p']==o) & (htm['taz_a']==d)]['tot_truck'].sum()
        controls.loc[controls['Gateway']==o, 'HHDT_EI'] = controls.loc[controls['Gateway']==o, 'HHDT_EI'] - t
        controls.loc[controls['Gateway']==d, 'HHDT_IE'] = controls.loc[controls['Gateway']==d, 'HHDT_IE'] - t

# Subtract only the columns with the same name
deltas = controls[common_columns].sub(od_comp[common_columns])
# Include the Gateway column in the result
deltas.insert(0, 'Gateway', controls['Gateway'])
deltas = deltas[controls.columns]
deltas[deltas < 0] = 0 # If we have more modeled trips than control trips, we are leaving the modeled total for now
#===================================================================================================================

### Heavy Scaling
# Scale up the heavy trips using the control totals. Because there is a high volume of heavy trips, we will just scale up 
# all the trip totals based on control total / modeled total. 
hie_factors = controls['HHDT_IE'] / od_comp['HHDT_IE']
hie_factors[(hie_factors < 1) | (hie_factors == np.inf)] = 1
hie_factors = dict(zip(controls['Gateway'],hie_factors))

hei_factors = controls['HHDT_EI'] / od_comp['HHDT_EI']
hei_factors[(hei_factors < 1) | (hei_factors == np.inf)] = 1
hei_factors = dict(zip(controls['Gateway'],hei_factors))

htm['updated_tot'] = htm['tot_truck']
#Heavy_IE scaling
# Multiply relevant columns by the appropriate factor
# We only want to distibute the additiona trips to internal TAZs
htm.loc[
    (htm['Truck_Type']=='Heavy') & (htm['taz_a'].isin(gateways)) & (~htm['taz_p'].isin(external)),'updated_tot'
    ] = htm.loc[
        (htm['Truck_Type']=='Heavy') & (htm['taz_a'].isin(gateways))& (~htm['taz_p'].isin(external)),'tot_truck'
        ] * htm.loc[
            (htm['Truck_Type']=='Heavy') & (htm['taz_a'].isin(gateways))& (~htm['taz_p'].isin(external)),'taz_a'
            ].map(hie_factors)

# Heavy EI scaling
htm.loc[
    (htm['Truck_Type']=='Heavy') & (htm['taz_p'].isin(gateways))& (~htm['taz_a'].isin(external)),'updated_tot'
    ] = htm.loc[
        (htm['Truck_Type']=='Heavy') & (htm['taz_p'].isin(gateways))& (~htm['taz_a'].isin(external)),'tot_truck'
        ] * htm.loc[
            (htm['Truck_Type']=='Heavy') & (htm['taz_p'].isin(gateways))& (~htm['taz_a'].isin(external)),'taz_p'
            ].map(hei_factors)

#===================================================================================================================

# Light and Medium Scaling
prop_df = define_dist(taz, 0.0, 0.5, 1)
taz_list = list(taz[~taz['taz'].isin(external)]['taz'].unique())

gateway_dfs = []
for i in deltas.index: # Iterate through each gateway
    for j,c in enumerate(['LHDT_IE','LHDT_EI','MHDT_IE','MHDT_EI']):
        n = deltas.iloc[i,j+1] #Get the number of trips to be allocated
        trip_dist = allocate_trips(n, prop_df, taz_list, i+1, sra_dist) 
        # and turn into a datafram
        new_trip_df = pd.DataFrame([trip_dist]).T.reset_index().rename(columns = {'index':'taz',0:'new_trips'})
        new_trip_df['taz'] = new_trip_df['taz'].astype(float)
        if c == 'LHDT_IE':
            # Look only at the subset we want
            df_sub1 = htm.loc[
                (htm['Truck_Type']=='Light') & (htm['taz_a']==i+1) & (~htm['taz_p'].isin(external))
                ]
            # Do an outer merge with our dataframe of allocated trips. Allows us to give trips to
            # OD pairs that may not be in the dataframe
            df_sub1['taz_p'] = df_sub1['taz_p'].astype(float)
            df_sub1 = df_sub1.merge(new_trip_df, how = 'outer', left_on = 'taz_p', right_on = 'taz')
            # Make sure proper info is carried over to all the right columns
            df_sub1['updated_tot'] = df_sub1['tot_truck'].fillna(0) + df_sub1['new_trips'].fillna(0)
            df_sub1['Truck_Type'] = 'Light'
            df_sub1['taz_a'] = i+1
            # Make taz_p the original value from the matrix unless its null, then give it the value
            # from the merged dataframe of allocated trips.
            df_sub1['taz_p'] = df_sub1['taz_p'].combine_first(df_sub1['taz'])
            gateway_dfs.append(df_sub1)
        if c == 'LHDT_EI':
            df_sub2 = htm.loc[
                (htm['Truck_Type']=='Light') & (htm['taz_p']==i+1) & (~htm['taz_a'].isin(external))
                ]
            df_sub2['taz_a'] = df_sub2['taz_a'].astype(float)
            df_sub2 = df_sub2.merge(new_trip_df, how = 'outer', left_on = 'taz_a', right_on = 'taz')
            df_sub2['updated_tot'] = df_sub2['tot_truck'].fillna(0) + df_sub2['new_trips'].fillna(0)
            df_sub2['Truck_Type'] = 'Light'
            df_sub2['taz_p'] = i+1
            df_sub2['taz_a'] = df_sub2['taz_a'].combine_first(df_sub2['taz'])
            gateway_dfs.append(df_sub2)
        if c == 'MHDT_IE':
            df_sub3 = htm.loc[
                (htm['Truck_Type']=='Medium') & (htm['taz_a']==i+1) & (~htm['taz_p'].isin(external))
                ]
            df_sub3['taz_p'] = df_sub3['taz_p'].astype(float)
            df_sub3 = df_sub3.merge(new_trip_df, how = 'outer', left_on = 'taz_p', right_on = 'taz')
            df_sub3['updated_tot'] = df_sub3['tot_truck'].fillna(0) + df_sub3['new_trips'].fillna(0)
            df_sub3['Truck_Type'] = 'Medium'
            df_sub3['taz_a'] = i+1
            df_sub3['taz_p'] = df_sub3['taz_p'].combine_first(df_sub3['taz'])
            gateway_dfs.append(df_sub3)
        if c == 'MHDT_EI':
            df_sub4 = htm.loc[
                (htm['Truck_Type']=='Medium') & (htm['taz_p']==i+1) & (~htm['taz_a'].isin(external))
                ]
            df_sub4['taz_a'] = df_sub4['taz_a'].astype(float)
            df_sub4 = df_sub4.merge(new_trip_df, how = 'outer', left_on = 'taz_a', right_on = 'taz')
            df_sub4['updated_tot'] = df_sub4['tot_truck'].fillna(0) + df_sub4['new_trips'].fillna(0)
            df_sub4['Truck_Type'] = 'Medium'
            df_sub4['taz_p'] = i+1
            df_sub4['taz_a'] = df_sub4['taz_a'].combine_first(df_sub4['taz'])
            gateway_dfs.append(df_sub4)

        print(i,j)
# Concatenate all the saved dfs into one
non_faf = pd.concat(gateway_dfs).drop(['taz','new_trips', 'tot_truck'], axis=1)
# Merge to original od_matrix
htm_merge = htm.merge(non_faf, how = 'outer', on = ['Truck_Type','taz_a','taz_p'])
# Make sure updated_tot represents the updated data where it is relevant. 
htm_merge['updated_tot'] = htm_merge['updated_tot_y'].combine_first(htm_merge['updated_tot_x'])
htm_merge = htm_merge.drop(['updated_tot_x', 'updated_tot_y'], axis=1)
htm_merge.head()


#===================================================================================================================
# Identify the trip types: ei,ie,ee (drop ii since it is not an HTM trip)
htm_merge['ei'] = 0
htm_merge['ie'] = 0
htm_merge['ee'] = 0
htm_merge.loc[(~htm_merge.taz_a.isin(external)) & (htm_merge.taz_p.isin(external)), 'ei'] = 1
htm_merge.loc[(htm_merge.taz_a.isin(external)) & (~htm_merge.taz_p.isin(external)), 'ie'] = 1
htm_merge.loc[(htm_merge.taz_a.isin(external)) & (htm_merge.taz_p.isin(external)), 'ee'] = 1


# Distribute the number of trucks by time of day
time_of_day = df_dict['time_of_day']

peak_periods = ['AM', 'MD', 'PM', 'EA', 'EV']
for period in peak_periods:
    factor_column = time_of_day.loc[time_of_day['Peak_Period'] == period, 'Factor'].values
    htm_merge[f'{period.lower()}_truck'] = htm_merge['updated_tot'] * factor_column


#===================================================================================================================
# Generate Final Trips (Summary)
htmnew = htm_merge[(htm_merge['ei'] != 0) | (htm_merge['ie'] != 0) | (htm_merge['ee'] != 0)]
htmnew = htmnew.drop(['updated_tot','ei','ie','ee','tot_truck'], axis=1)
htmnew = pd.melt(htmnew, id_vars=['taz_p', 'taz_a','Truck_Type'], var_name='tod', value_name='trips')
htmnew['tod'] = htmnew['tod'].str[:2]

# Merge Skim Distance
htmnew = htmnew.merge(traffic_skims_Dist, how='left', left_on=['taz_a','taz_p'], right_on=['destination','origin']).drop(['origin', 'destination'], axis=1).rename(columns={f'{skim_name}_DIST__{skim_tod}': 'distanceDrive'})
htmnew = htmnew.merge(traffic_skims_Time, how='left', left_on=['taz_a','taz_p'], right_on=['destination','origin']).drop(['origin', 'destination'], axis=1).rename(columns={f'{skim_name}_TIME__{skim_tod}': 'timeDrive'})
htmnew = htmnew.merge(traffic_skims_TollCost, how='left', left_on=['taz_a','taz_p'], right_on=['destination','origin']).drop(['origin', 'destination'], axis=1).rename(columns={f'{skim_name}_TOLLCOST__{skim_tod}': 'costTollDrive'})

htmnew['costOperatingDrive'] = None

# Mapping dictionary for conditions
auto_path = f"{model_dir}\input\parametersByYears.csv"
df_auto_cost = pd.read_csv(auto_path)

# Mapping dictionary for conditions

mapping = {'Light': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.light'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.light'].iloc[0],
           'Medium': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.medium'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.medium'].iloc[0],
           'Heavy': df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.fuel.high'].iloc[0] + df_auto_cost[df_auto_cost['year']==scenario_year_with_suffix]['aoc.truck.maintenance.high'].iloc[0]}


def calculate_new_column(row):
    return row['distanceDrive'] * mapping[row['Truck_Type']]

htmnew['costOperatingDrive'] = htmnew.apply(calculate_new_column, axis=1)

file_name = 'final_trips.csv'
htmnew.to_csv(output_dir + "/htm/" + file_name,index=False)
print(f'{file_name} is saved')


#===================================================================================================================

#Generate new summary file
summ_dfs = []
for tt in ['Light','Medium','Heavy']:
    df = pd.concat([
        pd.concat([
        htm_merge[
        (htm_merge['Truck_Type']==tt) &
        (htm_merge['taz_p'].isin(external)) &
        (htm_merge['ei']==1)
        ].groupby('taz_p')['updated_tot'].sum().fillna(0),
        htm_merge[
        (htm_merge['Truck_Type']==tt) &
        ((htm_merge['taz_p'].isin(external)) |
        (htm_merge['taz_a'].isin(external))) &
        (htm_merge['ee']==1)
        ].groupby(['taz_p','taz_a'])['updated_tot'].sum().unstack().fillna(0)
    ], axis=1).rename(columns = {'updated_tot':'SD'}),
    pd.DataFrame(htm_merge[
        (htm_merge['Truck_Type']==tt) &
        (htm_merge['taz_a'].isin(external)) &
        (htm_merge['ie']==1)
        ].groupby('taz_a')['updated_tot'].sum().fillna(0)).rename(columns = {'updated_tot':'SD'}).T
    ])
    summ_dfs.append(df)

summary_file_name = 'summary_file.xlsx'
with pd.ExcelWriter(output_dir + "\htm\htm" + summary_file_name, engine='openpyxl') as writer:
    summ_dfs[0].to_excel(writer, sheet_name='Light')
    summ_dfs[1].to_excel(writer, sheet_name='Medium')
    summ_dfs[2].to_excel(writer, sheet_name='Heavy')

print(f'{summary_file_name} is saved')


#===================================================================================================================

# Define truck types and columns to process
truck_types = htm_merge['Truck_Type'].unique()
columns_to_process = ['am_truck', 'md_truck', 'pm_truck', 'ea_truck', 'ev_truck']
conditions = ['ei', 'ie', 'ee']

# Create empty matrices - set it to the number of TAZs within the region
matrix_size = mgra.taz.max() 

empty_matrix = np.zeros((matrix_size, matrix_size))

for column in columns_to_process:
    matrices = {}
    for condition in conditions:
        condition_matrices = {}
        for i, truck_type in enumerate(truck_types):
            filtered_data = htm_merge[(htm_merge[condition] == 1) & (htm_merge['Truck_Type'] == truck_type)]
            filtered_data_group = filtered_data.groupby(['Truck_Type', 'taz_a', 'taz_p'], as_index=False)[[column]].sum()
            
            # Create a matrix filled with zeros
            matrix = pd.pivot_table(filtered_data_group, values=column, index='taz_p', columns='taz_a').fillna(0)
            
            # Reindex to ensure the matrix size is correct
            matrix = matrix.reindex(range(1, matrix_size + 1), fill_value=0).fillna(0)
            matrix = matrix.reindex(columns=range(1, matrix_size + 1), fill_value=0).fillna(0)
            
            # Convert the matrix to a NumPy array
            condition_matrices[f"{truck_type}_{condition}"] = matrix.values

        matrices[f"{column}_{condition}_matrices"] = condition_matrices

    # Create OMX file for each column
    htm_period = column.split("_")[0].upper()
    file_name = f"\htmtrips_{htm_period}.omx"
    matrix_file = output_dir + "\htm" + file_name

    # Create an OMX file and write matrices
    #with omx.open_file(file_name, 'w') as f:
    with omx.open_file(matrix_file, 'w') as f:
        for matrix_name, matrix_data in matrices.items():
            for sub_matrix_name, sub_matrix_data in matrix_data.items():
                #clean_matrix_name = matrix_name.replace('_', '')  # Remove underscores
                matrix_tod = matrix_name.split("_")[0].upper()
                clean_sub_matrix_name = sub_matrix_name.replace('_', '')  # Remove underscores
                #f[f"{clean_matrix_name}_{clean_sub_matrix_name}"] = sub_matrix_data
                f[f"{clean_sub_matrix_name}_{matrix_tod}"] = sub_matrix_data
    #copy_dst = assignment_dir + "\\" + file_name
    #shutil.copyfile(matrix_file, copy_dst)
    print(f"Matrices for '{column}' saved as '{file_name}'")

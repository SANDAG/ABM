import pandas as pd
import numpy as np
import os

# Input attributes

base_path = r'T:\projects\sr15\abm3_sdia\Calibration\Calibration0'
scenarioYear = 2022
tol = 0.01
max_iter = 3 #25 #3

target_file = 'final_santrips.csv'
target_col = 'arrival_mode'
coef_file = 'trip_mode_choice_coefficients.csv'

targets = {
    # 'res_drop_off': 0.181,
    'res_parked_offsite': 0.021,
    'res_parked_onsite': 0.035,
    # 'res_shuttle': 0.002,
    'res_taxi': 0.004,
    'res_tnc': 0.127,
    'res_transit': 0.004,
    # 'vis_drop_off': 0.146,
    'vis_rental': 0.178,
    'vis_shuttle': 0.05,
    'vis_taxi': 0.019,
    'vis_tnc': 0.226,
    'vis_transit': 0.005
 }


def read_data(output_dir:str, target_file:str, target_col:str, iter:int) -> pd.Series:
    '''
    '''
    model_output_table = pd.read_csv(os.path.join(output_dir, target_file),
                           index_col = 0)
    model_output_table = model_output_table[['primary_purpose',target_col]]
    model_output_table[target_col] = (
        model_output_table[target_col]
        .str.lower()
        .replace({'shuttlevan':'shuttle','park_loc1':'parked_onsite','park_loc4':'parked_offsite',
                'curb_loc1':'drop_off','knr_loc':'transit','walk_loc':'transit', 'ridehail_loc1':'tnc'})
        .str.replace(r'_loc[0-9]*', '', regex=True)    
    )
    model_output_table['primary_purpose'] = model_output_table['primary_purpose'].str.split('_', expand=True)[0]

    return model_output_table['primary_purpose'] + '_' + model_output_table[target_col]


def get_adjustment(model_data:pd.Series, targets:dict, iter:int):
    '''
    Calculates adjustments for ####

    Parameters
    ----------
    model_data (pandas.Series):
        model output data to be adjusted
    targets (dict):
        Dictionary with keys "BEV" and "PEV" indicating the target shares for each

    Returns
    -------
    coef_adj (dict):
        Dictionary with target keys showing adjustment factors to calibration constants for each alternative
    '''
    shares = model_data.value_counts(normalize = True)
    coef_adj = {}
    for key in targets:
        coef_adj[key] = np.log(targets[key]/shares[key])
    
    print("\nCALIBRATING")
    results = pd.DataFrame({
            "Targets": targets,
            "Observed": shares,
            "Adjustment": coef_adj
        })
    print(
        results
    )
    results.to_csv("calibrationResultsIter{}.csv".format(iter))
    print("\n\n")

    return coef_adj


def adjust_coefs(target_configs:str, year:int, coef_adj:dict, coef_file:str, iter:int):
    '''
    '''
    coef_file = os.path.join(target_configs, coef_file)
    coefs = pd.read_csv(coef_file, index_col = 0)
    coefs.to_csv(coef_file.replace(".csv", f"_iter{iter}.csv"))

    # Adjust coefficients
    print('new_coefs', coefs)
    for target_coef in coef_adj:
        print(target_coef, 'base', coefs.loc[f"coef_asc_{target_coef}", "value"],'new', coef_adj[target_coef])
        coefs.loc[f"coef_asc_{target_coef}", "value"] += coef_adj[target_coef]
        
    coefs.to_csv(coef_file) # TODO remove commenting out
    return


def run_calibration(asim_call:str, output_dir:str, target_configs:str, target_file:str, year:int, target_col:str, targets:dict, tol:float, max_iter:int):
    '''
    Runs calibration
    '''
    model_output_target_col = read_data(output_dir, target_file, target_col, 0)
    print(model_output_target_col.value_counts())
    coef_adj = get_adjustment(model_output_target_col, targets, 0)
    adjust_coefs(target_configs, year, coef_adj, coef_file, 0)

    for i in range(max_iter):
        print("\n\n\nRUNNING ITERATION {}\n\n\n".format(i+1))
        os.system(asim_call)

        model_output_target_col = read_data(output_dir, target_file, target_col, i+1)  
        coef_adj = get_adjustment(model_output_target_col, targets, i+1)
        adjust_coefs(target_configs, year, coef_adj, coef_file, i+1)

        if pd.Series(coef_adj).abs().max() < tol:
            print("Calibration converged")
            break

# simpy_file = os.path.join(base_path, 'asim', 'simulation.py')
simpy_file = os.path.join(base_path, 'asim', 'scripts/airport/airport_model.py')
common_configs = os.path.join(base_path, 'asim', 'configs', 'common')
target_configs = os.path.join(base_path, 'asim', 'configs', 'airport.SAN')
data = os.path.join(base_path, 'data')
output = os.path.join(base_path, 'output')
# settings_file = 'settings.yaml'

asim_call = f'python {simpy_file} -a -c {common_configs} -c {target_configs} -d {data} -o {output}'# -s {settings_file}' # TODO verify for airport.SAN -> different structure for calling simulation.py

run_calibration(
    asim_call,
    output,
    target_configs,
    target_file,
    scenarioYear,
    target_col,
    targets,
    tol,
    max_iter
)
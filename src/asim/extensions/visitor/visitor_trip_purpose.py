import os
import pandas as pd


def update_trip_purpose_probs(stop_purp_probs, parameters):
    print("Creating trip purpose probability lookup table.")
    purpose_id_map = parameters['purpose_ids']
    purp_dict = {v: k for k, v in purpose_id_map.items()}

    # Rename columns to the purpose
    colnames = {'StopNum': 'trip_num', 'Multiple': 'multistop'}
    for x in stop_purp_probs.columns:
        if 'StopPurp' in x:
            colnames[x] = purp_dict[int(x.replace('StopPurp', ''))]
    stop_purp_probs.rename(columns=colnames, inplace=True)

    # Make outbound column, is a reflection of inbound boolean t/f
    stop_purp_probs['outbound'] = ~stop_purp_probs['Inbound'].astype(bool)

    # Add primary purpose names
    stop_purp_probs['primary_purpose'] = stop_purp_probs['TourPurp'].map(purp_dict)

    # Select column subset
    stop_probs = stop_purp_probs[['primary_purpose', 'outbound', 'trip_num', 'multistop'] + list(purpose_id_map.keys())]

    # Save to CSV
    file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
    if parameters['overwrite'] or not os.path.exists(file_path):
        stop_probs.to_csv(file_path, index=False)

    return stop_probs


# NOTE: This script is relatively unchanged from xborder, needs some review/cleanup
def create_trip_scheduling_duration_probs(inbound, outbound, parameters):
    print("Creating trip scheduling probability lookup table.")

    num_asim_periods = 48

    # Assign dummy variable to each to indicate which is which in long form
    outbound['outbound'], inbound['outbound'] = True, False

    # Reshape wide tables to long format so they can be stacked into one data frame
    duration_probs = []
    for df in [inbound, outbound]:
        duration_probs.append(
            df.melt(
                id_vars=['RemainingLow', 'RemainingHigh', 'Stop', 'outbound'],
                var_name='duration_offset', value_name='prob')
        )
    duration_probs = pd.concat(duration_probs, axis=0, ignore_index=True)

    # Cleanup column names
    duration_probs.rename(columns={
        'Stop': 'stop_num',
        'RemainingHigh': 'periods_left_max',
        'RemainingLow': 'periods_left_min'}, inplace=True)

    # convert to asim format
    max_periods_left = duration_probs['periods_left_max'].max()
    duration_probs['periods_left_max'] = duration_probs['periods_left_max'].replace(max_periods_left, num_asim_periods - 1)

    # Save output
    file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_scheduling_probs'])
    if parameters['overwrite'] or not os.path.exists(file_path):
        duration_probs.to_csv(file_path, index=False)

    return duration_probs

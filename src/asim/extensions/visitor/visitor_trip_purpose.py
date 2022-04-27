import os
import pandas as pd


def update_trip_purpose_probs(stop_probs, parameters):
    print("Creating trip purpose probability lookup table.")
    purpose_id_map = parameters['purpose_ids']
    purp_dict = {v: k for k, v in purpose_id_map.items()}

    # Rename columns to the purpose
    colnames = {'StopNum': 'trip_num', 'Multiple': 'multistop'}
    for x in stop_probs.columns:
        if 'StopPurp' in x:
            colnames[x] = purp_dict[int(x.replace('StopPurp', ''))]
    stop_probs.rename(columns=colnames, inplace=True)

    # Make outbound column, is a reflection of inbound boolean t/f
    stop_probs['outbound'] = ~stop_probs['Inbound'].astype(bool)

    # Add primary purpose names
    stop_probs['primary_purpose'] = stop_probs['TourPurp'].map(purp_dict)

    # Select column subset
    stop_probs = stop_probs[['primary_purpose', 'outbound', 'trip_num', 'multistop'] + list(purpose_id_map.keys())]

    # Save to CSV
    file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
    if parameters['overwrite'] or not os.path.exists(file_path):
        stop_probs.to_csv(file_path, index=False)

    return stop_probs


# NOTE: This script is relatively unchanged from xborder, needs some review/cleanup
def create_trip_scheduling_duration_probs(settings, los_settings):
    print("Creating trip scheduling probability lookup table.")
    data_dir = settings['data_dir']
    config_dir = settings['config_dir']
    period_settings = los_settings['skim_time_periods']
    num_asim_periods = int(
        period_settings['time_window'] / period_settings['period_minutes'])

    outbound = pd.read_csv(os.path.join(
        data_dir,
        settings['trip_scheduling_probs_input_fnames']['outbound']))
    outbound['outbound'] = True
    inbound = pd.read_csv(os.path.join(
        data_dir,
        settings['trip_scheduling_probs_input_fnames']['inbound']))
    inbound['outbound'] = False
    assert len(outbound) == len(inbound)

    outbound = outbound.melt(
        id_vars=['RemainingLow', 'RemainingHigh', 'Stop', 'outbound'],
        var_name='duration_offset', value_name='prob')
    inbound = inbound.melt(
        id_vars=['RemainingLow', 'RemainingHigh', 'Stop', 'outbound'],
        var_name='duration_offset', value_name='prob')

    duration_probs = pd.concat((outbound, inbound), axis=0, ignore_index=True)
    duration_probs.rename(columns={
        'Stop': 'stop_num',
        'RemainingHigh': 'periods_left_max',
        'RemainingLow': 'periods_left_min'}, inplace=True)

    # convert to asim format
    max_periods_left = duration_probs['periods_left_max'].max()
    duration_probs['periods_left_max'] = duration_probs[
        'periods_left_max'].replace(
        max_periods_left, num_asim_periods - 1)

    duration_probs.to_csv(os.path.join(
        config_dir, settings['trip_scheduling_probs_output_fname']),
        index=False)

    return

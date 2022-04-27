import os
import pandas as pd


# NOTE: This script is relatively unchanged from xborder, needs some review/cleanup


def update_trip_purpose_probs(settings):
    print("Creating trip purpose probability lookup table.")
    probs_df = pd.read_csv(os.path.join(
        settings['data_dir'], settings['trip_purpose_probs_input_fname']))
    purpose_id_map = settings['tours']['purpose_ids']
    purp_id_to_name = {v: k for k, v in purpose_id_map.items()}

    # drop cargo
    if probs_df['TourPurp'].max() == 5:
        probs_df = probs_df.loc[probs_df['TourPurp'] != 2, :]
        del probs_df['StopPurp2']
        probs_df.rename(columns={
            'StopPurp3': 'StopPurp2',
            'StopPurp4': 'StopPurp3',
            'StopPurp5': 'StopPurp4'}, inplace=True)
        probs_df.loc[probs_df['TourPurp'] > 2, 'TourPurp'] = probs_df.loc[
                                                                 probs_df['TourPurp'] > 2, 'TourPurp'] - 1

    probs_df.rename(columns={
        'StopNum': 'trip_num', 'Multiple': 'multistop',
        'StopPurp0': purp_id_to_name[0], 'StopPurp1': purp_id_to_name[1],
        'StopPurp2': purp_id_to_name[2], 'StopPurp3': purp_id_to_name[3],
        'StopPurp4': purp_id_to_name[4]}, inplace=True)
    probs_df['outbound'] = ~probs_df['Inbound'].astype(bool)
    probs_df['primary_purpose'] = probs_df['TourPurp'].map(purp_id_to_name)
    probs_df = probs_df[
        ['primary_purpose', 'outbound', 'trip_num', 'multistop'] + [
            purp for purp in purpose_id_map.keys()]]
    probs_df.to_csv(os.path.join(
        settings['config_dir'], settings['trip_purpose_probs_output_fname']),
        index=False)

    return


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

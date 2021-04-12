import pandas as pd
import numpy as np
import os
import yaml
from collections import OrderedDict
import openmatrix as omx


def compute_poe_accessibility(poe_id, colonias, colonia_pop_field, distance_param):

    pop = colonias[colonia_pop_field]
    dist_col = 'Distance_poe' + str(int(poe_id))
    dists = colonias[dist_col]
    dist_factors = np.exp(dists * distance_param)
    weighted_pop = dist_factors * pop
    total_poe_pop_access = np.log(weighted_pop.sum())

    return total_poe_pop_access


def get_poe_wait_times(settings):

    data_dir = settings['data_dir']
    wait_times = pd.read_csv(
        os.path.join(data_dir, settings['poe_wait_times_input_fname']))
    wait_times.rename(columns={
        'StandardWait': 'std_wait', 'SENTRIWait': 'sentri_wait',
        'PedestrianWait':'ped_wait'}, inplace=True)
    wait_times_wide = wait_times.pivot(
        index='poe',columns='StartPeriod', values=['std_wait','sentri_wait','ped_wait'])
    wait_times_wide.columns = [
        '_'.join([top, str(bottom)]) for top, bottom in wait_times_wide.columns]

    return wait_times_wide


def create_tours(tour_settings):

    num_tours = tour_settings['num_tours']
    purpose_probs = OrderedDict(tour_settings['purpose_shares'])
    id_to_purpose = {i: purpose for i, purpose in enumerate(purpose_probs.keys())}
    lane_shares_by_purpose = tour_settings['lane_shares_by_purpose']

    
    tours = pd.DataFrame(
        index=range(tour_settings['num_tours']),
        columns=['lane_type', 'lane_id', 'tour_type', 'purpose_id'])
    tours.index.name = 'tour_id'

    purpose_cum_probs = np.array(list(purpose_probs.values())).cumsum()
    purpose_scaled_probs = np.subtract(purpose_cum_probs, np.random.rand(num_tours, 1))
    purpose = np.argmax((purpose_scaled_probs + 1.0).astype('i4'), axis=1)
    tours['purpose_id'] = purpose
    tours['tour_type'] = tours['purpose_id'].map(id_to_purpose)
    tours['tour_category'] = 'non_mandatory'
    tours.loc[tours['tour_type'].isin(['work', 'school', 'cargo']), 'tour_category'] = 'mandatory'
    
    # for xborder model, only 1 person per tour and 1 tour per person
    tours['number_of_participants'] = 1
    tours['tour_num'] = 1
    tours['tour_count'] = 1

    for purpose, df in tours.groupby('tour_type'):
        lane_probs = OrderedDict(lane_shares_by_purpose[purpose])
        id_to_lane = {i: lane for i, lane in enumerate(lane_probs.keys())}
        lane_cum_probs = np.array(list(lane_probs.values())).cumsum()
        lane_scaled_probs = np.subtract(lane_cum_probs, np.random.rand(len(df), 1))
        lane_id = np.argmax((lane_scaled_probs + 1.0).astype('i4'), axis=1)
        df['lane_id'] = lane_id
        df['lane_type'] = df['lane_id'].map(id_to_lane)
        tours.loc[df.index, 'lane_id'] = df['lane_id']
        tours.loc[df.index, 'lane_type'] = df['lane_type']

    return tours


def create_tour_od_alts(mazs, settings):
    # parameterize poe columns to include into the yaml
    poe_mazs = mazs.loc[mazs['poe_id'].notnull(), ['MAZ', 'poe_id', 'colonia_pop_accessibility']]
    poe_mazs.rename(columns={'poe_id': 'origin_poe_id', 'MAZ': 'origin_maz_id'}, inplace=True)
    ods = poe_mazs.reindex(poe_mazs.index.repeat(len(mazs))).reset_index(drop=True)
    ods['destination_maz_id'] = mazs.MAZ.tolist() * len(poe_mazs)
    ods = pd.merge(
    ods, mazs[[col for col in mazs.columns if col not in poe_mazs.columns]],
    left_on='destination_maz_id', right_on='MAZ')
    ods['alt'] = ods['origin_poe_id'].astype(int).astype(str) + '_' + ods['destination_maz_id'].astype(str)
    od_cols = ['origin_poe_id', 'origin_maz_id', 'destination_maz_id'] + [
    col for col in ods.columns if col in mazs.columns]
    ods = ods.set_index('alt')[od_cols]
    return ods


def create_households(num_tours):

    # one household per tour
    households = pd.DataFrame({'household_id': range(num_tours)})

    return households


def create_persons(settings, num_households):

    # one person per household
    persons = pd.DataFrame({'person_id': range(num_households)})
    persons['household_id'] = np.random.choice(num_households, num_households, replace=False)

    return persons


def _update_table(settings, data_dir, filter_col=None, filter_list=None):

    input_fname = settings['input_fname']
    output_fname = settings['output_fname']
    filter_col = settings.get('filter_col', filter_col)
    
    df = pd.read_csv(os.path.join(data_dir, input_fname))
    df.rename(columns=settings['rename_columns'], inplace=True)
    if filter_col:
        if filter_list is None:
            raise ValueError("filter_list param needed to filter table.")
        df = df.loc[df[filter_col].isin(filter_list)]
    df.to_csv(os.path.join(data_dir, output_fname), index=False)

    return


def create_maz_to_tap_drive(mazs, drive_skims):

    merged = pd.merge(mazs[['MAZ', 'TAZ']], drive_skims, on='TAZ')
    
    return merged


def _rename_skims(settings, skim_type):
    
    data_dir = settings['data_dir']
    skims_settings = settings['skims'][skim_type]
    periods = skims_settings.get('periods', [None])

    for period in periods:
        
        if period:
            print('Processing {0} {1} skims.'.format(period, skim_type))
            input_base_fname = skims_settings['input_base_fname']
            output_base_fname = skims_settings['output_base_fname']
            input_fname = input_base_fname + '_' + period + '.omx'
            output_fname = output_base_fname + '_' + period + '.omx'
        else:
            print('Processing {0} skims.'.format(skim_type))
            input_fname = skims_settings['input_fname']
            output_fname = skims_settings['output_fname']

        skims = omx.open_file(
            os.path.join(data_dir, input_fname), 'a')
        skims.copy_file(
            os.path.join(data_dir, output_fname), overwrite=True)
        output_skims = omx.open_file(
            os.path.join(data_dir, output_fname), 'a')

        for skims_name in output_skims.list_matrices():
            name_elems = skims_name.split('_')
            new_name = '_'.join(name_elems[1:]) + '__' + name_elems[0]
            output_skims[skims_name].rename(new_name)
        skims.close()
        output_skims.close()

    return


def create_taps_tap_lines(settings):
    
    skims_settings = settings['skims']
    data_dir = settings['data_dir']

    transit_skims = omx.open_file(
        os.path.join(data_dir, skims_settings['tap_to_tap']['input_fname']), 'a')
    all_taps = pd.DataFrame(pd.Series(list(transit_skims.root.lookup.zone_number)))
    all_taps.columns = ['TAP']
    all_taps.to_csv(os.path.join(data_dir, settings['taps_output_fname']), index=False)
    tap_lines = pd.read_csv(os.path.join(data_dir, settings['tap_lines_input_fname']))
    tap_lines.to_csv(os.path.join(data_dir, settings['tap_lines_output_fname'])) 

    transit_skims.close()

    return


def create_skims_and_tap_files(settings):

    skims_settings = settings['skims']
    data_dir = settings['data_dir']

    # create taps files and transit skims
    print('Creating tap files.')
    transit_skims = omx.open_file(
        os.path.join(data_dir, skims_settings['tap_to_tap']['input_fname']), 'a')
    all_taps = pd.DataFrame(pd.Series(list(transit_skims.root.lookup.zone_number)))
    all_taps.columns = ['TAP']
    all_taps.to_csv(os.path.join(data_dir, settings['taps_output_fname']), index=False)
    tap_lines = pd.read_csv(os.path.join(data_dir, settings['tap_lines_input_fname']))
    tap_lines.to_csv(os.path.join(data_dir, settings['tap_lines_output_fname'])) 
    transit_skims.close()


    # update skims/network data
    print('Updating maz-to-maz skims.')
    _update_table(skims_settings['maz_to_maz']['walk'], data_dir=settings['data_dir'])
    
    print('Updating maz-to-tap skims.')
    _update_table(
        skims_settings['maz_to_tap']['walk'],
        data_dir=settings['data_dir'],
        filter_col='TAP', filter_list=all_taps.TAP.values)

    # rename transit and auto skims
    print('Renaming skim keys')
    for skim_type in ['tap_to_tap', 'taz_to_taz']:
        _rename_skims(settings, skim_type)


def create_stop_freq_specs(settings):

    probs_df = pd.read_csv(
        os.path.join(settings['data_dir'], settings['stop_frequency_input_fname']))
    probs_df.rename(columns={'Outbound': 'out', 'Inbound': 'in'}, inplace=True)
    probs_df['alt'] = probs_df['out'].astype(str) + 'out_' + probs_df['in'].astype(str) + 'in'

    # convert probs to utils
    probs_df['util'] = np.log(probs_df['Percent']).clip(lower=-999)
    
    # write out alts table
    alts_df = probs_df.drop_duplicates(['out','in','alt'])[['alt','out','in']]
    alts_df.to_csv(
        os.path.join(settings['config_dir'], settings['stop_frequency_output_fname']), index=False)

    purpose_id_map = settings['tours']['purpose_ids']

    # iterate through purposes and pivot probability lookup tables to
    # create MNL spec files with only ASC's (no covariates).
    for purpose, purpose_id in purpose_id_map.items():
        purpose_probs = probs_df.loc[probs_df['Purpose'] == purpose_id]
        alt_cols = alts_df['alt'].tolist()
        expr_df =  purpose_probs.pivot(
            index=['DurationLo','DurationHi'], columns='alt', values='util').reset_index()
        expr_df['Description'] = 'ASC for tour durations between ' + expr_df['DurationLo'].astype(str) + ' and ' + expr_df['DurationHi'].astype(str)
        expr_df['Expression'] = expr_df['DurationLo'].astype(str) + ' < duration_hours <= ' + expr_df['DurationHi'].astype(str)
        expr_df = expr_df.drop(columns=['DurationLo','DurationHi'])
        required_cols = ['Description', 'Expression']
        expr_df = expr_df[required_cols + [col for col in expr_df.columns if col not in required_cols]]

        # write out purpose-specific model spec
        expr_df.to_csv(
            os.path.join(settings['config_dir'], 'stop_frequency_{0}.csv'.format(purpose)), index=False)

    return


def update_trip_purpose_probs(settings):

    probs_df = pd.read_csv(os.path.join(settings['data_dir'], settings['trip_purpose_probs_input_fname']))
    purpose_id_map = settings['tours']['purpose_ids']
    purp_id_to_name = {v: k for k, v in purpose_id_map.items()}
    probs_df.rename(columns={
        'StopNum': 'trip_num', 'Multiple': 'multistop',
        'StopPurp0': purp_id_to_name[0], 'StopPurp1': purp_id_to_name[1],
        'StopPurp2': purp_id_to_name[2], 'StopPurp3': purp_id_to_name[3],
        'StopPurp4': purp_id_to_name[4], 'StopPurp5': purp_id_to_name[5]}, inplace=True)
    probs_df['outbound'] = ~probs_df['Inbound'].astype(bool)
    probs_df['primary_purpose'] = probs_df['TourPurp'].map(purp_id_to_name)
    probs_df = probs_df[
        ['primary_purpose','outbound', 'trip_num', 'multistop'] + [purp for purp in purpose_id_map.keys()]]
    probs_df.to_csv(
        os.path.join(settings['config_dir'], settings['trip_purpose_probs_output_fname']),
        index=False)

    return


def create_trip_scheduling_duration_probs(settings):

    data_dir = settings['data_dir']
    config_dir = settings['config_dir']

    outbound = pd.read_csv(os.path.join(data_dir, settings['trip_scheduling_probs_input_fnames']['outbound']))
    outbound['outbound'] = True
    inbound = pd.read_csv(os.path.join(data_dir, settings['trip_scheduling_probs_input_fnames']['inbound']))
    inbound['outbound'] = False
    assert len(outbound) == len(inbound)

    outbound = outbound.melt(
        id_vars=['RemainingLow','RemainingHigh','Stop','outbound'], var_name='duration_offset', value_name='prob')
    inbound = inbound.melt(
        id_vars=['RemainingLow','RemainingHigh','Stop','outbound'], var_name='duration_offset', value_name='prob')
    
    duration_probs = pd.concat((outbound, inbound), axis=0, ignore_index=True)
    duration_probs.rename(columns={
        'Stop': 'stop_num',
        'RemainingHigh': 'periods_left_max',
        'RemainingLow': 'periods_left_min'}, inplace=True)
    duration_probs.to_csv(
        os.path.join(config_dir, settings['trip_scheduling_probs_output_fname']),
        index=False)

    return


if __name__ == '__main__':

    # load settings
    with open('cross_border_preprocessing.yaml') as f:
        settings = yaml.load(f, Loader=yaml.FullLoader)
    data_dir = settings['data_dir']
    config_dir = settings['config_dir']
    maz_input_fname = settings['maz_input_fname']
    maz_id_field = settings['maz_id_field']
    poe_id_field = settings['poe_id_field']
    poe_access_field = settings['poe_access_field']
    colonia_input_fname = settings['colonia_input_fname']
    colonia_pop_field = settings['colonia_pop_field']
    distance_param = settings['distance_param']
    tour_settings = settings['tours']
    poe_settings = settings['poes']
    mazs_output_fname = settings['mazs_output_fname']
    households_output_fname = settings['households_output_fname']
    persons_output_fname = settings['persons_output_fname']
    tours_output_fname = settings['tours_output_fname']
    skims_settings = settings['skims']
    
    # # load land use data
    colonias = pd.read_csv(os.path.join(data_dir, colonia_input_fname))
    mazs = pd.read_csv(os.path.join(data_dir, maz_input_fname))

    # get poe id for maz's that have one
    mazs[poe_id_field] = None
    for poe_id, poe_attrs in poe_settings.items():
        maz_mask = mazs[maz_id_field] == poe_attrs['maz_id']
        mazs.loc[maz_mask, poe_id_field] = poe_id

    # compute colonia accessibility for poe mazs
    mazs[poe_access_field] = None
    poe_mask = ~pd.isnull(mazs[poe_id_field])
    mazs.loc[poe_mask, poe_access_field] = mazs.loc[poe_mask, poe_id_field].apply(
        compute_poe_accessibility, colonias=colonias, colonia_pop_field=colonia_pop_field,
        distance_param=distance_param)
    mazs = mazs.rename(columns={'mgra': 'MAZ', 'taz': 'TAZ'})

    # merge in wide wait times
    wide_wait_times = get_poe_wait_times(settings)
    mazs = pd.merge(mazs, wide_wait_times, left_on='poe_id', right_on='poe', how='left')

    # create tours
    tours = create_tours(tour_settings)

    # create households, 1 per tour
    num_tours = tour_settings['num_tours']
    households = create_households(num_tours)

    # create persons, 1 per household
    num_households = len(households)
    persons = create_persons(settings, num_households)

    # assign persons and households to tours
    tours['household_id'] = np.random.choice(num_tours, num_tours, replace=False)
    tours['person_id'] = persons.set_index('household_id').reindex(tours['household_id'])['person_id'].values

    # reformat table of tour scheduling prob
    scheduling_probs = pd.read_csv(
        os.path.join(settings['data_dir'], settings['tour_scheduling_probs_input_fname']))
    tour_scheduling_alts = scheduling_probs.rename(
        columns={'EntryPeriod': 'start', 'ReturnPeriod': 'end'}).drop_duplicates(['start','end'])[['start', 'end']]
    scheduling_probs.rename(columns={
        'Purpose': 'purpose_id', 'EntryPeriod': 'entry_period',
        'ReturnPeriod': 'return_period', 'Percent': 'prob'}, inplace=True)
    scheduling_probs = scheduling_probs.pivot(
        index='purpose_id', columns=['entry_period','return_period'], values='prob')
    scheduling_probs.columns = [str(col[0]) + '_' + str(col[1]) for col in scheduling_probs.columns]

    # store results
    mazs.to_csv(os.path.join(data_dir, mazs_output_fname), index=False)
    tours.to_csv(os.path.join(data_dir, tours_output_fname))
    households.to_csv(os.path.join(data_dir, households_output_fname), index=False)
    persons.to_csv(os.path.join(data_dir, persons_output_fname), index=False)
    scheduling_probs.to_csv(os.path.join(config_dir, settings['tour_scheduling_probs_output_fname']))
    tour_scheduling_alts.to_csv(os.path.join(config_dir, settings['tour_scheduling_alts_output_fname']), index=False)

    # update the skims
    create_skims_and_tap_files(settings)

    # create_stop_freq_specs(settings)
    update_trip_purpose_probs(settings)

    # process duration-based trip scheduling probs
    create_trip_scheduling_duration_probs(settings)

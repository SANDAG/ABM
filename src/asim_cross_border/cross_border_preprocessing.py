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

    crossing_type_dict = {0: 'non_sentri', 1: 'sentri'}
    num_tours = tour_settings['num_tours']
    sentri_share = tour_settings['sentri_share']
    lane_shares_by_purpose = tour_settings['lane_shares_by_purpose']
    purpose_probs_by_sentri = tour_settings['purpose_shares']
    
    tours = pd.DataFrame(
        index=range(num_tours),
        columns=['sentri_crossing', 'lane_type', 'lane_id', 'tour_type', 'purpose_id'])
    tours.index.name = 'tour_id'

    sentri_scaled_probs = np.subtract(np.array(list([sentri_share])), np.random.rand(num_tours, 1))
    sentri = (sentri_scaled_probs + 1).astype('i4')
    tours['sentri_crossing'] = sentri
    for sentri, group in tours.groupby('sentri_crossing'):
        num_xing_type_tours = len(group)
        crossing_type = crossing_type_dict[sentri]
        purpose_probs = OrderedDict(purpose_probs_by_sentri[crossing_type])
        id_to_purpose = {i: purpose for i, purpose in enumerate(purpose_probs.keys())}
        purpose_cum_probs = np.array(list(purpose_probs.values())).cumsum()
        purpose_scaled_probs = np.subtract(purpose_cum_probs, np.random.rand(num_xing_type_tours, 1))
        purpose = np.argmax((purpose_scaled_probs + 1.0).astype('i4'), axis=1)
        group['purpose_id'] = purpose
        tours.loc[group.index, 'purpose_id'] = purpose

    tours['tour_type'] = tours['purpose_id'].map(id_to_purpose)
    tours['tour_category'] = 'non_mandatory'
    tours.loc[tours['tour_type'].isin(['work', 'school']), 'tour_category'] = 'mandatory'
    
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


def create_households(settings):

    num_tours = tour_settings['num_tours']

    # one household per tour
    households = pd.DataFrame({'household_id': range(num_tours)})

    return households


def create_persons(settings, num_households):

    # one person per household
    persons = pd.DataFrame({'person_id': range(num_households)})
    persons['household_id'] = np.random.choice(num_households, num_households, replace=False)

    return persons


def _update_sparse_skims(
    settings, data_dir, filter_col=None, filter_list=None, new_mazs=None):

    input_fname = settings['input_fname']
    output_fname = settings['output_fname']
    filter_col = settings.get('filter_col', filter_col)
    
    df = pd.read_csv(os.path.join(data_dir, input_fname))

    # rename columns from spec
    df.rename(columns=settings['rename_columns'], inplace=True)

    # filter columns from spec
    if filter_col:
        if filter_list is None:
            raise ValueError("filter_list param needed to filter table.")
        df = df.loc[df[filter_col].isin(filter_list)]

    # create new rows for new MAZs based on original MAZ counterparts 
    cols_to_match = ['OMAZ', 'DMAZ', 'MAZ']
    if new_mazs is not None:
        for i, row in new_mazs.iterrows():
            original_maz = row['original_MAZ']
            new_maz_id = row['MAZ']
            for col in cols_to_match:
                if col in df.columns:
                    new_rows = df[df[col] == original_maz].copy()
                    new_rows[col] = new_maz_id
                    df = df.append(new_rows, ignore_index=True)

    df.to_csv(os.path.join(data_dir, output_fname), index=False)

    return


def _rename_skims(settings, skim_type):
    
    data_dir = settings['data_dir']
    skims_settings = settings['skims'][skim_type]
    periods = skims_settings.get('periods', [None])
    walk_speed = settings['walk_speed']

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

        if period == 'MD':
            output_skims.create_matrix('walkTime', obj=output_skims['SOV_NT_M_DIST__MD'][:, :])
            output_skims['walkTime'][:, :] = output_skims['walkTime'][:, :] / walk_speed * 60

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


def create_skims_and_tap_files(settings, new_mazs=None):

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
    _update_sparse_skims(
        skims_settings['maz_to_maz']['walk'],
        data_dir=settings['data_dir'],
        new_mazs=new_mazs)
    
    print('Updating maz-to-tap skims.')
    _update_sparse_skims(
        skims_settings['maz_to_tap']['walk'],
        data_dir=settings['data_dir'],
        filter_col='TAP',
        filter_list=all_taps.TAP.values,
        new_mazs=new_mazs)
    
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
    probs_df['value'] = np.log(probs_df['Percent']).clip(lower=-999)
    
    # write out alts table
    alts_df = probs_df.drop_duplicates(['out','in','alt'])[['alt','out','in']]
    alts_df.to_csv(
        os.path.join(settings['config_dir'], settings['stop_frequency_alts_output_fname']), index=False)

    purpose_id_map = settings['tours']['purpose_ids']

    # iterate through purposes and pivot probability lookup tables to
    # create MNL spec files with only ASC's (no covariates).
    required_cols = ['Label', 'Description', 'Expression']
    for purpose, purpose_id in purpose_id_map.items():
        purpose_probs = probs_df.loc[probs_df['Purpose'] == purpose_id, :].copy()
        purpose_probs['coefficient_name'] = purpose_probs.apply(
            lambda x: 'coef_asc_dur_{0}_{1}_stops_{2}'.format(
                x['DurationLo'],x['DurationHi'],x['alt']), axis=1)

        coeffs_file = purpose_probs[['value','coefficient_name']].copy()
        coeffs_file['Description'] = None
        coeffs_file = coeffs_file[['Description','value','coefficient_name']]
        coeffs_file_fname = settings['stop_frequency_coeffs_output_formattable_fname'].format(
            purpose=purpose)
        coeffs_file.to_csv(
            os.path.join(settings['config_dir'], coeffs_file_fname), index=False)

        alt_cols = alts_df['alt'].tolist()
        expr_file =  purpose_probs.pivot(
            index=['DurationLo','DurationHi'], columns='alt',
            values='coefficient_name').reset_index()
        expr_file['Label'] = 'util_ASC_tour_dur_' + expr_file['DurationLo'].astype(str) + \
            '_' + expr_file['DurationHi'].astype(str)
        expr_file['Description'] = 'ASC for tour durations between ' + \
            expr_file['DurationLo'].astype(str) + ' and ' + expr_file['DurationHi'].astype(str)
        expr_file['Expression'] = expr_file['DurationLo'].astype(str) + \
            ' < duration_hours <= ' + expr_file['DurationHi'].astype(str)
        expr_file = expr_file.drop(columns=['DurationLo','DurationHi'])
        expr_file = expr_file[
            required_cols + [col for col in expr_file.columns if col not in required_cols]]
        expr_file_fname = settings['stop_frequency_expressions_output_formattable_fname'].format(
            purpose=purpose)
        expr_file.to_csv(os.path.join(
            settings['config_dir'], expr_file_fname.format(purpose)),
            index=False)

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


def update_tour_purpose_reassignment_probs(settings):

    data_dir = settings['data_dir']
    config_dir = settings['config_dir']
    poe_settings = settings['poes']
    probs_df = pd.read_csv(
        os.path.join(data_dir, settings['tour_purpose_control_probs_input_fname']), 
        index_col=['Description', 'Purpose'])
    probs_df = probs_df[[str(poe) for poe in list(poe_settings.keys())]]
    probs_df.reset_index(inplace=True)
    probs_df['Description'] = probs_df['Description'].str.lower()
    probs_df.to_csv(os.path.join(config_dir, settings['tour_purpose_control_probs_output_fname']), index=False)

    return


def create_land_use_file(settings):

    print('Creating land use (maz) table.')

    data_dir = settings['data_dir']
    maz_input_fname = settings['maz_input_fname']
    maz_id_field = settings['maz_id_field']
    poe_id_field = settings['poe_id_field']
    poe_access_field = settings['poe_access_field']
    colonia_input_fname = settings['colonia_input_fname']
    colonia_pop_field = settings['colonia_pop_field']
    distance_param = settings['distance_param']

    # load maz/mgra + colonia data
    colonias = pd.read_csv(os.path.join(data_dir, colonia_input_fname))
    mazs = pd.read_csv(os.path.join(data_dir, maz_input_fname))

    
    mazs[poe_id_field] = -1
    mazs['original_MAZ'] = -1
    mazs['external_TAZ'] = -1
    mazs['external_MAZ'] = -1
    for poe_id, poe_attrs in poe_settings.items():

        # get poe id for maz's that have one
        maz_mask = mazs[maz_id_field] == poe_attrs['maz_id']
        mazs.loc[maz_mask, poe_id_field] = poe_id
        mazs.loc[maz_mask, 'external_TAZ'] = poe_attrs['ext_taz_id']

        # add poes as new mazs
        row = mazs.loc[maz_mask].copy()
        for col in row.columns:
            if pd.api.types.is_float_dtype(row[col]):
                row[col] = 0.0
            elif pd.api.types.is_integer_dtype(row[col]):
                row[col] = 0
            else:
                row[col] = None
        row[maz_id_field] = mazs[maz_id_field].max() + 1
        row['external_TAZ'] = -1
        row['external_MAZ'] = -1
        mazs.loc[maz_mask, 'external_MAZ'] = row[maz_id_field]
        row['taz'] = poe_attrs['ext_taz_id']
        row[poe_id_field] = poe_id
        row['original_MAZ'] = mazs.loc[maz_mask, 'mgra']
        mazs = mazs.append(row, ignore_index=True)

    # compute colonia accessibility for poe mazs
    mazs[poe_access_field] = None
    poe_mask = mazs[poe_id_field] >= 0
    mazs.loc[poe_mask, poe_access_field] = mazs.loc[poe_mask, poe_id_field].apply(
        compute_poe_accessibility, colonias=colonias, colonia_pop_field=colonia_pop_field,
        distance_param=distance_param)
    mazs = mazs.rename(columns={'mgra': 'MAZ', 'taz': 'TAZ'})

    # merge in wide wait times
    wide_wait_times = get_poe_wait_times(settings)
    mazs = pd.merge(mazs, wide_wait_times, left_on='poe_id', right_on='poe', how='left')

    return mazs


def create_scheduling_probs_and_alts(settings):

    config_dir = settings['config_dir']
    data_dir = settings['data_dir']
    input_fname = settings['tour_scheduling_probs_input_fname']
    output_probs_fname = settings['tour_scheduling_probs_output_fname']
    output_alts_fname = settings['tour_scheduling_alts_output_fname']

    scheduling_probs = pd.read_csv(os.path.join(data_dir, input_fname))
    tour_scheduling_alts = scheduling_probs.rename(
        columns={'EntryPeriod': 'start', 'ReturnPeriod': 'end'}).drop_duplicates(
        ['start','end'])[['start', 'end']]
    scheduling_probs.rename(columns={
        'Purpose': 'purpose_id', 'EntryPeriod': 'entry_period',
        'ReturnPeriod': 'return_period', 'Percent': 'prob'}, inplace=True)
    scheduling_probs = scheduling_probs.pivot(
        index='purpose_id', columns=['entry_period','return_period'], values='prob')
    scheduling_probs.columns = [
        str(col[0]) + '_' + str(col[1]) for col in scheduling_probs.columns]

    scheduling_probs.to_csv(os.path.join(config_dir, output_probs_fname))
    tour_scheduling_alts.to_csv(
        os.path.join(config_dir, output_alts_fname), index=False)

    return


def assign_hh_p_to_tours(tours, persons):

    num_tours = len(tours)

    # assign persons and households to tours
    tours['household_id'] = np.random.choice(num_tours, num_tours, replace=False)
    tours['person_id'] = persons.set_index('household_id').reindex(
        tours['household_id'])['person_id'].values

    return tours


if __name__ == '__main__':

    # load settings
    with open('cross_border_preprocessing.yaml') as f:
        settings = yaml.load(f, Loader=yaml.FullLoader)

    # data_dir = settings['data_dir']
    # config_dir = settings['config_dir']
    # maz_input_fname = settings['maz_input_fname']
    # maz_id_field = settings['maz_id_field']
    # poe_id_field = settings['poe_id_field']
    # poe_access_field = settings['poe_access_field']
    # colonia_input_fname = settings['colonia_input_fname']
    # colonia_pop_field = settings['colonia_pop_field']
    # distance_param = settings['distance_param']
    # tour_settings = settings['tours']
    # poe_settings = settings['poes']
    # mazs_output_fname = settings['mazs_output_fname']
    # households_output_fname = settings['households_output_fname']
    # persons_output_fname = settings['persons_output_fname']
    # tours_output_fname = settings['tours_output_fname']
    # skims_settings = settings['skims']
    
    # # create input data
    # mazs = create_land_use_file(settings)
    # new_mazs = mazs[mazs['original_MAZ'] > 0]
    # tours = create_tours(tour_settings)
    # households = create_households(settings)  # 1 per tour
    # persons = create_persons(settings, num_households=len(households))
    # tours = assign_hh_p_to_tours(tours, persons)

    # # store input files to disk
    # mazs.to_csv(os.path.join(data_dir, mazs_output_fname), index=False)
    # tours.to_csv(os.path.join(data_dir, tours_output_fname))
    # households.to_csv(os.path.join(data_dir, households_output_fname), index=False)
    # persons.to_csv(os.path.join(data_dir, persons_output_fname), index=False)

    # create/update configs in place
    # create_scheduling_probs_and_alts(settings)
    # create_skims_and_tap_files(settings, new_mazs)
    create_stop_freq_specs(settings)
    # update_trip_purpose_probs(settings)
    # create_trip_scheduling_duration_probs(settings)
    # update_tour_purpose_reassignment_probs(settings)

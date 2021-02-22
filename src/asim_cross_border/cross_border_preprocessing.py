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


def create_tours(tour_settings):

    num_tours = tour_settings['num_tours']
    purpose_probs = OrderedDict(tour_settings['purpose_shares'])
    id_to_purpose = {i: purpose for i, purpose in enumerate(purpose_probs.keys())}
    lane_shares_by_purpose = tour_settings['lane_shares_by_purpose']

    
    tours = pd.DataFrame(
        index=range(tour_settings['num_tours']),
        columns=['lane_type', 'lane_id', 'tour_purpose', 'purpose_id'])
    tours.index.name = 'tour_id'

    purpose_cum_probs = np.array(list(purpose_probs.values())).cumsum()
    purpose_scaled_probs = np.subtract(purpose_cum_probs, np.random.rand(num_tours, 1))
    purpose = np.argmax((purpose_scaled_probs + 1.0).astype('i4'), axis=1)
    tours['purpose_id'] = purpose
    tours['tour_purpose'] = tours['purpose_id'].map(id_to_purpose)
    tours['tour_category'] = 'non_mandatory'
    tours.loc[tours['tour_purpose'].isin(['work', 'school', 'cargo']), 'tour_category'] = 'mandatory'
    tours['tour_type'] = tours['tour_purpose']

    for purpose, df in tours.groupby('tour_purpose'):
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


def create_persons(num_households):

    # one person per household
    persons = pd.DataFrame({'person_id': range(num_households)})
    persons['household_id'] = np.random.choice(num_households, num_households, replace=False)

    return persons


def update_input_table(settings, data_dir):

    input_fname = settings['input_fname']
    output_fname = settings['output_fname']

    df = pd.read_csv(os.path.join(data_dir, input_fname))
    df.rename(columns=settings['rename_columns'], inplace=True)
    df.to_csv(os.path.join(data_dir, output_fname), index=False)

    return


def create_maz_to_tap_drive(mazs, drive_skims):

    merged = pd.merge(mazs[['MAZ', 'TAZ']], drive_skims, on='TAZ')
    
    return merged


def rename_skims(settings, skim_type):
    
    data_dir = settings['data_dir']
    skims_settings = settings['skims'][skim_type]
    periods = skims_settings.get('periods', [None])

    for period in periods:
        print('Processing {0} {1} skims.'.format(period, skim_type))
        if period:
            input_base_fname = skims_settings['input_base_fname']
            output_base_fname = skims_settings['output_base_fname']
            input_fname = input_base_fname + '_' + period + '.omx'
            output_fname = output_base_fname + '_' + period + '.omx'
        else:
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
    settings['tour_scheduling_probs_output_fname']
    skims_settings = settings['skims']
    
    # # read input data
    # colonias = pd.read_csv(os.path.join(data_dir, colonia_input_fname))
    # mazs = pd.read_csv(os.path.join(data_dir, maz_input_fname))

    # # get poe id
    # mazs[poe_id_field] = None
    # for poe_id, poe_attrs in poe_settings.items():
    #     maz_mask = mazs[maz_id_field] == poe_attrs['maz_id']
    #     mazs.loc[maz_mask, poe_id_field] = poe_id

    # # compute colonia accessibility for poe mazs
    # mazs[poe_access_field] = None
    # poe_mask = ~pd.isnull(mazs[poe_id_field])
    # mazs.loc[poe_mask, poe_access_field] = mazs.loc[poe_mask, poe_id_field].apply(
    #     compute_poe_accessibility, colonias=colonias, colonia_pop_field=colonia_pop_field,
    #     distance_param=distance_param)
    # mazs = mazs.rename(columns={'mgra': 'MAZ', 'taz': 'TAZ'})

    # # create tours
    # tours = create_tours(tour_settings)

    # # create households, 1 per tour
    # num_tours = tour_settings['num_tours']
    # households = create_households(num_tours)

    # # create persons, 1 per household
    # num_households = len(households)
    # persons = create_persons(num_households)

    # # assign persons and households to tours
    # tours['household_id'] = np.random.choice(num_tours, num_tours, replace=False)
    # tours['person_id'] = persons.set_index('household_id').reindex(tours['household_id'])['person_id'].values

    # # reformat table of tour scheduling prob
    # scheduling_probs = pd.read_csv(
    #     os.path.join(settings['data_dir'], settings['tour_scheduling_probs_input_fname']))
    # scheduling_probs.rename(columns={
    #     'Purpose': 'purpose_id', 'EntryPeriod': 'entry_period',
    #     'ReturnPeriod': 'return_period', 'Percent': 'prob'}, inplace=True)
    # scheduling_probs = scheduling_probs.pivot(
    #     index='purpose_id', columns=['entry_period','return_period'], values='prob')
    # scheduling_probs.columns = [str(col[0]) + '_' + str(col[1]) for col in scheduling_probs.columns]

    # # get poe wait times in the right place
    # poe_wait_times = pd.read_csv(os.path.join(data_dir, settings['poe_wait_times_input_fname']))
    # poe_wait_times = pd.merge(poe_wait_times, mazs[['MAZ','poe_id']], left_on='poe', right_on='poe_id')
    # poe_wait_times.to_csv(os.path.join(config_dir, settings['poe_wait_times_output_fname']))

    # # store results
    # mazs.to_csv(os.path.join(data_dir, mazs_output_fname), index=False)
    # tours.to_csv(os.path.join(data_dir, tours_output_fname))
    # households.to_csv(os.path.join(data_dir, households_output_fname), index=False)
    # persons.to_csv(os.path.join(data_dir, persons_output_fname), index=False)
    # scheduling_probs.to_csv(os.path.join(config_dir, settings['tour_scheduling_probs_output_fname']))

    # # update skims/network data
    # update_input_table(skims_settings['maz_to_maz']['walk'], data_dir)
    # update_input_table(skims_settings['maz_to_tap']['walk'], data_dir)

    # # rename transit and auto skims
    for skim_type in [
            # 'tap_to_tap',
            'taz_to_taz']:
        rename_skims(settings, skim_type)

    # # create taps and taplines
    # create_taps_tap_lines(settings)
    


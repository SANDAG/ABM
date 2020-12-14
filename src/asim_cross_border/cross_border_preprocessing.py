import pandas as pd
import numpy as np
import os
import yaml
from collections import OrderedDict


def create_poe_mazs(poe_settings, mazs, poe_id_field):

    poe_mazs = pd.DataFrame(columns=list(mazs.columns) + [poe_id_field])
    poe_dist_cols = [col for col in colonias.columns if 'Distance_' in col]

    for i, poe in enumerate(poe_dist_cols):
        poe_mazs = poe_mazs.append({poe_id_field: i}, ignore_index=True)

    return poe_mazs


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


def create_households(num_tours):

    # one household per tour
    households = pd.DataFrame({'household_id': range(num_tours)})

    return households


def create_persons(num_households):

    # one person per household
    persons = pd.DataFrame({'person_id': range(num_households)})
    persons['household_id'] = np.random.choice(num_households, num_households, replace=False)

    return persons


if __name__ == '__main__':

    # load settings
    with open('cross_border_preprocessing.yaml') as f:
        settings = yaml.load(f)
    data_dir = settings['data_dir']
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
    
    # read input data
    colonias = pd.read_csv(os.path.join(data_dir, colonia_input_fname))
    mazs = pd.read_csv(os.path.join(data_dir, maz_input_fname))

    # get poe id
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

    # create tours
    tours = create_tours(tour_settings)

    # create households, 1 per tour
    num_tours = tour_settings['num_tours']
    households = create_households(num_tours)

    # assign tours to households
    tours['household_id'] = np.random.choice(num_tours, num_tours, replace=False)

    # create persons, 1 per household
    num_households = len(households)
    persons = create_persons(num_households)

    # store results
    mazs.to_csv(os.path.join(data_dir, mazs_output_fname), index=False)
    tours.to_csv(os.path.join(data_dir, tours_output_fname))
    households.to_csv(os.path.join(data_dir, households_output_fname), index=False)
    persons.to_csv(os.path.join(data_dir, persons_output_fname), index=False)


import pandas as pd
import numpy as np
import os
import yaml
from collections import OrderedDict

colonia_fname = 'crossBorder_supercolonia.csv'
maz_input_fname = 'mgra13_based_input2016.csv'
maz_output_fname = 'mazs.csv'
data_dir = 'data'
config_dir = 'configs_sandag_cb'
tour_settings_fname = 'initialize_tours.yaml'

distance_param = -0.19
colonia_pop_field = 'Population'
poe_access_field = 'poe_population_accessibility'


def create_poe_mazs(colonias, mazs, colonia_pop_field=colonia_pop_field):

    poe_mazs = pd.DataFrame(columns=list(mazs.columns) + ['poe_id'])
    poe_dist_cols = [col for col in colonias.columns if 'Distance_' in col]

    for i, poe in enumerate(poe_dist_cols):
        poe_mazs = poe_mazs.append({'poe_id': i}, ignore_index=True)

    return poe_mazs


def compute_poe_accessibility(
        poe_id, colonias, colonia_pop_field=colonia_pop_field,
        distance_param=distance_param):

    pop = colonias[colonia_pop_field]
    dist_col = 'Distance_poe' + str(int(poe_id))
    dists = colonias[dist_col]
    dist_factors = np.exp(dists * distance_param)
    weighted_pop = dist_factors * pop
    total_poe_pop_access = np.log(weighted_pop.sum())

    return total_poe_pop_access


def create_tours(tour_settings_fname, config_dir=config_dir):

    with open(os.path.join(config_dir, tour_settings_fname)) as f:
        tour_settings = yaml.load(f)

    num_tours = tour_settings['num_tours']
    purpose_probs = OrderedDict(tour_settings['purpose_shares'])
    id_to_purpose = {i: purpose for i, purpose in enumerate(purpose_probs.keys())}
    lane_shares_by_purpose = tour_settings['lane_shares_by_purpose']

    
    tours = pd.DataFrame(
        index=range(tour_settings['num_tours']),
        columns=['lane_type', 'lane_id', 'purpose', 'purpose_id'])
    tours.index.name = 'tour_id'

    purpose_cum_probs = np.array(list(purpose_probs.values())).cumsum()
    purpose_scaled_probs = np.subtract(purpose_cum_probs, np.random.rand(num_tours, 1))
    purpose = np.argmax((purpose_scaled_probs + 1.0).astype('i4'), axis=1)
    tours['purpose_id'] = purpose
    tours['purpose'] = tours['purpose_id'].map(id_to_purpose)

    for purpose, df in tours.groupby('purpose'):
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
    
    # read input data
    colonias = pd.read_csv(os.path.join(data_dir, colonia_fname))
    mazs = pd.read_csv(os.path.join(data_dir, maz_fname))

    # create poe maz rows
    poe_mazs = create_poe_mazs(colonias, mazs)

    # compute poe accessibility
    poe_mazs[poe_access_field] = poe_mazs['poe_id'].apply(compute_poe_accessibility, colonias=colonias)

    # add poe mazs to maz file
    mazs = mazs.append(poe_mazs, ignore_index=True)

    # create tours
    tours = create_tours(tour_settings_fname)

    # create households, 1 per tour
    num_tours = len(tours)
    households = create_households(num_tours)

    # assign tours to households
    tours['household_id'] = np.random.choice(num_tours, num_tours, replace=False)

    # create persons, 1 per household
    num_households = len(households)
    persons = create_persons(num_households)

    # store results
    mazs.to_csv(os.path.join(data_dir, maz_output_fname))
    tours.to_csv(os.path.join(data_dir, tours))
    households.to_csv(os.path.join(data_dir, households))
    persons.to_csv(os.path.join(data_dir, persons))


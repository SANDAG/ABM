# Import Modules
import copy
import pandas as pd
import numpy as np
import yaml
import os

#   This script prepares visitor tour enumeration data for Activity Sim by performing the following steps:
#   1. Generate number of visitor parties by segment (Personal or Business) [calculate_n_parties()]
#   2. Generate tours by tour_type for each segment [simulate_tour_types()]
#   3. Generate features party size, income, and car availability [simulate_tour_features()]


# Generic functions
def load_tables(file_path, nested_dict):
    # This function recursively loads all the CSV tables from the yaml file
    # and returns a mirrored nested dict with tables where CSV file paths were stored
    output = copy.deepcopy(nested_dict)
    for k, v in output.items():
        if isinstance(v, dict):
            output[k] = load_tables(file_path, v)
        else:
            output[k] = pd.read_csv(os.path.join(file_path, v))
    return output

# Main injection point for preprocessing
def preprocess_visitor():
    # Read the visitor settings YAML
    with open('configs/visitor/visitors.yaml') as f:
        parameters = yaml.load(f, Loader=yaml.FullLoader)

    # Read in the CSV-stored distribution values indicated in the yaml
    tables = load_tables(
        file_path=parameters['tables_dir'],
        nested_dict=parameters['visitor_tables']
    )

    # Add in the land_use table from data
    tables['land_use'] = pd.read_csv(
        os.path.join(*[parameters.get(x) for x in ['data_dir', 'land_use']])
    )

    # Setup
    setup_tour_scheduling(tables['tour_TOD'])

    # Generate tours
    print("Generating visitor tours")
    tours, households, persons = simulate_tours(tables, parameters)

    # Save to csv

    return tours, households, persons


# Visitor model functions
def simulate_tours(tables, parameters):
    # Probability distribution tables
    tours = []
    for index, maz in tables['land_use'].iterrows():
        # Calculate number of parties per visitor segment [personal/business] in the origin
        n_seg_parties = calculate_n_parties(n_hh=maz['hh'],
                                            n_hotel=maz['hotelroomtotal'],
                                            parameters=parameters)

        # If not empty
        if n_seg_parties:
            # Generate tours for each segment (personal or business)
            maz_tours = simulate_tour_features(n_seg_parties, tables)
            maz_tours['origin'] = int(maz['MAZ'])
            tours.append(maz_tours)
    tours = pd.concat(tours)

    # Assign travel household_id, which we can assume as equivalent to party_id
    tours['household_id'] = tours.groupby(['origin', 'party']).ngroup()
    tours.household_id += 1

    # Assign tour id
    tours = tours.reset_index(drop=True).drop(columns=['party'])
    tours = tours.reset_index().rename(columns={'index': 'tour_id'})
    tours.tour_id += 1

    # Create person and household data, then assign person to each tour from the associated household
    tours, households, persons = assign_person_households(tours)
    return tours, households, persons


def calculate_n_parties(n_hh, n_hotel, parameters):
    # Estimate number of visitor parties in hotel rooms and households
    hotel_parties = n_hotel * parameters['occupancy_rate']['hotel']
    household_parties = n_hh * parameters['occupancy_rate']['household']
    # Estimate number of parties by segment
    business_percent = parameters['business_percent']

    n_parties = {
        'business': round(
            hotel_parties * business_percent['hotel'] + household_parties * business_percent['household']
        ),
        'personal': round(
            hotel_parties * (1-business_percent['hotel']) + household_parties * (1-business_percent['household'])
        )
    }

    # Removing any zero parties to avoid simulating non-parties.
    n_parties = {k: v for (k, v) in n_parties.items() if v > 0}

    return n_parties


def simulate_tour_features(n_seg_parties, tables):
    # Function to simulate tours for k visitor parties' features (party size, auto availability, and income
    # Require tour_type and segment strings, returns a data frame

    # Probability distribution tables
    probs_size = tables['party_size']
    probs_auto = tables['auto_available']
    probs_income = tables['income']

    # Generates a list of tour tour_type frequencies per party
    party_tours = simulate_tour_types(n_seg_parties, tables)

    # Expand the tour tour_types by N tour counts into a list
    party_tours = party_tours.loc[party_tours.index.repeat(party_tours['count'])].drop(columns='count')

    # Group by tour tour_type frequency and segment, to bulk simulate k tours by tour_type
    freq_group = party_tours.groupby(['tour_type', 'segment'])

    # Estimate the features for each of the tours, based on tour_type, result to data frame
    # tour tour_type, segment, party size [int], auto availability [true/false], and income [int]
    tour_list = []
    for (tour_type, segment), group in freq_group:
        k = len(group)
        tour = {
            'tour_type': [tour_type] * k, 'segment': [segment] * k,
            'number_of_participants': probs_size.sample(n=k, weights=tour_type).PartySize.values,
            'auto_available': np.random.binomial(1, probs_auto[tour_type], k),
            'income': probs_income.sample(n=k, weights=segment).Income.values,
            'tour_category': 'mandatory' if tour_type == 'work' else 'non-mandatory'
        }
        tour_list.append(pd.DataFrame(tour))
    tour_features = pd.concat(tour_list)

    # Join tour party features by segment and tour_type. Need to align the indices first
    party_tours = party_tours.sort_values(by=['tour_type', 'segment']).reset_index(drop=True)
    tour_features = tour_features.sort_values(by=['tour_type', 'segment']).reset_index(drop=True)
    party_tours = party_tours[['party']].join(tour_features).sort_values(by=['party']).reset_index(drop=True)

    # calc the tour_num out of tour_count
    party_tours['tour_num'] = party_tours.groupby(['party']).cumcount() + 1
    party_tours = party_tours.merge(
        party_tours.groupby(['party']).size().reset_index(name='tour_count'),
        on='party')

    return party_tours


def simulate_tour_types(parties, tables):
    # Probability distribution tables
    probs_segment = tables['segment_frequency']

    segment_parties = []
    for s, n in parties.items():
        party_tours = probs_segment[s].sample(n=n, weights='Percent').reset_index(drop=True)
        party_tours.index += 1  # Ensures that there's no party id of 0 that would get dropped

        # Cleanup labels
        party_tours = party_tours.rename(
            columns={'WorkTours': 'work', 'RecreationTours': 'recreate', 'DiningTours': 'dining'}
        ).drop(columns=['Percent', 'TotalTours']).reset_index().rename(columns={'index': 'party'})

        # Reshape to long and remove 0s
        party_tours = pd.melt(party_tours,
                              id_vars='party',
                              var_name='tour_type',
                              value_name='count').replace(0, pd.NA).dropna()

        # Add visitor segment group
        party_tours['segment'] = s

        # Add to data list
        segment_parties.append(party_tours)

    # Stack the result
    return pd.concat(segment_parties)


def assign_person_households(tours):
    # Group by hh party
    hh_parties = tours.groupby(['household_id'])['number_of_participants']

    # Calculate total number of visitors/households based on N parties and sum of max party sizes for each party
    total_households = tours.household_id.max()

    # Create households and persons
    households = pd.DataFrame({'household_id': range(0, total_households)})
    households.household_id += 1

    # For each hh party, create n-persons into it
    persons = pd.DataFrame()
    person_count = 1
    for hh_id, party in hh_parties:
        household = pd.DataFrame({'person_id': range(0, party.max()), 'household_id': hh_id})
        household.person_id += person_count
        person_count += party.max()
        persons = pd.concat([persons, household])
    persons.reset_index(drop=True, inplace=True)

    # Assign person id to the tour based on household_id, just assign the first person in each household
    tours = tours.merge(persons.groupby('household_id').first().reset_index(), on='household_id')

    # Return all three
    return tours, households, persons


def setup_tour_scheduling(tour_tod):
    # Concatenate entry/return periods into i_j formatted columns
    probs = tour_tod
    probs['period'] = probs[['EntryPeriod', 'ReturnPeriod']].astype(str).agg('_'.join, axis=1)

    # Reshape from long to wide
    probs = tour_tod.pivot(index='Purpose', columns='period', values='Percent').reset_index()

    # Relabel cols/rows
    probs = probs.rename(columns={'Purpose': 'purpose_id'})

    # TODO create yaml too
    return probs


def setup_stop_purpose():
    pass


def setup_stop_frequency():
    pass


def create_joint_tours():
    pass


def write_to_csv():
    # probs.to_csv(
    #     os.path.join(self.parameters['data_dir'], 'tour_scheduling_probs.csv'),
    #     index=False)
    pass


if __name__ == '__main__':
    # Testing
    os.chdir('src/asim')
    tours, households, persons = preprocess_visitor()
    # maz = self.tables['land_use'].loc[0,]

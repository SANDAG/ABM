# Import Modules
import copy
import random
import pandas as pd
import numpy as np
import yaml
import os

#####################################################################################################
#   This script prepares visitor distribution data for simulation by performing the following steps:
#   1. Visitor Tour Enumeration
#       a. Generate number of visitor parties by segment (Personal or Business) [calculate_n_parties()]
#       b. Generate the number of tours by segment, party size, income, and car availability [create_tours()]
#   2.
#####################################################################################################

# Generic functions
def load_tables_recursively(file_path, nested_dict):
    # This function loads all the CSV tables from the yaml file
    # and returns a mirrored nested dict with tables where CSV file paths were stored
    output = copy.deepcopy(nested_dict)
    for k, v in output.items():
        if isinstance(v, dict):
            output[k] = load_tables_recursively(file_path, v)
        else:
            output[k] = pd.read_csv(os.path.join(file_path, v))
    return output


# Visitor tour generation model class
class Visitor:
    def __init__(self):
        # Read the visitor settings YAML
        with open('configs/visitor/visitors.yaml') as f:
            self.parameters = yaml.load(f, Loader=yaml.FullLoader)

        # Read in the CSV-stored distribution values indicated in the yaml
        self.tables = load_tables_recursively(
            file_path=self.parameters['data_dir'],
            nested_dict=self.parameters['visitor_tables']
        )
        # self.simulate_maz_tours()

    def calculate_n_parties(self, n_hh, n_hotel):
        # Estimate number of visitor parties in hotel rooms and households
        hotel_parties = n_hotel * self.parameters['occupancy_rate']['hotel']
        household_parties = n_hh * self.parameters['occupancy_rate']['household']

        # Estimate number of parties by segment
        business_percent = self.parameters['business_percent']
        n_parties = {
            'business': round(
                hotel_parties * business_percent['hotel'] + household_parties * business_percent['household']
            ),
            'personal': round(
                hotel_parties * (1-business_percent['hotel']) + household_parties * (1-business_percent['household'])
            )
        }

        # Removing any zero parties to avoid simulating non-parties.
        n_parties = {k:v for (k,v) in n_parties.items() if v > 0}

        return n_parties

    def simulate_tour_purposes(self, parties):
        segment_parties = []
        for segment, n in parties.items():
            party_tours = self.tables['segment_frequency'][segment].sample(n=n, weights='Percent').reset_index(drop=True)
            party_tours.index += 1 # Ensures that theres no party id of 0 that would get dropped

            # Cleanup labels
            party_tours = party_tours.rename(
                columns={'WorkTours': 'work', 'RecreationTours': 'recreate', 'DiningTours': 'dining'}
            ).drop(columns=['Percent', 'TotalTours']).reset_index().rename(columns={'index': 'party'})

            # Reshape to long and remove 0s
            party_tours = pd.melt(party_tours,
                                  id_vars='party',
                                  var_name='purpose',
                                  value_name='count').replace(0,pd.NA).dropna()

            # Add visitor segment group
            party_tours['segment'] = segment

            # Add to data list
            segment_parties.append(party_tours)

        #Stack the result
        return pd.concat(segment_parties)

    def simulate_tour_features(self, purpose, segment,k ):
        # This function simulates tour features in bulk based on k tours purposes, result gets joined to party table

        # Estimate the features for each of the tours, based on purpose, result to data frame
        # tour purpose, segment, party size [int], auto availability [true/false], and income [int]
        tour = {
            'purpose': [purpose] * k, 'segment': [segment] * k,
            'number_of_participants': self.tables['party_size'].sample(n=k, weights=purpose).PartySize.values,
            'auto_available': np.random.binomial(1, self.tables['auto_available'][purpose], k),
            'income': self.tables['income'].sample(n=k, weights=segment).Income.values
        }

        return pd.DataFrame(tour)

    def simulate_tours(self, n_seg_parties):
        # Function to simulate tours for k visitor parties' features (party size, auto availability, and income
        # Require purpose and segment strings, returns a data frame

        # Generates a list of tour purpose frequencies per party
        party_tours = self.simulate_tour_purposes(n_seg_parties)

        # Expand the tour purposes by N tour counts into a list
        party_tours = party_tours.loc[party_tours.index.repeat(party_tours['count'])].drop(columns='count')

        # Group by tour purpose frequency and segment, to bulk simulate k tours by purpose
        # then expanded feature table for the tour purposes
        freq_group = party_tours.groupby(['purpose', 'segment'])
        tour_features = pd.concat([self.simulate_tour_features(p, s, k=len(g)) for (p, s), g in freq_group])

        # Join tour party to features by segment and purpose. Need to align the indices first
        party_tours = party_tours.sort_values(by=['purpose', 'segment']).reset_index(drop=True)
        tour_features = tour_features.sort_values(by=['purpose', 'segment']).reset_index(drop=True)
        party_tours = party_tours[['party']].join(tour_features).sort_values(by=['party']).reset_index(drop=True)

        # calc the tour_num out of tour_count
        party_tours['tour_num'] = party_tours.groupby(['party']).cumcount() + 1
        party_tours = party_tours.merge(
            party_tours.groupby(['party']).size().reset_index(name='tour_count'),
            on='party')

        return party_tours

    def simulate_maz_tours(self):
        print("Generating visitor tours")

        tours = []
        for index, maz in self.tables['land_use'].iterrows():
            # Calculate number of parties per visitor segment [personal/business] in the MAZ
            n_seg_parties = self.calculate_n_parties(n_hh=maz['hh'], n_hotel=maz['hotelroomtotal'])

            # If not empty
            if n_seg_parties:
                # Generate tours for each segment (personal or business)
                maz_tours = self.simulate_tours(n_seg_parties)
                maz_tours['MAZ'] = int(maz['MAZ'])
                tours.append(maz_tours)
        tours = pd.concat(tours)

        # Assign person id
        tours['person_id'] = tours.groupby(['MAZ','party']).ngroup()

        # Assign tour id
        tours = tours.reset_index(drop=True).drop(columns=['party'])
        tours = tours.reset_index().rename(columns={'index': 'tour_id'})

        return pd.DataFrame(tours)


if __name__ == '__main__':
    os.chdir('src/asim')
    self = Visitor()
    maz = self.tables['land_use'].loc[5,]

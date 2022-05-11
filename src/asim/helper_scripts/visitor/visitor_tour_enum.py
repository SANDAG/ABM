import os
import pandas as pd
import numpy as np


class TourEnumMixin:
    def create_tour_enumeration(self, tables, parameters):
        # This is the main tour generation function.
        # 1. Generate n number of parties per visitor_travel_type
        # 2. Simulate n tours and tour features for each party
        # 3. Assign households and persons to the tours

        # Probability distribution tables
        tours = []
        for index, maz in tables['land_use'].iterrows():
            # Calculate number of parties per visitor visitor_travel_type [personal/business] in the origin
            n_seg_parties = self.calculate_n_parties(n_hh=maz['hh'],
                                                     n_hotel=maz['hotelroomtotal'],
                                                     parameters=parameters)

            # If not empty
            if n_seg_parties:
                # Generate tours for each visitor_travel_type (personal or business)
                maz_tours = self.simulate_tour_features(n_seg_parties, tables)
                maz_tours['origin'] = int(maz['MAZ'])
                tours.append(maz_tours)
        tours = pd.concat(tours)

        # Map purpose IDs to column
        tours['purpose_id'] = tours.tour_type.map(parameters['purpose_ids'])

        # Assign travel household_id, which we can assume as equivalent to party_id
        tours['household_id'] = tours.groupby(['origin', 'party']).ngroup()
        tours.household_id += 1

        # Assign tour id
        tours = tours.reset_index(drop=True).drop(columns=['party'])
        tours = tours.reset_index().rename(columns={'index': 'tour_id'})
        tours.tour_id += 1

        # Create person and household data, then assign person to each tour from the associated household
        tours_enum = self.assign_person_households(tours)

        # Save output to corresponding csv
        if parameters['overwrite']:
            for name, output in tours_enum.items():
                output.to_csv(os.path.join(parameters['data_dir'], parameters['output_fname'][name]), index=False)

        return tours_enum

    def calculate_n_parties(self, n_hh, n_hotel, parameters):
        # Estimate number of visitor parties in hotel rooms and households
        hotel_parties = n_hotel * parameters['occupancy_rate']['hotel']
        household_parties = n_hh * parameters['occupancy_rate']['household']
        # Estimate number of parties by visitor_travel_type
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

    def simulate_tour_features(self, n_seg_parties, tables):
        # Function to simulate tours for k visitor parties' features (party size, auto availability, and income
        # Require tour_type and visitor_travel_type strings, returns a data frame

        # Probability distribution tables
        probs_size = tables['party_size']
        probs_auto = tables['auto_available']
        probs_income = tables['income']

        # Generates a list of tour tour_type frequencies per party
        party_tours = self.simulate_tour_types(n_seg_parties, tables)

        # Expand the tour tour_types by N tour counts into a list
        party_tours = party_tours.loc[party_tours.index.repeat(party_tours['count'])].drop(columns='count')

        # Group by tour tour_type frequency and visitor_travel_type, to bulk simulate k tours by tour_type
        freq_group = party_tours.groupby(['tour_type', 'visitor_travel_type'])

        # Estimate the features for each of the tours, based on tour_type, result to data frame
        # tour tour_type, visitor_travel_type, party size [int], auto availability [true/false], and income [int]

        # 0 <$30k
        # 1 $30-60k
        # 2 $60-100k
        # 3 $100-150k
        # 4 $150k +

        tour_list = []
        for (tour_type, visitor_travel_type), group in freq_group:
            k = len(group)
            tour = {
                'tour_type': [tour_type] * k,
                'visitor_travel_type': [visitor_travel_type] * k,
                'number_of_participants': probs_size.sample(n=k, weights=tour_type, replace=True).PartySize.values,
                'auto_available': np.random.binomial(1, probs_auto[tour_type], k),
                'income': probs_income.sample(n=k, weights=visitor_travel_type, replace=True).Income.values,
                'tour_category': 'non_mandatory'
            }
            tour_list.append(pd.DataFrame(tour))
        tour_features = pd.concat(tour_list)

        # Join tour party features by visitor_travel_type and tour_type. Need to align the indices first
        party_tours = party_tours.sort_values(by=['tour_type', 'visitor_travel_type']).reset_index(drop=True)
        tour_features = tour_features.sort_values(by=['tour_type', 'visitor_travel_type']).reset_index(drop=True)
        party_tours = party_tours[['party']].join(tour_features).sort_values(by=['party']).reset_index(drop=True)

        # calc the tour_num out of tour_count
        party_tours['tour_num'] = party_tours.groupby(['party']).cumcount() + 1
        party_tours = party_tours.merge(
            party_tours.groupby(['party']).size().reset_index(name='tour_count'),
            on='party')

        return party_tours

    def simulate_tour_types(self, parties, tables):
        # Probability distribution tables
        probs_visitor_travel_type = tables['visitor_travel_type_frequency']

        visitor_travel_type_parties = []
        for s, n in parties.items():
            party_tours = probs_visitor_travel_type[s].sample(n=n, weights='Percent').reset_index(drop=True)
            party_tours.index += 1  # Ensures that there's no party id of 0 that would get dropped

            # Cleanup labels
            party_tours = party_tours.rename(
                columns={'WorkTours': 'work', 'RecreationTours': 'recreation', 'DiningTours': 'dining'}
            ).drop(columns=['Percent', 'TotalTours']).reset_index().rename(columns={'index': 'party'})

            # Reshape to long and remove 0s
            party_tours = pd.melt(party_tours,
                                  id_vars='party',
                                  var_name='tour_type',
                                  value_name='count').replace(0, pd.NA).dropna()

            # Add visitor visitor_travel_type group
            party_tours['visitor_travel_type'] = s

            # Add to data list
            visitor_travel_type_parties.append(party_tours)

        # Stack the result
        return pd.concat(visitor_travel_type_parties)

    def assign_person_households(self, tours, count_hh_members=False):
        # Calculate total number of visitors/households based on N parties and sum of max party sizes for each party
        total_households = tours.household_id.max()

        # Create households and persons
        households = pd.DataFrame({'household_id': range(0, total_households)})
        households.household_id += 1

        # For each hh party, create a household and create n-persons into it
        # persons = pd.DataFrame()
        persons = []
        households = []
        person_count = 1
        for hh_id, party in tours.groupby(['household_id']):
            hh_size = party.number_of_participants.max() if count_hh_members else 1
            household_persons = pd.DataFrame({'person_id': range(0, hh_size),
                                              'household_id': hh_id,
                                              'home_zone_id': party.origin.iloc[0]})
            household_persons.person_id += person_count
            person_count += hh_size
            persons.append(household_persons)
            households.append(household_persons.drop(columns='person_id').head(1))

        persons = pd.concat(persons).reset_index(drop=True)
        households = pd.concat(households).reset_index(drop=True)

        # Assign person id to the tour based on household_id, just assign the first person in each household
        tours = tours.merge(persons.groupby('household_id').first().reset_index(), on='household_id')

        # Return all three
        return {'tours': tours, 'households': households, 'persons': persons}

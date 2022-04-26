# Import Modules
import copy
import yaml
import os
import itertools
import seaborn as sns
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import Rbf


#   This script prepares visitor tour enumeration data for Activity Sim by performing the following steps:
#   1. Generate number of visitor parties by segment (Personal or Business) [calculate_n_parties()]
#   2. Generate tours by tour_type for each segment [simulate_tour_types()]
#   3. Generate features party size, income, and car availability [simulate_tour_features()]

# Main injection point for preprocessing
def preprocess_visitor():
    # Read the visitor settings YAML
    with open('configs/visitor/preprocessing.yaml') as f:
        parameters = yaml.load(f, Loader=yaml.FullLoader)

    # Read in the CSV-stored distribution values indicated in the yaml
    tables = load_tables(
        file_path=parameters['tables_dir'],
        nested_dict=parameters['input_data']
    )

    # Add in the land_use table from data
    tables['land_use'] = pd.read_csv(
        os.path.join(*[parameters.get(x) for x in ['data_dir', 'land_use']])
    )

    # Generate tours
    print("Generating visitor tours")

    # TODO NEED TO WRITE THESE TO CSV WITHIN EACH FUNCTION

    processed_data = simulate_tours(tables, parameters)

    # Setup
    processed_data['tour_scheduling_probs'] = create_tour_scheduling_probs(tables['tour_TOD'], parameters)
    # create_stop_freq_specs(tables['stop_frequency'], parameters)

    # create/update configs in place
    ##### create_scheduling_probs_and_alts(settings, los_settings)
    # create_skims_and_tap_files(settings, new_mazs)
    # create_stop_freq_specs(settings)
    # update_trip_purpose_probs(settings)
    # create_trip_scheduling_duration_probs(settings, los_settings)


    # Save to csv
    # tour_scheduling_probs.to_csv()

    return processed_data


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


def simulate_tours(tables, parameters):

    # This is the main tour generation function.
    # 1. Generate n number of parties per segment
    # 2. Simulate n tours and tour features for each party
    # 3. Assign households and persons to the tours

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
    tours_persons_households = assign_person_households(tours)

    # Save output to corresponding csv
    for name, output in tours_persons_households.items():
        output.to_csv(os.path.join(parameters['config_dir'], parameters['output_data'][name]))

    return tours_persons_households


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


def assign_person_households(tours, count_persons=False):
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
        person_count += party.max() if count_persons else 1
        persons = pd.concat([persons, household])
    persons.reset_index(drop=True, inplace=True)

    # Assign person id to the tour based on household_id, just assign the first person in each household
    tours = tours.merge(persons.groupby('household_id').first().reset_index(), on='household_id')

    # Return all three
    return {'tours': tours, 'households': households, 'persons': persons}


def tod_aggregate_melt(todi):
    todiagg = []
    for x in ['EntryPeriod', 'ReturnPeriod']:
        agg = todi.groupby(x)['Percent'].sum().reset_index().rename(columns={x: 'Time Period'})
        agg['Period Type'] = x
        todiagg.append(agg)
    todiagg = pd.concat(todiagg)
    newperiods = (todiagg['Time Period'] > 40) & (todiagg['Time Period'] <= 48)

    todiagg.loc[newperiods, 'Period Type'] = todiagg.loc[newperiods, 'Period Type'] + ' (extrapolated)'

    return todiagg.reset_index()


def create_tour_scheduling_probs(tour_tod, parameters):
    # convert ctramp 40-period to 48 period asim

    tour_todi = []
    for purpose_id, tod in tour_tod.groupby('Purpose'):
        # Inform Temporal Loop (i.e., tell the computer that 24hr day repeats and 41-48 starts back at 1)
        extend = {'diag': ['EntryPeriod', 'ReturnPeriod'], 'top': ['EntryPeriod'], 'right': ['ReturnPeriod']}

        # Expand to full 1-48 matrix size
        todx_base = pd.DataFrame(itertools.product(np.linspace(1,48, 48), repeat=2), columns=["EntryPeriod", "ReturnPeriod"])
        todx_base = todx_base.merge(tod.drop(columns='Purpose'), on=['EntryPeriod', 'ReturnPeriod'], how='outer')

        todx = copy.deepcopy(todx_base)
        for side, cols in extend.items():
            tody = copy.deepcopy(todx_base)
            tody[cols] += 48
            todx = pd.concat([todx, tody])

        # Extract into x,y,z for interpolation
        x, y, z = [tod[col].values for col in todx[['EntryPeriod', 'ReturnPeriod', 'Percent']]]

        # Radial Function Interpolation
        rbf = Rbf(x, y, z, function='gaussian')

        # Extrapolate the missing points
        todxi = copy.deepcopy(todx)
        todxi.loc[todxi.Percent.isnull(), 'Percent'] = rbf(todxi[todxi.Percent.isnull()].EntryPeriod,
                                                           todxi[todxi.Percent.isnull()].ReturnPeriod)

        # Set floor
        todxi.loc[todxi.Percent < 0, 'Percent'] = 0

        # Re-normalize to unit scale
        todxi['Percent'] = todxi.Percent / todxi.Percent.sum()

        # Extract imputed tables
        todxi = todxi[(todxi.EntryPeriod <= 48) & (todxi.ReturnPeriod <= 48)]

        if parameters['plot_figs']:
            sns.heatmap(data=todxi.pivot("EntryPeriod", "ReturnPeriod", "Percent"))
            plt.savefig(os.path.join(parameters['plot_dir'], 'tod_heatmap_{}.png'.format(purpose_id)))
            plt.show()
            # Aggregate view
            todiagg = tod_aggregate_melt(todxi)
            sns.set_palette("Paired")
            # sns.barplot(data=todiagg, x='Time Period', y='Percent', hue='Period Type', dodge=False, alpha=0.3)
            sns.scatterplot(data=todiagg, x='Time Period', y='Percent', hue='Period Type')
            plt.xticks(list(range(0, 48, 4)), labels=list(range(0, 48, 4)))
            plt.savefig(os.path.join(parameters['plot_dir'], 'tod_{}.png'.format(purpose_id)))
            plt.show()

        todxi['Purpose'] = purpose_id

        tour_todi.append(todxi)


    # Concatenate the imputed tables
    tour_todi = pd.concat(tour_todi)

    # Concatenate entry/return periods into i_j formatted columns
    tour_todi['period'] = tour_todi[['EntryPeriod', 'ReturnPeriod']].astype(str).agg('_'.join, axis=1)
    # Reshape from long to wide
    tour_todi = tour_todi.pivot(index='Purpose', columns='period', values='Percent').reset_index()
    # Relabel cols/rows
    tour_todi = tour_todi.rename(columns={'Purpose': 'purpose_id'})

    # TODO create yaml too
    return tour_todi

# TODO NOT FULLY CONVERTED YET
def create_stop_freq_specs(probs_df, parameters):
    print("Creating stop frequency alts and probability lookups.")

    # Setup general vars
    output_names = parameters['output_names']
    config_dir = parameters['config_dir']
    purpose_id_map = parameters['purpose_ids']

    # convert probs to utils
    probs_df['value'] = probs_df['Percent'].apply(lambda x: np.log(x) if x > 0 else -999)

    # write out alts table
    # Format alt names
    probs_df.rename(columns={'Outbound': 'out', 'Inbound': 'in'}, inplace=True)
    probs_df['alt'] = probs_df[['out', 'in']].apply(lambda x: '{}out_{}in'.format(x[0], x[1]), axis=1)

    alts_df = probs_df.drop_duplicates(['out', 'in', 'alt'])[['alt', 'out', 'in']]
    alts_df.to_csv(os.path.join(config_dir, output_names['stop_frequency_alts_output_fname']), index=False)


    # iterate through purposes and pivot probability lookup tables to
    # create MNL spec files with only ASC's (no covariates).
    required_cols = ['Label', 'Description', 'Expression']
    for purpose, purpose_id in purpose_id_map.items():
        purpose_probs = probs_df.loc[probs_df['Purpose'] == purpose_id, :].copy()
        purpose_probs['coefficient_name'] = purpose_probs.apply(
            lambda x: 'coef_asc_dur_{0}_{1}_stops_{2}'.format(
                x['DurationLo'], x['DurationHi'], x['alt']), axis=1)

        coeffs_file = purpose_probs[['value', 'coefficient_name']].copy()
        coeffs_file['Description'] = None
        coeffs_file = coeffs_file[['Description', 'value', 'coefficient_name']]
        coeffs_file_fname = output_names[
            'stop_frequency_coeffs_output_formattable_fname'].format(
            purpose=purpose)
        coeffs_file.to_csv(
            os.path.join(config_dir, coeffs_file_fname),
            index=False)

        alt_cols = alts_df['alt'].tolist()
        expr_file = purpose_probs.pivot(
            index=['DurationLo', 'DurationHi'], columns='alt',
            values='coefficient_name').reset_index()
        expr_file['Label'] = 'util_ASC_tour_dur_' + \
                             expr_file['DurationLo'].astype(str) + \
                             '_' + expr_file['DurationHi'].astype(str)
        expr_file['Description'] = 'ASC for tour durations between ' + \
                                   expr_file['DurationLo'].astype(str) + ' and ' + \
                                   expr_file['DurationHi'].astype(str)
        expr_file['Expression'] = expr_file['DurationLo'].astype(str) + \
                                  ' < duration_hours <= ' + expr_file['DurationHi'].astype(str)
        expr_file = expr_file.drop(columns=['DurationLo', 'DurationHi'])
        expr_file = expr_file[required_cols + [
            col for col in expr_file.columns if col not in required_cols]]
        expr_file_fname = output_names['stop_frequency_expressions_output_formattable_fname'].format(purpose=purpose)
        expr_file.to_csv(os.path.join(config_dir, expr_file_fname.format(purpose)), index=False)

    return

if __name__ == '__main__':
    # Testing
    os.chdir('src/asim')
    # os.chdir('../asim')
    data = preprocess_visitor()
    # maz = self.tables['land_use'].loc[0,]

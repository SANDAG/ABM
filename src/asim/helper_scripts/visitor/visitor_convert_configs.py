import itertools
import copy
import os
import yaml
import pandas as pd
import numpy as np

# For interpolation
import seaborn as sns
import matplotlib.pyplot as plt
from scipy.interpolate import Rbf


class TripStopFrequencyMixin:
    def update_trip_purpose_probs(self, stop_purp_probs, parameters):
        print("Creating trip purpose probability lookup table.")
        purp_dict = self.parameters['purpose_map']
        # Rename columns to the purpose
        colnames = {'StopNum': 'trip_num', 'Multiple': 'multistop'}
        for x in stop_purp_probs.columns:
            if 'StopPurp' in x:
                colnames[x] = purp_dict[int(x.replace('StopPurp', ''))]
        stop_purp_probs.rename(columns=colnames, inplace=True)

        # Make outbound column, is a reflection of inbound boolean t/f
        stop_purp_probs['outbound'] = ~stop_purp_probs['Inbound'].astype(bool)

        # Add primary purpose names
        stop_purp_probs['primary_purpose'] = stop_purp_probs['TourPurp'].map(purp_dict)

        # Select column subset
        trip_purpose_probs = stop_purp_probs[['primary_purpose', 'outbound', 'trip_num', 'multistop'] +
                                     list(self.parameters['purpose_ids'].keys())]

        # Save to CSV
        file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
        if parameters['overwrite'] or not os.path.exists(file_path):
            trip_purpose_probs.to_csv(file_path, index=False)

        return trip_purpose_probs

    def create_trip_scheduling_duration_probs(self, inbound, outbound, parameters):
        print("Creating trip scheduling probability lookup table.")

        num_asim_periods = 48

        # Assign dummy variable to each to indicate which is which in long form
        outbound['outbound'], inbound['outbound'] = True, False

        # Reshape wide tables to long format so they can be stacked into one data frame
        duration_probs = []
        for df in [inbound, outbound]:
            duration_probs.append(
                df.melt(
                    id_vars=['RemainingLow', 'RemainingHigh', 'Stop', 'outbound'],
                    var_name='duration_offset', value_name='prob')
            )
        duration_probs = pd.concat(duration_probs, axis=0, ignore_index=True)

        # Cleanup column names
        duration_probs.rename(columns={
            'Stop': 'stop_num',
            'RemainingHigh': 'periods_left_max',
            'RemainingLow': 'periods_left_min'}, inplace=True)

        # convert to asim format
        max_periods_left = duration_probs['periods_left_max'].max()
        duration_probs['periods_left_max'] = duration_probs['periods_left_max'].replace(max_periods_left,
                                                                                        num_asim_periods - 1)

        # Save output
        file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_scheduling_probs'])
        if parameters['overwrite'] or not os.path.exists(file_path):
            duration_probs.to_csv(file_path, index=False)

            # Create associated yaml
            trip_scheduling = {'PROBS_SPEC': 'trip_scheduling_probs.csv',
                               'DEPART_ALT_BASE': 0,
                               'MAX_ITERATIONS': 100,
                               'FAILFIX': 'choose_most_initial',
                               'scheduling_mode': 'stop_duration'
                               }

            # Save YAML
            with open(os.path.join(parameters['config_dir'], 'trip_scheduling.yaml'), 'w') as file:
                yaml.dump(trip_scheduling, file)

        return duration_probs

    # NOTE: This script is relatively unchanged from xborder, probably needs some review/cleanup
    def create_stop_freq_specs(self, stop_freq_probs, parameters):
        print("Creating stop frequency alts and probability lookups.")

        # convert probs to utils
        stop_freq_probs['value'] = stop_freq_probs['Percent'].apply(lambda x: np.log(x) if x > 0 else -999)

        # Format alt names and write out alts table
        stop_freq_probs.rename(columns={'Outbound': 'out', 'Inbound': 'in'}, inplace=True)
        stop_freq_probs['alt'] = stop_freq_probs[['out', 'in']].apply(lambda x: '{}out_{}in'.format(x[0], x[1]), axis=1)

        stop_frequency_alts = stop_freq_probs.drop_duplicates(['out', 'in', 'alt'])[['alt', 'out', 'in']]

        # Annotation tables
        stop_frequency_annotate = pd.DataFrame({'Description': [None, 'copied from CTRAMP'],
                                         'Target': ['primary_purpose', 'duration_hours'],
                                         'Expression': ['df.tour_type', '(df.end - df.start + 1) * 0.5']})

        trip_purpose_annotate = pd.DataFrame({'Description': ['leg has multiple intermediate stops'],
                                         'Target': ['multistop'],
                                         'Expression': ['df.trip_count > 2']})

        # Create associated YAMLs for trip purpose and stop frequency
        trips_purpose_spec = {'PROBS_SPEC': 'trip_purpose_probs.csv',
                              'preprocessor':
                                  {'SPEC': 'trip_purpose_annotate_trips_preprocessor',
                                   'DF': 'trips',
                                   'TABLES': ['persons', 'tours']},
                              'probs_join_cols': ['primary_purpose', 'outbound', 'trip_num', 'multistop'],
                              'use_depart_time': False
                              }

        stop_frequency_spec = {'LOGIT_TYPE': 'MNL',
                              'preprocessor':
                                  {'SPEC': 'stop_frequency_annotate_tours_preprocessor',
                                   'DF': 'tours_merged'},
                              'SEGMENT_COL': 'primary_purpose',
                              'SPEC_SEGMENTS': []
                              }

        # Store output
        output = {'stop_frequency_alts': stop_frequency_alts}
        required_cols = ['Label', 'Description', 'Expression']

        # iterate through purposes and pivot probability lookup tables to
        # create MNL spec files with only ASC's (no covariates).

        for purpose, purpose_id in parameters['purpose_ids'].items():
            purpose_probs = stop_freq_probs.loc[stop_freq_probs['Purpose'] == purpose_id, :].copy()
            purpose_probs['coefficient_name'] = purpose_probs.apply(
                lambda x: 'coef_asc_dur_{0}_{1}_stops_{2}'.format(x['DurationLo'], x['DurationHi'], x['alt']), axis=1)

            coeffs_file = purpose_probs[['value', 'coefficient_name']].copy()
            coeffs_file['Description'] = None
            coeffs_file = coeffs_file[['Description', 'value', 'coefficient_name']]
            coeffs_file_fname = parameters['output_fname']['stop_frequency_coeffs'].format(purpose=purpose)

            expr_file = purpose_probs.pivot(
                index=['DurationLo', 'DurationHi'], columns='alt',
                values='coefficient_name').reset_index()

            expr_labels = {'Label': 'util_ASC_tour_dur_{}_{}',
                           'Description': 'ASC for tour durations between {} and {}',
                           'Expression': '{} < duration_hours <= {}'}

            for col, label in expr_labels.items():
                expr_file[col] = expr_file.apply(lambda x: label.format(x['DurationLo'], x['DurationHi']), axis=1)

            expr_file = expr_file.drop(columns=['DurationLo', 'DurationHi'])
            expr_file = expr_file[required_cols + [col for col in expr_file.columns if col not in required_cols]]
            expr_file_fname = parameters['output_fname']['stop_frequency_expressions'].format(purpose=purpose)

            # Update the YAML spec
            stop_frequency_spec['SPEC_SEGMENTS'].append({'primary_purpose': purpose,
                                                         'SPEC': expr_file_fname,
                                                         'COEFFICIENTS': coeffs_file_fname})

            # Save coefficients table to CSV
            if parameters['overwrite'] or not os.path.exists(coeffs_file_fname):
                coeffs_file.to_csv(os.path.join(parameters['config_dir'], coeffs_file_fname), index=False)
            if parameters['overwrite'] or not os.path.exists(expr_file_fname):
                expr_file.to_csv(os.path.join(parameters['config_dir'], expr_file_fname), index=False)

            # Store output
            output[coeffs_file_fname.replace('.csv', '')] = coeffs_file
            output[expr_file_fname.replace('.csv', '')] = expr_file

        if parameters['overwrite'] or not os.path.exists(os.path.join(parameters['config_dir'], 'trip_purpose.yaml')):
            # Save YAML
            with open(os.path.join(parameters['config_dir'], 'stop_frequency.yaml'), 'w') as file:
                yaml.dump(stop_frequency_spec, file)

            with open(os.path.join(parameters['config_dir'], 'trip_purpose.yaml'), 'w') as file:
                yaml.dump(trips_purpose_spec, file)

        # Save to CSV
        for config in ['stop_frequency_alts', 'trip_purpose_annotate', 'stop_frequency_annotate']:
            df = locals()[config]
            file_path = os.path.join(parameters['config_dir'], parameters['output_fname'][config])
            if parameters['overwrite'] or not os.path.exists(file_path):
                df.to_csv(file_path, index=False)


        # file_path_probs = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
        # if parameters['overwrite'] or not os.path.exists(file_path_probs):
        #     stop_frequency_alts.to_csv(file_path_probs, index=False)
        #
        # file_path_freq_annote = os.path.join(parameters['config_dir'], parameters['output_fname']['stop_frequency_annotate'])
        # if parameters['overwrite'] or not os.path.exists(file_path_freq_annote):
        #     stop_frequency_annotate.to_csv(file_path_freq_annote, index=False)
        #
        # file_path_purp_annote = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_annotate'])
        # if parameters['overwrite'] or not os.path.exists(file_path_purp_annote):
        #     trip_purpose_annotate.to_csv(file_path_purp_annote, index=False)

        # Save config YAML
        with open(os.path.join(parameters['config_dir'], 'stop_frequency.yaml'), 'w') as file:
            yaml.dump(stop_frequency_spec, file)


        return output


# Note on comparison with cross border method:
# The cross border preprocessing expanded the 40 to 48 half-hour time periods using deterministic expansion factors.
# Basically filling in the missing periods by dividing aggregated probability into the expanded periods
# and then readjusting if constraints are violated (can't return before entry).

# Instead of that, I consider the entry/return/Percent as X/Y/Z and used multivariate interpolation
# to interpolate missing points, considering both entry/return simultaneously using a gaussian radial basis function.
# I remove the periods 1 and 40, storing the data elsewhere, and interpolate for 40-49, treating 49 as 1.
# Assuming the next day follows a similar pattern, I expand the 48x48 grid to 96x96,
# copying the data from each day, leaving missing values as NA. This lets me interpolate instead of extrapolate.
# Essentially bridging the two days, filling in the missing nighttime hours between, rather than trying to forecast.
# As final checks:
# Zero any negative values, setting a floor to 0. (rare cases)
# Zero and backward trips where return is before entry [EnterPeriod <= ReturnPeriod] (rare cases)
# Then normalize the imputed periods to sum up to the sum of aggregated periods to ensure the entire matrix sums to 1.

# tod_probs, parameters = copy.deepcopy(self.tables['tour_TOD']), self.parameters
class TourSchedulingMixin:
    def create_tour_scheduling_specs(self, tod_probs, parameters):
        # Convert CTRAMP 40-period to 48 period asim
        tod_probs = self.convert_periods(tod_probs)

        # Interpolate missing time periods
        tod_probs_extra = self.interpolate_tour_probs(tod_probs, parameters)

        # Concatenate entry/return periods into i_j formatted columns
        tod_probs_extra['period'] = tod_probs_extra[['EntryPeriod', 'ReturnPeriod']].astype(str).agg('_'.join, axis=1)

        # Reshape from long to wide
        tod_probs_extra = tod_probs_extra.pivot(index='Purpose', columns='period', values='Percent').reset_index()

        # Relabel cols/rows
        tod_probs_extra = tod_probs_extra.rename(columns={'Purpose': 'purpose_id'})

        # Create tour_departure_and_duration_alternatives
        tour_scheduling_alts = pd.DataFrame(itertools.product(range(1, 49), repeat=2), columns=['start', 'end'])

        # Save to CSV
        if parameters['overwrite']:
            tour_scheduling_alts.to_csv(
                os.path.join(
                    parameters['config_dir'],
                    parameters['output_fname']['tour_scheduling_alts']),
                index=False
            )

            tod_probs_extra.to_csv(
                os.path.join(
                    parameters['config_dir'],
                    parameters['output_fname']['tour_scheduling_probs']),
                index=False
            )

            # Create associated yaml
            tod_probs_spec = {'PROBS_SPEC': 'tour_scheduling_probs.csv',
                              'PROBS_JOIN_COLS': ['purpose_id']}

            with open(os.path.join(parameters['config_dir'], 'tour_scheduling_probabilistic.yaml'), 'w') as file:
                yaml.dump(tod_probs_spec, file)

        return {'tour_scheduling_probs': tod_probs_extra, 'tour_scheduling_alts': tour_scheduling_alts}

    def convert_periods(self, tod_probs):
        tod_probs = copy.deepcopy(tod_probs)
        # This converts the period number ONLY, it does not extrapolate beyond.
        for p in ['EntryPeriod', 'ReturnPeriod']:
            tod_probs.loc[(tod_probs[p] != 40) & (tod_probs[p] != 1), p] += 3
            tod_probs.loc[tod_probs[p] == 1, p] = 4
            tod_probs.loc[tod_probs[p] == 40, p] = 43

        return tod_probs

    def tod_aggregate_melt(self, probsxi):
        todiagg = []
        for x in ['EntryPeriod', 'ReturnPeriod']:
            agg = probsxi.groupby(x)['Percent'].sum().reset_index().rename(columns={x: 'Time Period'})
            agg['Period Type'] = x
            todiagg.append(agg)
        todiagg = pd.concat(todiagg)
        newperiods = (todiagg['Time Period'] == 1) | ((todiagg['Time Period'] >= 40) & (todiagg['Time Period'] <= 48))

        # new or extrapolated
        todiagg.loc[newperiods, 'Data Type'] = 'Extrapolated'
        todiagg.loc[~newperiods, 'Data Type'] = 'Existing'

        # Label
        todiagg['Label'] = todiagg['Period Type']
        todiagg.loc[newperiods, 'Label'] = todiagg.loc[newperiods, 'Label'] + ' (extrapolated)'

        return todiagg.reset_index()

    def expand_square(self, probs_clipped):
        # Inform Temporal Loop (i.e., tell the computer that 24hr day repeats and 41-48 starts back at 1)
        extend = {'diag': ['EntryPeriod', 'ReturnPeriod'], 'top': ['EntryPeriod'], 'right': ['ReturnPeriod']}

        # Expand to full 1-48 matrix size that starts at 5 and loops back at 5+48 = 53
        # This is so that the missing data is positioned in the middle, not at the ends of the cycled data.
        nrange = range(probs_clipped.EntryPeriod.min(), probs_clipped.EntryPeriod.min() + 48)

        combos = itertools.product(nrange, repeat=2)
        probs_base = pd.DataFrame(combos, columns=["EntryPeriod", "ReturnPeriod"])
        probs_base = probs_base.merge(probs_clipped.drop(columns='Purpose'),
                                      on=['EntryPeriod', 'ReturnPeriod'],
                                      how='outer')

        probsx = copy.deepcopy(probs_base)
        for side, cols in extend.items():
            probsy = copy.deepcopy(probs_base)
            probsy[cols] += 48
            probsx = pd.concat([probsx, probsy])

        return probsx

    def tod_plots(self, purpose_id, probsxi, parameters):
        purpose = parameters['purpose_map'][purpose_id]

        sns.heatmap(data=probsxi.pivot("EntryPeriod", "ReturnPeriod", "Percent"), cmap="Blues").set(
            title=purpose.capitalize() + ' purpose')
        plt.xticks(list(range(0, 49, 4)), labels=list(range(0, 49, 4)))
        plt.yticks(list(range(0, 49, 4)), labels=list(range(0, 49, 4)))

        if parameters['plot_save']:
            plt.savefig(os.path.join(parameters['plot_dir'], 'tod_heatmap_{}.png'.format(purpose)))
        if parameters['plot_show']:
            plt.show()
        plt.clf()
        # Aggregate view
        todiagg = self.tod_aggregate_melt(probsxi)
        for period in todiagg['Period Type'].unique():
            tod = todiagg[todiagg['Period Type'] == period]
            sns.set_palette("Paired")
            # sns.scatterplot(data=todiagg, x='Time Period', y='Percent', hue='Period Type')
            sns.barplot(data=tod, x='Time Period', y='Percent', color='blue', hue='Data Type', dodge=False).set(
                title=purpose.capitalize() + ' purpose')
            plt.xticks(list(range(0, 49, 4)), labels=list(range(0, 49, 4)))
            if parameters['plot_save']:
                plt.savefig(os.path.join(parameters['plot_dir'],
                                         'tod_dist_{purpose}_{period}.png'.format(purpose=purpose, period=period))
                            )
            if parameters['plot_show']:
                plt.show()
            plt.clf()

    def scale_to(self, df_vector, target_sum):
        return target_sum * (df_vector / df_vector.sum())

    def interpolate_tour_probs(self, tod_probs, parameters):
        # Interpolate 2d grid values by purpose
        tod_probs_extra = []
        for purpose_id, probs in tod_probs.groupby('Purpose'):
            # Remove 1 and 40 for fitting, storing for later
            probs_clipped = probs[~((probs.EntryPeriod == 4) | (probs.ReturnPeriod == 4)) &
                                  ~((probs.EntryPeriod == 43) | (probs.ReturnPeriod == 43))]

            # Extract into x,y,z for interpolation
            x, y, z = [probs_clipped[col].values for col in ['EntryPeriod', 'ReturnPeriod', 'Percent']]

            # Radial Function Interpolation
            rbf = Rbf(x, y, z, function='gaussian')

            # Expand the grid to loop over 48 time periods to 96, interpolate the missing in between
            probsx = self.expand_square(probs_clipped)

            # Extrapolate the missing points
            probsxi = copy.deepcopy(probsx)
            nulls = probsxi.Percent.isnull()
            probsxi.loc[nulls, 'Percent'] = rbf(probsxi[nulls].EntryPeriod, probsxi[nulls].ReturnPeriod)

            # sns.heatmap(data=probsxi.pivot("EntryPeriod", "ReturnPeriod", "Percent"), cmap="Blues")
            # plt.show()

            # Set first 4 periods in cycle to as 1-4
            probsxi.loc[(probsxi.EntryPeriod > 48) & (probsxi.EntryPeriod < 53), 'EntryPeriod'] -= 48
            probsxi.loc[(probsxi.ReturnPeriod > 48) & (probsxi.ReturnPeriod < 53), 'ReturnPeriod'] -= 48

            # Extract imputed tables, ditching the extra cycled data
            probsxi = probsxi[(probsxi.EntryPeriod <= 48) & (probsxi.ReturnPeriod <= 48)]

            # Set floor to 0 just in case any go below 0
            probsxi.loc[probsxi.Percent < 0, 'Percent'] = 0

            # Ensure that there are no trips that arrive before they depart!
            probsxi.loc[probsxi.ReturnPeriod < probsxi.EntryPeriod, 'Percent'] = 0

            # Re-scale the 1st and 40th half hours for the interpolated 45-48 and 40-44 half hours
            first_sum = probs[(probs.EntryPeriod == 4) | (probs.ReturnPeriod == 4)].Percent.sum()
            last_sum = probs[(probs.EntryPeriod == 43) | (probs.ReturnPeriod == 43)].Percent.sum()

            first_filt = (probsxi.EntryPeriod.isin(range(1, 5)) | probsxi.ReturnPeriod.isin(range(1, 5)))
            last_filt = (probsxi.EntryPeriod.isin(range(40, 49)) | probsxi.ReturnPeriod.isin(range(40, 49)))

            # Scale the imputed values to match the original sum
            probsxi.loc[first_filt, 'Percent'] = self.scale_to(probsxi.loc[first_filt, 'Percent'], first_sum)
            probsxi.loc[last_filt, 'Percent'] = self.scale_to(probsxi.loc[last_filt, 'Percent'], last_sum)

            # Re-normalize to unit scale, some floored values might get lost
            probsxi['Percent'] = probsxi.Percent / probsxi.Percent.sum()

            if parameters['plot_show'] or parameters['plot_save']:
                self.tod_plots(purpose_id, probsxi, parameters)

            probsxi['Purpose'] = purpose_id

            tod_probs_extra.append(probsxi)

        # Concatenate the imputed tables
        tod_probs_extra = pd.concat(tod_probs_extra)

        return tod_probs_extra

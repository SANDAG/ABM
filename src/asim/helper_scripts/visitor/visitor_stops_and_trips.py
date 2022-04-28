import os
import pandas as pd
import numpy as np


class Mixin:
    def update_trip_purpose_probs(self, stop_purp_probs, parameters):
        print("Creating trip purpose probability lookup table.")
        purpose_id_map = parameters['purpose_ids']
        purp_dict = {v: k for k, v in purpose_id_map.items()}

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
        stop_probs = stop_purp_probs[['primary_purpose', 'outbound', 'trip_num', 'multistop'] +
                                     list(purpose_id_map.keys())]

        # Save to CSV
        file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
        if parameters['overwrite'] or not os.path.exists(file_path):
            stop_probs.to_csv(file_path, index=False)

        return stop_probs

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

        return duration_probs

    # NOTE: This script is relatively unchanged from xborder, probably needs some review/cleanup
    def create_stop_freq_specs(self, stop_freq_probs, parameters):
        print("Creating stop frequency alts and probability lookups.")

        # convert probs to utils
        stop_freq_probs['value'] = stop_freq_probs['Percent'].apply(lambda x: np.log(x) if x > 0 else -999)

        # Format alt names and write out alts table
        stop_freq_probs.rename(columns={'Outbound': 'out', 'Inbound': 'in'}, inplace=True)
        stop_freq_probs['alt'] = stop_freq_probs[['out', 'in']].apply(lambda x: '{}out_{}in'.format(x[0], x[1]), axis=1)

        alts_df = stop_freq_probs.drop_duplicates(['out', 'in', 'alt'])[['alt', 'out', 'in']]

        # Save to CSV
        file_path = os.path.join(parameters['config_dir'], parameters['output_fname']['trip_purpose_probs'])
        if parameters['overwrite'] or not os.path.exists(file_path):
            alts_df.to_csv(file_path, index=False)

        # Store output
        output = {'stop_frequency_alts': alts_df}

        # iterate through purposes and pivot probability lookup tables to
        # create MNL spec files with only ASC's (no covariates).

        # purpose, purpose_id = list(purpose_id_map.items())[0]
        required_cols = ['Label', 'Description', 'Expression']
        for purpose, purpose_id in parameters['purpose_ids'].items():
            purpose_probs = stop_freq_probs.loc[stop_freq_probs['Purpose'] == purpose_id, :].copy()
            purpose_probs['coefficient_name'] = purpose_probs.apply(
                lambda x: 'coef_asc_dur_{0}_{1}_stops_{2}'.format(x['DurationLo'], x['DurationHi'], x['alt']), axis=1)

            coeffs_file = purpose_probs[['value', 'coefficient_name']].copy()
            coeffs_file['Description'] = None
            coeffs_file = coeffs_file[['Description', 'value', 'coefficient_name']]
            coeffs_file_fname = parameters['output_fname']['stop_frequency_coeffs'].format(purpose=purpose)
            coeffs_file_fname = os.path.join(parameters['config_dir'], coeffs_file_fname)

            expr_file = purpose_probs.pivot(
                index=['DurationLo', 'DurationHi'], columns='alt',
                values='coefficient_name').reset_index()

            expr_file['Label'] = 'util_ASC_tour_dur_{}_{}'.format(
                expr_file['DurationLo'].astype(str),
                expr_file['DurationHi'].astype(str))

            expr_file['Description'] = 'ASC for tour durations between {} and {}'.format(
                expr_file['DurationLo'].astype(str),
                expr_file['DurationHi'].astype(str))

            expr_file['Expression'] = '{} < duration_hours <= {}'.format(
                expr_file['DurationLo'].astype(str),
                expr_file['DurationHi'].astype(str))

            expr_file = expr_file.drop(columns=['DurationLo', 'DurationHi'])
            expr_file = expr_file[required_cols + [col for col in expr_file.columns if col not in required_cols]]
            expr_file_fname = parameters['output_fname']['stop_frequency_expressions'].format(purpose=purpose)
            expr_file_fname = os.path.join(parameters['config_dir'], expr_file_fname)

            # Save to CSV
            if parameters['overwrite'] or not os.path.exists(coeffs_file_fname):
                coeffs_file.to_csv(coeffs_file_fname, index=False)
            if parameters['overwrite'] or not os.path.exists(expr_file_fname):
                expr_file.to_csv(expr_file_fname, index=False)

            # Store output
            output[coeffs_file_fname.replace('.csv', '')] = coeffs_file
            output[expr_file_fname.replace('.csv', '')] = expr_file

        return output

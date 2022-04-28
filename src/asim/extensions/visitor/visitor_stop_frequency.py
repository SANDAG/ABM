import os
import pandas as pd
import numpy as np

# NOTE: This script is relatively unchanged from xborder, probably needs some review/cleanup

def create_stop_freq_specs(stop_freq_probs, parameters):
    print("Creating stop frequency alts and probability lookups.")

    # convert probs to utils
    stop_freq_probs['value'] = stop_freq_probs['Percent'].apply(lambda x: np.log(x) if x > 0 else -999)

    # write out alts table
    # Format alt names
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
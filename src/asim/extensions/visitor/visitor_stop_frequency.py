import os
import pandas as pd
import numpy as np


def create_stop_freq_specs(probs_df, parameters):
    print("Creating stop frequency alts and probability lookups.")

    # Setup general vars
    output_names = parameters['output_fname']
    config_dir = parameters['config_dir']
    purpose_id_map = parameters['purpose_ids']

    # convert probs to utils
    probs_df['value'] = probs_df['Percent'].apply(lambda x: np.log(x) if x > 0 else -999)

    # write out alts table
    # Format alt names
    probs_df.rename(columns={'Outbound': 'out', 'Inbound': 'in'}, inplace=True)
    probs_df['alt'] = probs_df[['out', 'in']].apply(lambda x: '{}out_{}in'.format(x[0], x[1]), axis=1)

    alts_df = probs_df.drop_duplicates(['out', 'in', 'alt'])[['alt', 'out', 'in']]
    if parameters['overwrite']:
        alts_df.to_csv(os.path.join(config_dir, output_names['stop_frequency_alts']), index=False)

    # Store output
    output = {'stop_frequency_alts': alts_df}

    # iterate through purposes and pivot probability lookup tables to
    # create MNL spec files with only ASC's (no covariates).

    # purpose, purpose_id = list(purpose_id_map.items())[0]
    required_cols = ['Label', 'Description', 'Expression']
    for purpose, purpose_id in purpose_id_map.items():
        purpose_probs = probs_df.loc[probs_df['Purpose'] == purpose_id, :].copy()
        purpose_probs['coefficient_name'] = purpose_probs.apply(
            lambda x: 'coef_asc_dur_{0}_{1}_stops_{2}'.format(x['DurationLo'], x['DurationHi'], x['alt']), axis=1)

        coeffs_file = purpose_probs[['value', 'coefficient_name']].copy()
        coeffs_file['Description'] = None
        coeffs_file = coeffs_file[['Description', 'value', 'coefficient_name']]
        coeffs_file_fname = output_names['stop_frequency_coeffs'].format(purpose=purpose)

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
        expr_file_fname = output_names['stop_frequency_expressions'].format(purpose=purpose)

        # Save to CSV
        if parameters['overwrite']:
            coeffs_file.to_csv(os.path.join(config_dir, coeffs_file_fname), index=False)
            expr_file.to_csv(os.path.join(config_dir, expr_file_fname), index=False)

        # Store output
        output[coeffs_file_fname.replace('.csv', '')] = coeffs_file
        output[expr_file_fname.replace('.csv', '')] = expr_file

    return output
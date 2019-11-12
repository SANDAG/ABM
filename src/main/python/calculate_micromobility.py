#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: RSG Inc.
"""calculate_micromobility.py

This Python 3 script calculates Generalized Time between MGRAs for micromobility
modes in the SANDAG activity-based model using the following equation:

GenTime = (Length_od*60)/Speed + rentalTime + microAccessTime_o + constant +
          (variableCost*(Length_od*60)/Speed) + fixedCost)*60/VOT

Micromobility-related constants are kept in a Java properties file which
can be specified at the command line. The properties file is expected to
contain the following attributes:
    - mgra.socec.file
    - active.logsum.matrix.file.walk.mgra
    - active.logsum.matrix.file.walk.mgratap
    - active.micromobility.speed
    - active.micromobility.rentalTime
    - active.micromobility.constant
    - active.micromobility.variableCost
    - active.micromobility.fixedCost
    - active.micromobility.vot
    - active.coef.distance.walk

Currently this script uses three files to perform its calculations:
    - mgra.socec.file, contains MicroAccessTime for origin MRGAs
    - active.logsum.matrix.file.walk.mgra, contains the pre-calculated
      walk times between MGRAs
    - active.logsum.matrix.file.walk.mgratap, contains the pre-calculated
      walk times between TAPs

The script then replaces each perceived walk time in the output files with the
minimum of the existing walk time and the newly calculated micromobility time.
Alternatively the script can write the calculations to a fresh file with the
[-k] command-line flag.

Run `python calculate_micromobility.py -h` for more command-line usage.
"""

import argparse
import os
import pandas as pd

ATTRIBUTES = {
    'mgra_file':                    'mgra.socec.file',
    'walk_mgra_output_file':        'active.logsum.matrix.file.walk.mgra',
    'walk_mgra_tap_output_file':    'active.logsum.matrix.file.walk.mgratap',
    'speed':                        'active.micromobility.speed',
    'rental_time':                  'active.micromobility.rentalTime',
    'constant':                     'active.micromobility.constant',
    'variable_cost':                'active.micromobility.variableCost',
    'fixed_cost':                   'active.micromobility.fixedCost',
    'vot':                          'active.micromobility.vot',
    'walk_coef':                    'active.coef.distance.walk',
}


def main():
    """Script entry point
    """

    parse_cli_args()
    props = parse_properties_file(CLI.properties_file)
    mat = read_micro_access_time(props['mgra_file'])

    calculate_micromobility('walk_mgra_output_file', mat, props,
                            target='actual', actual='actual',
                            orig='i', dest='j')
    calculate_micromobility('walk_mgra_tap_output_file', mat, props,
                            target='boardingActual', actual='boardingActual',
                            orig='mgra', dest='tap')

    print('Finished!')


def parse_cli_args():
    """Use argparse to set command-line args

    Sets global vars PARSER and CLI
    """
    global PARSER
    PARSER = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    PARSER.add_argument('-p', '--properties_file',
                        default=os.path.join('..', 'conf', 'sandag_abm.properties'),
                        help="Java properties file.")
    PARSER.add_argument('-o', '--outputs_directory',
                        default=os.path.join('..', 'output'),
                        help="Directory containing walk MGRA output files.")
    PARSER.add_argument('-i', '--inputs_parent_directory',
                        default='..',
                        help="Directory containing 'input' folder")
    PARSER.add_argument('-k', '--keep',
                        action='store_true',
                        default=False,
                        help="Save micromobility calculations to new output files "
                        "without overwriting originals.")

    global CLI
    CLI = PARSER.parse_args()


def parse_properties_file(filename):
    """Parses attributes from a Java properties file

    Returns hash of attributes as strings
    """

    validate_file(filename)
    print('Parsing tokens from %s ...' % filename)

    all_props = {}
    with open(filename, 'r') as f:
        for line in f:
            if line.startswith("#"):
                continue
            if '=' in line:
                atr, val = list(map(str.strip, line.split('=')))
                all_props[atr] = val

    try:
        useable_props = {k: all_props[v] for k, v in ATTRIBUTES.items()}
    except KeyError as error:
        print("ERROR:!!! Missing attributes from %s !!!" % filename)
        print("ERROR: Please ensure the following keys are present:")
        for atr in ATTRIBUTES.values():
            print("  %s" % atr)
        raise error

    print("Using tokens from properties file:")
    pretty_print_dictionary(useable_props)

    return useable_props


def read_micro_access_time(mgra_file):
    """Reads the MicroAccessTime for each origin MGRA from
    the provided MGRA file. If no MicroAccessTime is found,
    a simple calculation is performed instead.

    Returns the data as a pandas Series
    """
    mgra_file = os.path.join(CLI.inputs_parent_directory, mgra_file)
    validate_file(mgra_file)

    with open(mgra_file, 'r') as f:
        use_dummy = 'MicroAccessTime' not in f.readline()

    if use_dummy:
        print('No MicroAccessTime column found in %s, using 2 minute '
              'default for PARKAREA==1, 15 minutes otherwise.' % mgra_file)
        park_area = pd.read_csv(mgra_file, usecols=['mgra', 'parkarea'],
                                index_col='mgra', dtype='Int64', squeeze=True)
        mat = pd.Series(index=park_area.index, data=15.0, name='MicroAccessTime')
        mat.loc[park_area == 1] = 2.0
    else:
        mat = pd.read_csv(mgra_file, usecols=['mgra', 'MicroAccessTime'],
                          index_col='mgra', squeeze=True)

    return mat


def calculate_micromobility(output_file_key, mat, props, **cols):
    """Performs micromobility calculations using given output_file
    and attributes from the provided MGRA file and properties file

    Overwrites output file with minimum of existing target column
    and calculated micromobility General Time. Can also write
    calculations to new file given the [-k] command line switch.
    """
    output_file = os.path.join(CLI.outputs_directory, props[output_file_key])
    validate_file(output_file)

    with open(output_file, 'r') as f:
        header = f.readline()
        if not all(col in header for col in cols.values()):
            raise "Did not find all expected columns %s in %s" % (cols, output_file)

    print('Reading %s ...' % output_file)
    df = pd.read_csv(output_file)

    # OD vectors
    length = df[cols['actual']]/float(props['walk_coef'])
    travel_time = length*60/float(props['speed'])
    orig_mat = mat.reindex_like(df.set_index(cols['orig'])).reset_index(drop=True)

    print('Calculating Generalized Time for %s using columns:' % output_file)
    pretty_print_dictionary(cols)

    # calculate micromobility Generalized Time
    df['genTime'] = \
        travel_time + float(props['rental_time']) + orig_mat + float(props['constant']) + \
        (float(props['variable_cost'])*travel_time + float(props['fixed_cost']))*60/float(props['vot'])

    # calculate the minimum of walk time vs. micromobility time
    df['minTime'] = df[['genTime', cols['target']]].min(axis=1)

    # write output
    if CLI.keep:
        outfile = os.path.join(
            CLI.outputs_directory,
            # '_mm' for micro-mobility
            os.path.basename(output_file).replace('.csv', '_mm.csv')
        )
        print("Writing final table to %s" % outfile)
        df.to_csv(outfile, index=False)
    else:
        print("Overwriting %s with new calcs ..." % output_file)
        df[cols['target']] = df['minTime']
        df.drop(columns=['genTime', 'minTime'])
        df.to_csv(output_file, index=False)


def validate_file(filename):
    if not os.path.isfile(filename):
        PARSER.print_help()
        raise FileNotFoundError("Could not locate %s" % filename)


def pretty_print_dictionary(dictionary):
    for k, v in dictionary.items():
        print("  %s: %s" % (k, v))


if __name__ == '__main__':
    main()

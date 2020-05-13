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
      walk times from MGRAs to TAPs

The script then writes a fresh MGRA file with the newly calculated micromobility calculations:
    - walkTime: the original walk time
    - mmTime: the micro-mobility time, including travel time, rental time, access time
    - mmCost: variable cost * travel time + fixed cost
    - mmGenTime: mmTime + mmCost converted to time + constant
    - minTime: minimum of walkTime and mmGenTime

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

    ### Global variables ###
    # PARSER  # argparse parser
    # CLI  # command line args
    # PROPS  # parsed properties dictionary
    # MAT  # Micro Access Time Pandas series

    parse_cli_args()
    parse_properties_file()
    read_micro_access_time()

    calculate_micromobility('walk_mgra_output_file',
                            walk_time='actual',
                            orig_col='i', dest_col='j')
    calculate_micromobility('walk_mgra_tap_output_file',
                            walk_time='boardingActual',
                            orig_col='mgra', dest_col='tap')

    print('Finished!')


def parse_cli_args():
    """Use argparse to set command-line args

    Sets global vars PARSER and CLI
    """
    global PARSER
    global CLI

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

    CLI = PARSER.parse_args()


def parse_properties_file():
    """Parses attributes from a Java properties file

    Saves the relevant attributes as a dictionary to the
    global PROPS variable.
    """

    filename = CLI.properties_file
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

    global PROPS
    PROPS = useable_props


def read_micro_access_time():
    """Reads the MicroAccessTime for each origin MGRA from
    the provided MGRA file. If no MicroAccessTime is found,
    a simple calculation is performed instead.

    Saves the resulting pandas series to the global MAT variable.
    """
    mgra_file = os.path.join(CLI.inputs_parent_directory, PROPS['mgra_file'])
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

    global MAT
    MAT = mat


def calculate_micromobility(output_file_key, walk_time, orig_col, dest_col):
    """Performs micromobility calculations using given output_file
    and attributes from the provided MGRA file and properties file

    Writes newly calculated micromobility time and intermediate calculations
    """
    output_file = os.path.join(CLI.outputs_directory, PROPS[output_file_key])
    validate_file(output_file)

    print('Reading %s ...' % output_file)
    df = pd.read_csv(output_file, usecols=[walk_time, orig_col, dest_col])
    df.rename({walk_time: 'walkTime'}, axis=1, inplace=True)

    # OD vectors
    length = df['walkTime']/float(PROPS['walk_coef'])
    travel_time = length*60/float(PROPS['speed'])
    orig_mat = df[orig_col].map(MAT)  # micro-access time at origin
    mm_time = travel_time + float(PROPS['rental_time']) + orig_mat  # total mm time
    mm_cost = float(PROPS['variable_cost'])*travel_time + float(PROPS['fixed_cost'])
    mm_cost_as_time = mm_cost * 60 / float(PROPS['vot'])

    # save intermediate calculations
    df['mmTime'] = mm_time
    df['mmCost'] = mm_cost

    # calculate micromobility Generalized Time
    df['mmGenTime'] = mm_time + mm_cost_as_time + float(PROPS['constant'])

    # calculate the minimum of walk time vs. generalized time
    df['minTime'] = df[['mmGenTime', 'walkTime']].min(axis=1)

    # write output
    outfile = os.path.join(
        CLI.outputs_directory,
        os.path.basename(output_file).replace('walk', 'microMobility')
    )

    print("Writing final table to %s" % outfile)
    df.to_csv(outfile, index=False)
    print("Done.")


def validate_file(filename):
    if not os.path.isfile(filename):
        PARSER.print_help()
        raise IOError("Could not locate %s" % filename)


def pretty_print_dictionary(dictionary):
    for k, v in dictionary.items():
        print("  %s: %s" % (k, v))


if __name__ == '__main__':
    main()

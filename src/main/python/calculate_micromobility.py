#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: RSG Inc.
"""calculate_micromobility.py

This Python 3 script calculates Generalized Time between MGRAs for micromobility
and microtransit modes in the SANDAG activity-based model.

Constants are read from the properties file specified at the command line.

Currently this script uses three files to perform its calculations:
    - mgra.socec.file, contains MicroAccessTime for origin MRGAs
    - active.logsum.matrix.file.walk.mgra, contains the pre-calculated
      walk times between MGRAs
    - active.logsum.matrix.file.walk.mgratap, contains the pre-calculated
      walk times from MGRAs to TAPs
    - active.microtransit.tap.file, contains a list of TAPs with microtransit availability
    - active.microtransit.mgra.file, contains a list of MGRAs with microtransit availability 


The script then writes a fresh MGRA file with the newly calculated micromobility calculations:
    - walkTime: the original walk time
    - mmTime: the micro-mobility time, including travel time, rental time, access time
    - mmCost: micro-mobility variable cost * travel time + fixed cost
    - mmGenTime: mmTime + mmCost converted to time + constant
    - mtTime: the micro-transit time, including travel time, wait time, access time
    - mtCost: micro-transit variable cost * travel time + fixed cost
    - minTime: minimum of walkTime, mmGenTime, and mtGenTime

Run `python calculate_micromobility.py -h` for more command-line usage.
"""

import argparse
import os
import pandas as pd


def process_file(config, zone):
    """Performs micromobility calculations using given output_file
    and attributes from the provided MGRA file and properties file

    Writes newly calculated micromobility time and intermediate calculations
    """

    filename = config.walk_mgra_output_file if zone == 'mgra' else config.walk_mgra_tap_output_file

    output_file = os.path.join(config.cli.outputs_directory, filename)
    config.validate_file(output_file)

    if zone == 'mgra':
        walk_time_col = 'actual'
        orig_col = 'i'
        dest_col = 'j'

    else:
       walk_time_col = 'boardingActual'
       orig_col = 'mgra'
       dest_col = 'tap'

    print('Processing %s ...' % output_file)
    df = pd.read_csv(output_file, usecols=[walk_time_col, orig_col, dest_col])
    df.rename(columns={walk_time_col:'walkTime'}, inplace=True)

    # OD vectors
    length = df['walkTime'] / config.walk_coef

    # availability masks
    if zone == 'mgra':
        mt_avail = \
            (df[orig_col].isin(config.mt_mgras) & df[dest_col].isin(config.mt_mgras)) & \
            (length <= config.mt_max_dist_mgra)

        walk_avail = length <= config.walk_max_dist_mgra
        mm_avail = length <= config.mm_max_dist_mgra

    else:
        mt_avail = \
            df[orig_col].isin(config.mt_mgras) & df[dest_col].isin(config.mt_taps) & \
            (length <= config.mt_max_dist_tap)
        walk_avail = length <= config.walk_max_dist_tap
        mm_avail = length <= config.mm_max_dist_tap

    all_rows = df.shape[0]
    df = df[mt_avail | walk_avail | mm_avail]
    print('Filtered out %s unavailable pairs' % str(all_rows - df.shape[0]))

    # micro-mobility
    mm_ivt = length * 60 / config.mm_speed # micro-mobility in-vehicle time
    orig_mat = df[orig_col].map(config.mat)  # micro-access time at origin
    mm_time = mm_ivt + config.mm_rental_time + orig_mat  # total mm time
    mm_cost = config.mm_variable_cost * mm_ivt + config.mm_fixed_cost
    mm_cost_as_time = mm_cost * 60 / config.vot

    # micro-transit
    mt_ivt = length * 60 / config.mt_speed
    mt_time = mt_ivt + 2 * config.mt_wait_time + config.mt_access_time
    mt_cost = mt_time * config.mt_variable_cost + config.mt_fixed_cost
    mt_cost_as_time = mt_cost * 60 / config.vot

    # save intermediate calculations
    df['dist'] = length
    df['mmTime'] = mm_time
    df['mmCost'] = mm_cost
    df['mtTime'] = mt_time
    df['mtCost'] = mt_cost

    # calculate micromobility and microtransit Generalized Time
    df['mmGenTime'] = mm_time + mm_cost_as_time + config.mm_constant
    df['mtGenTime'] = mt_time + mt_cost_as_time + config.mt_constant

    # update zones with unavailable walk, micromobility, and microtransit
    df.loc[~walk_avail, ['walkTime']] = config.mt_not_avail
    df.loc[~mm_avail, ['mmTime', 'mmCost', 'mmGenTime']] = config.mt_not_avail
    df.loc[~mt_avail, ['mtTime', 'mtCost', 'mtGenTime']] = config.mt_not_avail

    # calculate the minimum of walk time vs. generalized time
    df['minTime'] = df[['walkTime', 'mmGenTime', 'mtGenTime']].min(axis=1)

    # write output
    outfile = os.path.join(
        config.cli.outputs_directory,
        os.path.basename(output_file).replace('walk', 'micro')
    )

    print("Writing final table to %s" % outfile)
    df.to_csv(outfile, index=False)
    print("Done.")


class Config():

    def __init__(self):

        self.init_cli_args()
        self.init_properties()
        self.init_micro_access_time()
        self.init_mgra_lists()

    def init_cli_args(self):
        """Use argparse to set command-line args

        """

        self.parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)

        self.parser.add_argument(
            '-p', '--properties_file',
            default=os.path.join('..', 'conf', 'sandag_abm.properties'),
            help="Java properties file.")

        self.parser.add_argument(
            '-o', '--outputs_directory',
            default=os.path.join('..', 'output'),
            help="Directory containing walk MGRA output files.")

        self.parser.add_argument(
            '-i', '--inputs_parent_directory',
            default='..',
            help="Directory containing 'input' folder")

        self.cli = self.parser.parse_args()

    def validate_file(self, filename):
        if not os.path.isfile(filename):
            self.parser.print_help()
            raise IOError("Could not locate %s" % filename)

    def init_properties(self):
        """Parses attributes from a Java properties file

        """

        filename = self.cli.properties_file
        self.validate_file(filename)
        print('Parsing tokens from %s ...' % filename)

        all_props = {}
        with open(filename, 'r') as f:
            for line in f:
                if line.startswith("#"):
                    continue
                if '=' in line:
                    atr, val = list(map(str.strip, line.split('=')))
                    all_props[atr] = val

        def parse(property_name):
            if property_name not in all_props:
                raise KeyError("Could not find %s in %s" % (property_name, filename))

            return all_props.get(property_name)

        self.mgra_file =                   parse('mgra.socec.file')
        self.walk_mgra_output_file =       parse('active.logsum.matrix.file.walk.mgra')
        self.mt_mgra_file =                parse('active.microtransit.mgra.file')

        self.walk_coef =                   float(parse('active.walk.minutes.per.mile'))
        self.walk_max_dist_mgra =          float(parse('active.maxdist.walk.mgra'))

        self.vot =                         float(parse('active.micromobility.vot'))

        self.mm_speed =                    float(parse('active.micromobility.speed'))
        self.mm_rental_time =              float(parse('active.micromobility.rentalTime'))
        self.mm_constant =                 float(parse('active.micromobility.constant'))
        self.mm_variable_cost =            float(parse('active.micromobility.variableCost'))
        self.mm_fixed_cost =               float(parse('active.micromobility.fixedCost'))
        self.mm_max_dist_mgra =            float(parse('active.maxdist.micromobility.mgra'))

        self.mt_speed =                    float(parse('active.microtransit.speed'))
        self.mt_wait_time =                float(parse('active.microtransit.waitTime'))
        self.mt_access_time =              float(parse('active.microtransit.accessTime'))
        self.mt_constant =                 float(parse('active.microtransit.constant'))
        self.mt_variable_cost =            float(parse('active.microtransit.variableCost'))
        self.mt_fixed_cost =               float(parse('active.microtransit.fixedCost'))
        self.mt_not_avail =                float(parse('active.microtransit.notAvailable'))
        self.mt_max_dist_mgra =            float(parse('active.maxdist.microtransit.mgra'))

    def init_micro_access_time(self):
        """Reads the MicroAccessTime for each origin MGRA from
        the provided MGRA file. If no MicroAccessTime is found,
        a simple calculation is performed instead.

        """
        mgra_file_path = os.path.join(self.cli.inputs_parent_directory, self.mgra_file)
        self.validate_file(mgra_file_path)

        with open(mgra_file_path, 'r') as f:
            use_dummy = 'MicroAccessTime' not in f.readline()

        if use_dummy:
            print('No MicroAccessTime column found in %s, using 2 minute '
                  'default for PARKAREA==1, 15 minutes otherwise.' % mgra_file_path)
            park_area = pd.read_csv(mgra_file_path, usecols=['mgra', 'parkarea'],
                                    index_col='mgra', dtype='Int64', squeeze=True)
            mat = pd.Series(index=park_area.index, data=15.0, name='MicroAccessTime')
            mat.loc[park_area == 1] = 2.0
        else:
            mat = pd.read_csv(mgra_file_path, usecols=['mgra', 'MicroAccessTime'],
                              index_col='mgra', squeeze=True)

        self.mat = mat

    def init_mgra_lists(self):
        """Reads in lists of ids that identify micro-transit accessibility MGRAs

        """
        mt_mgra_file_path = os.path.join(self.cli.inputs_parent_directory, self.mt_mgra_file)
        self.validate_file(mt_mgra_file_path)

        self.mt_mgras = \
            pd.read_csv(mt_mgra_file_path,
                        usecols=lambda x: x.strip().lower() == 'mgra',
                        squeeze=True).values


if __name__ == '__main__':

    config = Config()
    process_file(config, zone='mgra')

    print('Finished!')
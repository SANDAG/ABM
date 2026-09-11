# Import Modules
import sys
# from src.asim.helper_scripts.visitor.visitor_tour_enum import TourEnumMixin
# from src.asim.helper_scripts.visitor.visitor_convert_configs import TripStopFrequencyMixin, TourSchedulingMixin
from visitor_tour_enum import TourEnumMixin
from visitor_convert_configs import TripStopFrequencyMixin, TourSchedulingMixin
import argparse
import subprocess
import yaml
import copy
import os
import errno
import pandas as pd


# General functions
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


def find_root_level(target):
    # Finds the target path if it is in a parent directory
    OS_ROOT = os.path.abspath('.').split(os.path.sep)[0] + os.path.sep
    pardir = ''
    while not os.path.exists(os.path.join(pardir, target)):
        pardir = os.path.join(os.pardir, pardir)
        if os.path.abspath(pardir) == OS_ROOT:
            print("Could not find directory specified in settings.yaml!")
            raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), target)
    return pardir


#   This script prepares visitor data for:
#   1.  Tour enumeration generates tours with party size, income,
#       segment (Personal or Business) [calculate_n_parties()], and car availability.
#   2.  Tour time of day scheduling


# Main injection point for tour enumeration and preprocessing
# Utilized python Mixin method to help organize the helper functions into separate files
# The functions get "mixed in" to this main class
class VisitorModel(TourEnumMixin,
                   TripStopFrequencyMixin,
                   TourSchedulingMixin):

    def __init__(self):
        # Find asim location
        self.parameters_path = 'configs/visitor/settings_visitor.yaml'
        self.parameters_path = os.path.join(find_root_level(self.parameters_path), self.parameters_path)
        self.processed_data = {}

        # Read the visitor settings YAML
        with open(self.parameters_path) as f:
            self.parameters = yaml.load(f, Loader=yaml.FullLoader)

        # Purpose category id map
        self.parameters['purpose_map'] = {v: k for k, v in self.parameters['purpose_ids'].items()}

        # Default to print plots
        for x in ['plot_show', 'plot_save', 'overwrite']:
            if x not in self.parameters.keys():
                self.parameters[x] = True

        # Add root directory
        for x in ['tables_dir', 'data_dir', 'config_dir', 'plot_dir', 'output_dir']:
            self.parameters[x] = os.path.join(find_root_level(self.parameters[x]), self.parameters[x])

        # Default to not overwrite any files
        if 'overwrite' not in self.parameters.keys():
            self.parameters['overwrite'] = False

        # Read in the CSV-stored distribution values indicated in the yaml
        self.tables = load_tables(
            file_path=self.parameters['tables_dir'],
            nested_dict=self.parameters['input_data']
        )

        # Add in the land_use table from data
        self.tables['land_use'] = pd.read_csv(
            os.path.join(*[self.parameters.get(x) for x in ['data_dir', 'land_use']])
        )

    def tour_enum(self):
        # Generate tours
        print("Generating visitor tours")
        self.processed_data = self.create_tour_enumeration(self.tables, self.parameters)

    def setup_configs(self):
        # create/update configs
        inbound, outbound = self.tables['stop_duration']['inbound'], self.tables['stop_duration']['outbound']
        self.processed_data['stop_duration_probs'] = self.create_trip_scheduling_duration_probs(
            inbound, outbound, self.parameters
        )

        self.processed_data['tour_scheduling_specs'] = self.create_tour_scheduling_specs(
            self.tables['tour_TOD'], self.parameters
        )

        self.processed_data['stop_frequency_specs'] = self.create_stop_freq_specs(
            self.tables['stop_frequency'], self.parameters
        )

        self.processed_data['stop_purpose_probs'] = self.update_trip_purpose_probs(
            self.tables['stop_purpose'], self.parameters
        )


if __name__ == '__main__':

    # runtime args
    parser = argparse.ArgumentParser(prog='preprocessor')
    parser.add_argument(
        '-t', '--tour_enum',
        action='store_true', help='Run tour enumeration.')
    parser.add_argument(
        '-s', '--setup_configs',
        action='store_true', help='Run config creation.')
    parser.add_argument(
        '-a', '--asim',
        action='store_true', help='Run activitysim.')

    args = parser.parse_args()

    # Initialize the class
    visitor = VisitorModel()
    if args.setup_configs:
        print('CREATING CONFIG FILES!')
        visitor.setup_configs()

    if args.tour_enum:
        print('RUNNING TOUR ENUMERATION!')
        visitor.tour_enum()

    if args.asim:
        print('RUNNING ACTIVITYSIM!')

        # Find asim location
        asim_root = find_root_level('simulation.py')

        # need to pass the configs, data, and output folders to sub process
        config_args = ['-c', '../../configs/visitor',
                       '-c', '../../configs/common',
                       '-d', visitor.parameters['data_dir'],
                       '-o', visitor.parameters['output_dir']]

        process = subprocess.Popen(
            [sys.executable, '-u', asim_root + 'simulation.py'] + config_args,
            stdout=sys.stdout, stderr=subprocess.PIPE)
        _, stderr = process.communicate()
        if process.returncode != 0:
            raise subprocess.SubprocessError(stderr.decode())

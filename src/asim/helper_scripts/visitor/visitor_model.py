# Import Modules
import visitor_tour_scheduling
import visitor_tour_enum
import visitor_stops_and_trips
import argparse
import subprocess
import sys
import yaml
import copy
import os
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
    pardir = ''
    while not os.path.isfile(os.path.join(pardir, target)):
        pardir = os.path.join(os.pardir, pardir)
    return pardir


#   This script prepares visitor data for:
#   1.  Tour enumeration generates tours with party size, income,
#       segment (Personal or Business) [calculate_n_parties()], and car availability.
#   2.  Tour time of day scheduling


# Main injection point for tour enumeration and preprocessing
# Utilized python Mixin method to help organize the helper functions into separate files
# The functions get "mixed in" to this main class
class VisitorModel(visitor_tour_enum.Mixin,
                   visitor_stops_and_trips.Mixin,
                   visitor_tour_scheduling.Mixin):

    def __init__(self):
        self.parameters_path = '../../configs/visitor/preprocessing.yaml'
        self.processed_data = None

        # Read the visitor settings YAML
        with open(self.parameters_path) as f:
            self.parameters = yaml.load(f, Loader=yaml.FullLoader)

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
        outbound, inbound = self.tables['stop_duration']['inbound'], self.tables['stop_duration']['outbound']
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
        '-c', '--create_configs',
        action='store_true', help='Run config creation.')
    parser.add_argument(
        '-p', '--preprocess',
        action='store_true', help='Run preprocessor.')
    parser.add_argument(
        '-a', '--asim',
        action='store_true', help='Run activitysim.')

    args = parser.parse_args()

    # Initialize the class
    visitor = VisitorModel()
    if args.preprocess:
        print('RUNNING PREPROCESSOR!')
        visitor.tour_enum()

    if args.create_configs:
        print('CREATING CONFIG FILES!')
        visitor.setup_configs()

    if args.asim:
        print('RUNNING ACTIVITYSIM!')
        process = subprocess.Popen(
            ['python', '-u', 'simulation.py'],
            stdout=sys.stdout, stderr=subprocess.PIPE)
        _, stderr = process.communicate()
        if process.returncode != 0:
            raise subprocess.SubprocessError(stderr.decode())

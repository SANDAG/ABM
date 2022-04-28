# Import Modules
from visitor_tour_scheduling import *
from visitor_tour_enum import *
from visitor_stop_frequency import *
from visitor_trip_purpose import *

#   This script prepares visitor data for:
#   1.  Tour enumeration generates tours with party size, income,
#       segment (Personal or Business) [calculate_n_parties()], and car availability.
#   2.  Tour time of day scheduling


# Main injection point for preprocessing
def preprocess_visitor(settings_path='../../configs/visitor/preprocessing.yaml'):

    # Find config root
    # root = find_settings_path(settings_path)

    # Read the visitor settings YAML
    with open(settings_path) as f:
        parameters = yaml.load(f, Loader=yaml.FullLoader)

    # Default to not overwrite any files
    if 'overwrite' not in parameters.keys():
        parameters['overwrite'] = False

    # Add root
    # parameters = {k: ''.join([root, v]) if '_dir' in k else {k: v} for k, v in parameters.items()}

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
    processed_data = create_tour_enumeration(tables, parameters)

    outbound, inbound = tables['stop_duration']['inbound'], tables['stop_duration']['outbound']

    # create/update configs in place
    processed_data['tour_scheduling_specs'] = create_tour_scheduling_specs(tables['tour_TOD'], parameters)
    processed_data['stop_frequency_specs'] = create_stop_freq_specs(tables['stop_frequency'], parameters)
    processed_data['stop_duration_probs'] = create_trip_scheduling_duration_probs(inbound, outbound, parameters)
    processed_data['stop_purpose_probs'] = update_trip_purpose_probs(tables['stop_purpose'], parameters)
    # TODO create_skims_and_tap_files(settings, new_mazs)

    return {'parameters': parameters,
            'tables': tables,
            'processed': processed_data}


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


def find_settings_path(target):
    # Finds the target path if it is in a parent directory
    pardir = ''
    while not os.path.isfile(os.path.join(pardir, target)):
        pardir = os.path.join(os.pardir, pardir)
    return pardir


if __name__ == '__main__':
    # Testing
    visitor = preprocess_visitor()

    parameters = visitor['parameters']
    # stop_freq_probs = visitor['tables']['stop_frequency']
    # outbound, inbound = visitor['tables']['stop_duration']['inbound'],\
    #                     visitor['tables']['stop_duration']['outbound']

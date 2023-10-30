""" Output Checker

Checks that ABM components successfully generate required files.

"""

# Import libraries
import os
import sys


# Define model-output dictionary
output_dict = {
    "Setup": [
        "walkMgraTapEquivMinutes.csv",
        "microMgraTapEquivMinutes.csv",
        "microMgraEquivMinutes.csv",
        "bikeTazLogsum.csv",
        "bikeMgraLogsum.csv",
        "walkMgraEquivMinutes.csv"
    ],
    "SDRM": [
         "wsLocResults_ITER.csv",
         "aoResults.csv",
         "householdData_ITER.csv",
         "indivTourData_ITER.csv",
         "indivTripData_ITER.csv",
         "jointTourData_ITER.csv",
         "jointTripData_ITER.csv",
         "personData_ITER.csv"
    ],
    "IE": [
         "internalExternalTrips.csv"
    ],
    "SAN": [
         "airport_out.SAN.csv"
    ],
    "CBX": [
         "airport_out.CBX.csv"
    ],
    "CBM": [
         "crossBorderTrips.csv",
         "crossBorderTours.csv"
    ],
    "Visitor": [
         "visitorTrips.csv",
         "visitorTours.csv"
    ],
    "TNC": [
         "TNCTrips.csv"
    ],
    "AV": [
         "householdAVTrips.csv"
    ],
    "CVM": [
         "Gen and trip sum.csv"
    ]
}


def check_output(scenario_fp, component, iteration=None):
    """
    Checks that a specific ABM component generated
    required files.

    :param component: String representing ABM component
    :param scenario_fp: String representing scenario file path
    :param iteration: Integer representing ABM iteration
    :returns: Exit code
    """

    # Get required files
    files = output_dict[component]

    # Construct output file path
    output_dir = os.path.join(scenario_fp, 'output')

    # Check that required files were generated
    missing = []
    for file in files:

        # Append iteration integer if needed
        if 'ITER' in file:
            file = file.replace('ITER', iteration)
        
        if component == "CVM":
            file_path = os.path.join(output_dir, 'cvm', file)
        else:
            file_path = os.path.join(output_dir, file)
            
        if os.path.exists(file_path):
            continue
        else:
            missing.append(file+'\n')

    # Write out missing files to log file
    if len(missing) > 0:
        create_log(scenario_fp, component, missing)
        return sys.exit(2)

    return sys.exit(0)


def create_log(scenario_fp, component, lst):
    """
    Creates a log file containing the files an ABM component
    failed to generate.

    :param scenario_fp: String representing scenario file path
    :param component: ABM component
    :param lst: List of missing file names
    """

    # Create log file path
    log_fp = os.path.join(scenario_fp, 'logFiles', 'missing_files.log')

    # Create and write to log file
    with open(log_fp, "w") as log:
        header = "'{}' failed to generate the following files:\n".format(component)
        log.write(header)
        log.writelines(lst)

    return


if __name__ == '__main__':
    targets = sys.argv[1:]
    check_output(*targets)


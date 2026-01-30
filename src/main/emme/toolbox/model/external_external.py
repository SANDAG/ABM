#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/external_external.py                                            ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
# 
# 
# Runs the external-external, cross-regional demand model. Imports the total
# daily demand from file and splits by time-of-day and SOVGP, HOV2HOV, and
# HOV3HOV classes using fixed factors.
#
#
# Inputs:
#    input_directory: source directory for input file
#    external_zones: the set of external zones specified as a range, default is "1-12"
#    scenario: traffic scenario to use for reference zone system
#
# Files referenced:
#    Note: YEAR is replaced by scenarioYear in the conf/sandag_abm.properties file 
#    input/mgra13_based_inputYEAR.csv
#    input/externalInternalControlTotalsByYear.csv
#    input/externalInternalControlTotals.csv 
#        (if externalInternalControlTotalsByYear.csv is unavailable)
#
# Matrix results:
#    pp_SOV_EETRIPS, pp_HOV2_EETRIPS, pp_HOV3_EETRIPS
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    input_dir = os.path.join(main_directory, "input")
    external_zones = "1-12"
    base_scenario = modeller.scenario
    external_external = modeller.tool("sandag.model.external_external")
    external_external(input_dir, external_zones, base_scenario)
"""

 
TOOLBOX_ORDER = 62


import inro.modeller as _m

import multiprocessing as _multiprocessing
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExternalExternal(_m.Tool(), gen_utils.Snapshot):
    input_directory = _m.Attribute(str)
    external_zones = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.external_zones = "1-12"
        self.attributes = ["external_zones", "num_processors"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "External external model"
        pb.description = """
            Total trips are read from externalExternalTripsByYear.csv for
            the year in sandag_abm.properties. If this file does not exist 
            externalExternalTrips.csv will be used instead. 
            The total trips are split by time-of-day and traffic class
            SOVGP, HOV2HOV, and HOV3HOV using fixed factors.
        """
        pb.branding_text = "- SANDAG - Model"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')
        pb.add_text_box("external_zones", title="External zones:")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.input_directory, self.external_zones, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('External-external model', save_arguments=True)
    def __call__(self, input_directory, external_zones, scenario):
        attributes = {
            "external_zones": external_zones, 
            "input_directory": input_directory,
        }
        gen_utils.log_snapshot("External-external model", str(self), attributes)
        emmebank = scenario.emmebank
        matrix_calc = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_calculator")

        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(
            os.path.join(os.path.dirname(input_directory), "conf", "sandag_abm.properties"))
        year = int(props['scenarioYear'])

        periods = ["EA", "AM", "MD", "PM", "EV"]
        time_of_day_factors = [0.074, 0.137, 0.472, 0.183, 0.133]
        modes = ["SOV", "HOV2", "HOV3"]
        mode_factors = [0.43, 0.42, 0.15]

        ee_matrix = emmebank.matrix("ALL_TOTAL_EETRIPS")
        matrix_data = ee_matrix.get_data(scenario)
        file_path = os.path.join(
            input_directory, "externalExternalTripsByYear.csv")
        if os.path.isfile(file_path):
            with open(file_path, 'r') as f:
                header = f.readline()
                for line in f:
                    tyear, orig, dest, trips = line.split(",")
                    if int(tyear) == year:
                        matrix_data.set(int(orig), int(dest), float(trips))
        else:
            file_path = os.path.join(
                input_directory, "externalExternalTrips.csv")
            if not os.path.isfile(file_path):
                raise Exception("External-external model: no file 'externalExternalTrips.csv' or 'externalExternalTripsByYear.csv'")
            with open(file_path, 'r') as f:
                header = f.readline()
                for line in f:
                    orig, dest, trips = line.split(",")
                    matrix_data.set(int(orig), int(dest), float(trips))
        _m.logbook_write("Control totals read from %s" % file_path)
        ee_matrix.set_data(matrix_data, scenario)

        # factor for final demand matrix by time and mode type
        # all external-external trips are non-toll
        # SOV_GP, SR2_HOV SR3_HOV = "SOV", "HOV2", "HOV3"
        for period, tod_fac in zip(periods, time_of_day_factors):
            for mode, mode_fac in zip(modes, mode_factors):
                spec = {
                    "expression": "ALL_TOTAL_EETRIPS * %s * %s" % (tod_fac, mode_fac),
                    "result": "mf%s_%s_EETRIPS" % (period, mode),
                    "constraint": {
                        "by_zone": {
                            "origins": external_zones, 
                            "destinations": external_zones
                        }
                    },
                    "type": "MATRIX_CALCULATION"
                }
                matrix_calc(spec, scenario=scenario)

        precision = float(props['RunModel.MatrixPrecision'])
        self.matrix_rounding(scenario, precision)

    @_m.logbook_trace('Controlled rounding of demand')
    def matrix_rounding(self, scenario, precision):
        round_matrix = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_controlled_rounding")
        emmebank = scenario.emmebank
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        modes = ["SOV", "HOV2", "HOV3"]
        for period in periods:
            for mode in modes:
                matrix = emmebank.matrix("mf%s_%s_EETRIPS" % (period, mode))
                report = round_matrix(demand_to_round=matrix,
                                      rounded_demand=matrix,
                                      min_demand=precision,
                                      values_to_round="SMALLER_THAN_MIN",
                                      scenario=scenario)


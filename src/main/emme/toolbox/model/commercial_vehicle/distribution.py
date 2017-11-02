#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/commercial_vehicle/distribution.py                              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
# 
# Distributes the total daily trips from production and attraction vectors.
# Friction factors are calculated based on a blended travel time skim of 
# 1/3 AM_SOVGP_TIME and 2/3 MD_SOVGP_TIME, and a table of friction factor 
# lookup values from commVehFF.csv.
#
# Inputs:
#    input_directory: source directory for input files
#    scenario: traffic scenario to use for reference zone system
#
# Files referenced:
#   input/commVehFF.csv
#
# Matrix inputs:
#    moCOMVEH_PROD, mdCOMVEH_ATTR
#    mfAM_SOVGP_TIME, mfMD_SOVGP_TIME
#
# Matrix intermediates (only used internally):
#    mfCOMVEH_BLENDED_SKIM, mfCOMVEH_FRICTION
#
# Matrix results:
#    mfCOMVEH_TOTAL_DEMAND
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    project_dir = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    input_dir = os.path.join(project_dir, "input")
    base_scenario = modeller.scenario
    distribution = modeller.tool("sandag.model.commercial_vehicle.distribution")
    distribution(input_dir, base_scenario)
"""


TOOLBOX_ORDER = 53


import inro.modeller as _m
import traceback as _traceback

import pandas as pd
import os
import numpy as np


gen_utils = _m.Modeller().module("sandag.utilities.general")


class CommercialVehicleDistribution(_m.Tool(), gen_utils.Snapshot):

    input_directory = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["input_directory"]

    def page(self)
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial Vehicle Distribution"
        pb.description = """
<div style="text-align:left">
    Calculates total daily trips. 
    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
    <br>
    A simple gravity model is used to distribute the truck trips.  
    A blended travel time of 
    1/3 AM_SOVGP_TIME and 2/3 MD_SOVGP_TIME is used, along with 
    friction factor lookup table stored in commVehFF.csv.
    <br>
    Input:  
    <ul><li>
     (1) Level-of-service matrices for the AM peak period (6 am to 10 am) 'mfAM_SOVGP_TIME'
         and midday period (10 am to 3 pm) 'mfMD_SOVGP_TIME'
         which contain congested travel time (in minutes).
    </li><li>        
     (2) Trip generation results 'moCOMVEH_PROD' and 'mdCOMVEH_ATTR'
    </li><li>
     (4) A table of friction factors in commVehFF.csv with: 
        <ul><li>
        (a) impedance measure (blended travel time) index;
        </li><li> 
        (b) friction factors 
        </li></ul>
    </li></ul>
    Output: 
    <ul><li>
        (1) A trip table matrix 'mfCOMVEH_TOTAL_DEMAND'
            of daily class-specific truck trips for very small trucks. 
    </li><li> 
        (2) A blended travel time matrix 'mfCOMVEH_BLENDED_SKIM'
    </li></ul>
</div>"""
        pb.branding_text = "- SANDAG - Model - Commercial vehicle"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.input_directory, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Commercial vehicle distribution')
    def __call__(self, input_directory, scenario):
        attributes = {"input_directory": input_directory}
        gen_utils.log_snapshot("Commercial vehicle distribution", str(self), attributes)
        self.calc_blended_skims(scenario)
        self.calc_friction_matrix(input_directory, scenario)
        self.balance_matrix(scenario)

    @_m.logbook_trace('Calculate blended skims')
    def calc_blended_skims(self, scenario):
        matrix_calc = _m.Modeller().tool(
            'inro.emme.matrix_calculation.matrix_calculator')
        spec = {
            "expression": "0.3333 * mfAM_SOVGP_TIME + 0.6666 * mfMD_SOVGP_TIME",
            "result": "mfCOMVEH_BLENDED_SKIM",
            "type": "MATRIX_CALCULATION"
        }
        matrix_calc(spec, scenario=scenario)

        # Prevent intrazonal trips
        spec = {
            "expression": "99999 * (p.eq.q) + mfCOMVEH_BLENDED_SKIM * (p.ne.q)",
            "result": "mfCOMVEH_BLENDED_SKIM",
            "type": "MATRIX_CALCULATION"
        }
        matrix_calc(spec, scenario=scenario)

    @_m.logbook_trace('Calculate friction factor matrix')
    def calc_friction_matrix(self, input_directory, scenario):
        emmebank = scenario.emmebank

        imp_matrix = emmebank.matrix('mfCOMVEH_BLENDED_SKIM')
        friction_matrix = emmebank.matrix('mfCOMVEH_FRICTION')
        imp_array = imp_matrix.get_numpy_data(scenario_id=scenario.id)

        # create the vector function to bin the impedances and get friction values
        friction_table = pd.read_csv(
            os.path.join(input_directory, 'commVehFF.csv'),
            header=None, names=['index', 'factors'])

        factors_array = friction_table.factors.values
        max_index = len(factors_array) - 1

        # interpolation: floor
        values_array = np.clip(np.floor(imp_array).astype(int) - 1, 0, max_index)
        friction_array = np.take(factors_array, values_array)
        friction_matrix.set_numpy_data(friction_array, scenario_id=scenario.id)

    def balance_matrix(self, scenario):
        balance = _m.Modeller().tool(
            'inro.emme.matrix_calculation.matrix_balancing')
        spec = {
            "type": "MATRIX_BALANCING",
            "od_values_to_balance": "mfCOMVEH_FRICTION",
            "origin_totals": "moCOMVEH_PROD",
            "destination_totals": "mdCOMVEH_ATTR",
            "results": {
                "od_balanced_values": "mfCOMVEH_TOTAL_DEMAND",
            },
            "max_iterations": 100,
            "max_relative_error": 0.001
        }
        balance(spec, scenario=scenario)

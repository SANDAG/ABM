#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/commercial_vehicle/time_of_day.py                               ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
# 
# Applies time-of-day factoring to the Commercial vehicle total daily demand.  
# The diurnal factors are taken from the BAYCAST-90 model with adjustments 
# made during calibration to the very small truck values to better match counts. 
#
# Inputs:
#    scenario: traffic scenario to use for reference zone system
#
# Matrix inputs:
#    mfCOMVEH_TOTAL_DEMAND
#
# Matrix results:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    mfpp_COMVEH
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    base_scenario = modeller.scenario
    time_of_day = modeller.tool("sandag.model.commercial_vehicle.time_of_day")
    time_of_day(base_scenario)
"""

TOOLBOX_ORDER = 54


import inro.modeller as _m
import traceback as _traceback


dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TimeOfDay(_m.Tool()):

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial Vehicle Time of Day split"
        pb.description = """
<div style="text-align:left">
    Commercial vehicle time-of-day factoring.  
    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
    <br>
    The diurnal factors are taken from the BAYCAST-90 model with adjustments 
    made during calibration to the very small truck values to better match counts. 
    <p>Input:   A production/attraction format trip table matrix of daily very small truck trips.</p>
    <p>Output: Five, time-of-day-specific trip table matrices for very small trucks,
       of the form 'mfpp_COMVEH'. 
    </p>
</div>"""
        pb.branding_text = "- SANDAG - Model - Commercial vehicle"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Commercial vehicle Time of Day split')
    def __call__(self, scenario):
        matrix_calc = dem_utils.MatrixCalculator(scenario, 0)
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        period_factors = [0.0235, 0.1, 0.5080, 0.1980, 0.1705]
        for p, f in zip(periods, period_factors):
            matrix_calc.add(
                "mf%s_COMVEH" % p,
                "%s * 0.5 * (mfCOMVEH_TOTAL_DEMAND + mfCOMVEH_TOTAL_DEMAND')" % f)
        matrix_calc.run()

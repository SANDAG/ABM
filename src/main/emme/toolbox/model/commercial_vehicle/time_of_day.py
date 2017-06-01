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

TOOLBOX_ORDER = 54

 # Input:   A production/attraction format trip table matrix of daily very small truck trips.
 # Output: Five, time-of-day-specific trip table matrices containing very small trucks. 


import inro.modeller as _m
import traceback as _traceback


class TimeOfDay(_m.Tool()):

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def page(self):
        # Equivalent to commVehDist.rsc
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial Vehicle Time of Day distribution"
        pb.description = """
<div style="text-align:left">
    Commercial vehicle time-of-day factoring.  
    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
    <br>
    The diurnal factors are taken from the BAYCAST-90 model with adjustments 
    made during calibration to the very
    small truck values to better match counts. 
    <p>Input:   A production/attraction format trip table matrix of daily very small truck trips.</p>
    <p>Output: Five, time-of-day-specific trip table matrices for very small trucks,
       of the form 'mfXX_COMMVEH'. 
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

    @_m.logbook_trace('Commercial vehicle Time of Day distribution')
    def __call__(self, scenario):
        matrix_calc = _m.Modeller().tool(
            'inro.emme.matrix_calculation.matrix_calculator')

        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        period_factors = [0.0235, 0.1, 0.5080, 0.1980, 0.1705]
        for p, f in zip(periods, period_factors):
            spec = {
                "expression": "%s * 0.5 * (mfCOMMVEH_TOTAL_DEMAND + mfCOMMVEH_TOTAL_DEMAND')" % f,
                "result": "mf%s_COMMVEH" % p,
                "constraint": None,
                "type": "MATRIX_CALCULATION"
            }
            matrix_calc(spec, scenario=scenario)

#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/commercial_vehicle/toll_diversion.py                            ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
# 
# Applies toll and non-toll split to Commercial vehicle time period demand.
# Uses the travel TIME for GP and TOLL modes as well as the TOLLCOST 
# by time period.
#
# Inputs:
#    scenario: traffic scenario to use for reference zone system
#
# Matrix inputs:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    mfpp_COMVEH
#    mfpp_COMVEHGP_TIME, mfpp_COMVEHTOLL_TIME, mfpp_COMVEHTOLL_TOLLCOST
#
# Matrix results:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    mfpp_COMVEHGP, mfpp_COMVEHTOLL
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    base_scenario = modeller.scenario
    toll_diversion = modeller.tool("sandag.model.commercial_vehicle.toll_diversion")
    toll_diversion(base_scenario)
"""

TOOLBOX_ORDER = 55


import inro.modeller as _m
import traceback as _traceback


gen_utils = _m.Modeller().module('sandag.utilities.general')
dem_utils = _m.Modeller().module('sandag.utilities.demand')


class TollDiversion(_m.Tool()):

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial vehicle toll diversion"
        pb.description = """
<div style="text-align:left">
    Commercial vehicle toll and non-toll (GP) split.  
    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
    <br>    
    <p>Input: Time-of-day-specific trip table matrices 'mfpp_COMVEH', 
        and travel time for GP and TOLL modes 'mfpp_SOVGP_TIME', 'mfpp_SOVTOLL_TIME', 
        and toll cost 'mfpp_SOVTOLL_TOLLCOST'
    </p>
    <p>Output: Corresponding time-of-day 'mfpp_COMVEHGP' and 'mfpp_COMVEHTOLL'
        trip demand matrices.</p>
</div>
"""
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

    @_m.logbook_trace('Commercial vehicle toll diversion')
    def __call__(self, scenario):
        emmebank = scenario.emmebank
        matrix_calc = dem_utils.MatrixCalculator(scenario, 0)
        init_matrix = _m.Modeller().tool(
            'inro.emme.data.matix.initialize_matrix')

        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        init_matrix(["mf%s_COMVEHTOLL" % p for p in periods], scenario=scenario)

        nest = 10
        vot = 0.02
        toll_factor = 1
        for p in periods:
            with matrix_calc.trace_run("Diversion for %s" % p):
                params = {'p': p, 'v': vot, 'tf': toll_factor, 'n': nest}
                utility = ('(mf%(p)s_SOVGP_TIME - mf%(p)s_SOVTOLL_TIME'
                           '- %(v)s * mf%(p)s_SOVTOLL_TOLLCOST * %(tf)s) / %(n)s') % params
                matrix_calc.add(
                    "mf%s_COMVEHTOLL" % p, 
                    "mf%s_COMVEH / (1 + exp(- %s))" % (p, utility),
                    [0, 0, "EXCLUDE", "mf%s_SOVTOLL_TOLLCOST" % p])

                matrix_calc.add(
                    "mf%s_COMVEHGP" % p, "mf%(p)s_COMVEH - mf%(p)s_COMVEHTOLL" % {'p': p})

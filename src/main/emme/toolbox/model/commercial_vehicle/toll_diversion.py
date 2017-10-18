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

TOOLBOX_ORDER = 55


import inro.modeller as _m
import traceback as _traceback


class TollDiversion(_m.Tool()):

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def page(self):
        # Equivalent to commVehDiversion.rsc
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial vehicle toll diversion"
        pb.description = """
<div style="text-align:left">
    Commercial vehicle toll and non-toll (GP) split.  
    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
    <br>
    <p>Input: Time-of-day-specific trip table matrices 'mfXX_COMVEH'. </p>
    <p>Output: Corresponding time-of-day 'mfXX_COMVEHGP' and 'mfXX_COMVEHTOLL'
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
        gen_utils = _m.Modeller().module('sandag.utilities.general')
        matrix_calc = _m.Modeller().tool(
            'inro.emme.matrix_calculation.matrix_calculator')

        nest = 10
        vot = 0.02
        toll_factor = 1

        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        with gen_utils.temp_matrices(emmebank, "FULL") as [toll_cost]:
            toll_cost.name = "TOLL_COST_TEMP"
            for p in periods:
                with _m.logbook_trace("Diversion for %s" % p):
                    spec = {
                        "expression": 'mf%s_SOVTOLL_TOLLCOST .mod. 10000' % p,
                        "result": "mfTOLL_COST_TEMP",
                        "constraint": None,
                        "type": "MATRIX_CALCULATION"
                    }
                    matrix_calc(spec, scenario=scenario)
                    params = {'p': p, 'v': vot, 'tf': toll_factor, 'n': nest}
                    utility = ('(mf%(p)s_SOVGP_TIME - mf%(p)s_SOVTOLL_TIME'
                               '- %(v)s * mfTOLL_COST_TEMP * %(tf)s) / %(n)s') % params

                    spec = {
                        "expression": "mf%s_COMVEH / (1 + exp(- %s))" % (p, utility),
                        "result": "mf%s_COMVEHTOLL" % p,
                        "constraint": {
                            "by_value": {
                                "interval_min": 0,
                                "interval_max": 0,
                                "condition": "EXCLUDE",
                                "od_values": "mfTOLL_COST_TEMP"
                            },
                        },
                        "type": "MATRIX_CALCULATION"
                    }
                    matrix_calc(spec, scenario=scenario)

                    spec = {
                        "expression": "mf%(p)s_COMVEH - mf%(p)s_COMVEHTOLL" % {'p': p},
                        "result": "mf%s_COMVEHGP" % p,
                        "type": "MATRIX_CALCULATION"
                    }
                    matrix_calc(spec, scenario=scenario)

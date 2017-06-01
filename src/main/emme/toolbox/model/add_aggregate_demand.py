#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/add_aggregate_demand.py                                         ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 63


import traceback as _traceback
import os

import inro.modeller as _m


class AddAggregateDemand(_m.Tool()):

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Add aggregate demand"
        pb.description = """ 
<div style="text-align:left">    
    Adds the aggregate demand from the commercial vehicle model, 
    external-external and external-internal to the time-of-day
    total demand matrices.
    <br>
    Note: do not run repeatedly.
</div>
        """
        pb.branding_text = "- SANDAG - Model"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Add aggregate demand', save_arguments=True)
    def __call__(self, scenario):
        matrix_calc = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        periods = ["EA", "AM", "MD", "PM", "EV"]
        with _m.logbook_trace("Add external-external trips to auto demand"):
            for period in periods:
                spec = {
                    "expression": "mf%(p)s_SOVGP + mf%(p)s_COMVEHGP " % ({'p': period}),
                    "result": "mf%s_SOVGP" % period,
                    "type": "MATRIX_CALCULATION"
                }
                matrix_calc(spec, scenario=scenario)
                spec = {
                    "expression": "mf%(p)s_SOVTOLL + mf%(p)s_COMVEHGP" % ({'p': period}),
                    "result": "mf%s_SOVTOLL" % period,
                    "type": "MATRIX_CALCULATION"
                }
                matrix_calc(spec, scenario=scenario)

        with _m.logbook_trace("Add external-internal trips to auto demand"):
            modes = ["SOVGP", "SOVTOLL", "HOV2HOV", "HOV2TOLL", "HOV3HOV", "HOV3TOLL"]
            for period in periods:
                for mode in modes:
                    spec = {
                        "expression": "mf%(p)s_%(m)s "
                                      "+ mf%(p)s_%(m)s_EIWORK "
                                      "+ mf%(p)s_%(m)s_EINONWORK" % ({'p': period, 'm': mode}),
                        "result": "mf%s_%s" % (period, mode),
                        "type": "MATRIX_CALCULATION"
                    }
                    matrix_calc(spec, scenario=scenario)

        with _m.logbook_trace("Add external-external trips to auto demand"):
            modes = ["SOVGP", "HOV2HOV", "HOV3HOV"]
            for period in periods:
                for mode in modes:
                    spec = {
                        "expression": "mf%(p)s_%(m)s + mf%(p)s_%(m)s_EETRIPS" % ({'p': period, 'm': mode}),
                        "result": "mf%s_%s" % (period, mode),
                        "constraint": {
                            "by_zone": {
                                "origins": external_zones, 
                                "destinations": external_zones
                            }
                        },
                        "type": "MATRIX_CALCULATION"
                    }
                    matrix_calc(spec, scenario=scenario)

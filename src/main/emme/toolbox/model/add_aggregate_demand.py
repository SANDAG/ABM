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


import inro.modeller as _m
import traceback as _traceback
import os


dem_utils = _m.Modeller().module('sandag.utilities.demand')


class AddAggregateDemand(_m.Tool()):

    external_zones = _m.Attribute(str)
    num_processors = _m.Attribute(str)
    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.external_zones = "1-12"
        self.num_processors = "MAX-1"

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Add aggregate demand"
        pb.description = """ 
<div style="text-align:left">    
    Adds the aggregate demand from the commercial vehicle model, 
    external-external and external-internal to the time-of-day
    total demand matrices.
    <br>
    Note: for Run emme components only. Replaced by Sum demand for use with
    full ABM model.
</div>
        """
        pb.branding_text = "- SANDAG - Model"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        pb.add_text_box("external_zones", title="External zones:")
        dem_utils.add_select_processors("num_processors", pb, self)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.external_zones, self.num_processors, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Add aggregate demand', save_arguments=True)
    def __call__(self, external_zones, num_processors, scenario):
        matrix_calc = dem_utils.MatrixCalculator(scenario, num_processors)
        periods = ["EA", "AM", "MD", "PM", "EV"]
        with matrix_calc.trace_run("Add commercial vehicle trips to auto demand"):
            for period in periods:
                matrix_calc.add("mf%s_SOVGP" % period, "mf%(p)s_SOVGP + mf%(p)s_COMVEHGP " % ({'p': period}))
                matrix_calc.add("mf%s_SOVTOLL" % period, "mf%(p)s_SOVTOLL + mf%(p)s_COMVEHGP" % ({'p': period}))

        with matrix_calc.trace_run("Add external-internal trips to auto demand"):
            modes = ["SOVGP", "SOVTOLL", "HOV2HOV", "HOV2TOLL", "HOV3HOV", "HOV3TOLL"]
            for period in periods:
                for mode in modes:
                    matrix_calc.add("mf%s_%s" % (period, mode),
                         "mf%(p)s_%(m)s + mf%(p)s_%(m)s_EIWORK + mf%(p)s_%(m)s_EINONWORK" % ({'p': period, 'm': mode}))

        # External - external faster with single-processor as number of O-D pairs is so small (12 X 12)
        matrix_calc.num_processors = 0
        with matrix_calc.trace_run("Add external-external trips to auto demand"):
            modes = ["SOVGP", "HOV2HOV", "HOV3HOV"]
            for period in periods:
                for mode in modes:
                    matrix_calc.add("mf%s_%s" % (period, mode),
                        "mf%(p)s_%(m)s + mf%(p)s_%(m)s_EETRIPS" % ({'p': period, 'm': mode}),
                        {"origins": external_zones, "destinations": external_zones})

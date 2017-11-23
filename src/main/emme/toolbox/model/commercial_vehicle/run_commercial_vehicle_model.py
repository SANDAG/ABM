#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/commercial_vehicle/run_commercial_vehicle_model.py              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# Runs the commercial vehicle model, the generation, distribution, time-of-day,
# and toll diversion tools, in sequence.
#    1) Generates 
#    2) Generates 
#    3) Distributes trips based on congested skims 
#    4) Time-of-day split
#    5) Applies toll diversion model with toll and non-toll SOV skims 
#
#   The very small truck generation model is based on the Phoenix 
#   four-tire truck model documented in the TMIP Quick Response Freight Manual. 
#
# Inputs:
#    run_generation: boolean, if True run generation tool.
#    input_directory: source directory for most input files, including demographics and trip rates 
#        (see generation and distribtuion tools)
#    scenario: traffic scenario to use for reference zone system
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    project_dir = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    input_dir = os.path.join(project_dir, "input")
    base_scenario = modeller.scenario
    run_truck = modeller.tool("sandag.model.commercial_vehicle.run_commercial_vehicle")
    run_truck(True, input_dir, base_scenario)
"""

TOOLBOX_ORDER = 51


import inro.modeller as _m
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class CommercialVehicleModel(_m.Tool(), gen_utils.Snapshot):

    input_directory = _m.Attribute(str)
    run_generation = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.run_generation = True
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["input_directory", "run_generation"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Commercial vehicle model"
        pb.description = """
<div style="text-align:left">
    Run the 4 steps of the commercial vehicle model: generation, distribution,
    time of day, toll diversion.

    The very small truck generation model is based on the Phoenix 
    four-tire truck model documented in the TMIP Quick Response Freight Manual. 
</div>
"""
        pb.branding_text = "- SANDAG - Model - Commercial vehicle"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        pb.add_checkbox("run_generation", title=" ", label="Run generation (first iteration)")

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.run_generation, self.input_directory, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Commercial vehicle model', save_arguments=True)
    def __call__(self, run_generation, input_directory, scenario):
        attributes = {"run_generation": run_generation, "input_directory": input_directory}
        gen_utils.log_snapshot("Commercial vehicle model", str(self), attributes)
        generation = _m.Modeller().tool(
            'sandag.model.commercial_vehicle.generation')
        distribution = _m.Modeller().tool(
            'sandag.model.commercial_vehicle.distribution')
        time_of_day = _m.Modeller().tool(
            'sandag.model.commercial_vehicle.time_of_day')
        diversion = _m.Modeller().tool(
            'sandag.model.commercial_vehicle.toll_diversion')
        if run_generation:
            generation(input_directory, scenario)
        distribution(input_directory, scenario)
        time_of_day(scenario)
        diversion(scenario)

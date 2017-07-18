#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// truck/run_truck_model.py                                              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 41


import inro.modeller as _m
import traceback as _traceback
import os


dem_utils = _m.Modeller().module("sandag.utilities.demand")
gen_utils = _m.Modeller().module("sandag.utilities.general")


class TruckModel(_m.Tool(), gen_utils.Snapshot):

    input_directory = _m.Attribute(str)
    input_truck_directory = _m.Attribute(str)
    run_generation = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.run_generation = True
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.input_truck_directory = os.path.join(os.path.dirname(project_dir), "input_truck")
        self.num_processors = "MAX-1"
        self.attributes = ["input_directory", "input_truck_directory", "run_generation", "num_processors"]

    def page(self):
        # Equivalent to TruckModel.rsc
        pb = _m.ToolPageBuilder(self)
        pb.title = "Truck model"
        pb.description = """
<div style="text-align:left">

    1) Generates standard truck trip and special (military) truck trips<br>
    2) Gets regional truck trips, IE trips, EI trips and EE trips and balances truck trips<br>
    3) Distributes truck trips with congested skims and splits by time of day<br>
    4) Applies truck toll diversion model with free-flow toll and non-toll skims<br>
</div>
"""
        pb.branding_text = "- SANDAG - Model - Truck"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        pb.add_checkbox("run_generation", title=" ", label="Run generation (first iteration)")

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')
        pb.add_select_file('input_truck_directory', 'directory',
                           title='Select truck input directory')
        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.run_generation, self.input_directory, self.input_truck_directory, self.num_processors, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Truck model', save_arguments=True)
    def __call__(self, run_generation, input_directory, input_truck_directory, num_processors, scenario):
        attributes = {
            "input_directory": input_directory, "input_truck_directory": input_truck_directory,
            "run_generation": run_generation, "num_processors": num_processors
        }
        gen_utils.log_snapshot("Truck model", str(self), attributes)

        generation = _m.Modeller().tool('sandag.model.truck.generation')
        distribution = _m.Modeller().tool('sandag.model.truck.distribution')

        if run_generation:
            generation(input_directory, input_truck_directory, scenario)
        demand_as_pce = True
        distribution(input_directory, demand_as_pce, num_processors, scenario)

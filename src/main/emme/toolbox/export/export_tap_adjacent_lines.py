#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export/export_tap_adjacent_lines.py                                   ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Exports a list of transit lines adjacent to each TAP.
# 
#
# Inputs:
#    file_path: export path for the tap adjacency file
#    scenario: scenario ID for the base scenario (same used in the Import network tool)
#
# Files created:
#    output/tapLines.csv (or as specified)
#
#
# Script example:
"""
import inro.modeller as _m
import os
modeller = _m.Modeller()
desktop = modeller.desktop

export_tap_adjacent_lines = modeller.tool("sandag.export.export_tap_adjacent_lines")

project_dir = os.path.dirname(desktop.project_path())
main_directory = os.path.dirname(project_dir)
output_dir = os.path.join(main_directory, "output")

main_emmebank = os.path.join(project_dir, "Database", "emmebank")
scenario_id = 100
base_scenario = main_emmebank.scenario(scenario_id)

export_tap_adjacent_lines(os.path.join(output_dir, "tapLines.csv"), base_scenario)

"""


TOOLBOX_ORDER = 75


import inro.modeller as _m
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExportLines(_m.Tool(), gen_utils.Snapshot):

    file_path = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        main_dir = os.path.dirname(project_dir)
        self.file_path = os.path.join(main_dir, "output", "tapLines.csv")
        self.attributes = ["file_path"]

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export TAP adjacent lines"
        pb.description = """Exports a list of the transit lines adjacent to each tap."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('file_path', 'save_file',title='Select file path')

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.file_path, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export list of TAP adjacent lines", save_arguments=True)
    def __call__(self, file_path, scenario):
        attributes = {"file_path": file_path}
        gen_utils.log_snapshot("Export list of TAP adjacent lines", str(self), attributes)

        network = scenario.get_partial_network(
            ["NODE", "TRANSIT_LINE"], include_attributes=False)
        values = scenario.get_attribute_values("NODE", ["@tap_id"])
        network.set_attribute_values("NODE", ["@tap_id"], values)            
        with open(file_path, 'w', newline='') as f:
            f.write("TAP,LINES\n")
            for node in network.nodes():
                if node["@tap_id"] == 0:
                    continue
                lines = set([])
                for link in node.outgoing_links():
                    for seg in link.j_node.outgoing_segments(include_hidden=True):
                        if seg.allow_alightings:
                            lines.add(seg.line)
                if not lines:
                    continue
                f.write("%d," % node["@tap_id"])
                f.write(" ".join([l.id for l in lines]))
                f.write("\n")

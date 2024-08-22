#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export/export_traffic_skims.py                                        ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Exports the traffic skims for use in the disaggregate demand models (CT-RAMP)
# and the data loader process.
# 
# Note the matrix name mapping from the OMX file names to the Emme database names.
#
# Inputs:
#    omx_file: output directory to read the OMX files from
#    period: the period for which to export the skim matrices, "EA", "AM", "MD", "PM", "EV"
#    scenario: base traffic scenario to use for reference zone system
#
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    output_dir = os.path.join(main_directory, "output")
    scenario = modeller.scenario
    periods = ["EA", "AM", "MD", "PM", "EV"]
    export_traffic_skims = modeller.tool("sandag.import.export_traffic_skims")
    for period in periods:
        omx_file_path = os.path.join(output_dir, "traffic_skims_%s.omx" % period
        export_traffic_skims(output_dir, period, scenario)
"""

TOOLBOX_ORDER = 71


import inro.modeller as _m
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExportSkims(_m.Tool(), gen_utils.Snapshot):

    omx_file = _m.Attribute(str)
    period = _m.Attribute(str)
    tool_run_msg = ""

    def __init__(self):
        self.attributes = ["omx_file", "period"]

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export traffic skims"
        pb.description = """Export the skim matrices to OMX format for the selected period."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        pb.add_select_file('omx_file', 'save_file', title='Select OMX file')
        options = [(x, x) for x in ["EA", "AM", "MD", "PM", "EV"]]
        pb.add_select("period", keyvalues=options, title="Select corresponding period")
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.period, self.omx_file, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export traffic skims to OMX", save_arguments=True)
    def __call__(self, period, omx_file, scenario):
        attributes = {"omx_file": omx_file, "period": period}
        gen_utils.log_snapshot("Export traffic skims to OMX", str(self), attributes)
        init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
        matrices = init_matrices.get_matrix_names("traffic_skims", [period], scenario)
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME") as exporter:
            exporter.write_matrices(matrices)

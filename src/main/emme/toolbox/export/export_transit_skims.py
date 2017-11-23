#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export/export_transit_skims.py                                        ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Exports the transit skims for use in the disaggregate demand models (CT-RAMP)
# and the data loader process.
# 
# Note the matrix name mapping from the OMX file names to the Emme database names.
#
# Inputs:
#    omx_file: output directory to read the OMX files from
#    big_to_zero: replace big values (>10E6) with zero
#       This is used in the final iteration skim (after the demand models are 
#       complete) to filter large values from the OMX files which are not 
#       compatible with the data loader process
#    scenario: transit scenario to use for reference zone system
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    output_dir = os.path.join(main_directory, "output")
    scenario = modeller.scenario
    export_transit_skims = modeller.tool("sandag.model.import.export_transit_skims")
    omx_file_path = os.path.join(output_dir, "transit_skims.omx"
    export_transit_skims(output_dir, period, scenario)
"""


TOOLBOX_ORDER = 72


import inro.modeller as _m
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExportSkims(_m.Tool(), gen_utils.Snapshot):
    omx_file = _m.Attribute(unicode)

    tool_run_msg = ""

    def __init__(self):
        self.attributes = ["omx_file"]

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export transit skim matrices"
        pb.description = """Export the skim matrices to OMX format for all periods."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        pb.add_select_file('omx_file', 'save_file', title='Select OMX file')
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.omx_file, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export transit skims to OMX", save_arguments=True)
    def __call__(self, omx_file, scenario):
        attributes = {"omx_file": omx_file}
        gen_utils.log_snapshot("Export transit skims to OMX", str(self), attributes)
        init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
        matrices = init_matrices.get_matrix_names(
            "transit_skims", ["EA", "AM", "MD", "PM", "EV"], scenario)
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME") as exporter:
            exporter.write_matrices(matrices)

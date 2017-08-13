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

        pb.add_select_file('omx_file', 'save_file',
                           title='Select OMX file')

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
        matrices = []
        for period in ["EA", "AM", "MD", "PM", "EV"]:
            for name in self._skim_names():
                matrices.append(period + "_" + name)
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME") as exporter:
            exporter.write_matrices(matrices)

    def _skim_names(self):
        matrices = [
            "BUS_GENCOST",
            "BUS_TOTALWAIT",
            "BUS_TOTALWALK",
            "BUS_TOTALIVTT",
            "BUS_DWELLTIME",
            "BUS_FIRSTWAIT",
            "BUS_XFERWAIT",
            "BUS_FARE",
            "BUS_XFERS",
            "BUS_ACCWALK",
            "BUS_XFERWALK",
            "BUS_EGRWALK",
            "ALL_GENCOST",
            "ALL_TOTALWAIT",
            "ALL_TOTALWALK",
            "ALL_TOTALIVTT",
            "ALL_FIRSTWAIT",
            "ALL_XFERWAIT",
            "ALL_FARE",
            "ALL_XFERS",
            "ALL_ACCWALK",
            "ALL_XFERWALK",
            "ALL_EGRWALK",
            "ALL_DWELLTIME",
            "ALL_BUSIVTT",
            "ALL_LRTIVTT",
            "ALL_CMRIVTT",
            "ALL_EXPIVTT",
            "ALL_LTDEXPIVTT",
            "ALL_BRTREDIVTT",
            "ALL_BRTYELIVTT",
        ]
        return matrices

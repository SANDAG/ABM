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

TOOLBOX_ORDER = 71


import inro.modeller as _m
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExportSkims(_m.Tool(), gen_utils.Snapshot):

    omx_file = _m.Attribute(unicode)
    period = _m.Attribute(str)
    tool_run_msg = ""

    def __init__(self):
        self.attributes = ["omx_file", "period"]

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export traffic skims"
        pb.description = """Export the skim matrices to OMX format for the selected period."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('omx_file', 'save_file',
                           title='Select OMX file')
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
        matrices = [period + "_" + name for name in self._skim_names()]
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME") as exporter:
            exporter.write_matrices(matrices)

    def _skim_names(self):
        matrices = [
            "SOVGPL_GENCOST",
            "SOVGPL_TIME",
            "SOVGPL_DIST",
            "SOVGPL_REL",
            "SOVTOLLL_GENCOST",
            "SOVTOLLL_TIME",
            "SOVTOLLL_DIST",
            "SOVTOLLL_MLCOST",
            "SOVTOLLL_TOLLCOST",
            "SOVTOLLL_TOLLDIST",
            "SOVTOLLL_REL",
            "HOV2HOVL_GENCOST",
            "HOV2HOVL_TIME",
            "HOV2HOVL_DIST",
            "HOV2HOVL_HOVDIST",
            "HOV2HOVL_REL",
            "HOV2TOLLL_GENCOST",
            "HOV2TOLLL_TIME",
            "HOV2TOLLL_DIST",
            "HOV2TOLLL_MLCOST",
            "HOV2TOLLL_TOLLCOST",
            "HOV2TOLLL_TOLLDIST",
            "HOV2TOLLL_REL",
            "HOV3HOVL_GENCOST",
            "HOV3HOVL_TIME",
            "HOV3HOVL_DIST",
            "HOV3HOVL_HOVDIST",
            "HOV3HOVL_REL",
            "HOV3TOLLL_GENCOST",
            "HOV3TOLLL_TIME",
            "HOV3TOLLL_DIST",
            "HOV3TOLLL_MLCOST",
            "HOV3TOLLL_TOLLCOST",
            "HOV3TOLLL_TOLLDIST",
            "HOV3TOLLL_REL",
            "SOVGPM_GENCOST",
            "SOVGPM_TIME",
            "SOVGPM_DIST",
            "SOVGPM_REL",
            "SOVTOLLM_GENCOST",
            "SOVTOLLM_TIME",
            "SOVTOLLM_DIST",
            "SOVTOLLM_MLCOST",
            "SOVTOLLM_TOLLCOST",
            "SOVTOLLM_TOLLDIST",
            "SOVTOLLM_REL",
            "HOV2HOVM_GENCOST",
            "HOV2HOVM_TIME",
            "HOV2HOVM_DIST",
            "HOV2HOVM_HOVDIST",
            "HOV2HOVM_REL",
            "HOV2TOLLM_GENCOST",
            "HOV2TOLLM_TIME",
            "HOV2TOLLM_DIST",
            "HOV2TOLLM_MLCOST",
            "HOV2TOLLM_TOLLCOST",
            "HOV2TOLLM_TOLLDIST",
            "HOV2TOLLM_REL",
            "HOV3HOVM_GENCOST",
            "HOV3HOVM_TIME",
            "HOV3HOVM_DIST",
            "HOV3HOVM_HOVDIST",
            "HOV3HOVM_REL",
            "HOV3TOLLM_GENCOST",
            "HOV3TOLLM_TIME",
            "HOV3TOLLM_DIST",
            "HOV3TOLLM_MLCOST",
            "HOV3TOLLM_TOLLCOST",
            "HOV3TOLLM_TOLLDIST",
            "HOV3TOLLM_REL",
            "SOVGPH_GENCOST",
            "SOVGPH_TIME",
            "SOVGPH_DIST",
            "SOVGPH_REL",
            "SOVTOLLH_GENCOST",
            "SOVTOLLH_TIME",
            "SOVTOLLH_DIST",
            "SOVTOLLH_MLCOST",
            "SOVTOLLH_TOLLCOST",
            "SOVTOLLH_TOLLDIST",
            "SOVTOLLH_REL",
            "HOV2HOVH_GENCOST",
            "HOV2HOVH_TIME",
            "HOV2HOVH_DIST",
            "HOV2HOVH_HOVDIST",
            "HOV2HOVH_REL",
            "HOV2TOLLH_GENCOST",
            "HOV2TOLLH_TIME",
            "HOV2TOLLH_DIST",
            "HOV2TOLLH_MLCOST",
            "HOV2TOLLH_TOLLCOST",
            "HOV2TOLLH_TOLLDIST",
            "HOV2TOLLH_REL",
            "HOV3HOVH_GENCOST",
            "HOV3HOVH_TIME",
            "HOV3HOVH_DIST",
            "HOV3HOVH_HOVDIST",
            "HOV3HOVH_REL",
            "HOV3TOLLH_GENCOST",
            "HOV3TOLLH_TIME",
            "HOV3TOLLH_DIST",
            "HOV3TOLLH_MLCOST",
            "HOV3TOLLH_TOLLCOST",
            "HOV3TOLLH_TOLLDIST",
            "HOV3TOLLH_REL",
            "TRKHGP_GENCOST",
            "TRKHGP_TIME",
            "TRKHGP_DIST",
            "TRKHTOLL_GENCOST",
            "TRKHTOLL_TIME",
            "TRKHTOLL_DIST",
            "TRKHTOLL_TOLLCOST",
            "TRKLGP_GENCOST",
            "TRKLGP_TIME",
            "TRKLGP_DIST",
            "TRKLTOLL_GENCOST",
            "TRKLTOLL_TIME",
            "TRKLTOLL_DIST",
            "TRKLTOLL_TOLLCOST",
            "TRKMGP_GENCOST",
            "TRKMGP_TIME",
            "TRKMGP_DIST",
            "TRKMTOLL_GENCOST",
            "TRKMTOLL_TIME",
            "TRKMTOLL_DIST",
            "TRKMTOLL_TOLLCOST",
        ]
        return matrices

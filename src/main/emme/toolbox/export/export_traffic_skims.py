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


class ExportSkims(_m.Tool()):

    omx_file = _m.Attribute(unicode)
    period = _m.Attribute(str)
    tool_run_msg = ""

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

        pb.add_select_file('omx_file', 'file',
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
        export_to_omx = _m.Modeller().tool(
            "inro.emme.data.matrix.export_to_omx")
        
        matrices = [period + "_" + name for name in self._skim_names()]
        export_to_omx(matrices=matrices, 
                      export_file=omx_file,
                      append_to_file=False, 
                      scenario=scenario,
                      omx_key="NAME")

    def _skim_names(self):
        matrices = [
            "SOVGP_GENCOST",
            "SOVGP_TIME",
            "SOVGP_DIST",
            "SOVTOLL_GENCOST",
            "SOVTOLL_TIME",
            "SOVTOLL_DIST",
            "SOVTOLL_MLCOST",
            "SOVTOLL_TOLLCOST",
            "SOVTOLL_TOLLDIST",
            "HOV2HOV_GENCOST",
            "HOV2HOV_TIME",
            "HOV2HOV_DIST",
            "HOV2HOV_HOVDIST",
            "HOV2TOLL_GENCOST",
            "HOV2TOLL_TIME",
            "HOV2TOLL_DIST",
            "HOV2TOLL_MLCOST",
            "HOV2TOLL_TOLLCOST",
            "HOV2TOLL_TOLLDIST",
            "HOV3HOV_GENCOST",
            "HOV3HOV_TIME",
            "HOV3HOV_DIST",
            "HOV3HOV_HOVDIST",
            "HOV3TOLL_GENCOST",
            "HOV3TOLL_TIME",
            "HOV3TOLL_DIST",
            "HOV3TOLL_MLCOST",
            "HOV3TOLL_TOLLCOST",
            "HOV3TOLL_TOLLDIST",
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

    def _verify_matrices(self, matrices, scenario):
        import numpy
        emmbank = scenario.emmebank

        for name in matrices:
            matrix = emmebank.matrix(name)
            data = matrix.get_numpy_data(scenario)
            data = numpy.ma.masked_greater(data, 9999999)
            print "%-25s %9.3g %9.3g %9.3g %9.3g" % (name, data.sum(), data.min(), data.max(), data.mean())

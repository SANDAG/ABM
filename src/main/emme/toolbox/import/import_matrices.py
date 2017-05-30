#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/import_matrices.py                                             ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////


import inro.modeller as _m
import traceback as _traceback


class ImportMatrices(_m.Tool()):

    omx_file = _m.Attribute(unicode)
    demand_type = _m.Attribute(str)
    period = _m.Attribute(str)
    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Import demand matrices"
        pb.description = """."""
        pb.branding_text = "- SANDAG - Import"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('omx_file', 'file',
                           title='Select input OMX file')
        options = [(x, x) for x in ["AUTO", "TRUCK", "TRANSIT"]]
        pb.add_select("demand_type", keyvalues=options, title="Select corresponding demand type")
        options = [(x, x) for x in ["EA", "AM", "MD", "PM", "EV"]]
        pb.add_select("period", keyvalues=options, title="Select corresponding period")
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.omx_file, self.demand_type, self.period, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Import matrices", save_arguments=True)
    def __call__(self, omx_file, demand_type, period, scenario):
        demand_types = ["AUTO", "TRUCK", "TRANSIT"]
        if demand_type not in demand_types:
            raise Exception("Invalid demand_type, must be one of %s" % demand_types)
        periods = ["EA", "AM", "MD", "PM", "EV"]
        if period not in periods:
            raise Exception("Invalid period, must be one of %s" % periods)

        import_from_omx = _m.Modeller().tool(
            "inro.emme.data.matrix.import_from_omx")

        if demand_type == "AUTO":
            matrices = {
                'SOV_GP':   'mf"%s_SOVGP"',
                'SOV_PAY':  'mf"%s_SOVTOLL"',
                'SR2_GP':   'mf"%s_HOV2GP"',
                'SR2_HOV':  'mf"%s_HOV2HOV"',
                'SR2_PAY':  'mf"%s_HOV2TOLL"',
                'SR3_GP':   'mf"%s_HOV3GP"',
                'SR3_HOV':  'mf"%s_HOV3HOV"',
                'SR3_PAY':  'mf"%s_HOV3TOLL"'}
        if demand_type == "TRUCK":
            matrices = {
                'hhdn':     'mf"%s_TRKHGP"',
                'hhdt':     'mf"%s_TRKHTOLL"',
                'lhdn':     'mf"%s_TRKLGP"',
                'lhdt':     'mf"%s_TRKLTOLL"',
                'mhdn':     'mf"%s_TRKMGP"',
                'mhdt':     'mf"%s_TRKMTOLL"'}

        if demand_type == "TRANSIT":
            matrices = {
                'WLK_LOC':  'mf"%s_WLKBUS"',
                'WLK_LRT':  'mf"%s_WLKLRT"',
                'WLK_CMR':  'mf"%s_WLKCMR"',
                'WLK_EXP':  'mf"%s_WLKEXP"',
                'WLK_BRT':  'mf"%s_WLKBRT"',
                'PNR_LOC':  'mf"%s_PNRBUS"',
                'PNR_LRT':  'mf"%s_PNRLRT"',
                'PNR_CMR':  'mf"%s_PNRCMR"',
                'PNR_EXP':  'mf"%s_PNREXP"',
                'PNR_BRT':  'mf"%s_PNRBRT"',
                'KNR_LOC':  'mf"%s_KNRBUS"',
                'KNR_LRT':  'mf"%s_KNRLRT"',
                'KNR_CMR':  'mf"%s_KNRCMR"',
                'KNR_EXP':  'mf"%s_KNREXP"',
                'KNR_BRT':  'mf"%s_KNRBRT"'}

        matrices = dict((k, v % period) for k, v in matrices.iteritems())
        import_from_omx(file_path=omx_file, matrices=matrices, scenario=scenario)

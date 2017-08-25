#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/import_seed_demand.py                                          ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 12


import inro.modeller as _m
import inro.emme.matrix as _matrix
import traceback as _traceback
import omx as _omx


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ImportMatrices(_m.Tool(), gen_utils.Snapshot):

    omx_file = _m.Attribute(unicode)
    demand_type = _m.Attribute(str)
    period = _m.Attribute(str)
    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.attributes = ["omx_file", "demand_type", "period"]
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Import demand matrices"
        pb.description = """Imports the seed demand matrices."""
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

    def __call__(self, omx_file, demand_type, period, scenario, convert_truck_to_pce=None):
        attributes = {
            "omx_file": omx_file, 
            "demand_type": demand_type, 
            "period": period, 
            "scenario": scenario.id, 
            "convert_truck_to_pce": convert_truck_to_pce,
            "self": str(self)
        }
        with _m.logbook_trace("Import %s matrices for period %s" % (demand_type, period), attributes=attributes):
            gen_utils.log_snapshot("Import matrices", str(self), attributes)
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
            if demand_type == "TRANSIT":
                # special custom mapping from subset of TAPs to all TAPs
                emme_zones = scenario.zone_numbers
                emmebank = scenario.emmebank
                omx_file_obj = _omx.openFile(omx_file, 'r')
                try:
                    zone_mapping = omx_file_obj.mapping(omx_file_obj.listMappings()[0]).items()
                    zone_mapping.sort(key=lambda x: x[1])
                    omx_zones = [x[0] for x in zone_mapping]
                    for omx_name, emme_name in matrices.iteritems():
                        omx_data = omx_file_obj[omx_name].read()
                        matrix_data = _matrix.MatrixData(type='f', indices=[omx_zones, omx_zones])
                        matrix_data.from_numpy(omx_data)
                        expanded_matrix_data = matrix_data.expand([emme_zones, emme_zones])
                        matrix = emmebank.matrix(emme_name)
                        matrix.set_data(expanded_matrix_data, scenario)
                finally:
                    omx_file_obj.close()
            else:
                import_from_omx(file_path=omx_file, matrices=matrices, scenario=scenario)
                if demand_type == "TRUCK" :
                    self.convert_truck(scenario, period, convert_truck_to_pce)

    @_m.logbook_trace('Convert truck vehicle demand to PCE')
    def convert_truck(self, scenario, period, convert_truck_to_pce):
        matrix_calc = _m.Modeller().tool(
            'inro.emme.matrix_calculation.matrix_calculator')
        # Calculate PCEs for trucks
        mat_trucks = ['TRKHGP', 'TRKHTOLL', 'TRKLGP', 'TRKLTOLL', 'TRKMGP', 'TRKMTOLL']
        if convert_truck_to_pce:
            pce_values = [2.5,      2.5,        1.3,      1.3,        1.5,      1.5]
        else:
            pce_values = [1.0,      1.0,        1.0,      1.0,        1.0,      1.0]
        for name, pce in zip(mat_trucks, pce_values):
            demand_name = "%s_%s" % (period, name)
            mat_spec = {
                "expression": '(mf"%s" * %s).max.0' % (demand_name, pce), 
                "result": 'mf"%s"' % demand_name,
                "type": "MATRIX_CALCULATION"
            }
            matrix_calc(mat_spec, scenario)

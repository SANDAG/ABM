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
# 
# Imports the warm start demand matrices from specified OMX files for auto, truck and transit.
# 
# Note the matrix name mapping from the OMX file names to the Emme database names.
#
# Inputs:
#    omx_file: source 
#    demand_type: The type of demand in the provided OMX file, one of "AUTO", "TRUCK", "TRANSIT".
#        Used to determine the matrix mapping for the import.
#    period: The period for which to import the matrices, one of "EA", "AM", "MD", "PM", "EV"
#    scenario: traffic scenario to use for reference zone system
#    convert_truck_to_pce: boolean, if True the result matrices are adjusted to PCEs instead of 
#        vehicles (default, and required for traffic assignment). Only used if the demand_type is TRUCK.
#
# Matrix results:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    For AUTO:
#       pp_SOVGP, pp_SOVTOLL, pp_HOV2GP, pp_HOV2HOV, pp_HOV2TOLL, pp_HOV3GP, pp_HOV3HOV, pp_HOV3TOLL
#    For TRUCK:
#       pp_TRKHGP, pp_TRKHTOLL, pp_TRKLGP, pp_TRKLTOLL, pp_TRKMGP, pp_TRKMTOLL
#    For TRANSIT:
#       pp_WLKBUS, pp_WLKLRT, pp_WLKCMR, pp_WLKEXP, pp_WLKBRT, 
#       pp_PNRBUS, pp_PNRLRT, pp_PNRCMR, pp_PNREXP, pp_PNRBRT, 
#       pp_KNRBUS, pp_KNRLRT, pp_KNRCMR, pp_KNREXP, pp_KNRBRT
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    period = "AM"
    input_omx_file = os.path.join(main_directory, "input", "trip_%s.omx" % period)
    demand_type = "TRUCK"
    demand_as_pce = True
    base_scenario = modeller.scenario
    import_seed_demand = modeller.tool("sandag.import.import_seed_demand")
    import_seed_demand(input_omx_file, demand_type, period, demand_as_pce, base_scenario)
"""


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

    def __call__(self, omx_file, demand_type, period, scenario):
        attributes = {
            "omx_file": omx_file, 
            "demand_type": demand_type, 
            "period": period, 
            "scenario": scenario.id, 
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
                    '%s_SOVGPL':    'mf"%s_SOVGPL"',
                    '%s_SOVTOLLL':  'mf"%s_SOVTOLLL"',
                    '%s_HOV2HOVL':  'mf"%s_HOV2HOVL"',
                    '%s_HOV2TOLLL': 'mf"%s_HOV2TOLLL"',
                    '%s_HOV3HOVL':  'mf"%s_HOV3HOVL"',
                    '%s_HOV3TOLLL': 'mf"%s_HOV3TOLLL"',
                    '%s_SOVGPM':    'mf"%s_SOVGPM"',
                    '%s_SOVTOLLM':  'mf"%s_SOVTOLLM"',
                    '%s_HOV2HOVM':  'mf"%s_HOV2HOVM"',
                    '%s_HOV2TOLLM': 'mf"%s_HOV2TOLLM"',
                    '%s_HOV3HOVM':  'mf"%s_HOV3HOVM"',
                    '%s_HOV3TOLLM': 'mf"%s_HOV3TOLLM"',
                    '%s_SOVGPH':    'mf"%s_SOVGPH"',
                    '%s_SOVTOLLH':  'mf"%s_SOVTOLLH"',
                    '%s_HOV2HOVH':  'mf"%s_HOV2HOVH"',
                    '%s_HOV2TOLLH': 'mf"%s_HOV2TOLLH"',
                    '%s_HOV3HOVH':  'mf"%s_HOV3HOVH"',
                    '%s_HOV3TOLLH': 'mf"%s_HOV3TOLLH"'}
                matrices = dict((k % period, v % period) for k, v in matrices.iteritems())
                import_from_omx(file_path=omx_file, matrices=matrices, scenario=scenario)

            if demand_type == "TRUCK":
                matrices = {
                    '%s_TRKHGP':     'mf"%s_TRKHGP"',
                    '%s_TRKHTOLL':   'mf"%s_TRKHTOLL"',
                    '%s_TRKLGP':     'mf"%s_TRKLGP"',
                    '%s_TRKLTOLL':   'mf"%s_TRKLTOLL"',
                    '%s_TRKMGP':     'mf"%s_TRKMGP"',
                    '%s_TRKMTOLL':   'mf"%s_TRKMTOLL"'}
                matrices = dict((k % period, v % period) for k, v in matrices.iteritems())
                import_from_omx(file_path=omx_file, matrices=matrices, scenario=scenario)

            if demand_type == "TRANSIT":
                matrices = {
                    'SET1':  'mf"%s_WLKBUS"',
                    'SET2':  'mf"%s_WLKPREM"',
                    'SET3':  'mf"%s_WLKALLPEN"',}
                matrices = dict((k, v % period) for k, v in matrices.iteritems())
                # special custom mapping from subset of TAPs to all TAPs
                emme_zones = scenario.zone_numbers
                emmebank = scenario.emmebank
                omx_file_obj = _omx.openFile(omx_file, 'r')
                try:
                    zone_mapping = omx_file_obj.mapping(omx_file_obj.listMappings()[0]).items()
                    zone_mapping.sort(key=lambda x: x[1])
                    omx_zones = [x[0] for x in zone_mapping]
                    if omx_zones != emme_zones:
                        for omx_name, emme_name in matrices.iteritems():
                            omx_data = omx_file_obj[omx_name].read()
                            matrix_data = _matrix.MatrixData(type='f', indices=[omx_zones, omx_zones])
                            matrix_data.from_numpy(omx_data)
                            expanded_matrix_data = matrix_data.expand([emme_zones, emme_zones])
                            matrix = emmebank.matrix(emme_name)
                            matrix.set_data(expanded_matrix_data, scenario)
                    else:
                        for omx_name, emme_name in matrices.iteritems():
                            omx_data = omx_file_obj[omx_name].read()
                            matrix.set_numpy_data(omx_data, scenario)
                finally:
                    omx_file_obj.close()

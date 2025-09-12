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
#       pp_SOVNTP, pp_SOVTB, pp_HOV2, pp_HOV3
#    For TRUCK:
#       pp_TRKH, pp_TRKL, pp_TRKM
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


gen_utils = _m.Modeller().module("sandag.utilities.general")
_omx = _m.Modeller().module("sandag.utilities.omxwrapper")


class ImportMatrices(_m.Tool(), gen_utils.Snapshot):

    omx_file = _m.Attribute(str)
    demand_type = _m.Attribute(str)
    period = _m.Attribute(str)
    tool_run_msg = ""

    @_m.method(return_type=str)
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

            if demand_type == "AUTO":
                # TODO: update for new seed matrices
                matrices = {
                    '%s_SOV_NT_L':  'mf"%s_SOV_NT_L"',
                    '%s_SOV_TR_L':  'mf"%s_SOV_TR_L"',
                    '%s_HOV2_L':  'mf"%s_HOV2_L"',
                    '%s_HOV3_L':  'mf"%s_HOV3_L"',
                    '%s_SOV_NT_M':  'mf"%s_SOV_NT_M"',
                    '%s_SOV_TR_M':  'mf"%s_SOV_TR_M"',
                    '%s_HOV2_M':  'mf"%s_HOV2_M"',
                    '%s_HOV3_M':  'mf"%s_HOV3_M"',
                    '%s_SOV_NT_H':  'mf"%s_SOV_NT_H"',
                    '%s_SOV_TR_H':  'mf"%s_SOV_TR_H"',
                    '%s_HOV2_H':  'mf"%s_HOV2_H"',
                    '%s_HOV3_H':  'mf"%s_HOV3_H"'}
                matrices = dict((k % period, v % period) for k, v in matrices.items())
                self._import_from_omx(omx_file, matrices, scenario)

            if demand_type == "TRUCK":
                # TODO: update for new seed matrices
                matrices = {
                    '%s_TRK_H':     'mf"%s_TRK_H"',
                    '%s_TRK_L':     'mf"%s_TRK_L"',
                    '%s_TRK_M':   'mf"%s_TRK_M"'}
                matrices = dict((k % period, v % period) for k, v in matrices.items())
                self._import_from_omx(omx_file, matrices, scenario)

            if demand_type == "TRANSIT":
                matrices = {
                    'SET1':  'mf"%s_WLKBUS"',
                    'SET2':  'mf"%s_WLKPREM"',
                    'SET3':  'mf"%s_WLKALLPEN"',}
                matrices = dict((k, v % period) for k, v in matrices.items())
                # special custom mapping from subset of TAPs to all TAPs
                self._import_from_omx(omx_file, matrices, scenario)

    def _import_from_omx(self, file_path, matrices, scenario):
        matrices_to_write = {}
        emme_zones = scenario.zone_numbers
        emmebank = scenario.emmebank
        omx_file_obj = _omx.open_file(file_path, 'r')
        try:
            zone_mapping = list(omx_file_obj.mapping(omx_file_obj.list_mappings()[0]).items())
            zone_mapping.sort(key=lambda x: x[1])
            omx_zones = [x[0] for x in zone_mapping]
            for omx_name, emme_name in matrices.items():
                omx_data = omx_file_obj[omx_name].read()
                if emme_name not in matrices_to_write:
                    matrices_to_write[emme_name] = omx_data
                else:
                    # Allow multiple src matrices from OMX to sum to same matrix in Emme
                    matrices_to_write[emme_name] = omx_data + matrices_to_write[emme_name]
        except Exception as error:
            import traceback
            print(traceback.format_exc())
        omx_file_obj.close()

        if omx_zones != emme_zones:
            # special custom mapping from subset of TAPs to all TAPs
            for emme_name, omx_data in matrices_to_write.items():
                matrix_data = _matrix.MatrixData(type='f', indices=[omx_zones, omx_zones])
                matrix_data.from_numpy(omx_data)
                expanded_matrix_data = matrix_data.expand([emme_zones, emme_zones])
                matrix = emmebank.matrix(emme_name)
                matrix.set_data(expanded_matrix_data, scenario)
        else:
            for emme_name, omx_data in matrices_to_write.items():
                matrix = emmebank.matrix(emme_name)
                matrix.set_numpy_data(omx_data, scenario)

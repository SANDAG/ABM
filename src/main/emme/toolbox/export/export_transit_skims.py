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
#    periods: list of periods, using the standard two-character abbreviation
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
    export_transit_skims = modeller.tool("sandag.import.export_transit_skims")
    omx_file_path = os.path.join(output_dir, "transit_skims.omx"
    export_transit_skims(output_dir, period, scenario)
"""


TOOLBOX_ORDER = 72


import inro.modeller as _m
import traceback as _traceback
import os
import pandas as pd

gen_utils = _m.Modeller().module("sandag.utilities.general")
compute_matrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

class ExportSkims(_m.Tool(), gen_utils.Snapshot):
    omx_file = _m.Attribute(unicode)
    periods = _m.Attribute(unicode)
    big_to_zero = _m.Attribute(bool)

    tool_run_msg = ""

    def __init__(self):
        self.attributes = ["omx_file", "periods", "big_to_zero"]
        self.periods = "EA, AM, MD, PM, EV"
        self.num_processors = "MAX-1"
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
        pb.add_text_box('periods', title="Selected periods:")
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            periods = [x.strip() for x in self.periods.split(",")]
            self(self.omx_file, periods, scenario, self.big_to_zero)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export transit skims to OMX", save_arguments=True)
    def __call__(self, omx_file, periods, scenario, big_to_zero=True):
        attributes = {"omx_file": omx_file, "periods": periods, "big_to_zero": big_to_zero}
        gen_utils.log_snapshot("Export transit skims to OMX", str(self), attributes)
        init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
        matrices = init_matrices.get_matrix_names("transit_skims", periods, scenario)
        #TODO: This is a kludgy way to do this...
        """
        ['LOC_FIRSTWAIT', 'LOC_XFERWAIT', 'LOC_FARE', 'LOC_XFERS',  'LOC_ACC', 'LOC_XFERWALK', 'LOC_EGR', 'LOC_TOTALWALK',
                  'LOC_TOTALIVTT', 'LOC_DWELLTIME', 'LOC_BUSIVTT', 'PRM_FIRSTWAIT', 'PRM_XFERWAIT', 'PRM_FARE', 'PRM_XFERS', 'PRM_ACC',
                  'PRM_XFERWALK', 'PRM_EGR', 'PRM_TOTALWALK', 'PRM_TOTALIVTT', 'PRM_LRTIVTT', 'PRM_CMRIVTT', 'PRM_EXPIVTT', 
                  'PRM_LTDEXPIVTT', 'PRM_BRTIVTT', 'MIX_FIRSTWAIT', 'MIX_XFERWAIT', 'MIX_FARE', 'MIX_XFERS', 'MIX_ACC', 'MIX_XFERWALK',
                  'MIX_EGR', 'MIX_TOTALIVTT', 'MIX_BUSIVTT', 'MIX_LRTIVTT', 'MIX_CMRIVTT', 'MIX_EXPIVTT', 'MIX_LTDEXPIVTT', 'MIX_BRTIVTT']
        """
        mnames = ['LOC_FIRSTWAIT', 'LOC_XFERWAIT', 'LOC_FARE', 'LOC_XFERS',  'LOC_ACC', 'LOC_XFERWALK', 'LOC_EGR', 'LOC_TOTALWALK',
                  'LOC_TOTALIVTT', 'LOC_DWELLTIME', 'LOC_BUSIVTT', 'PRM_FIRSTWAIT', 'PRM_XFERWAIT', 'PRM_FARE', 'PRM_XFERS', 'PRM_ACC',
                  'PRM_XFERWALK', 'PRM_EGR', 'PRM_TOTALWALK', 'PRM_TOTALIVTT', 'PRM_LRTIVTT', 'PRM_CMRIVTT', 'PRM_EXPIVTT', 
                  'PRM_LTDEXPIVTT', 'PRM_BRTIVTT', 'MIX_FIRSTWAIT', 'MIX_XFERWAIT', 'MIX_FARE', 'MIX_XFERS', 'MIX_ACC', 'MIX_XFERWALK',
                  'MIX_EGR', 'MIX_TOTALIVTT', 'MIX_BUSIVTT', 'MIX_LRTIVTT', 'MIX_CMRIVTT', 'MIX_EXPIVTT', 'MIX_LTDEXPIVTT', 'MIX_BRTIVTT']
                  
        #pd.DataFrame({'id': matrices}).to_csv("C:\\ABM_runs\\Emme_Setup\\emme_project\\Scripts\\matrix_list.csv")
        matrices_to_export = []
        for name in matrices:
            m_chk_arr = name.split("_")
            m_check = "_".join(m_chk_arr[1:-2])
            if m_check in mnames:
                matrices_to_export.append(name)
                specification = {
                    "type": "MATRIX_CALCULATION",
                    "result": name,
                    "expression": "0",
                    "constraint": {
                        "by_zone": None,
                        "by_value": {
                        "od_values": name,
                        "interval_min": 0,
                        "interval_max": 1e6,
                        "condition": "EXCLUDE" }}
                }
                report = compute_matrix(specification) 
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME") as exporter:
            exporter.write_matrices(matrices_to_export)
        """
        with gen_utils.ExportOMX(omx_file, scenario, omx_key="NAME", big_to_zero = True) as exporter:
            if big_to_zero:
                emmebank = scenario.emmebank
                for name in matrices:
                    m_chk_arr = name.split("_")
                    m_check = "_".join(m_chk_arr[1:-2])
                    if m_check in mnames:
                        matrix = emmebank.matrix(name)
                        array = matrix.get_numpy_data(scenario)
                        array[array>10E6] = 0
                        array = array.round(2).astype('float32')
                        exporter.write_array(array, exporter.generate_key(matrix))
            else:
                emmebank = scenario.emmebank
                for name in matrices:
                    matrix = emmebank.matrix(name)
                    array = matrix.get_numpy_data(scenario)
                    array = array.astype('float32')
                    exporter.write_array(array, exporter.generate_key(matrix))
                    #exporter.write_matrices(matrices)
        """

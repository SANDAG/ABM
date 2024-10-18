#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export/export_for_commercial_vehicle.py                               ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Exports the required skims in CSV format for the commercial vehicle model.
# 
#
# Inputs:
#    source: 
#
# Files referenced:
#
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    source_dir = os.path.join(main_directory, "input")
    title = "Base 2012 scenario"
    tool = modeller.tool("sandag.export.export_for_commercial_vehicle")
"""


TOOLBOX_ORDER = 51


import inro.modeller as _m
import numpy as _np
import subprocess as _subprocess
import tempfile as _tempfile
import traceback as _traceback
import os

_join = os.path.join
_dir = os.path.dirname

gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExportForCommercialVehicleModel(_m.Tool(), gen_utils.Snapshot):

    output_directory = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.output_directory = _join(_dir(project_dir), "output")
        self.attributes = ["output_directory"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export for commercial vehicle model"
        pb.description = """
        Exports the required skims in CSV format for the commercial vehicle model.
        """
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('output_directory', 'directory',
                           title='Select output directory')

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.output_directory, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Export skims for commercial vehicle model', save_arguments=True)
    def __call__(self, output_directory, scenario):
        emmebank = scenario.emmebank
        modes = ['ldn', 'ldt', 'lhdn', 'lhdt', 'mhdn', 'mhdt', 'hhdn', 'hhdt']
        classes = ['SOV_NT_H', 'SOV_TR_H', 'TRK_L', 'TRK_L', 'TRK_M', 'TRK_M', 'TRK_H', 'TRK_H']
        # Mappings between COMMVEH modes and Emme classes
        mode_class = dict(list(zip(modes, classes)))
        class_mode = dict(list(zip(classes, modes)))
        
        is_toll_mode = lambda m: m.endswith('t')
        #periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        period = "MD"
        skims = ['TIME', 'DIST', 'TOLLCOST']
        DUCoef = [
            [-0.313, -0.138, -0.01],
            [-0.313, -0.492, -0.01],
            [-0.302, -0.580, -0.02]
        ]
        # Mappings for DUCoef utility index
        modes_util = {
            'ldn': 0,
            'ldt': 0,
            'lhdn': 1,
            'lhdt': 1,
            'mhdn': 1,
            'mhdt': 1,
            'hhdn': 2,
            'hhdt': 2
        }

        # Lookup relevant skims as numpy arrays
        skim_mat = {}
        for cls in classes:
            for skim in skims:
                name = '%s_%s__%s' % (cls, skim, period)
                if name not in skim_mat:
                    skim_mat[name] = emmebank.matrix(name).get_numpy_data(scenario)
                    
        output_matrices = {
            'impldt_MD_Time.txt': skim_mat['SOV_TR_H_TIME__MD'],
            'impldt_MD_Dist.txt': skim_mat['SOV_TR_H_DIST__MD'],
        }
        
        # Calculate DU matrices in numpy
        for mode in modes:
            time = skim_mat['%s_TIME__%s' % (mode_class[mode], period)]
            distance = skim_mat['%s_DIST__%s' % (mode_class[mode], period)]
            # All classes now have a tollcost skim available
            toll_cost = skim_mat['%s_TOLLCOST__%s' % (mode_class[mode], period)]
            _np.fill_diagonal(toll_cost, 0)

            coeffs = DUCoef[modes_util[mode]]
            disutil_mat = coeffs[0] * time + coeffs[1] * distance + coeffs[2] * toll_cost
            output_matrices['imp%s_%s_DU.txt' % (mode, period)] = disutil_mat

        # Insert row number into first column of the array
        # Note: assumes zone IDs are continuous
        for key, array in output_matrices.items():
            output_matrices[key] = _np.insert(array, 0, range(1, array.shape[0]+1), axis=1)

        # Output DU matrices to CSV
        # Print first column as integer, subsequent columns as floats rounded to 6 decimals
        fmt_spec = ['%i'] + ['%.6f'] * (disutil_mat.shape[0])
        # Save to separate files
        for name, array in output_matrices.items():
            _np.savetxt(_join(output_directory, name), array, fmt=fmt_spec, delimiter=',')

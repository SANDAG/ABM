#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export/export_data_loader_matrices.py                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 74


import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import traceback as _traceback
from collections import OrderedDict
import os
import warnings
import tables


warnings.filterwarnings('ignore', category=tables.NaturalNameWarning)
gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class ExportDataLoaderMatrices(_m.Tool(), gen_utils.Snapshot):

    output_dir = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    transit_scenario_id = _m.Attribute(int)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.output_dir = os.path.join(os.path.dirname(project_dir), "output")
        self.base_scenario_id = 100
        self.transit_scenario_id = 100
        self.num_processors = "MAX-1"
        self.attributes = ["main_directory", "base_scenario_id", "transit_scenario_id", "num_processors"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export matrices for Data Loader"
        pb.description = """
            Export model results to OMX files for export by Data Exporter 
            to CSV format for load in SQL Data loader."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('output_dir', 'directory',
                           title='Select output directory')

        pb.add_text_box('base_scenario_id', title="Base scenario ID:", size=10)
        pb.add_text_box('transit_scenario_id', title="Transit scenario ID:", size=10)

        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            base_emmebank = _eb.Emmebank(os.path.join(project_dir, "Database", "emmebank"))
            transit_emmebank = _eb.Emmebank(os.path.join(project_dir, "Database_transit", "emmebank"))
            base_scenario = base_emmebank.scenario(self.base_scenario_id)
            transit_scenario = transit_emmebank.scenario(self.transit_scenario_id)
            
            results = self(self.output_dir, base_scenario, transit_scenario, self.num_processors)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise
            
    @_m.logbook_trace("Export matrices for Data Loader", save_arguments=True)
    def __call__(self, output_dir, base_scenario, transit_scenario, num_processors):
        attrs = {
            "output_dir": output_dir,
            "base_scenario_id": base_scenario.id,
            "transit_scenario_id": transit_scenario.id,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export Matrices for Data Loader", str(self), attrs)
        num_processors = dem_utils.parse_num_processors(num_processors)
        matrix_calc = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        export_to_omx = gen_utils.export_to_omx
        periods = ["EA", "AM", "MD", "PM", "EV"]

        with _m.logbook_trace("Commercial vehicle demand"):
            omx_file = os.path.join(output_dir, "commVehTODTrips")
            base_map = [("Trips", "COMMVEH"), ("NonToll", "COMVEHGP"), ("Toll", "COMVEHTOLL")]
            matrices = OrderedDict()
            for period in periods:
                for key, name in base_map:
                    matrices[period + " " + key] = (period + "_" + name)
            matrices["OD Trips"] = "COMMVEH_TOTAL_DEMAND"
            export_to_omx(matrices, omx_file, base_scenario)

        with _m.logbook_trace("Export truck demand"):
            for period in periods:
                omx_file = os.path.join(output_dir, "Trip_" + period)
                base_map = [
                    ("lhdn", "TRKLGP"),
                    ("mhdn", "TRKMGP"),
                    ("hhdn", "TRKHGP"),
                    ("lhdt", "TRKLTOLL"),
                    ("mhdt", "TRKMTOLL"),
                    ("hhdt", "TRKHTOLL"),
                ]
                matrices = OrderedDict()
                for key, name in base_map:
                    matrices[key] = (period + "_" + name)
                export_to_omx(matrices, omx_file, base_scenario)

        with _m.logbook_trace("Export external-external demand"):
            omx_file = os.path.join(output_dir, "externalExternalTrips")
            matrices = {"Trips": "ALL_TOTAL_EETRIPS"}
            export_to_omx(matrices, omx_file, base_scenario)

        with _m.logbook_trace("Export external-internal demand"):
            for file_name, purpose in [("Wrk", "EIWORK"), ("Non", "EINONWORK")]:
                for period in periods:
                    omx_file = os.path.join(output_dir, "usSd" + file_name + "_" + period)
                    base_map = [
                        ("DAN", "SOVGP"),
                        ("DAT", "SOVTOLL"),
                        ("S2N", "HOV2HOV"),
                        ("S2T", "HOV2TOLL"),
                        ("S3N", "HOV3HOV"),
                        ("S3T", "HOV3TOLL"),
                    ]
                    matrices = OrderedDict()
                    for key, name in base_map:
                        matrices[key] = (period + "_" + name + "_" + purpose)
                    export_to_omx(matrices, omx_file, base_scenario)

        with _m.logbook_trace("Export transit skims"):
            base_map = [
                ("Walk Time", "BUS_TOTALWALK"),
                ("Total IV Time", ["BUS_TOTALIVTT", "BUS_DWELLTIME"]),
                ("Initial Wait Time", "BUS_FIRSTWAIT"),
                ("Transfer Wait Time", "BUS_XFERWAIT"),
                ("Fare", "BUS_FARE"),
                ("Number of Transfers", "BUS_XFERS"),
            ]
            transit_emmebank = transit_scenario.emmebank
            for period in periods:
                matrices = OrderedDict()
                for key, name in base_map:
                    if isinstance(name, list):
                        pname = [period + "_" + n for n in name]
                    else:
                        pname = period + "_" + name
                    matrices[key] = pname
                omx_file = os.path.join(output_dir, "implocl_" + period + "o")
                export_to_omx(matrices, omx_file, transit_scenario)

            base_map = [
                ("Walk Time", "ALL_TOTALWALK"),
                ("IVT:Sum", ["ALL_TOTALIVTT", "ALL_DWELLTIME"]),
                ("Initial Wait Time", "ALL_FIRSTWAIT"),
                ("Transfer Wait Time", "ALL_XFERWAIT"),
                ("Fare", "ALL_FARE"),
                ("Number of Transfers", "ALL_XFERS"),
                ("Length:LB", "ALL_BUSDIST"),
                ("Length:LR", "ALL_LRTDIST"),
                ("Length:CR", "ALL_CMRDIST"),
                ("Length:EXP", "ALL_EXPDIST"),
                ("Length:BRT", "ALL_BRTDIST"),
                ("IVT:LB", "ALL_BUSIVTT"),
                ("IVT:LR", "ALL_LRTIVTT"),
                ("IVT:CR", "ALL_CMRIVTT"),
                ("IVT:EXP", ["ALL_EXPIVTT", "ALL_LTDEXPIVTT"]),
                ("IVT:BRT", ["ALL_BRTREDIVTT", "ALL_BRTYELIVTT"]),
                ("Main Mode", "ALL_MAINMODE"),
            ]

            for period in periods:
                dwell_mats = ["BUSIVTT", "BRTREDIVTT", "BRTYELIVTT", "EXPIVTT", "LTDEXPIVTT"]
                dwell_mats = [period + "_ALL_" + m for m in dwell_mats]
                backups = dict((m, transit_emmebank.matrix(m).get_data(transit_scenario)) for m in dwell_mats)
                try:
                    with _m.logbook_trace("Allocate dwell time"):
                        # Note: this is an approximation, the true dwell time could be skimmed directly
                        with gen_utils.temp_matrices(transit_emmebank, "FULL", 1) as matrices:
                            matrices[0].name = "TEMP_SUM_IVTT"
                            spec = {
                                "type": "MATRIX_CALCULATION", "constraint": None,
                                "result": 'mf"TEMP_SUM_IVTT"', 
                                "expression": " + ".join(dwell_mats),
                            }
                            matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                            for mat in dwell_mats:
                                spec = {
                                    "type": "MATRIX_CALCULATION", "constraint": None,
                                    "result": 'mf"%s"' % mat, 
                                    "expression": "{0} * (1.0 + {1}_ALL_DWELLTIME / TEMP_SUM_IVTT)".format(mat, period),
                                }
                                matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                    with _m.logbook_trace("Assign main mode"):
                        # Assign a main mode of travel by IVTT
                        # if TOTAL == 0 or TOTAL > 999999: 0 ( -1 used as initial flag and replaced with 0 at end)
                        # elif BUS > TOTAL * 0.5: 8
                        # elif EXP > any other one mode CR, LRT, BRT, BUS: 7
                        # elif LRT > CMR : 5
                        # else: 4
                        name = period + "_ALL"
                        spec = {
                            "type": "MATRIX_CALCULATION", "constraint": None,
                            "result": 'mf"%s_MAINMODE"' % name, 
                            "expression": '-1 * ( ({0}_TOTALIVTT <= 0 ) .or. ({0}_TOTALIVTT > 999999) )'.format(name),
                        }
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        spec["constraint"] = {
                            "by_value": {
                                "od_values": 'mf"%s_MAINMODE"' % name, 
                                "interval_min": 0, "interval_max": 0,  "condition": "INCLUDE"},
                        }
                        spec["expression"] = '8 * ({0}_BUSIVTT > (0.5 * {0}_TOTALIVTT) )'.format(name)
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        spec["expression"] = ('7 * (({0}_EXPIVTT + {0}_LTDEXPIVTT) > '
                            '({0}_LRTIVTT .max. {0}_CMRIVTT .max. {0}_BUSIVTT .max. ({0}_BRTREDIVTT + {0}_BRTREDIVTT) ) )').format(name)
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        spec["expression"] = '5 * ({0}_LRTIVTT > {0}_CMRIVTT )'.format(name)
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        spec["expression"] = '4'
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        spec = {
                            "type": "MATRIX_CALCULATION", 
                            "constraint": {
                                "by_value": {
                                    "od_values": 'mf"%s_MAINMODE"' % name, 
                                    "interval_min": -1, "interval_max": -1,  "condition": "INCLUDE"},
                            },
                            "result": 'mf"%s_MAINMODE"' % name, 
                            "expression": '0',
                        }
                        matrix_calc(spec, scenario=transit_scenario, num_processors=num_processors)
                        
                    matrices = OrderedDict()
                    for key, name in base_map:
                        if isinstance(name, list):
                            pname = [period + "_" + n for n in name]
                        else:
                            pname = period + "_" + name
                        matrices[key] = pname
                    omx_file = os.path.join(output_dir, "impprem_" + period + "o")
                    export_to_omx(matrices, omx_file, transit_scenario)
                finally:
                    for name, data in backups.iteritems():
                        transit_emmebank.matrix(name).set_data(data, transit_scenario)
                        
        with _m.logbook_trace("Export traffic skims"):
            emmebank = base_scenario.emmebank
            base_mapping = [
                ("dan", "SOVGP", [
                    ("*SCST_{0}",           "GENCOST"),
                    ("*STM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST")
                ]),
                ("dat", "SOVTOLL", [
                    ("*SCST_{0}",           "GENCOST"),
                    ("*STM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                    ("dat_{0} - itoll_{0}", "TOLLCOST"),
                ]),
                ("s2nh", "HOV2HOV", [
                    ("*H2CST_{0}",          "GENCOST"),
                    ("*HTM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                ]),
                ("s2th", "HOV2TOLL", [
                    ("*H2CST_{0}",          "GENCOST"),
                    ("*HTM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                    ("s2t_{0} - itoll_{0}", "TOLLCOST"),
                ]),
                ("s3nh", "HOV3HOV", [
                    ("*H3CST_{0}",          "GENCOST"),
                    ("*HTM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                ]),
                ("s3th", "HOV3TOLL", [
                    ("*H3CST_{0}",          "GENCOST"),
                    ("*HTM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                    ("s3t_{0} - itoll_{0}", "TOLLCOST"),
                ]),
                ("hhdn", "TRKHGP", [
                    ("*SCST_{0}",           "GENCOST"),
                    ("*STM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST")
                ]),
                ("hhdt", "TRKHTOLL", [
                    ("*SCST_{0}",           "GENCOST"),
                    ("*STM_{0} (Skim)",     "TIME"),
                    ("Length (Skim)",       "DIST"),
                    ("hhdt - ITOLL2_{0}",   "TOLLCOST"),
                ]),
            ]
            for period in periods:
                for file_name, mode, mat_mapping in base_mapping:
                    omx_file = os.path.join(output_dir, "imp" + file_name + "_" + period)
                    matrices = OrderedDict()
                    for key, skim in mat_mapping:
                        if "{0}" in key:
                            key = key.format(period)
                        matrices[key] = (period + "_" + mode + "_" + skim)
                    export_to_omx(matrices, omx_file, base_scenario)

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
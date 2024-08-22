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
#
# Exports the matrix results to OMX and csv files for use by the Java Data
# export process and the Data loader to the reporting database.
#
#
# Inputs:
#    output_dir: the output directory for the created files
#    base_scenario_id: scenario ID for the base scenario (same used in the Import network tool)
#    transit_scenario_id: scenario ID for the base transit scenario
#
# Files created:
#   CSV format files
#       ../report/trucktrip.csv
#       ../report/eetrip.csv
#       ../report/eitrip.csv
#   OMX format files
#        trip_pp.omx
#
#
# Script example:
"""
    import os
    import inro.emme.database.emmebank as _eb
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    main_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    transit_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    output_dir = os.path.join(main_directory, "output")
    num_processors = "MAX-1"
    export_data_loader_matrices = modeller.tool(
        "sandag.export.export_data_loader_matrices")
    export_data_loader_matrices(output_dir, 100, main_emmebank, transit_emmebank, num_processors)
"""
TOOLBOX_ORDER = 74


import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import traceback as _traceback
from collections import OrderedDict
import os
import numpy
import warnings
import tables


warnings.filterwarnings('ignore', category=tables.NaturalNameWarning)
gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

_join = os.path.join
_dir = os.path.dirname

class ExportDataLoaderMatrices(_m.Tool(), gen_utils.Snapshot):

    output_dir = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.output_dir = os.path.join(os.path.dirname(project_dir), "output")
        self.base_scenario_id = 100
        self.periods = ["EA", "AM", "MD", "PM", "EV"]
        self.attributes = ["main_directory", "base_scenario_id"]

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
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            base_emmebank = _eb.Emmebank(os.path.join(project_dir, "Database", "emmebank"))
            base_scenario = base_emmebank.scenario(self.base_scenario_id)

            results = self(self.output_dir, base_scenario)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export matrices for Data Loader", save_arguments=True)
    def __call__(self, output_dir, base_scenario):
        attrs = {
            "output_dir": output_dir,
            "base_scenario_id": base_scenario.id,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export Matrices for Data Loader", str(self), attrs)
        self.output_dir = output_dir
        self.base_scenario = base_scenario
        self.external_demand()
        self.total_demand()


    def external_demand(self):
        #get auto operating cost
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(_dir(self.output_dir), "conf", "sandag_abm.properties"))
        try:
            aoc = float(props["aoc.fuel"]) + float(props["aoc.maintenance"])
        except ValueError:
            raise Exception("Error during float conversion for aoc.fuel or aoc.maintenance from sandag_abm.properties file")

        # EXTERNAL-EXTERNAL TRIP TABLE (toll-eligible)
        name_mapping = [
            ("DA", "SOV"),
            ("S2", "HOV2"),
            ("S3", "HOV3"),
        ]
        scenario = self.base_scenario
        emmebank = scenario.emmebank
        zones = scenario.zone_numbers
        formater = lambda x: ("%.5f" % x).rstrip('0').rstrip(".")
        ee_trip_path = os.path.join(os.path.dirname(self.output_dir), "report", "eetrip.csv")
        with _m.logbook_trace("Export external-external demand"):
            with open(ee_trip_path, 'w', newline='') as f:
                f.write("OTAZ,DTAZ,TOD,MODE,TRIPS,TIME,DIST,AOC,TOLLCOST\n")
                for period in self.periods:
                    matrix_data_time = emmebank.matrix("SOV_NT_M_TIME__" + period).get_data(scenario)
                    matrix_data_dist = emmebank.matrix("SOV_NT_M_DIST__" + period).get_data(scenario)
                    matrix_data_tollcost = emmebank.matrix("SOV_NT_M_TOLLCOST__" + period).get_data(scenario)
                    for key, name in name_mapping:
                        matrix_data = emmebank.matrix(period + "_" + name + "_EETRIPS").get_data(scenario)
                        rounded_demand = 0
                        for orig in zones:
                            for dest in zones:
                                value = matrix_data.get(orig, dest)
                                # skip trips less than 0.00001 to avoid 0 trips records in database
                                if value < 0.00001:
                                    rounded_demand += value
                                    continue
                                time = matrix_data_time.get(orig, dest)
                                distance = matrix_data_dist.get(orig, dest)
                                tollcost = 0
                                tollcost = matrix_data_tollcost.get(orig, dest)
                                od_aoc = distance * aoc
                                f.write(",".join(
                                    [str(orig), str(dest), period, key, formater(value), formater(time),
                                     formater(distance), formater(od_aoc), formater(tollcost)]))
                                f.write("\n")
                        if rounded_demand > 0:
                            print (period + "_" + name + "_EETRIPS", "rounded_demand", rounded_demand)

        # EXTERNAL-INTERNAL TRIP TABLE
        name_mapping = [
                ("DAN", "SOVGP"),
                ("DAT", "SOVTOLL"),
                ("S2N", "HOV2HOV"),
                ("S2T", "HOV2TOLL"),
                ("S3N", "HOV3HOV"),
                ("S3T", "HOV3TOLL"),
        ]
        ei_trip_path = os.path.join(os.path.dirname(self.output_dir), "report", "eitrip.csv")

        with _m.logbook_trace("Export external-internal demand"):
            with open(ei_trip_path, 'w', newline='') as f:
                f.write("OTAZ,DTAZ,TOD,MODE,PURPOSE,TRIPS,TIME,DIST,AOC,TOLLCOST\n")
                for period in self.periods:
                    matrix_data_time = emmebank.matrix("SOV_TR_M_TIME__" + period).get_data(scenario)
                    matrix_data_dist = emmebank.matrix("SOV_TR_M_DIST__" + period).get_data(scenario)
                    if "TOLL" in name:
                        matrix_data_tollcost = emmebank.matrix("SOV_NT_M_TOLLCOST__" + period).get_data(scenario)
                    for purpose in ["WORK", "NONWORK"]:
                        for key, name in name_mapping:
                            matrix_data = emmebank.matrix(period + "_" + name + "_EI" + purpose).get_data(scenario)
                            rounded_demand = 0
                            for orig in zones:
                                for dest in zones:
                                    value = matrix_data.get(orig, dest)
                                    # skip trips less than 0.00001 to avoid 0 trips records in database
                                    if value < 0.00001:
                                        rounded_demand += value
                                        continue
                                    time = matrix_data_time.get(orig, dest)
                                    distance = matrix_data_dist.get(orig, dest)
                                    tollcost = 0
                                    if "TOLL" in name:
                                        tollcost = matrix_data_tollcost.get(orig, dest)
                                    od_aoc = distance * aoc
                                    f.write(",".join(
                                        [str(orig), str(dest), period, key, purpose, formater(value), formater(time),
                                         formater(distance), formater(od_aoc), formater(tollcost)]))
                                    f.write("\n")
                            if rounded_demand > 0:
                                print (period + "_" + name + "_EI" + purpose, "rounded_demand", rounded_demand)

    @_m.logbook_trace("Export total auto and truck demand to OMX")
    def total_demand(self):
        for period in self.periods:
            matrices = {
                "%s_SOV_NT_L":  'mf"%s_SOV_NT_L"',
                "%s_SOV_TR_L":  'mf"%s_SOV_TR_L"',
                "%s_HOV2_L":    'mf"%s_HOV2_L"',
                "%s_HOV3_L":    'mf"%s_HOV3_L"',
                "%s_SOV_NT_M":  'mf"%s_SOV_NT_M"',
                "%s_SOV_TR_M":  'mf"%s_SOV_TR_M"',
                "%s_HOV2_M":    'mf"%s_HOV2_M"',
                "%s_HOV3_M":    'mf"%s_HOV3_M"',
                "%s_SOV_NT_H":  'mf"%s_SOV_NT_H"',
                "%s_SOV_TR_H":  'mf"%s_SOV_TR_H"',
                "%s_HOV2_H":    'mf"%s_HOV2_H"',
                "%s_HOV3_H":    'mf"%s_HOV3_H"',
                "%s_TRK_H":     'mf"%s_TRK_H"',
                "%s_TRK_L":     'mf"%s_TRK_L"',
                "%s_TRK_M":     'mf"%s_TRK_M"',
            }
            matrices = dict((k % period, v % period) for k, v in matrices.items())
            omx_file = os.path.join(self.output_dir, "trip_%s.omx" % period)
            with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
                exporter.write_matrices(matrices)

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

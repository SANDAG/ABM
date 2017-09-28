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
import numpy
import warnings
import tables


warnings.filterwarnings('ignore', category=tables.NaturalNameWarning)
gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class ExportDataLoaderMatrices(_m.Tool(), gen_utils.Snapshot):

    output_dir = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    transit_scenario_id = _m.Attribute(int)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.output_dir = os.path.join(os.path.dirname(project_dir), "output")
        self.base_scenario_id = 100
        self.transit_scenario_id = 100
        self.periods = ["EA", "AM", "MD", "PM", "EV"]
        self.attributes = ["main_directory", "base_scenario_id", "transit_scenario_id"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export matrices for Data Loader"
        pb.description = """
            Export model results to OMX files for use by the Data Exporter 
            to generate CSV format to be loaded in SQL Data loader."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('output_dir', 'directory',
                           title='Select output directory')

        pb.add_text_box('base_scenario_id', title="Base scenario ID:", size=10)
        pb.add_text_box('transit_scenario_id', title="Transit scenario ID:", size=10)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            base_emmebank = _eb.Emmebank(os.path.join(project_dir, "Database", "emmebank"))
            transit_emmebank = _eb.Emmebank(os.path.join(project_dir, "Database_transit", "emmebank"))
            base_scenario = base_emmebank.scenario(self.base_scenario_id)
            transit_scenario = transit_emmebank.scenario(self.transit_scenario_id)
            
            results = self(self.output_dir, base_scenario, transit_scenario)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise
            
    @_m.logbook_trace("Export matrices for Data Loader", save_arguments=True)
    def __call__(self, output_dir, base_scenario, transit_scenario):
        attrs = {
            "output_dir": output_dir,
            "base_scenario_id": base_scenario.id,
            "transit_scenario_id": transit_scenario.id,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export Matrices for Data Loader", str(self), attrs)
        self.output_dir = output_dir
        self.base_scenario = base_scenario
        self.transit_scenario = transit_scenario

        self.commercial_vehicle_demand()
        self.truck_demand()
        self.external_demand()
        self.transit_skims()
        self.traffic_skims()

    @_m.logbook_trace("Export commercial vehicle demand")
    def commercial_vehicle_demand(self):
        omx_file = os.path.join(self.output_dir, "commVehTODTrips")
        name_mapping = [("Trips", "COMMVEH"), ("NonToll", "COMVEHGP"), ("Toll", "COMVEHTOLL")]
        matrices = OrderedDict()
        for period in self.periods:
            for key, name in name_mapping:
                matrices[period + " " + key] = (period + "_" + name)
        matrices["OD Trips"] = "COMMVEH_TOTAL_DEMAND"
        with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
            exporter.write_matrices(matrices)

    @_m.logbook_trace("Export truck demand")
    def truck_demand(self):
        name_mapping = [
            ("lhdn", "TRKLGP"),
            ("mhdn", "TRKMGP"),
            ("hhdn", "TRKHGP"),
            ("lhdt", "TRKLTOLL"),
            ("mhdt", "TRKMTOLL"),
            ("hhdt", "TRKHTOLL"),
        ]
        for period in self.periods:
            omx_file = os.path.join(self.output_dir, "Trip_" + period)
            matrices = OrderedDict()
            for key, name in name_mapping:
                matrices[key] = (period + "_" + name)
            with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
                exporter.write_matrices(matrices)

        scenario = self.base_scenario
        emmebank = scenario.emmebank
        zones = scenario.zone_numbers
        formater = lambda x: ("%.7f" % x).rstrip('0').rstrip(".")
        truck_trip_path = os.path.join(os.path.dirname(self.output_dir), "report", "trucktrip.csv")
        with open(truck_trip_path, 'w') as f:
            f.write("ORIG,DEST,TOD,CLASS,TRIPS\n")
            for period in self.periods:
                for key, name in name_mapping:
                    matrix_name = (period + "_" + name)
                    matrix_data = emmebank.matrix(matrix_name).get_data(scenario)
                    for orig in zones:
                        for dest in zones:
                            value = matrix_data.get(orig, dest)
                            if value == 0: 
                                continue
                            f.write(",".join([str(orig), str(dest), period, key, formater(value)]))
                            f.write("\n")

    def external_demand(self):
        with _m.logbook_trace("Export external-external demand"):
            omx_file = os.path.join(self.output_dir, "externalExternalTrips")
            with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
                exporter.write_matrices({"Trips": "ALL_TOTAL_EETRIPS"})
        with _m.logbook_trace("Export external-internal demand"):
            name_mapping = [
                ("DAN", "SOVGP"),
                ("DAT", "SOVTOLL"),
                ("S2N", "HOV2HOV"),
                ("S2T", "HOV2TOLL"),
                ("S3N", "HOV3HOV"),
                ("S3T", "HOV3TOLL"),
            ]
            for file_name, purpose in [("Wrk", "EIWORK"), ("Non", "EINONWORK")]:
                for period in self.periods:
                    omx_file = os.path.join(self.output_dir, "usSd" + file_name + "_" + period)
                    matrices = OrderedDict()
                    for key, name in name_mapping:
                        matrices[key] = (period + "_" + name + "_" + purpose)
                    with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
                        exporter.write_matrices(matrices)

    @_m.logbook_trace("Export transit skims")
    def transit_skims(self):
        name_mapping = [
            ("Walk Time", "_BUS_TOTALWALK"),
            ("Initial Wait Time", "_BUS_FIRSTWAIT"),
            ("Transfer Wait Time", "_BUS_XFERWAIT"),
            ("Fare", "_BUS_FARE"),
            ("Number of Transfers", "_BUS_XFERS"),
        ]
        transit_emmebank = self.transit_scenario.emmebank
        for period in self.periods:
            get_numpy_data = lambda name: transit_emmebank.matrix(period + name).get_numpy_data(self.transit_scenario)
            omx_file = os.path.join(self.output_dir, "implocl_" + period + "o")
            with gen_utils.ExportOMX(omx_file, self.transit_scenario) as exporter:
                # cap  matrices to 999.99 - TODO: double check which matrices are required
                for key, matrix_name in name_mapping:
                    numpy_array = get_numpy_data(matrix_name)
                    exporter.write_clipped_array(numpy_array, key, 0, 999.99)
                ivtt = get_numpy_data("_BUS_TOTALIVTT")
                dwell = get_numpy_data("_BUS_DWELLTIME")
                exporter.write_clipped_array(ivtt+dwell, "Total IV Time", 0, 999.99)

        name_mapping = [
            ("Walk Time", "_ALL_TOTALWALK"),
            ("Initial Wait Time", "_ALL_FIRSTWAIT"),
            ("Transfer Wait Time", "_ALL_XFERWAIT"),
            ("Fare", "_ALL_FARE"),
            ("Number of Transfers", "_ALL_XFERS"),
            ("Length:LB", "_ALL_BUSDIST"),
            ("Length:LR", "_ALL_LRTDIST"),
            ("Length:CR", "_ALL_CMRDIST"),
            ("Length:EXP", "_ALL_EXPDIST"),
            ("Length:BRT", "_ALL_BRTDIST"),
        ]
        for period in self.periods:
            get_numpy_data = lambda name: transit_emmebank.matrix(period + name).get_numpy_data(self.transit_scenario)

            bus_ivtt = get_numpy_data("_ALL_BUSIVTT")
            brtred_ivtt = get_numpy_data("_ALL_BRTREDIVTT")
            brtyel_ivtt = get_numpy_data("_ALL_BRTYELIVTT")
            exp_ivtt = get_numpy_data("_ALL_EXPIVTT")
            ltdexp_ivtt = get_numpy_data("_ALL_LTDEXPIVTT")
            lrt_ivtt = get_numpy_data("_ALL_LRTIVTT")
            cmr_ivtt = get_numpy_data("_ALL_CMRIVTT")
            dwell_time = get_numpy_data("_ALL_DWELLTIME")
            total_ivtt = get_numpy_data("_ALL_TOTALIVTT")

            # Note: this is an approximation, the true dwell time could be skimmed directly                
            sum_ivtt_dwell_modes = bus_ivtt + brtred_ivtt + brtyel_ivtt + exp_ivtt + ltdexp_ivtt
            bus_ivtt = bus_ivtt * (1.0 + dwell_time / sum_ivtt_dwell_modes)
            brtred_ivtt = brtred_ivtt * (1.0 + dwell_time / sum_ivtt_dwell_modes)
            brtyel_ivtt = brtyel_ivtt * (1.0 + dwell_time / sum_ivtt_dwell_modes)
            brt_ivtt = brtred_ivtt + brtyel_ivtt
            exp_ivtt = exp_ivtt * (1.0 + dwell_time / sum_ivtt_dwell_modes)
            ltdexp_ivtt = ltdexp_ivtt * (1.0 + dwell_time / sum_ivtt_dwell_modes)
            prem_bus_ivtt = exp_ivtt + ltdexp_ivtt
            sum_ivtt = total_ivtt + dwell_time

            # Assign main mode of travel by IVTT
            # main_mode_matrix = transit_emmebank.matrix(period + "_ALL_MAINMODE")
            # main_mode_matrix.initialize(0)
            # main_mode = get_numpy_data("_ALL_MAINMODE")
            # max_non_exp_time = lrt_ivtt
            # for a2 in [cmr_ivtt, bus_ivtt, brtred_ivtt, brtyel_ivtt]:
                # max_non_exp_time = numpy.maximum(max_non_exp_time, a2)
            # main_mode = numpy.where((main_mode==0) & ((bus_ivtt>0.5*total_ivtt) ), 8, main_mode)
            # main_mode = numpy.where((main_mode==0) & ((exp_ivtt + ltdexp_ivtt)>max_non_exp_time), 7, main_mode)
            # main_mode = numpy.where((main_mode==0) & (lrt_ivtt>cmr_ivtt), 5, main_mode)
            # main_mode = numpy.where((main_mode==0), 4, main_mode)
            # main_mode = main_mode * ((total_ivtt>=0) & (total_ivtt<1000))
            # main_mode_matrix.set_numpy_data(main_mode, self.transit_scenario)

            omx_file = os.path.join(self.output_dir, "impprem_" + period + "o")
            with gen_utils.ExportOMX(omx_file, self.transit_scenario) as exporter:
                # cap  matrices to 999.99 - TODO: double check which matrices are required
                for key, matrix_name in name_mapping:
                    numpy_array = get_numpy_data(matrix_name)
                    exporter.write_clipped_array(numpy_array, key, 0, 999.99)
                exporter.write_clipped_array(sum_ivtt, "IVT:Sum", 0, 999.99)
                exporter.write_clipped_array(bus_ivtt, "IVT:LB", 0, 999.99)
                exporter.write_clipped_array(lrt_ivtt, "IVT:LR", 0, 999.99)
                exporter.write_clipped_array(cmr_ivtt, "IVT:CR", 0, 999.99)
                exporter.write_clipped_array(prem_bus_ivtt, "IVT:EXP", 0, 999.99)
                exporter.write_clipped_array(brt_ivtt, "IVT:BRT", 0, 999.99)
                # exporter.write_array(main_mode, "Main Mode")
                        
    @_m.logbook_trace("Export traffic skims")
    def traffic_skims(self):
        emmebank = self.base_scenario.emmebank
        get_numpy_data = lambda name: emmebank.matrix(name).get_numpy_data(self.base_scenario)
        name_mapping = [
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
        for period in self.periods:
            for file_name, mode, mat_mapping in name_mapping:
                omx_file = os.path.join(self.output_dir, "imp" + file_name + "_" + period)
                matrices = OrderedDict()
                for key, skim in mat_mapping:
                    if "{0}" in key:
                        key = key.format(period)
                    matrices[key] = (period + "_" + mode + "_" + skim)
                with gen_utils.ExportOMX(omx_file, self.base_scenario) as exporter:
                    # filter out diagonal -99999999.0 values from GENCOST and TOLLCOST
                    for key, matrix in matrices.iteritems():
                        numpy_array = get_numpy_data(matrix)
                        exporter.write_clipped_array(numpy_array, key, 0)

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
        
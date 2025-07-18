# //////////////////////////////////////////////////////////////////////////////
# ////                                                                       ///
# //// Copyright INRO, 2016-2017.                                            ///
# //// Rights to use and modify are granted to the                           ///
# //// San Diego Association of Governments and partner agencies.            ///
# //// This copyright notice must be preserved.                              ///
# ////                                                                       ///
# //// model/master_run.py                                                   ///
# ////                                                                       ///
# ////                                                                       ///
# ////                                                                       ///
# ////                                                                       ///
# //////////////////////////////////////////////////////////////////////////////
#
# The Master run tool is the primary method to operate the SANDAG
# travel demand model. It operates all the model components.
#
#   main_directory: Main ABM directory: directory which contains all of the
#       ABM scenario data, including this project. The default is the parent
#       directory of the current Emme project.
#   scenario_id: Scenario ID for the base imported network data. The result
#       scenarios are indexed in the next five scenarios by time period.
#    scenario_title: title to use for the scenario.
#    emmebank_title: title to use for the Emmebank (Emme database)
#    num_processors: the number of processors to use for traffic and transit
#       assignments and skims, aggregate demand models (where required) and
#       other parallelized procedures in Emme. Default is Max available - 1.
#    Properties loaded from conf/sandag_abm.properties:
#       When using the tool UI, the sandag_abm.properties file is read
#       and the values cached and the inputs below are pre-set. When the tool
#       is started button is clicked this file is written out with the
#       values specified.
#           Sample rate by iteration: three values for the sample rates for each iteration
#           Start from iteration: iteration from which to start the model run
#           Skip steps: optional checkboxes to skip model steps.
#               Note that most steps are dependent upon the results of the previous steps.
#   Select link: add select link analyses for traffic.
#       See the Select link analysis section under the Traffic assignment tool.
#
#   Also reads and processes the per-scenario
#    vehicle_class_availability.csv (optional): 0 or 1 indicators by vehicle class and specified facilities to indicate availability
#
# Script example:
"""
import inro.modeller as _m
import os
modeller = _m.Modeller()
desktop = modeller.desktop

master_run = modeller.tool("sandag.master_run")
main_directory = os.path.dirname(os.path.dirname(desktop.project_path()))
scenario_id = 100
scenario_title = "Base 2015 scenario"
emmebank_title = "Base 2015 with updated landuse"
num_processors = "MAX-1"
master_run(main_directory, scenario_id, scenario_title, emmebank_title, num_processors)
"""

TOOLBOX_ORDER = 1
VIRUTALENV_PATH = "C:\\python_virtualenv\\abm15_4_0"

import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import inro.emme.desktop.app as _app

import traceback as _traceback
import glob as _glob
import subprocess as _subprocess
import ctypes as _ctypes
import json as _json
import importlib
import shutil as _shutil
import tempfile as _tempfile
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict
import time as _time
import socket as _socket
import sys
import os
import uuid
import yaml

import pandas as pd
import numpy as np
import csv
import datetime
import pyodbc
import win32com.client as win32
import shutil

import multiprocessing
import signal

_join = os.path.join
_dir = os.path.dirname
_norm = os.path.normpath

gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")
props_utils = _m.Modeller().module("sandag.utilities.properties")


class MasterRun(props_utils.PropertiesSetter, _m.Tool(), gen_utils.Snapshot):
    main_directory = _m.Attribute(str)
    scenario_id = _m.Attribute(int)
    scenario_title = _m.Attribute(str)
    emmebank_title = _m.Attribute(str)
    num_processors = _m.Attribute(str)
    select_link = _m.Attribute(str)

    properties_path = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        super(MasterRun, self).__init__()
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.main_directory = _dir(project_dir)
        self.properties_path = _join(_dir(project_dir), "conf", "sandag_abm.properties")
        self.scenario_id = 100
        self.scenario_title = ""
        self.emmebank_title = ""
        self.num_processors = "MAX-1"
        self.select_link = '[]'
        self.username = os.environ.get("USERNAME")
        self.attributes = [
            "main_directory", "scenario_id", "scenario_title", "emmebank_title",
            "num_processors", "select_link"
        ]
        self._log_level = "ENABLED"
        self.LOCAL_ROOT = "C:\\abm_runs"

    def page(self):
        self.load_properties()
        pb = _m.ToolPageBuilder(self)
        pb.title = "Master run ABM"
        pb.description = """Runs the SANDAG ABM, assignments, and other demand model tools."""
        pb.branding_text = "- SANDAG - Model"
        tool_proxy_tag = pb.tool_proxy_tag

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory', 'directory',
                           title='Select main ABM directory', note='')
        pb.add_text_box('scenario_id', title="Scenario ID:")
        pb.add_text_box('scenario_title', title="Scenario title:", size=80)
        pb.add_text_box('emmebank_title', title="Emmebank title:", size=60)
        dem_utils.add_select_processors("num_processors", pb, self)

        # defined in properties utilities
        self.add_properties_interface(pb, disclosure=True)
        # redirect properties file after browse of main_directory
        pb.add_html("""
<script>
    $(document).ready( function ()
    {
        var tool = new inro.modeller.util.Proxy(%(tool_proxy_tag)s) ;
        $("#main_directory").bind('change', function()    {
            var path = $(this).val();
            tool.properties_path = path + "/conf/sandag_abm.properties";
            tool.load_properties();
            $("input:checkbox").each(function() {
                $(this).prop('checked', tool.get_value($(this).prop('id')) );
            });
            $("#startFromIteration").prop('value', tool.startFromIteration);
            $("#sample_rates").prop('value', tool.sample_rates);
            $("#env").prop('value', tool.env);
        });
   });
</script>""" % {"tool_proxy_tag": tool_proxy_tag})

        traffic_assign = _m.Modeller().tool("sandag.assignment.traffic_assignment")
        traffic_assign._add_select_link_interface(pb)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self.save_properties()
            self(self.main_directory, self.scenario_id, self.scenario_title, self.emmebank_title,
                 self.num_processors, self.select_link, username=self.username)
            run_msg = "Model run complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(error, _traceback.format_exc())

            raise

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    @_m.logbook_trace("Master run model", save_arguments=True)
    def __call__(self, main_directory, scenario_id, scenario_title, emmebank_title, num_processors,
                 select_link=None, periods=["EA", "AM", "MD", "PM", "EV"], username=None):
        attributes = {
            "main_directory": main_directory,
            "scenario_id": scenario_id,
            "scenario_title": scenario_title,
            "emmebank_title": emmebank_title,
            "num_processors": num_processors,
            "select_link": select_link,
            "periods": periods,
            "username": username,
        }
        gen_utils.log_snapshot("Master run model", str(self), attributes)

        modeller = _m.Modeller()
        # Checking that the virtualenv path is set and the folder is installed
        if not os.path.exists(VIRUTALENV_PATH):
            raise Exception("Python virtual environment not installed at expected location %s" % VIRUTALENV_PATH)
        venv_path = os.environ.get("PYTHON_VIRTUALENV")
        if not venv_path:
            raise Exception("Environment variable PYTHON_VIRTUALENV not set, start Emme from 'start_emme_with_virtualenv.bat'")
        if not venv_path == VIRUTALENV_PATH:
            raise Exception("PYTHON_VIRTUALENV is not the expected value (%s instead of %s)" % (venv_path, VIRUTALENV_PATH))
        venv_path_found = False
        for path in sys.path:
            if VIRUTALENV_PATH in path:
                venv_path_found = True
                break
        if not venv_path_found:
            raise Exception("Python virtual environment not found in system path %s" % VIRUTALENV_PATH)
        copy_scenario = modeller.tool("inro.emme.data.scenario.copy_scenario")
        run4Ds = modeller.tool("sandag.import.run4Ds")
        import_network = modeller.tool("sandag.import.import_network")
        init_transit_db = modeller.tool("sandag.initialize.initialize_transit_database")
        init_matrices = modeller.tool("sandag.initialize.initialize_matrices")
        import_demand = modeller.tool("sandag.import.import_seed_demand")
        build_transit_scen = modeller.tool("sandag.assignment.build_transit_scenario")
        create_transit_connector = modeller.tool("sandag.assignment.create_transit_connector")
        transit_assign = modeller.tool("sandag.assignment.transit_assignment")
        #run_truck = modeller.tool("sandag.model.truck.run_truck_model")
        import_auto_demand = modeller.tool("sandag.import.import_auto_demand")
        import_transit_demand = modeller.tool("sandag.import.import_transit_demand")
        # export_transit_skims = modeller.tool("sandag.export.export_transit_skims")
        export_for_transponder = modeller.tool("sandag.export.export_for_transponder")
        export_network_data = modeller.tool("sandag.export.export_data_loader_network")
        export_matrix_data = modeller.tool("sandag.export.export_data_loader_matrices")
        export_for_commercial_vehicle = modeller.tool("sandag.export.export_for_commercial_vehicle")
        file_manager = modeller.tool("sandag.utilities.file_manager")
        utils = modeller.module('sandag.utilities.demand')
        load_properties = modeller.tool('sandag.utilities.properties')
        run_summary = modeller.tool("sandag.utilities.run_summary")      
        settings_manager = modeller.module("sandag.utilities.settings_manager")

        self.username = username

        manage_settings = settings_manager.SettingsManager(_join(main_directory, "conf", "abm3_settings.yaml"))
        manage_settings(_join(main_directory, "conf", "sandag_abm.properties"))
        manage_settings(_join(main_directory, "src", "asim", "configs"))
        manage_settings(_join(main_directory, "src", "asim-cvm", "configs"))

        props = load_properties(_join(main_directory, "conf", "sandag_abm.properties"))
        props.set_year_specific_properties(_join(main_directory, "input", "parametersByYears.csv"))
        props.set_year_specific_properties(_join(main_directory, "input", "filesByYears.csv"))
        props.save()
        # Log current state of props file for debugging of UI / file sync issues
        attributes = dict((name, props["RunModel." + name]) for name in self._run_model_names)
        _m.logbook_write("SANDAG properties file", attributes=attributes)
        if self._properties:  # Tool has been called via the UI
            # Compare UI values and file values to make sure they are the same
            error_text = ("Different value found in sandag_abm.properties than specified in UI for '%s'. "
                          "Close sandag_abm.properties if open in any text editor, check UI and re-run.")
            for name in self._run_model_names:
                if getattr(self, name) != props["RunModel." + name]:
                    raise Exception(error_text % name)

        scenarioYear = str(props["scenarioYear"])
        scenarioYearSuffix = str(props["scenarioYearSuffix"])
        # geographyID = str(props["geographyID"])
        prod_env = props["RunModel.env"]
        startFromIteration = props["RunModel.startFromIteration"]
        # precision = props["RunModel.MatrixPrecision"]
        minSpaceOnC = props["RunModel.minSpaceOnC"]
        sample_rate = props["sample_rates"]
        end_iteration = len(sample_rate)
        # visualizer_reference_path = props["visualizer.reference.path"]
        # visualizer_output_file = props["visualizer.output"]
        # visualizer_reference_label = props["visualizer.reference.label"]
        # visualizer_build_label = props["visualizer.build.label"]
        fafInputFile = props["faf.file"]
        aoc = props["aoc.fuel"] + props["aoc.maintenance"]
        truck_scenario_year = props["truck.FFyear"]
        htm_input_file = props["htm.input.file"]
        cvm_emp_input_file = props["cvm.emp.input.file"]

        period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

        useLocalDrive = props["RunModel.useLocalDrive"]

        skipMGRASkims = props["RunModel.skipMGRASkims"]
        skip4Ds = props["RunModel.skip4Ds"]
        skipInitialization = props["RunModel.skipInitialization"]
        deleteAllMatrices = props["RunModel.deleteAllMatrices"]
        skipCopyWarmupTripTables = props["RunModel.skipCopyWarmupTripTables"]
        skipBikeLogsums = props["RunModel.skipBikeLogsums"]
        skipBuildNetwork = props["RunModel.skipBuildNetwork"]
        skipHighwayAssignment = props["RunModel.skipHighwayAssignment"]
        skipTransitSkimming = props["RunModel.skipTransitSkimming"]
        skipTransitConnector = props["RunModel.skipTransitConnector"]
        skipSkimConversion = props["RunModel.skipSkimConversion"]
        skipTransponderExport = props["RunModel.skipTransponderExport"]
        skipScenManagement = props["RunModel.skipScenManagement"]
        skipABMPreprocessing = props["RunModel.skipABMPreprocessing"]
        skipABMResident = props["RunModel.skipABMResident"]
        skipABMAirport = props["RunModel.skipABMAirport"]
        skipABMXborderWait = props["RunModel.skipABMXborderWait"]
        skipABMXborder = props["RunModel.skipABMXborder"]
        skipABMVisitor = props["RunModel.skipABMVisitor"]
        skipCVMEstablishmentSyn = props["RunModel.skipCVMEstablishmentSyn"]
        skipMAASModel = props["RunModel.skipMAASModel"]
        skipCTM = props["RunModel.skipCTM"]
        skipEI = props["RunModel.skipEI"]
        skipExternal = props["RunModel.skipExternal"]
        skipTruck = props["RunModel.skipTruck"]
        external_internal = modeller.tool("sandag.model.external_internal")
        external_external = modeller.tool("sandag.model.external_external")
        skipTripTableCreation = props["RunModel.skipTripTableCreation"]
        skipFinalHighwayAssignment = props["RunModel.skipFinalHighwayAssignment"]
        skipFinalHighwayAssignmentStochastic = props["RunModel.skipFinalHighwayAssignmentStochastic"]
        if skipFinalHighwayAssignmentStochastic == True:
        	makeFinalHighwayAssignmentStochastic = False
        else:
        	makeFinalHighwayAssignmentStochastic = True
        skipFinalTransitAssignment = props["RunModel.skipFinalTransitAssignment"]
        skipVisualizer = props["RunModel.skipVisualizer"]
        skipDataExport = props["RunModel.skipDataExport"]
        skipTravelTimeReporter = props["RunModel.skipTravelTimeReporter"]
        skipValidation = props["RunModel.skipValidation"]
        skipDatalake = props["RunModel.skipDatalake"]
        skipDataLoadRequest = props["RunModel.skipDataLoadRequest"]
        skipDeleteIntermediateFiles = props["RunModel.skipDeleteIntermediateFiles"]
        # skipTransitShed = props["RunModel.skipTransitShed"]
        # transitShedThreshold = props["transitShed.threshold"]
        # transitShedTOD = props["transitShed.TOD"]

        #check if visualizer.reference.path is valid in filesbyyears.csv
        # if not os.path.exists(visualizer_reference_path):
        #     raise Exception("Visualizer reference %s does not exist. Check filesbyyears.csv." %(visualizer_reference_path))

        if useLocalDrive:
            folder_name = os.path.basename(main_directory)
            if not os.path.exists(_join(self.LOCAL_ROOT, username, folder_name, "report")): # check free space only if it is a new run
                self.check_free_space(minSpaceOnC)
            # if initialization copy ALL files from remote
            # else check file meta data and copy those that have changed
            initialize = (skipInitialization == False and startFromIteration == 1)
            local_directory = file_manager(
                "DOWNLOAD", main_directory, username, scenario_id, initialize=initialize)
            self._path = local_directory
        else:
            self._path = main_directory

        drive, path_no_drive = os.path.splitdrive(self._path)
        path_forward_slash = path_no_drive.replace("\\", "/")
        input_dir = _join(self._path, "input")
        output_dir = _join(self._path, "output")
        main_emmebank = _eb.Emmebank(_join(self._path, "emme_project", "Database", "emmebank"))
        if emmebank_title:
            main_emmebank.title = emmebank_title
        external_zones = "1-12"

        travel_modes = ["auto", "tran", "nmot", "othr"]
        core_abm_files = ["Trips*.omx"]
        core_abm_files = [mode + name for name in core_abm_files for mode in travel_modes]
        smm_abm_files = ["AirportTrips*.omx", "CrossBorderTrips*.omx", "VisitorTrips*.omx"]
        smm_abm_files = [mode + name for name in smm_abm_files for mode in travel_modes]
        smm_csv_files = ["airport_out.CBX.csv", "airport_out.SAN.csv", "crossBorderTours.csv", "crossBorderTrips.csv", "visitorTours.csv", "visitorTrips.csv"]
        smm_abm_files.extend(smm_csv_files)
        maas_abm_files = ["EmptyAVTrips.omx", "TNCVehicleTrips*.omx"]

        relative_gap = props["convergence"]
        max_assign_iterations = 100

        #change emme databank dimensions based on number of select links - SANDAG ABM2+ Enhancements (06-28-2021)
        num_select_links =  0
        if select_link:
            num_select_links = len(_json.loads(select_link))
        change_dimensions = modeller.tool("inro.emme.data.database.change_database_dimensions")
        dims = main_emmebank.dimensions
        num_nodes = dims["regular_nodes"] + dims['centroids']
        num_links = dims["links"]
        num_turn_entries = dims["turn_entries"]
        num_transit_lines = dims['transit_lines']
        num_transit_segments = dims['transit_segments']
        num_traffic_classes = 15

        additional_node_extra_attributes = 4
        additional_link_extra_attributes = 26
        additional_line_extra_attributes = 4
        additional_segment_extra_attributes = 12

        extra_attribute_values = 18000000
        extra_attribute_values += (num_nodes + 1) * additional_node_extra_attributes
        extra_attribute_values += (num_links + 1) * additional_link_extra_attributes
        extra_attribute_values += (num_transit_lines + 1)* additional_line_extra_attributes
        extra_attribute_values += (num_transit_segments + 1) * additional_segment_extra_attributes

        if num_select_links > 3:
            extra_attribute_values += (num_select_links - 3) * ((num_links + 1) * (num_traffic_classes + 1) + (num_turn_entries + 1) * (num_traffic_classes))

        if extra_attribute_values > dims["extra_attribute_values"] or dims["full_matrices"] < 9999:
            dims["extra_attribute_values"] = extra_attribute_values
            dims["full_matrices"] = 9999
            #add logging for when this setp is run, add before and after attribute value
            #change_dimensions(emmebank_dimensions=dims, emmebank=main_emmebank, keep_backup=False)
            #replaced the above line with the below lines - suggested by Antoine, Bentley (2022-06-02)
            if main_emmebank.scenario(1) is None:
                main_emmebank.create_scenario(1)
            change_dimensions(dims, main_emmebank, False)
        # with open(_join(self._path, "logFiles", "select_link_log.txt"),"a+") as f:
		#     f.write("Num Select links {}\nExtra Attribute Value {}".format(num_select_links,extra_attribute_values))
        # f.close()

        for period in periods:
            if os.path.exists(_join(self._path, "emme_project", "Database_transit_" + period, "emmebank")):
                with _eb.Emmebank(_join(self._path, "emme_project", "Database_transit_" + period, "emmebank")) as transit_db:
                    transit_db_dims = transit_db.dimensions
                    num_nodes = transit_db_dims["regular_nodes"] + transit_db_dims['centroids']
                    num_links = transit_db_dims["links"]
                    num_turn_entries = transit_db_dims["turn_entries"]
                    num_transit_lines = transit_db_dims['transit_lines']
                    num_transit_segments = transit_db_dims['transit_segments']
                    num_traffic_classes = 15

                    extra_attribute_values = 18000000
                    extra_attribute_values += (num_nodes + 1) * additional_node_extra_attributes
                    extra_attribute_values += (num_links + 1) * additional_link_extra_attributes
                    extra_attribute_values += (num_transit_lines + 1)* additional_line_extra_attributes
                    extra_attribute_values += (num_transit_segments + 1) * additional_segment_extra_attributes

                    if num_select_links > 3:
                        extra_attribute_values += 18000000 + (num_select_links - 3) * ((num_links + 1) * (num_traffic_classes + 1) + (num_turn_entries + 1) * (num_traffic_classes))

                    if extra_attribute_values > transit_db_dims["extra_attribute_values"] or transit_db_dims["full_matrices"] < 9999:
                        transit_db_dims["extra_attribute_values"] = extra_attribute_values
                        transit_db_dims["full_matrices"] = 9999
                        #change_dimensions(emmebank_dimensions=transit_db_dims, emmebank=transit_db, keep_backup=False)
                        #replaced the above line with the below lines - suggested by Antoine, Bentley (2022-06-02)
                        if transit_db.scenario(1) is None:
                            transit_db.create_scenario(1)
                        change_dimensions(transit_db_dims, transit_db, False)

        with _m.logbook_trace("Setup and initialization"):
            self.set_global_logbook_level(props)

            #get number of households to pass on sample size to activitysim
            householdFile = pd.read_csv(_join(self._path, "input", "households.csv"))
            hh_resident_size = len(householdFile)
            del(householdFile)

            if startFromIteration == 1:  # only run the setup / init steps if starting from iteration 1

                if not skipMGRASkims:
                    self.run_proc("runSandagMGRASkims.cmd", [drive, path_forward_slash],
                                  "Create MGRA-level skims", capture_output=True)

                if not skip4Ds:
                    run4Ds(path=self._path, int_radius=0.65, ref_path='visualizer_reference_path')

                mgraFile = 'mgra15_based_input' + str(scenarioYear) + '.csv'  # Should be read in from properties? -JJF
                self.complete_work(scenarioYear, input_dir, output_dir, mgraFile, "maz_maz_walk.csv")

                # Update rapid dwell time before importing network
                mode5tod = pd.read_csv(_join(input_dir,'MODE5TOD.csv'))
                mode5tod.loc[mode5tod['MODE_ID'].isin([6,7]), ['DWELLTIME']] = float(props['rapid.dwell'])
                mode5tod.to_csv(_join(input_dir,'MODE5TOD.csv'))

                if not skipBuildNetwork:
                    source_gdb = _glob.glob(os.path.join(input_dir, "*.gdb"))
                    if len(source_gdb) > 1:
                        raise Exception("Multiple *.gdb files found in input directory")
                    if len(source_gdb) < 1:
                        raise Exception("No *.gdb file found in input directory")
                    base_scenario = import_network(
                        source=source_gdb[0],
                        scenario_id=scenario_id,
                        title=scenario_title,
                        data_table_name=scenarioYear,
                        overwrite=True,
                        emmebank=main_emmebank)


                    # parse vehicle availablility file by time-of-day
                    availability_file = "vehicle_class_availability.csv"
                    availabilities = self.parse_availability_file(_join(input_dir, availability_file), periods)
                    # initialize per time-period scenarios
                    for number, period in period_ids:
                        scenario = main_emmebank.scenario(number)
                        # Apply availabilities by facility and vehicle class to this time period
                        self.apply_availabilities(period, scenario, availabilities)
                else:
                    base_scenario = main_emmebank.scenario(scenario_id)

                if not skipInitialization:
                    # initialize traffic demand, skims, truck, CV, EI, EE matrices
                    traffic_components = [
                        "traffic_skims",
                        "truck_model",
                        "external_internal_model", "external_external_model"]
                    if not skipCopyWarmupTripTables:
                        traffic_components.append("traffic_demand")
                    init_matrices(traffic_components, periods, base_scenario, deleteAllMatrices)

                    transit_scenario_dict = {}
                    transit_emmebank_dict = {}
                    for period in periods:
                        transit_scenario_dict[period] = init_transit_db(base_scenario, period, add_database=not useLocalDrive)
                        transit_emmebank_dict[period] = transit_scenario_dict[period].emmebank
                        transit_components = ["transit_skims"]
                        if not skipCopyWarmupTripTables:
                            transit_components.append("transit_demand")
                        init_matrices(transit_components, [period], transit_scenario_dict[period], deleteAllMatrices)
                else:
                    transit_scenario_dict = {}
                    transit_emmebank_dict = {}
                    for period in periods:
                        transit_emmebank_dict[period] = _eb.Emmebank(_join(self._path, "emme_project", "Database_transit_" + period, "emmebank"))
                        transit_scenario_dict[period] = transit_emmebank_dict[period].scenario(base_scenario.number)

                if not skipCopyWarmupTripTables:
                    # import seed auto demand and seed truck demand
                    for period in periods:
                        omx_file = _join(input_dir, "trip_%s.omx" % period)
                        import_demand(omx_file, "AUTO", period, base_scenario)
                        import_demand(omx_file, "TRUCK", period, base_scenario)

                if not skipBikeLogsums:
                    self.run_proc("runSandagBikeLogsums.cmd", [drive, path_forward_slash],
                                  "Bike - create AT logsums and impedances")
                    # Copy updated logsums to scenario input to avoid overwriting
                    self.copy_files(["bikeMgraLogsum.csv", "bikeTazLogsum.csv"], output_dir, input_dir)
                elif not os.path.exists(_join(output_dir, "bikeMgraLogsum.csv")):
                    self.copy_files(["bikeMgraLogsum.csv", "bikeTazLogsum.csv"], input_dir, output_dir)

            else:
                base_scenario = main_emmebank.scenario(scenario_id)
                transit_scenario_dict = {}
                transit_emmebank_dict = {}
                for period in periods:
                    transit_emmebank_dict[period] = _eb.Emmebank(_join(self._path, "emme_project", "Database_transit_" + period, "emmebank"))
                    transit_scenario_dict[period] = transit_emmebank_dict[period].scenario(base_scenario.number)

            # Check that setup files were generated
            # self.run_proc("CheckOutput.bat", [drive + path_no_drive, 'Setup'], "Check for outputs")

        # Note: iteration indexes from 0, msa_iteration indexes from 1
        for iteration in range(startFromIteration - 1, end_iteration):
            msa_iteration = iteration + 1
            with _m.logbook_trace("Iteration %s" % msa_iteration):
                #create a folder to store skims
                if not os.path.exists(_join(output_dir, "skims")):
                    os.mkdir(_join(output_dir, "skims"))

                if not skipHighwayAssignment[iteration]:
                    # run traffic assignment
                    # export traffic skims
                    with _m.logbook_trace("Traffic assignment and skims"):
                        self.run_traffic_assignments(
                            base_scenario, period_ids, msa_iteration, relative_gap,
                            max_assign_iterations, num_processors)

                if not skipTransitSkimming[iteration]:
                    # run transit assignment
                    # export transit skims
                    with _m.logbook_trace("Transit assignments and skims"):

                        for number, period in period_ids:
                            src_period_scenario = main_emmebank.scenario(number)
                            transit_assign_scen = build_transit_scen(
                               period=period, base_scenario=src_period_scenario,
                               transit_emmebank=transit_emmebank_dict[period],
                               scenario_id=src_period_scenario.id,
                               scenario_title="%s %s transit assign" % (base_scenario.title, period),
                               data_table_name=scenarioYear, overwrite=True)

                            if (not skipTransitConnector) and (msa_iteration == 1):
                                if not os.path.exists(_join(input_dir, "transit_connectors")):
                                    os.mkdir(_join(input_dir, "transit_connectors"))
                            #     #in case of new network, create transit connectors from scratch, and export them to the input folder for future runs/iterations
                            #     create_transit_connector(period, transit_assign_scen, create_connector_flag=True)
                            # else:
                            #     #this would import connectors from the input/transit_connectors folder, and not create them from scratch
                            #     create_transit_connector(period, transit_assign_scen, create_connector_flag=False)

                        # Run transit assignment in separate process
                        # Running in same process slows OMX skim export for unknown reason
                        # transit_emmebank need to be closed and re-opened to be accessed by separate process
                        transit_emmebank_dict = self.run_transit_assignments(transit_emmebank_dict, scenarioYear, output_dir, ((not skipTransitConnector) and (msa_iteration == 1)), main_directory)
                        for period in periods:
                            transit_scenario_dict[period] = transit_emmebank_dict[period].scenario(base_scenario.number)
                        # _m.Modeller().desktop.refresh_data()

                        # #output transit skims by period
                        # for number, period in period_ids:
                        #     transit_scenario = transit_emmebank.scenario(number)
                        #     omx_file = _join(output_dir, "skims", "transit_skims_" + period + ".omx")
                        #     export_transit_skims(omx_file, [period], transit_scenario, big_to_zero=False)

                if not skipSkimConversion[iteration]:
                    self.run_proc("convertSkimsToOMXZ.cmd",
                                  [drive, path_forward_slash],
                                  "Converting skims to omxz format", capture_output=True)

                if not skipTransponderExport[iteration]:
                    am_scenario = main_emmebank.scenario(base_scenario.number + 2)
                    export_for_transponder(output_dir, num_processors, am_scenario)

                if (not skipScenManagement) and (msa_iteration==1):
                    self.run_proc("runSandag_ScenManagement.cmd",
                            [drive + path_forward_slash, str(props["scenarioYear"]), str(props["scenarioYear"]) + str(props["scenarioYearSuffix"])],
                            "Running Scenario Management", capture_output=True)

                if not skipABMPreprocessing[iteration]:
                    self.run_proc(
                        "runSandagAbm_Preprocessing.cmd",
                        [drive, drive + path_forward_slash, msa_iteration, scenarioYear],
                        "Creating all the required files to run the ActivitySim models", capture_output=True)

                skip_asim = skipABMResident[iteration] and skipABMAirport[iteration] and skipABMXborder[iteration] and skipABMVisitor[iteration]

                if not skip_asim:
                    mem_manager = _subprocess.Popen(
                        [_join(self._path, "bin", "manage_skim_mem.cmd"),
                        drive, drive + path_forward_slash], 
                        stdout=_subprocess.PIPE, stderr=_subprocess.PIPE,
                        stdin=_subprocess.PIPE, creationflags=_subprocess.CREATE_NEW_PROCESS_GROUP
                    )
                try:
                    if not skipABMResident[iteration]:
                        self.set_sample_rate(_join(self._path, r"src\asim\configs\resident\settings_mp.yaml"), int(sample_rate[iteration] * hh_resident_size))
                        self.run_proc(
                            "runSandagAbm_ActivitySimResident.cmd",
                            [drive, drive + path_forward_slash],
                            "Running ActivitySim resident model", capture_output=True)
                    if not skipABMAirport[iteration]:
                        hh_airport_size = {}
                        for airport in ["san", "cbx"]:
                            householdFile = pd.read_csv(_join(self._path, "input", "households_airport.{}.csv".format(airport)))
                            hh_airport_size[airport] = len(householdFile)
                            del(householdFile)
                        self.set_sample_rate(_join(self._path, r"src\asim\configs\airport.CBX\settings.yaml"), int(sample_rate[iteration] * hh_airport_size["cbx"]))
                        self.set_sample_rate(_join(self._path, r"src\asim\configs\airport.SAN\settings.yaml"), int(sample_rate[iteration] * hh_airport_size["san"]))
                        self.run_proc(
                            "runSandagAbm_ActivitySimAirport.cmd",
                            [drive, drive + path_forward_slash],
                            "Running ActivitySim airport models", capture_output=True)
                    if (not skipABMXborderWait) and (iteration == 0):
                        self.run_proc(
                            "runSandagAbm_ActivitySimXborderWaitModel.cmd",
                            [drive, drive + path_forward_slash],
                            "Running ActivitySim wait time models", capture_output=True)
                    if not skipABMXborder[iteration]:
                        householdFile = pd.read_csv(_join(self._path, "input", "households_xborder.csv"))
                        hh_xborder_size = len(householdFile)
                        del(householdFile)
                        self.set_sample_rate(_join(self._path, r"src\asim\configs\crossborder\settings.yaml"), int(sample_rate[iteration] * hh_xborder_size))
                        self.run_proc(
                            "runSandagAbm_ActivitySimXborder.cmd",
                            [drive, drive + path_forward_slash],
                            "Running ActivitySim crossborder model", capture_output=True)
                    if not skipABMVisitor[iteration]:
                        householdFile = pd.read_csv(_join(self._path, "input", "households_visitor.csv"))
                        hh_visitor_size = len(householdFile)
                        del(householdFile)
                        self.set_sample_rate(_join(self._path, r"src\asim\configs\visitor\settings.yaml"), int(sample_rate[iteration] * hh_visitor_size))
                        self.run_proc(
                            "runSandagAbm_ActivitySimVisitor.cmd",
                            [drive, drive + path_forward_slash],
                            "Running ActivitySim visitor model", capture_output=True)
                finally:
                    if not skip_asim:
                        forced_stop = False
                        if mem_manager.poll() is None:
                            mem_manager.stdin.write(b"\n")
                            _time.sleep(5) 
                            if mem_manager.poll() is None:
                                mem_manager.send_signal(signal.CTRL_BREAK_EVENT)
                                forced_stop = True
                        out, err = mem_manager.communicate()
                        report = _m.PageBuilder(title="Command report")
                        self.add_html(report, 'Output:<br><br><div class="preformat">%s</div>' % out)
                        if err:
                            self.add_html(report, 'Error message(s):<br><br><div class="preformat">%s</div>' % err)
                        _m.logbook_write("Skim shared memory manager process record", report.render()) 
                        if mem_manager.returncode != 0 and not forced_stop:
                            raise Exception("Error in skim shared memory manager, view logbook for details")

                if not skipMAASModel[iteration]:
                    self.run_proc("runMtxMgr.cmd", [drive, drive + path_no_drive], "Start matrix manager")
                    self.run_proc(
                        "runSandagAbm_MAAS.cmd",
                        [drive, drive + path_forward_slash, 1, 0],
                        "Java-Run AV allocation model and TNC routing model", capture_output=True)

                if (not skipCVMEstablishmentSyn) and (iteration == 0):
                    self.run_proc("cvmEst.bat", [drive, path_no_drive, cvm_emp_input_file],
                        "Commercial vehicle model establishment synthesis", capture_output=True)
                if not skipCTM[iteration]:
                    #export_for_commercial_vehicle(output_dir + '/skims', base_scenario)
                    self.run_proc(
                        "cvm.bat",
                        [drive, path_no_drive, str(scenarioYear) + str(scenarioYearSuffix)],
                        "Commercial vehicle model", capture_output=True)
                if msa_iteration == startFromIteration:
                    external_zones = "1-12"
                    if not skipTruck[iteration]:
                        # run truck model (generate truck trips)
                        self.run_proc(
                            "htm.bat",
                            [drive, path_no_drive, fafInputFile, htm_input_file, "PM", truck_scenario_year, str(scenarioYear) + str(scenarioYearSuffix)],
                            "Heavy truck model", capture_output=True)
                    # run EI model "US to SD External Trip Model"
                    if not skipEI[iteration]:
                        external_internal(input_dir, base_scenario)
                    # run EE model
                    if not skipExternal[iteration]:
                        external_external(input_dir, external_zones, base_scenario)


                # import demand from all sub-market models from CT-RAMP and
                #       add CV trips to auto demand
                if not skipTripTableCreation[iteration]:
                    import_auto_demand(output_dir, external_zones, num_processors, base_scenario)

        if not skipFinalHighwayAssignment:
            with _m.logbook_trace("Final traffic assignments"):
                # Final iteration is assignment only, no skims
                final_iteration = 4
                self.run_traffic_assignments(
                    base_scenario, period_ids, final_iteration, relative_gap, max_assign_iterations,
                    num_processors, select_link, makeFinalHighwayAssignmentStochastic, input_dir)
                self.run_proc(
                        "runSandagAbm_Preprocessing.cmd",
                        [drive, drive + path_forward_slash, final_iteration, scenarioYear],
                        "Adding DIST skim", capture_output=True)

        if not skipFinalTransitAssignment:
            import_transit_demand(output_dir, transit_scenario_dict)
            with _m.logbook_trace("Final transit assignments"):
                # Final iteration includes the transit skims per ABM-1072
                for number, period in period_ids:
                    src_period_scenario = main_emmebank.scenario(number)
                    transit_assign_scen = build_transit_scen(
                        period=period, base_scenario=src_period_scenario,
                        transit_emmebank=transit_emmebank_dict[period],
                        scenario_id=src_period_scenario.id,
                        scenario_title="%s %s transit assign" % (base_scenario.title, period),
                        data_table_name=scenarioYear, overwrite=True)

                    #this would import connectors from the input/transit_connectors folder, and not create them from scratch
                    # create_transit_connector(period, transit_assign_scen, create_connector_flag=False)

                # Run transit assignment in separate process
                # Running in same process slows OMX skim export for unknown reason
                # transit_emmebank need to be closed and re-opened to be accessed by separate process
                transit_emmebank_dict = self.run_transit_assignments(transit_emmebank_dict, scenarioYear, output_dir, False, main_directory)
                for period in periods:
                    transit_scenario_dict[period] = transit_emmebank_dict[period].scenario(base_scenario.number)
                # _m.Modeller().desktop.refresh_data()

                # #output transit skims by period
                # for number, period in period_ids:
                #     transit_scenario = transit_emmebank.scenario(number)
                #     omx_file = _join(output_dir, "skims", "transit_skims_" + period + ".omx")
                #     export_transit_skims(omx_file, [period], transit_scenario, big_to_zero=False)

        # if not skipTransitShed:
        #     # write walk and drive transit sheds
        #     self.run_proc("runtransitreporter.cmd", [drive, path_forward_slash, transitShedThreshold, transitShedTOD],
        #                   "Create walk and drive transit sheds",
        #                   capture_output=True)

        if not skipVisualizer:
            self.run_proc("RunViz.bat",
                          [drive, drive + path_forward_slash],
                          "HTML Visualizer", capture_output=True)

        if not skipDataExport:

            # export network and matrix results from Emme directly to T if using local drive
            output_directory = _join(self._path, "output")
            export_network_data(self._path, scenario_id, main_emmebank, transit_emmebank_dict, num_processors)
            export_matrix_data(output_directory, base_scenario)
            # export core ABM data
            # Note: uses relative project structure, so cannot redirect to T drive
            # self.run_proc("DataExporter.bat", [drive, path_no_drive], "Export core ABM data",capture_output=True)
            # aggregate_models = {}
            # for agg_model in ['eetrip', 'eitrip', 'trucktrip']:#TODO ['commercialVehicleTrips','internalExternalTrips']:
            #     aggregate_models[agg_model] = str(os.path.join(self._path,'report',agg_model+'.csv'))
            # gen_utils.DataLakeExporter(ScenarioPath=self._path).write_to_datalake(aggregate_models)
            self.run_proc(
                "export_hwy_shape.cmd",
                [drive, drive + path_forward_slash],
                "Exporting highway shapefile", capture_output=True)
            
        if not skipTravelTimeReporter:
            self.run_proc(
                "run_travel_time_calculator.cmd",
                [drive, drive + path_forward_slash],
                "Exporting MGRA-level travel times", capture_output=True)
            
            self.run_proc(
                "deleteOMXZskims.cmd",
                [drive, drive + path_forward_slash],
                "Deleting OMXZ skims", capture_output=True)

        # This validation procedure only works with base (2022) scenarios utilizing TNED networks
        if scenarioYear == "2022" and not skipValidation:
            self.run_proc(
                "runValidation.bat",
                [drive, path_no_drive, scenarioYear],
                "Validation", capture_output=True)
                
        if not skipDatalake:
            self.write_metadata(main_directory, scenario_title, select_link, username, scenarioYear, sample_rate, prod_env, props)
            self.run_proc(
                "write_to_datalake.cmd",
                [drive, drive + path_forward_slash, prod_env],
                "Writing model output to datalake", capture_output=True)

        # # terminate all java processes
        # _subprocess.call("taskkill /F /IM java.exe")

        # # close all DOS windows
        # _subprocess.call("taskkill /F /IM cmd.exe")

        # UPLOAD DATA AND SWITCH PATHS
        if useLocalDrive:
            try:
                # # Uncomment to get disk usage at end of run
                # # Note that max disk usage occurs in resident model, not at end of run
                # disk_usage = win32.Dispatch('Scripting.FileSystemObject').GetFolder(self._path).Size
                # _m.logbook_write("Disk space usage: %f GB" % (disk_usage / (1024 ** 3)))
                file_manager("UPLOAD", main_directory, username, scenario_id,
                            delete_local_files=not skipDeleteIntermediateFiles)
                self._path = main_directory
                drive, path_no_drive = os.path.splitdrive(self._path)
                # self._path = main_directory
                # drive, path_no_drive = os.path.splitdrive(self._path)
                for period in periods:
                    init_transit_db.add_database(
                        _eb.Emmebank(_join(main_directory, "emme_project", "Database_transit_" + period, "emmebank")))
            except Exception as e:
                skipDeleteIntermediateFiles = True
                _m.logbook_write("WARNING: Copy to remote drive failed")
                _m.logbook_write(_traceback.format_exc())

        if not skipDataLoadRequest:
            start_db_time = datetime.datetime.now()  # record the time to search for request id in the load request table, YMA, 1/23/2019
            # start_db_time = start_db_time + datetime.timedelta(minutes=0)
            # self.run_proc("DataLoadRequest.bat",
            #               [drive + path_no_drive, end_iteration, scenarioYear, sample_rate[end_iteration - 1], geographyID],
            #               "Data load request")

        # delete trip table files in iteration sub folder if model finishes without errors
        if not useLocalDrive and not skipDeleteIntermediateFiles:
            for msa_iteration in range(startFromIteration, end_iteration + 1):
                self.delete_files(
                    ["auto*Trips*.omx", "tran*Trips*.omx", "nmot*.omx", "othr*.omx", "trip*.omx"],
                    _join(output_dir, "iter%s" % (msa_iteration)))

        # record run time
        run_summary(path=self._path)

    def set_global_logbook_level(self, props):
        self._log_level = props.get("RunModel.LogbookLevel", "ENABLED")
        log_all = _m.LogbookLevel.ATTRIBUTE | _m.LogbookLevel.VALUE | _m.LogbookLevel.COOKIE | _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG
        log_states = {
            "ENABLED": log_all,
            "DISABLE_ON_ERROR": log_all,
            "NO_EXTERNAL_REPORTS": log_all,
            "NO_REPORTS": _m.LogbookLevel.ATTRIBUTE | _m.LogbookLevel.COOKIE | _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG,
            "TITLES_ONLY": _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG,
            "DISABLED": _m.LogbookLevel.NONE,
        }
        _m.logbook_write("Setting logbook level to %s" % self._log_level)
        try:
            _m.logbook_level(log_states[self._log_level])
        except KeyError:
            raise Exception("properties.RunModel.LogLevel: value must be one of %s" % ",".join(log_states.keys()))

    def run_transit_assignments(self, transit_emmebank_dict, scenarioYear, output_dir, create_connector_flag, main_directory_original):

        scenario_id = 100
        periods = ["EA", "AM", "MD", "PM", "EV"]
        period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

        transit_processors = (multiprocessing.cpu_count() - 1) // 5

        transit_emmebank_path_dict = {}

        processes = []

        with _m.logbook_trace("Running all period transit assignmens"):
            for number, period in period_ids:

                transit_emmebank_path_dict[period] = transit_emmebank_dict[period].path
                emme_project_dir = _dir(_dir(transit_emmebank_path_dict[period]))
                main_directory = _dir(emme_project_dir)

                if os.path.exists(_join(emme_project_dir, "transit_assign_dummy_project_" + period)):
                    shutil.rmtree(_join(emme_project_dir, "transit_assign_dummy_project_" + period))
                project_path = _app.create_project(emme_project_dir, "transit_assign_dummy_project_" + period)
                dummy_desktop = _app.start_dedicated(visible=False, user_initials="SD", project=project_path)
                dummy_desktop.add_modeller_toolbox(_join(main_directory_original, "emme_project", "Scripts", "sandag_toolbox.mtbx"))
                data_explorer = dummy_desktop.data_explorer()
                db = data_explorer.add_database(transit_emmebank_dict[period].path)
                db.open()

                project_table_db = _m.Modeller().desktop.project.data_tables()
                data = project_table_db.table("%s_transit_passes" % scenarioYear).get_data()
                dummy_project_table_db = dummy_desktop.project.data_tables()
                dummy_project_table_db.create_table("%s_transit_passes" % scenarioYear, data, overwrite=True)

                dummy_desktop.project.save()
                dummy_desktop.close()
                transit_emmebank_dict[period].dispose()

                _time.sleep(2)

                script = _join(main_directory, "python", "emme", "run_transit_assignment.py")
                args = [sys.executable, script, "--root_dir", '"%s"' % main_directory, "--project_path", '"%s"' % project_path,
                    "--period", '"%s"' % period, "--number", '"%s"' % number, "--proc", '"%s"' % transit_processors,
                    "--output_dir", '"%s"' % output_dir]
                if create_connector_flag:
                    args.append("--create_connector_flag")
                p = _subprocess.Popen(args, shell=True)
                processes.append({
                    "p": p,
                    "period": period
                })
                _time.sleep(2)

                # NOTE: could remove project when done
                #shutil.rmtree(project_dir)
                # NOTE: need to pass back re-opened objects

            for p in processes:
                report = _m.PageBuilder(title="Command report")
                out, err = p["p"].communicate()
                self.add_html(report, 'Output:<br><br><div class="preformat">%s</div>' % out)
                if err:
                    self.add_html(report, 'Error message(s):<br><br><div class="preformat">%s</div>' % err)
                _m.logbook_write("Transit assignment process record for period " + p["period"], report.render())
                if p["p"].returncode != 0:
                    raise Exception("Error in transit assignment period %s, refer to logbook in dummy project" % p["period"])


        new_transit_emmebank_dict = {}
        for number, period in period_ids:
            new_transit_emmebank_dict[period] = _eb.Emmebank(transit_emmebank_path_dict[period])
        _m.Modeller().desktop.refresh_data()

        return new_transit_emmebank_dict

    def run_traffic_assignments(self, base_scenario, period_ids, msa_iteration, relative_gap,
                                max_assign_iterations, num_processors, select_link=None,
                                makeFinalHighwayAssignmentStochastic=False, input_dir=None):
        modeller = _m.Modeller()
        traffic_assign = modeller.tool("sandag.assignment.traffic_assignment")
        export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
        output_dir = _join(self._path, "output")
        main_emmebank = base_scenario.emmebank

        for number, period in period_ids:
            period_scenario = main_emmebank.scenario(number)
            traffic_assign(period, msa_iteration, relative_gap, max_assign_iterations,
                            num_processors, period_scenario, select_link, stochastic=makeFinalHighwayAssignmentStochastic, input_directory=input_dir)
            omx_file = _join(output_dir, "skims", "traffic_skims_%s.omx" % period)
            if msa_iteration <= 4:
                export_traffic_skims(period, omx_file, base_scenario)

    def run_proc(self, name, arguments, log_message, capture_output=False):
        path = _join(self._path, "bin", name)
        if not os.path.exists(path):
            raise Exception("No command / batch file '%s'" % path)
        command = path + " " + " ".join([str(x) for x in arguments])
        attrs = {"command": command, "name": name, "arguments": arguments}
        with _m.logbook_trace(log_message, attributes=attrs):
            if capture_output and self._log_level != "NO_EXTERNAL_REPORTS":
                report = _m.PageBuilder(title="Process run %s" % name)
                self.add_html(report, 'Command:<br><br><div class="preformat">%s</div><br>' % command)
                # temporary file to capture output error messages generated by Java
                err_file_ref, err_file_path = _tempfile.mkstemp(suffix='.log')
                err_file = os.fdopen(err_file_ref, "w")
                try:
                    output = _subprocess.check_output(command, stderr=err_file, cwd=self._path, shell=True)
                    self.add_html(report, 'Output:<br><br><div class="preformat">%s</div>' % output)
                except _subprocess.CalledProcessError as error:
                    self.add_html(report, 'Output:<br><br><div class="preformat">%s</div>' % error.output)
                    raise Exception("Error in %s, refer to process run report in logbook" % name)
                finally:
                    err_file.close()
                    with open(err_file_path, 'r') as f:
                        error_msg = f.read()
                    os.remove(err_file_path)
                    if error_msg:
                        self.add_html(report, 'Error message(s):<br><br><div class="preformat">%s</div>' % error_msg)
                    try:
                        # No raise on writing report error
                        # due to observed issue with runs generating reports which cause
                        # errors when logged
                        _m.logbook_write("Process run %s report" % name, report.render())
                    except Exception as error:
                        print (_time.strftime("%Y-%M-%d %H:%m:%S"))
                        print ("Error writing report '%s' to logbook" % name)
                        print (error)
                        print (_traceback.format_exc(error))
                        if self._log_level == "DISABLE_ON_ERROR":
                            _m.logbook_level(_m.LogbookLevel.NONE)
            else:
                _subprocess.check_call(command, cwd=self._path, shell=True)

    @_m.logbook_trace("Check free space on C")
    def check_free_space(self, min_space):
        path = "c:\\"
        temp, total, free = _ctypes.c_ulonglong(), _ctypes.c_ulonglong(), _ctypes.c_ulonglong()
        if sys.version_info >= (3,) or isinstance(path, str):
            fun = _ctypes.windll.kernel32.GetDiskFreeSpaceExW
        else:
            fun = _ctypes.windll.kernel32.GetDiskFreeSpaceExA
        ret = fun(path, _ctypes.byref(temp), _ctypes.byref(total), _ctypes.byref(free))
        if ret == 0:
            raise _ctypes.WinError()
        total = total.value / (1024.0 ** 3)
        free = free.value / (1024.0 ** 3)
        if free < min_space:
            raise Exception("Free space on C drive %s is less than %s" % (free, min_space))

    def remove_prev_iter_files(self, file_names, output_dir, iteration):
        if iteration == 0:
            self.delete_files(file_names, output_dir)
        else:
            self.move_files(file_names, output_dir, _join(output_dir, "iter%s" % (iteration)))

    def copy_files(self, file_names, from_dir, to_dir):
        with _m.logbook_trace("Copy files %s" % ", ".join(file_names)):
            for file_name in file_names:
                from_file = _join(from_dir, file_name)
                _shutil.copy(from_file, to_dir)

    def complete_work(self, scenarioYear, input_dir, output_dir, input_file, output_file):

        fullList = np.array(pd.read_csv(_join(input_dir, input_file))['mgra'])
        workList = np.array(pd.read_csv(_join(output_dir, "skims", output_file))['i'])

        list_set = set(workList)
        unique_list = (list(list_set))
        notMatch = [x for x in fullList if x not in unique_list]

        if notMatch:
            out_file = _join(output_dir, output_file)
            with open(out_file, 'a') as csvfile:
                spamwriter = csv.writer(csvfile)
                # spamwriter.writerow([])
                for item in notMatch:
                    # pdb.set_trace()
                    spamwriter.writerow([item, item, '30', '30', '30'])

    def move_files(self, file_names, from_dir, to_dir):
        with _m.logbook_trace("Move files %s" % ", ".join(file_names)):
            if not os.path.exists(to_dir):
                os.mkdir(to_dir)
            for file_name in file_names:
                all_files = _glob.glob(_join(from_dir, file_name))
                for path in all_files:
                    try:
                        dst_file = _join(to_dir, os.path.basename(path))
                        if os.path.exists(dst_file):
                            os.remove(dst_file)
                        _shutil.move(path, to_dir)
                    except Exception as error:
                        _m.logbook_write(
                            "Error moving file %s" % path, {"error": _traceback.format_exc(error)})

    def delete_files(self, file_names, directory):
        with _m.logbook_trace("Delete files %s" % ", ".join(file_names)):
            for file_name in file_names:
                all_files = _glob.glob(_join(directory, file_name))
                for path in all_files:
                    os.remove(path)

    def check_for_fatal(self, file_name, error_msg):
        with open(file_name, 'a+') as f:
            for line in f:
                if "FATAL" in line:
                    raise Exception(error_msg)

    def set_active(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if _norm(db.path) == _norm(str(emmebank)):
                db.open()
                return db
        return None

    def parse_availability_file(self, file_path, periods):
        if os.path.exists(file_path):
            availabilities = _defaultdict(lambda: _defaultdict(lambda: dict()))
            # NOTE: CSV Reader sets the field names to UPPERCASE for consistency
            with gen_utils.CSVReader(file_path) as r:
                for row in r:
                    name = row.pop("FACILITY_NAME")
                    class_name = row.pop("VEHICLE_CLASS")
                    for period in periods:
                        is_avail = int(row[period + "_AVAIL"])
                        if is_avail not in [1, 0]:
                            msg = "Error processing file '%s': value for period %s class %s facility %s is not 1 or 0"
                            raise Exception(msg % (file_path, period, class_name, name))
                        availabilities[period][name][class_name] = is_avail
        else:
            availabilities = None
        return availabilities

    def apply_availabilities(self, period, scenario, availabilities):
        if availabilities is None:
            return

        network = scenario.get_network()
        hov2 = network.mode("h")
        hov2_trnpdr = network.mode("H")
        hov3 = network.mode("i")
        hov3_trnpdr = network.mode("I")
        sov = network.mode("s")
        sov_trnpdr = network.mode("S")
        heavy_trk = network.mode("v")
        heavy_trk_trnpdr = network.mode("V")
        medium_trk = network.mode("m")
        medium_trk_trnpdr = network.mode("M")
        light_trk = network.mode("t")
        light_trk_trnpdr = network.mode("T")

        class_mode_map = {
            "DA":    set([sov_trnpdr, sov]),
            "S2":    set([hov2_trnpdr, hov2]),
            "S3":    set([hov3_trnpdr, hov3]),
            "TRK_L": set([light_trk_trnpdr, light_trk]),
            "TRK_M": set([medium_trk_trnpdr, medium_trk]),
            "TRK_H": set([heavy_trk_trnpdr, heavy_trk]),
        }
        report = ["<div style='margin-left:5px'>Link mode changes</div>"]
        for name, class_availabilities in availabilities[period].items():
            report.append("<div style='margin-left:10px'>%s</div>" % name)
            changes = _defaultdict(lambda: 0)
            for link in network.links():
                if name in link["#name"]:
                    for class_name, is_avail in class_availabilities.items():
                        modes = class_mode_map[class_name]
                        if is_avail == 1 and not modes.issubset(link.modes):
                            link.modes |= modes
                            changes["added %s to" % class_name] += 1
                        elif is_avail == 0 and modes.issubset(link.modes):
                            link.modes -= modes
                            changes["removed %s from" % class_name] += 1
            report.append("<div style='margin-left:20px'><ul>")
            for x in changes.items():
                report.append("<li>%s %s links</li>" % x)
            report.append("</div></ul>")
        scenario.publish_network(network)

        title = "Apply global class availabilities by faclity name for period %s" % period
        log_report = _m.PageBuilder(title=title)
        for item in report:
            log_report.add_html(item)
        _m.logbook_write(title, log_report.render())

    @_m.method(return_type=str)
    def get_link_attributes(self):
        export_utils = _m.Modeller().module("inro.emme.utility.export_utilities")
        return export_utils.get_link_attributes(_m.Modeller().scenario)

    def sql_select_scenario(self, year, iteration, sample, path, dbtime):  # YMA, 1/24/2019
        """Return scenario_id from [dimension].[scenario] given path"""

        import datetime

        sql_con = pyodbc.connect(driver='{SQL Server}',
                                 server='sql2014a8',
                                 database='abm_2',
                                 trusted_connection='yes')

        # dbtime = dbtime + datetime.timedelta(days=0)

        df = pd.read_sql_query(
            sql=("SELECT [scenario_id] "
                 "FROM [dimension].[scenario]"
                 "WHERE [year] = ? AND [iteration] = ? AND [sample_rate]= ? AND [path] Like ('%' + ? + '%') AND [date_loaded] > ? "),
            con=sql_con,
            params=[year, iteration, sample, path, dbtime]
        )

        if len(df) > 0:
            return (df.iloc[len(df) - 1]['scenario_id'])
        else:
            return 0

    def sql_check_load_request(self, year, path, user, ldtime):  # YMA, 1/24/2019
        """Return information from [data_load].[load_request] given path,username,and requested time"""

        import datetime

        t0 = ldtime + datetime.timedelta(minutes=-1)
        t1 = t0 + datetime.timedelta(minutes=30)

        sql_con = pyodbc.connect(driver='{SQL Server}',
                                 server='sql2014a8',
                                 database='abm_2',
                                 trusted_connection='yes')

        df = pd.read_sql_query(
            sql=(
                "SELECT [load_request_id],[year],[name],[path],[iteration],[sample_rate],[abm_version],[user_name],[date_requested],[loading],[loading_failed],[scenario_id] "
                "FROM [data_load].[load_request] "
                "WHERE [year] = ?  AND [path] LIKE ('%' + ? + '%') AND [user_name] LIKE ('%' + ? + '%') AND [date_requested] >= ? AND [date_requested] <= ?  "),
            con=sql_con,
            params=[year, path, user, t0, t1]
        )

        if len(df) > 0:
            return "You have successfully made the loading request, but the loading to the database failed. \r\nThe information is below. \r\n\r\n" + df.to_string()
        else:
            return "The data load request was not successfully made, please double check the [data_load].[load_request] table to confirm."

    def get_scenario_id(self, scenario_guid, scenario_name, prod_env):
        path = _join(self._path, "bin", "GetScenarioId.bat")
        err_file_ref, err_file_path = _tempfile.mkstemp(suffix='.log')
        err_file = os.fdopen(err_file_ref, "w")
        try:
            output = _subprocess.check_output(" ".join([path, '"' + scenario_guid + '"', '"' + scenario_name + '"', '"' + prod_env + '"']), stderr=err_file, cwd=self._path, shell=True)
            scenario_id = int(output.splitlines()[4])
            _m.logbook_write("Got new scenario_id: %s" % (scenario_id))
            err_file.close()
            return True, scenario_id
        except Exception as e:
            report = _m.PageBuilder(title="Error getting new scenario_id")
            self.add_html(report, 'Error:<br><br><div class="preformat">%s</div>' % e)
            err_file.close()
            with open(err_file_path, 'r') as f:
                error_msg = f.read()
            os.remove(err_file_path)
            if error_msg:
                self.add_html(report, 'Error message(s):<br><br><div class="preformat">%s</div>' % error_msg)
            try:
                # No raise on writing report error
                # due to observed issue with runs generating reports which cause
                # errors when logged
                _m.logbook_write("Error getting new scenario_id", report.render())
            except Exception as error:
                print(_time.strftime("%Y-%M-%d %H:%m:%S"))
                print("Error writing scenario_id report to logbook")
                print(error)
                print(_traceback.format_exc(error))
                if self._log_level == "DISABLE_ON_ERROR":
                    _m.logbook_level(_m.LogbookLevel.NONE)
            return False, -1

    def write_metadata(self, main_directory, scenario_title, select_link, username, scenarioYear, sample_rate, prod_env, props):
        '''Write YAML file containing scenario guid and other scenario info to output folder for writing to datalake'''
        datalake_metadata_dict = {
            "main_directory" : main_directory.encode('utf-8')
            ,"scenario_guid" : uuid.uuid4().hex
            ,'scenario_guid_created_at' : datetime.datetime.now()
            ,"scenario_title" : scenario_title.encode('utf-8')
            ,"scenario_year": scenarioYear
            ,"select_link" : select_link.encode('utf-8')
            ,"username" : username.encode('utf-8')
            ,"properties_path" : self.properties_path
            ,"sample_rate" : ",".join(map(str, sample_rate))
            ,"environment" : prod_env
            ,"network_path" : props["network"]
            ,"landuse_path" : props["landuse"]
            ,"release_path" : props["release"]
        }
        _m.logbook_write("Created new scenario_guid: %s" % (datalake_metadata_dict['scenario_guid']))
        got_id, scenario_id = self.get_scenario_id(datalake_metadata_dict['scenario_guid'], scenario_title, prod_env)
        if got_id:
            datalake_metadata_dict['scenario_id'] = scenario_id
        datalake_metadata_path = os.path.join(self._path,'output','datalake_metadata.yaml')
        with open(datalake_metadata_path, 'w') as file:
            yaml.safe_dump(datalake_metadata_dict, file, default_flow_style=False)
    
    def set_sample_rate(self, settings_path, sample):
        with open(settings_path, 'r') as file:
            settings = file.readlines()
        for index, line in enumerate(settings):
            pos = line.find("households_sample_size:")
            if pos != -1:
                settings[index] = line[:pos] + "households_sample_size: " + str(sample) + "\n"
        with open(settings_path, 'w') as file:
            file.writelines(settings)


    # def update_metadata_iteration(self, main_directory, msa_iteration):
    #     """update iteration value in metadata YAML"""
    #     datalake_metadata_path = os.path.join(main_directory,'output','datalake_metadata.yaml')
    #     with open(datalake_metadata_path, 'r') as file:
    #         datalake_metadata_dict = yaml.safe_load(file)
    #     datalake_metadata_dict['current_iteration'] = msa_iteration
    #     with open(datalake_metadata_path, 'w') as file:
    #         yaml.dump(datalake_metadata_dict, file, default_flow_style=False)
    #     _m.logbook_write("Updated Iteration in datalake_metadata.yaml file")

    def add_html(self, report, html):
        try:
            report.add_html(html)
        except Exception:
            fix_html = html.replace(r'\U', r'/U').replace(r'\u', r'/u')
            report.add_html(fix_html)

'''
    def send_notification(self,str_message,user):      # YMA, 1/24/2019, not working on server
        """automate to send email notification if load request or loading failed"""

        import win32com.client as win32

        outlook = win32.Dispatch('outlook.application')
        Msg = outlook.CreateItem(0)
        Msg.To = user + '@sandag.org'
        Msg.CC = 'yma@sandag.org'
        Msg.Subject = 'Loading Scenario to Database Failed'
        Msg.body = str_message + '\r\n' + '\r\n' + 'This email alert is auto generated.\r\n' +  'Please do not respond.\r\n'
        Msg.send'''

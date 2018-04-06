#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/master_run.py                                                   ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
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


import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import traceback as _traceback
import glob as _glob
import subprocess as _subprocess
import ctypes as _ctypes
import json as _json
import shutil as _shutil
import tempfile as _tempfile
from copy import deepcopy as _copy
import time as _time
import socket as _socket
import sys
import os

_join = os.path.join
_dir = os.path.dirname


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")
props_utils = _m.Modeller().module("sandag.utilities.properties")


class MasterRun(props_utils.PropertiesSetter, _m.Tool(), gen_utils.Snapshot):
    main_directory = _m.Attribute(unicode)
    scenario_id = _m.Attribute(int)
    scenario_title = _m.Attribute(unicode)
    emmebank_title = _m.Attribute(unicode)
    num_processors = _m.Attribute(str)
    select_link = _m.Attribute(unicode)
    username = _m.Attribute(unicode)
    password = _m.Attribute(unicode)

    properties_path = _m.Attribute(unicode)

    tool_run_msg = ""

    def __init__(self):
        super(MasterRun, self).__init__()
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.main_directory = _dir(project_dir)
        self.properties_path = _join(
            _dir(project_dir), "conf", "sandag_abm.properties")
        self.scenario_id = 100
        self.scenario_title = ""
        self.emmebank_title = ""
        self.num_processors = "MAX-1"
        self.select_link = '[]'
        self.attributes = ["main_directory", "scenario_id", "scenario_title", "emmebank_title", "num_processors", "select_link"]

    def page(self):
        self.load_properties()
        pb = _m.ToolPageBuilder(self)
        pb.title = "Master run ABM"
        pb.description = """Runs the SANDAG ABM, assignments, and other demand model tools."""
        pb.branding_text = "- SANDAG - Model"
        tool_proxy_tag = pb.tool_proxy_tag

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory','directory',
                           title='Select main ABM directory', note='')
        pb.add_text_box('scenario_id', title="Scenario ID:")
        pb.add_text_box('scenario_title', title="Scenario title:", size=80)
        pb.add_text_box('emmebank_title', title="Emmebank title:", size=60)
        dem_utils.add_select_processors("num_processors", pb, self)

        # optional username and password input for distributed assignment
        pb.add_html('''
<div class="t_element">
    <div class="t_local_title">Credentials for remote run</div>
    <div>
        <strong>Username:</strong>
        <input type="text" id="username" size="20" class="-inro-modeller"
                data-ref="parent.%(tool_proxy_tag)s.username"></input>
        <strong>Password:</strong>
        <input type="password" size="20" id="password" class="-inro-modeller"
                data-ref="parent.%(tool_proxy_tag)s.password"></input>
    </div>
    <div class="t_after_widget">
    Note: required for running distributed traffic assignments using PsExec. 
    Not used in single-node mode. <br>
    Distributed / single node modes are configured in "config/server-config.csv".
    </div>
</div>''' % {"tool_proxy_tag": tool_proxy_tag})

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
        });
   });
</script>""" % {"tool_proxy_tag": tool_proxy_tag})

        traffic_assign  = _m.Modeller().tool("sandag.assignment.traffic_assignment")
        traffic_assign._add_select_link_interface(pb)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self.save_properties()
            self(self.main_directory, self.scenario_id, self.scenario_title, self.emmebank_title,
                 self.num_processors, self.select_link, username=self.username, password=self.password)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    @_m.logbook_trace("Master run model", save_arguments=True)
    def __call__(self, main_directory, scenario_id, scenario_title, emmebank_title, num_processors, 
                 select_link=None, periods=["EA", "AM", "MD", "PM", "EV"], username=None, password=None):
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
        copy_scenario = modeller.tool("inro.emme.data.scenario.copy_scenario")
        import_network = modeller.tool("sandag.import.import_network")
        init_transit_db = modeller.tool("sandag.initialize.initialize_transit_database")
        init_matrices = modeller.tool("sandag.initialize.initialize_matrices")
        import_demand  = modeller.tool("sandag.import.import_seed_demand")
        build_transit_scen  = modeller.tool("sandag.assignment.build_transit_scenario")
        transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
        run_truck = modeller.tool("sandag.model.truck.run_truck_model")
        external_internal = modeller.tool("sandag.model.external_internal")
        external_external = modeller.tool("sandag.model.external_external")
        import_auto_demand = modeller.tool("sandag.import.import_auto_demand")
        import_transit_demand = modeller.tool("sandag.import.import_transit_demand")
        export_transit_skims = modeller.tool("sandag.export.export_transit_skims")
        export_network_data = modeller.tool("sandag.export.export_data_loader_network")
        export_matrix_data = modeller.tool("sandag.export.export_data_loader_matrices")
        export_tap_adjacent_lines = modeller.tool("sandag.export.export_tap_adjacent_lines")
        export_for_commercial_vehicle = modeller.tool("sandag.export.export_for_commercial_vehicle")

        utils = modeller.module('sandag.utilities.demand')
        load_properties = modeller.tool('sandag.utilities.properties')

        self.username = username
        self.password = password
        self._path = main_directory
        drive, path_no_drive = os.path.splitdrive(main_directory)
        path_forward_slash =  path_no_drive.replace("\\", "/")
        input_dir = _join(main_directory, "input")
        input_truck_dir = _join(main_directory, "input_truck")
        output_dir = _join(main_directory, "output")
        main_emmebank = _eb.Emmebank(_join(main_directory, "emme_project", "Database", "emmebank"))
        if emmebank_title:
            main_emmebank.title = emmebank_title
        external_zones = "1-12"

        props = load_properties(_join(main_directory, "conf", "sandag_abm.properties"))
        props.set_year_specific_properties(_join(main_directory, "input", "parametersByYears.csv"))
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
        startFromIteration =  props["RunModel.startFromIteration"]
        precision = props["RunModel.MatrixPrecision"]
        minSpaceOnC = props["RunModel.minSpaceOnC"]
        sample_rate = props["sample_rates"]
        end_iteration = len(sample_rate)

        period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

        skipInitialization = props["RunModel.skipInitialization"]
        deleteAllMatrices = props["RunModel.deleteAllMatrices"]
        skipCopyWarmupTripTables = props["RunModel.skipCopyWarmupTripTables"]
        skipCopyBikeLogsum = props["RunModel.skipCopyBikeLogsum"]
        skipCopyWalkImpedance= props["RunModel.skipCopyWalkImpedance"]
        skipWalkLogsums= props["RunModel.skipWalkLogsums"]
        skipBikeLogsums= props["RunModel.skipBikeLogsums"]
        skipBuildNetwork = props["RunModel.skipBuildNetwork"]
        skipHighwayAssignment = props["RunModel.skipHighwayAssignment"]
        skipTransitSkimming = props["RunModel.skipTransitSkimming"]
        skipCoreABM = props["RunModel.skipCoreABM"]
        skipOtherSimulateModel = props["RunModel.skipOtherSimulateModel"]
        skipCTM = props["RunModel.skipCTM"]
        skipEI = props["RunModel.skipEI"]
        skipExternal = props["RunModel.skipExternalExternal"]
        skipTruck = props["RunModel.skipTruck"]
        skipTripTableCreation = props["RunModel.skipTripTableCreation"]
        skipFinalHighwayAssignment = props["RunModel.skipFinalHighwayAssignment"]
        skipFinalTransitAssignment = props["RunModel.skipFinalTransitAssignment"]
        skipDataExport = props["RunModel.skipDataExport"]
        skipDataLoadRequest = props["RunModel.skipDataLoadRequest"]
        skipDeleteIntermediateFiles = props["RunModel.skipDeleteIntermediateFiles"]

        travel_modes = ["auto", "tran", "nmot", "othr"]
        core_abm_files = ["Trips*.omx", "InternalExternalTrips*.omx"]
        core_abm_files = [mode + name for name in core_abm_files for mode in travel_modes]
        smm_abm_files = ["AirportTrips*.omx", "CrossBorderTrips*.omx", "VisitorTrips*.omx"]
        smm_abm_files = [mode + name for name in smm_abm_files for mode in travel_modes]

        relative_gap = props["convergence"]
        max_assign_iterations = 1000
        mgra_lu_input_file = props["mgra.socec.file"]

        with _m.logbook_trace("Setup and initialization"):
            # Swap Server Configurations
            self.run_proc("serverswap.bat", [drive, path_no_drive, path_forward_slash], "Run ServerSwap")
            self.check_for_fatal(_join(main_directory, "logFiles", "serverswap.log"), 
                "ServerSwap failed! Open logFiles/serverswap.log for details.")
            self.check_free_space(minSpaceOnC)
            self.run_proc("checkAtTransitNetworkConsistency.cmd", [drive, path_forward_slash],
                "Checking if AT and Transit Networks are consistent")
            self.check_for_fatal(_join(main_directory, "logFiles", "AtTransitCheck_event.log"), 
                "AT and Transit network consistency checking failed! Open AtTransitCheck_event.log for details.")

            if not skipWalkLogsums:
                self.run_proc("runSandagWalkLogsums.cmd", [drive, path_forward_slash],
                    "Walk - create AT logsums and impedances")
            if not skipCopyWalkImpedance:
                self.copy_files(["walkMgraEquivMinutes.csv", "walkMgraTapEquivMinutes.csv"], 
                                input_dir, output_dir)
            if not skipBikeLogsums:
                self.run_proc("runSandagBikeLogsums.cmd",  [drive, path_forward_slash], 
                    "Bike - create AT logsums and impedances")
            if not skipCopyBikeLogsum:
                self.copy_files(["bikeMgraLogsum.csv", "bikeTazLogsum.csv"], input_dir, output_dir)

            if not skipBuildNetwork:
                base_scenario = import_network(
                    source=input_dir,
                    merged_scenario_id=scenario_id, 
                    title=scenario_title,
                    data_table_name=scenarioYear,
                    overwrite=True,
                    emmebank=main_emmebank)
                export_tap_adjacent_lines(_join(output_dir, "tapLines.csv"), base_scenario)
                # initialize per time-period scenarios
                for number, period in period_ids:
                    title = "%s - %s assign" % (base_scenario.title, period)
                    copy_scenario(base_scenario, number, title, overwrite=True)
            else:
                base_scenario = main_emmebank.scenario(scenario_id)

            if not skipInitialization:
                # initialize traffic demand, skims, truck, CV, EI, EE matrices
                traffic_components = [
                    "traffic_demand", "traffic_skims",
                    "truck_model", "commercial_vehicle_model",
                    "external_internal_model", "external_external_model"]
                init_matrices(traffic_components, periods, base_scenario, deleteAllMatrices)
                # import seed auto demand and seed truck demand
                transit_scenario = init_transit_db(base_scenario)
                transit_emmebank = transit_scenario.emmebank
                transit_components = ["transit_demand", "transit_skims"]
                init_matrices(transit_components, periods, transit_scenario, deleteAllMatrices)
            else:
                transit_emmebank = _eb.Emmebank(_join(main_directory, "emme_project", "Database_transit", "emmebank"))
                transit_scenario = transit_emmebank.scenario(base_scenario.number)

            if not skipCopyWarmupTripTables:
                for period in periods:
                    omx_file = _join(input_dir, "trip_%s.omx" % period)
                    import_demand(omx_file, "AUTO", period, base_scenario)
                    import_demand(omx_file, "TRUCK", period, base_scenario, convert_truck_to_pce=True)

        # Note: iteration indexes from 0, msa_iteration indexes from 1
        for iteration in range(startFromIteration - 1, end_iteration):
            msa_iteration = iteration + 1
            with _m.logbook_trace("Iteration %s" % msa_iteration):
                if not skipCoreABM[iteration] or not skipOtherSimulateModel[iteration]:
                    self.run_proc("runMtxMgr.cmd", [drive, drive + path_no_drive], "Start matrix manager")
                    self.run_proc("runDriver.cmd", [drive, drive + path_no_drive], "Start JPPF Driver")
                    self.run_proc("StartHHAndNodes.cmd", [drive, path_no_drive], 
                                  "Start HH Manager, JPPF Driver, and nodes")

                if not skipHighwayAssignment[iteration]:
                    # run traffic assignment
                    # export traffic skims
                    with _m.logbook_trace("Traffic assignment and skims"):
                        self.run_traffic_assignments(
                            base_scenario, period_ids, msa_iteration, relative_gap, 
                            max_assign_iterations, num_processors)
                    self.run_proc("CreateD2TAccessFile.bat", [drive, path_forward_slash],
                                  "Create drive to transit access file", capture_output=True)

                if not skipTransitSkimming[iteration]:
                    # run transit assignment
                    # export transit skims
                    with _m.logbook_trace("Transit assignments and skims"):
                        for number, period in period_ids:
                            src_period_scenario = main_emmebank.scenario(number)
                            transit_assign_scen = build_transit_scen(
                                period=period, base_scenario=src_period_scenario, 
                                transit_emmebank=transit_emmebank, 
                                scenario_id=src_period_scenario.id, 
                                scenario_title="%s %s transit assign" % (base_scenario.title, period),
                                data_table_name=scenarioYear, overwrite=True)
                            transit_assign(period, transit_assign_scen, data_table_name=scenarioYear,
                                           skims_only=True, num_processors=num_processors)

                        omx_file = _join(output_dir, "transit_skims.omx")   
                        export_transit_skims(omx_file, periods, transit_scenario)

                # For each step move trip matrices so run will stop if ctramp model
                # doesn't produced csv/omx files for assignment
                # also needed as CT-RAMP does not overwrite existing files
                if not skipCoreABM[iteration]:
                    self.remove_prev_iter_files(core_abm_files, output_dir, iteration)
                    self.run_proc(
                        "runSandagAbm_SDRM.cmd", 
                        [drive, drive + path_forward_slash, sample_rate[iteration], msa_iteration],
                        "Java-Run CT-RAMP", capture_output=True)
                if not skipOtherSimulateModel[iteration]:
                    self.remove_prev_iter_files(smm_abm_files, output_dir, iteration)
                    self.run_proc(
                        "runSandagAbm_SMM.cmd", 
                        [drive, drive + path_forward_slash, sample_rate[iteration], msa_iteration],
                        "Java-Run airport model, visitor model, cross-border model", capture_output=True)
                        
                if not skipCTM[iteration]:
                    export_for_commercial_vehicle(output_dir, base_scenario)
                    self.run_proc(
                        "cvm.bat",
                        [drive, path_no_drive, path_forward_slash, 1.0, mgra_lu_input_file, "tazcentroids_cvm.csv"],
                        "Commercial vehicle model", capture_output=True)
                if msa_iteration == startFromIteration:
                    external_zones = "1-12"
                    if not skipTruck[iteration]:
                        # run truck model (generate truck trips)
                        run_truck(True, input_dir, input_truck_dir, num_processors, base_scenario)
                    # run EI model "US to SD External Trip Model"
                    if not skipEI[iteration]:
                        external_internal(input_dir, base_scenario)
                    # run EE model
                    if not skipExternal[iteration]:
                        external_external(input_dir, external_zones, base_scenario)

                # import demand from all sub-market models from CT-RAMP and
                #       add CV trips to auto demand
                #       add EE and EI trips to auto demand
                if not skipTripTableCreation[iteration]:
                    import_auto_demand(output_dir, external_zones, num_processors, base_scenario)

        msa_iteration = len(sample_rate)
        if not skipFinalHighwayAssignment:
            with _m.logbook_trace("Final traffic assignments"):
                self.run_traffic_assignments(
                    base_scenario, period_ids, msa_iteration, relative_gap, max_assign_iterations, 
                    num_processors, select_link)

        if not skipFinalTransitAssignment:
            import_transit_demand(output_dir, transit_scenario)
            with _m.logbook_trace("Final transit assignments"):
                for number, period in period_ids:
                    src_period_scenario = main_emmebank.scenario(number)
                    transit_assign_scen = build_transit_scen(
                        period=period, base_scenario=src_period_scenario, 
                        transit_emmebank=transit_emmebank, scenario_id=src_period_scenario.id, 
                        scenario_title="%s - %s transit assign" % (base_scenario.title, period), 
                        data_table_name=scenarioYear, overwrite=True)
                    transit_assign(period, transit_assign_scen, data_table_name=scenarioYear,
                                   num_processors=num_processors)
                omx_file = _join(output_dir, "transit_skims.omx")
                export_transit_skims(omx_file, periods, transit_scenario, big_to_zero=True)

        if not skipDataExport:
            export_network_data(main_directory, scenario_id, main_emmebank, transit_emmebank, num_processors)
            export_matrix_data(output_dir, base_scenario, transit_scenario)
            # export core ABM data
            self.run_proc("DataExporter.bat", [drive, path_no_drive], "Export core ABM data", 
                          capture_output=True)
        if not skipDataLoadRequest:
            self.run_proc("DataLoadRequest.bat", 
                [drive + path_no_drive, end_iteration, sample_rate[end_iteration-1]], 
                "Data load request")

        # delete trip table files in iteration sub folder if model finishes without crashing
        if not skipDeleteIntermediateFiles:
            for msa_iteration in range(startFromIteration, end_iteration + 1):
                self.delete_files(
                    ["auto*Trips*.omx", "tran*Trips*.omx", "nmot*.omx", "othr*.omx", "trip*.omx"],
                    _join(output_dir, "iter%s" % (msa_iteration)))

    def run_traffic_assignments(self, base_scenario, period_ids, msa_iteration, relative_gap, 
                                max_assign_iterations, num_processors, select_link=None):
        modeller = _m.Modeller()
        traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
        export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
        output_dir = _join(self._path, "output")
        main_emmebank = base_scenario.emmebank

        machine_name = _socket.gethostname().lower()
        with open(_join(self._path, "conf", "server-config.csv")) as f:
            columns = f.next().split(",")
            for line in f:
                values = dict(zip(columns, line.split(",")))        
                name = values["ServerName"].lower()
                if name == machine_name:
                    server_config = values
                    break
            else:
                _m.logbook_write("Warning: current machine name not found in "
                                 "conf\server-config.csv ServerName column")
                server_config = {"SNODE": "yes"}
        distributed = server_config["SNODE"] == "no"
        if distributed:
            scen_map = dict((p, main_emmebank.scenario(n)) for n,p in period_ids)
            input_args = {
                "msa_iteration": msa_iteration,
                "relative_gap": relative_gap,
                "max_assign_iterations": max_assign_iterations,
                "select_link": select_link
            }

            periods_node1 = ["PM", "MD"]
            input_args["num_processors"] = server_config["THREADN1"], 
            database_path1, skim_names1 = self.setup_remote_database(
                [scen_map[p] for p in periods_node1], periods_node1, 1, msa_iteration)
            self.start_assignments(
                server_config["NODE1"], database_path1, periods_node1, scen_map, input_args)

            periods_node2 = ["AM"]
            input_args["num_processors"] = server_config["THREADN2"]
            database_path2, skim_names2 = self.setup_remote_database(
                [scen_map[p] for p in periods_node2], periods_node2, 2, msa_iteration)
            self.start_assignments(
                server_config["NODE2"], database_path2, periods_node2, scen_map, input_args)

            try:
                # run assignments locally
                periods_local = ["EA", "EV"]
                for period in periods_local:
                    local_scenario = scen_map[period]
                    traffic_assign(period, msa_iteration, relative_gap, max_assign_iterations, 
                                   num_processors, local_scenario, select_link)
                    omx_file = _join(output_dir, "traffic_skims_%s.omx" % period)
                    export_traffic_skims(local_period, omx_file, base_scenario)
                scenarios = {
                    database_path1: [scen_map[p] for p in periods_node1], 
                    database_path2: [scen_map[p] for p in periods_node2]
                }
                skim_names = {
                    database_path1: skim_names1, database_path2: skim_names2
                }
                self.wait_and_copy([database_path1, database_path2], scenarios, skim_names)
            except:
                # Note: this will kill ALL python processes - not suitable if servers are being
                # used for other tasks
                _subprocess.call("taskkill /F /T /S \\\\%s /IM python.exe" % server_config["NODE1"])
                _subprocess.call("taskkill /F /T /S \\\\%s /IM python.exe" % server_config["NODE2"])
                raise
        else:
            for number, period in period_ids:
                period_scenario = main_emmebank.scenario(number)
                traffic_assign(period, msa_iteration, relative_gap, max_assign_iterations, 
                               num_processors, period_scenario, select_link)
                omx_file = _join(output_dir, "traffic_skims_%s.omx" % period)
                export_traffic_skims(period, omx_file, base_scenario)

    def run_proc(self, name, arguments, log_message, capture_output=False):
        path = _join(self._path, "bin", name)
        if not os.path.exists(path):
            raise Exception("No command / batch file '%s'" % path)
        command = path + " " + " ".join([str(x) for x in arguments])
        attrs = {"command": command, "name": name, "arguments": arguments}
        with _m.logbook_trace(log_message, attributes=attrs):
            if capture_output:
                report = _m.PageBuilder(title="Process run %s" % name)
                report.add_html('Command:<br><br><div class="preformat">%s</div><br>' % command)
                # temporary file to capture output error messages generated by Java
                err_file_ref, err_file_path = _tempfile.mkstemp(suffix='.log')
                err_file = os.fdopen(err_file_ref, "w")
                try:
                    output = _subprocess.check_output(command, stderr=err_file, cwd=self._path, shell=True)
                    report.add_html('Output:<br><br><div class="preformat">%s</div>' % output)
                except _subprocess.CalledProcessError as error:
                    report.add_html('Output:<br><br><div class="preformat">%s</div>' % error.output)
                    raise
                finally:
                    err_file.close()
                    with open(err_file_path, 'r') as f:
                        error_msg = f.read()
                    os.remove(err_file_path)
                    if error_msg:
                        report.add_html('Error message(s):<br><br><div class="preformat">%s</div>' % error_msg)
                    _m.logbook_write("Process run %s report" % name, report.render())
            else:
                _subprocess.check_call(command, cwd=self._path, shell=True)

    @_m.logbook_trace("Check free space on C")
    def check_free_space(self, min_space):
        path = "c:\\"
        temp, total, free = _ctypes.c_ulonglong(), _ctypes.c_ulonglong(), _ctypes.c_ulonglong()
        if sys.version_info >= (3,) or isinstance(path, unicode):
            fun = _ctypes.windll.kernel32.GetDiskFreeSpaceExW
        else:
            fun = _ctypes.windll.kernel32.GetDiskFreeSpaceExA
        ret = fun(path, _ctypes.byref(temp), _ctypes.byref(total), _ctypes.byref(free))
        if ret == 0:
            raise _ctypes.WinError()
        total = total.value / (1024.0 ** 2)
        free = free.value / (1024.0 ** 2)
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
            if os.path.normpath(db.path) == os.path.normpath(unicode(emmebank)):
                db.open()
                return db
        return None

    def setup_remote_database(self, src_scenarios, periods, remote_num, msa_iteration):
        with _m.logbook_trace("Set up remote database #%s for %s" %(remote_num, ", ".join(periods))):
            init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
            create_function = _m.Modeller().tool("inro.emme.data.function.create_function")    
            src_emmebank = src_scenarios[0].emmebank
            remote_db_dir = _join(self._path, "emme_project", "Database_remote" + str(remote_num))
            if msa_iteration == 1:
                # Create and initialize database at first iteration, overwrite existing
                if os.path.exists(remote_db_dir):
                    _shutil.rmtree(remote_db_dir)
                    _time.sleep(1)
                os.mkdir(remote_db_dir)
                dimensions = src_emmebank.dimensions
                dimensions["scenarios"] = len(src_scenarios)
                remote_emmebank = _eb.create(_join(remote_db_dir, "emmebank"), dimensions)
                try:
                    remote_emmebank.title = src_emmebank.title
                    remote_emmebank.coord_unit_length = src_emmebank.coord_unit_length
                    remote_emmebank.unit_of_length = src_emmebank.unit_of_length
                    remote_emmebank.unit_of_cost = src_emmebank.unit_of_cost
                    remote_emmebank.unit_of_energy = src_emmebank.unit_of_energy
                    remote_emmebank.use_engineering_notation = src_emmebank.use_engineering_notation
                    remote_emmebank.node_number_digits = src_emmebank.node_number_digits

                    for src_scen in src_scenarios:
                        remote_scen = remote_emmebank.create_scenario(src_scen.id)
                        remote_scen.title = src_scen.title
                        for attr in sorted(src_scen.extra_attributes(), key=lambda x: x._id):
                            dst_attr = remote_scen.create_extra_attribute(
                                attr.type, attr.name, attr.default_value)
                            dst_attr.description = attr.description
                        for field in src_scen.network_fields():
                            remote_scen.create_network_field(
                                field.type, field.name, field.atype, field.description)
                        remote_scen.has_traffic_results = src_scen.has_traffic_results
                        remote_scen.has_transit_results = src_scen.has_transit_results
                        remote_scen.publish_network(src_scen.get_network())
                    for function in src_emmebank.functions():
                        create_function(function.id, function.expression, remote_emmebank)
                    init_matrices(["traffic_skims", "traffic_demand"], periods, remote_scen)
                finally:
                    remote_emmebank.dispose()

            src_scen = src_scenarios[0]
            with _m.logbook_trace("Copy demand matrices to remote database"):
                with _eb.Emmebank(_join(remote_db_dir, "emmebank")) as remote_emmebank:
                    demand_matrices = init_matrices.get_matrix_names("traffic_demand", periods, src_scen)
                    for matrix_name in demand_matrices:
                        matrix = remote_emmebank.matrix(matrix_name)
                        src_matrix = src_emmebank.matrix(matrix_name)
                        if matrix.type == "SCALAR":
                            matrix.data = src_matrix.data
                        else:
                            matrix.set_data(src_matrix.get_data(src_scen.id), src_scen.id)
            skim_matrices = init_matrices.get_matrix_names("traffic_skims", periods, src_scen)
            return remote_db_dir, skim_matrices

    def start_assignments(self, machine, database_path, periods, scenarios, assign_args):
        with _m.logbook_trace("Start remote process for traffic assignments %s" % (", ".join(periods))):
            assign_args["database_path"] = database_path
            end_path = _join(database_path, "finish")
            if os.path.exists(end_path):
                os.remove(end_path)
            for period in periods:
                assign_args["period_scenario"] = scenarios[period].id
                assign_args["period"] = period
                with open(_join(database_path, "start_%s.args" % period), 'w') as f:
                    _json.dump(assign_args, f, indent=4)
            script_dir = _join(self._path, "python")
            bin_dir = _join(self._path, "bin")
            args = [
                'start %s\\PsExec.exe' % bin_dir,
                '-c',
                '-f',
                '\\\\%s' % machine,
                '-u \%s' % self.username,
                '-p %s' % self.password,
                "-d",
                '%s\\emme_python.bat' % bin_dir, 
                "T:",
                self._path,
                '%s\\remote_run_traffic.py' % script_dir, 
                database_path,
            ]
            command = " ".join(args)
            p = _subprocess.Popen(command, shell=True)

    @_m.logbook_trace("Wait for remote assignments to complete and copy results")
    def wait_and_copy(self, database_dirs, scenarios, matrices):
        database_dirs = database_dirs[:]            
        wait = True
        while wait:
            _time.sleep(5)
            for path in database_dirs:
                end_path = _join(path, "finish")
                if os.path.exists(end_path):
                    database_dirs.remove(path)
                    _time.sleep(2)
                    self.check_for_fatal(
                        end_path, "error during remote run of traffic assignment. "
                                  "Check logFiles/traffic_assign_database_remote*.log")
                    self.copy_results(path, scenarios[path], matrices[path])
            if not database_dirs:
                wait = False

    @_m.logbook_trace("Copy skim results from remote database", save_arguments=True)
    def copy_results(self, database_path, scenarios, matrices):
        with _eb.Emmebank(_join(database_path, "emmebank")) as remote_emmebank :
            for dst_scen in scenarios:
                remote_scen = remote_emmebank.scenario(dst_scen.id)
                # Create extra attributes and network fields which do not exist
                for attr in sorted(remote_scen.extra_attributes(), key=lambda x: x._id):
                    if not dst_scen.extra_attribute(attr.name):
                        dst_attr = dst_scen.create_extra_attribute(
                            attr.type, attr.name, attr.default_value)
                        dst_attr.description = attr.description
                for field in remote_scen.network_fields():
                    if not dst_scen.network_field(field.type, field.name):
                        dst_scen.create_network_field(
                            field.type, field.name, field.atype, field.description)
                dst_scen.has_traffic_results = remote_scen.has_traffic_results
                dst_scen.has_transit_results = remote_scen.has_transit_results

                dst_scen.publish_network(remote_scen.get_network())

            dst_emmebank = dst_scen.emmebank
            scen_id = dst_scen.id
            for matrix_id in matrices:
                src_matrix = remote_emmebank.matrix(matrix_id)
                dst_matrix = dst_emmebank.matrix(matrix_id)
                dst_matrix.set_data(src_matrix.get_data(scen_id), scen_id)

    @_m.method(return_type=unicode)
    def get_link_attributes(self):
        export_utils = _m.Modeller().module("inro.emme.utility.export_utilities")
        return export_utils.get_link_attributes(_m.Modeller().scenario)

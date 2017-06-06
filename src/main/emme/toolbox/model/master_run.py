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

TOOLBOX_ORDER = 31


import inro.modeller as _m
import traceback as _traceback
import glob as _glob
import subprocess as _subprocess
import ctypes as _ctypes
import json as _json
import os
import sys

join = os.path.join


class MasterRun(_m.Tool()):
    main_directory = _m.Attribute(unicode)
    sample_rate = _m.Attribute(str)
    coaster_node_ids = _m.Attribute(str)
    scenario_id = _m.Attribute(int)
    sceanrio_desc = _m.Attribute(unicode)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.sample_rate = "[ 0.2, 0.5, 1.0 ]"
        self.coaster_node_ids = '{ "sorrento_valley": 6866 }'
        self.scenario_id = 100

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Master run ABM"
        pb.description = """  """
        pb.branding_text = "- SANDAG - Model"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory','directory',
                           title='Select main ABM directory', note='')
        pb.add_text_box('sample_rate', title="Sample rate by iteration:")
        pb.add_text_box('scenario_id', title="Scenario ID:")
        pb.add_text_box('sceanrio_desc', title="Scenario description:")

        pb.add_text_box('coaster_node_ids', title="Coaster node IDs for fares:",
            note="""2012: { "sorrento_valley": 6866 }, 2035: { "sorrento_valley": 15877 }""")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.main_directory, self.sample_rate, self.scenario_id, self.sceanrio_desc, self.coaster_node_ids)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __call__(self, main_directory, sample_rate, scenario_id, scenario_desc, coaster_node_ids):
        # Global variable from rsc macro:
        # path, inputDir, outputDir, inputTruckDir, 
        # mxzone, mxtap, mxext, mxlink, mxrte, scenarioYear

        modeller = _m.Modeller()
        copy_scenario = modeller.tool("inro.emme.data.scenario.copy_scenario")
        import_network = modeller.tool("sandag.import.import_network")
        initialize = modeller.tool("sandag.initialize_matrices")
        import_demand  = modeller.tool("sandag.import.import_matrices")
        traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
        transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
        run_truck = modeller.tool("sandag.model.truck.run_truck_model")
        run_commercial_veh = modeller.tool("sandag.model.commercial_vehicle.run_commercial_vehicle_model")
        external_internal = modeller.tool("sandag.model.external_internal")
        external_external = modeller.tool("sandag.model.external_external")
        add_demand = modeller.tool("sandag.model.add_aggregate_demand")
        export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
        export_transit_skims = modeller.tool("sandag.export.export_transit_skims")

        utils = modeller.module('sandag.utilities.demand')

        self._path = main_directory
        drive, path_no_drive = os.path.splitdrive(main_directory)
        path_forward_slash =  path_no_drive.replace("\\", "/")
        input_dir = join(main_directory, "input")
        input_truck_dir = join(main_directory, "input_truck")
        output_dir = join(main_directory, "output")
        main_emmebank = _eb.Emmebank(join(main_directory, "emme_project", "Database", "emmebank"))

        sample_rate = _json.loads(sample_rate)
        max_iterations = len(sample_rate)

        props = utils.Properties(join(main_directory, "conf", "sandag_abm.properties"))
        scenarioYear = props["scenarioYear"]
        skipCopyWarmupTripTables = props["RunModel.skipCopyWarmupTripTables"]
        skipCopyBikeLogsum = props["RunModel.skipCopyBikeLogsum"]
        skipCopyWalkImpedance= props["RunModel.skipCopyWalkImpedance"]
        skipWalkLogsums= props["RunModel.skipWalkLogsums"]
        skipBikeLogsums= props["RunModel.skipBikeLogsums"]
        skipBuildHwyNetwork = props["RunModel.skipBuildHwyNetwork"]
        skipBuildTransitNetwork= props["RunModel.skipBuildTransitNetwork"]
        startFromIteration =  pros["RunModel.startFromIteration"]
        skipHighwayAssignment = props["RunModel.skipHighwayAssignment"]
        skipHighwaySkimming = props["RunModel.skipHighwaySkimming"]
        skipTransitSkimming = props["RunModel.skipTransitSkimming"]
        skipCoreABM = props["RunModel.skipCoreABM"]
        skipOtherSimulateModel = props["RunModel.skipOtherSimulateModel"]
        skipCTM = props["RunModel.skipCTM"]
        skipEI = props["RunModel.skipEI"]
        skipTruck = props["RunModel.skipTruck"]
        skipTripTableCreation = props["RunModel.skipTripTableCreation"]
        skipFinalHighwayAssignment = props["RunModel.skipFinalHighwayAssignment"]
        skipFinalTransitAssignment = props["RunModel.skipFinalTransitAssignment"]
        skipFinalHighwaySkimming = props["RunModel.skipFinalHighwaySkimming"]
        skipFinalTransitSkimming = props["RunModel.skipFinalTransitSkimming"]
        skipLUZSkimCreation = props["RunModel.skipLUZSkimCreation"]
        skipDataExport = props["RunModel.skipDataExport"]
        skipDataLoadRequest = props["RunModel.skipDataLoadRequest"]
        skipDeleteIntermediateFiles = props["RunModel.skipDeleteIntermediateFiles"]
        precision = props["RunModel.MatrixPrecision"]
        minSpaceOnC = props["RunModel.minSpaceOnC"]

        with _m.logbook_trace("Setup and initialization"):
            # Swap Server Configurations
            self.run_proc("serverswap.bat", [drive, path_no_drive, path_forward_slash], "Run ServerSwap")
            self.check_for_fatal(join(main_directory, "logFiles", "serverswap.log"), 
                "ServerSwap failed! Open logFiles/serverswap.log for details.")
            # Update year specific properties
            self.run_proc("updateYearSpecificProps.bat", [drive, path_no_drive, path_forward_slash],
                "Update Year Specific Properties")
            self.check_free_space(minSpaceOnC)
            self.run_proc("checkAtTransitNetworkConsistency.cmd", [drive, path_forward_slash],
                "Checking if AT and Transit Networks are consistent")
            self.check_for_fatal(join(main_directory, "logFiles", "AtTransitCheck_event.log"), 
                "AT and Transit network consistency chekcing failed! Open AtTransitCheck_event.log for details.")end

            if not skipCopyBikeLogsum:
                self.copy_files(["bikeMgraLogsum.csv", "bikeTazLogsum.csv"], input_dir, output_dir)
            if not skipBikeLogsums:
                self.run_proc("runSandagBikeLogsums.cmd",  [drive, path_forward_slash], 
                    "Bike - create AT logsums and impedances")
            if not skipCopyWalkImpedance:
                self.copy_files(["walkMgraEquivMinutes.csv", "walkMgraTapEquivMinutes.csv"], 
                                input_dir, output_dir)
            if not skipWalkLogsums:
                self.run_proc("runSandagWalkLogsums.cmd", [drive, path_forward_slash],
                    "Walk - create AT logsums and impedances")            
            if not skipCopyWarmupTripTables:
                self.copy_files(["trip_EA.omx", "trip_AM.omx", "trip_MD.omx", "trip_PM.omx", "trip_EV.omx"],
                                input_dir, output_dir)

            self.set_active(main_emmebank)
            if not skipBuildHwyNetwork or not skipBuildTransitNetwork:
                import_network(
                    source=input_dir,
                    merged_scenario_id=scenario_id, 
                    description=scenario_desc,
                    coaster_node_ids=coaster_node_ids,
                    data_table_name=str(scenarioYear),
                    overwrite=True)
            base_scenairo = main_emmebank.scenario(scenairo_id)

            # TODO - skip initialization setting ?
            # initialize traffic demand, skims, truck, CV, EI, EE matrices
            periods = ["EA", "AM", "MD", "PM", "EV"]
            traffic_components = [
                "traffic_demand", "traffic_skims",
                "truck_model", "commercial_vehicle_model",
                "external_internal_model", "external_external_model"]
            initialize(traffic_components, periods, base_scenario)
            # TODO import auto demand ??
            # TODO import truck demand ??
            # for period in periods:
            #     omx_file = os.path.join(input_dir, "trip_%s.omx" % period)
            #     import_demand(omx_file, "AUTO", period, base_scenario)
            #     import_demand(omx_file, "TRUCK", period, base_scenario)

            transit_scenario = self.create_transit_database(base_scenario)
            initialize(["transit_demand", "transit_skims"], periods, transit_scenario)

        for iteration in range(startFromIteration, max_iterations + 1):
            with _m.logbook_trace("Iteration %s" % iteration):
                if not skipCoreABM[iteration] or not skipOtherSimulateModel[iteration]:
                    self.run_proc("runMtxMgr.cmd", [drive, path_no_drive], "Start matrix manager")
                    self.run_proc("runDriver.cmd", [drive, path_no_drive], "Start JPPF Driver")
                    self.run_proc("StartHHAndNodes.cmd", [drive, path_no_drive], 
                        "Start HH Manager, JPPF Driver, and nodes")

                if not skipHighwayAssignment[iteration] or not skipHighwaySkimming[iteration]:
                    # run traffic assignment
                    # export traffic skims
                    with _m.logbook_trace("Traffic assignment and skims"):
                        self.set_active(main_emmebank)
                        relative_gap = props["convergence"]
                        max_iterations = 1000  # TODO: double check max iterations - the hwyassign.rsc uses 1000 
                        for number, period in enumerate(periods, start=base_scenario.number + 10):
                            title = "%s- %s assign" % (base_scenario.title, period)
                            period_scenario = copy_scenario(base_scenario, number, title, overwrite=True)
                            traffic_assign(period, relative_gap, max_iterations, num_processors, period_scenario)
                            # TODO: previously separated "cars" and "trucks", may need to revert
                            omx_file = join(output_dir, "traffic_skims_%s.omx" % period)   
                            export_traffic_skims(period, omx_file, base_scenario)
                        self.run_proc("CreateD2TAccessFile.bat", [drive, path_forward_slash],
                            "Create drive to transit access file")

                if not skipTransitSkimming[iteration]:
                    # run transit assignment
                    # export transit skims
                    with _m.logbook_trace("Transit assignments and skims"):
                        self.set_active(transit_scenario.emmebank)                    
                        for number, period in enumerate(periods, start=base_scenario.number + 10):
                            src_period_scenario = main_emmebank.scenario(number)
                            timed_xfers = "%s_timed_xfer" % scenarioYear if period == "AM" else None
                            transit_assign(
                                period=period,
                                base_scenario=src_period_scenario,
                                emmebank=transit_scenario.emmebank, 
                                scenario_id=src_period_scenario.id, 
                                scenario_title="%s- %s transit assign" % (base_scenario.title, period),
                                skims_only=True,
                                timed_xfers_table=timed_xfers,
                                num_processors=num_processors,
                                overwrite=True)

                        omx_file = os.path.join(output_dir, "transit_skims.omx")   
                        export_transit_skims(omx_file, transit_scenario)

                # move some trip matrices so run will stop if ctramp model doesn't produced csv/mtx files for assignment
                # TODO: is this needed otherwise? there might be a better approach
                # TODO: review file names 
                if (iteration > startFromIteration):
                    self.move_files(
                        ["auto*.omx", "tran*.omx", "nmot*.omx", "othr*.omx", 
                         "trip*.omx", "*Trips.csv", "*airport_out.csv"],
                        output_dir, join(output_dir, "iter%s" % (iteration-1)))

                if not skipCoreABM[iteration]:
                    self.run_proc("runSandagAbm_SDRM.cmd", [drive, path_forward_slash, sample_rate[iteration], iteration],
                             "Java-Run CT-RAMP")
                if not skipOtherSimulateModel[iteration]:
                    self.run_proc("runSandagAbm_SMM.cmd", [drive, path_forward_slash, sample_rate[iteration], iteration],
                             "Java-Run airport model, visitor model, cross-border model")
                if not skipCTM[iteration]:
                    run_generation = (iteration == startFromIteration)
                    run_commercial_veh(run_generation, input_dir, base_scenario)
                if iteration == startFromIteration:
                    external_zones = "1-12"
                    if skipTruck[iteration]:
                        # run truck model (generate truck trips)
                        run_truck(run_generation=True, input_dir, input_truck_dir, base_scenario)
                    # run EI model "US to SD External Trip Model"
                    if not skipEI[iteration]:
                        external_internal(input_dir, base_scenario)
                    # run EE model
                    external_external(input_dir, external_zones, base_scenario)

                # add CV trips to auto demand
                # add EE and EI trips to auto demand
                add_demand(base_scenario)

        if not skipFinalHighwayAssignment or not skipFinalHighwaySkimming:
            self.set_active(main_emmebank)
            relative_gap = props["convergence"]
            max_iterations = 1000  # TODO: double check max iterations - the hwyassign.rsc uses 1000 ....
            for number, period in enumerate(periods, start=base_scenario.number + 10):
                title = "%s- %s assign" % (base_scenario.title, period)
                period_scenario = copy_scenario(base_scenario, number, title, overwrite=True)
                traffic_assign(period, relative_gap, max_iterations, num_processors, period_scenario)
                if not skipFinalHighwaySkimming:
                    #TODO: previously separated "cars" and "trucks", may need to revert
                    omx_file = os.path.join(output_directory, "traffic_skims_%s.omx" % period)
                    export_traffic_skims(period, omx_file, base_scenario)

        if not skipFinalTransitAssignment or not skipFinalTransitSkimming:
            self.set_active(transit_scenario.emmebank)
            for number, period in enumerate(periods, start=base_scenario.number + 10):
                src_period_scenario = main_emmebank.scenario(number)
                timed_xfers = "%s_timed_xfer" % scenarioYear if period == "AM" else None
                transit_assign(
                    period=period,
                    scenario_id=src_period_scenario.id, 
                    emmebank=transit_scenario.emmebank, 
                    base_scenario=src_period_scenario,
                    timed_xfers_table=timed_xfers,
                    scenario_title="%s- %s transit assign" % (base_scenario.title, period),
                    overwrite=True,
                    num_processors=num_processors)

            if not skipFinalTransitSkimming:
                omx_file = os.path.join(output_directory, "transit_skims.omx")   
                export_transit_skims(omx_file, transit_scenario)

        if not skipLUZSkimCreation:
            # TODO: recreate LUZskims ?
            pass
        if not skipDataExport:
            # TODO: create data export to CSV tool
            # export_data()
            # export core ABM data
            self.run_proc("DataExporter.bat", [drive, path_no_drive], "Export core ABM data")
        if not skipDataLoadRequest:
            self.run_proc("DataLoadRequest.bat", 
                [drive, path_no_drive, max_iterations, scenarioYear, sample_rate[max_iterations]], 
                "Data load request")

        # delete trip table files in iteration sub folder if model finishes without crashing
        if not skipDeleteIntermediateFiles:
            for iteration in range(startFromIteration, max_iterations + 1):
                self.delete_files(
                    ["auto*.omx", "tran*.omx", "nmot*.omx", "othr*.omx", "trip*.omx"],
                    join(output_dir, "iter%s" % (iteration-1)))

    def run_proc(self, name, arguments, log_message):
        path = join(self.path, bin, name)
        if not os.path.exists(path):
            raise Exception("No command / batch file '%s'" % path)
        command = path + " " + " ".join([str(x) for x in arguments])
        attrs = {"command": command, "name": name, "arguments": arguments}
        with _m.logbook_trace(log_message, attributes=attrs):
            _subprocess.check_call(command, stdout=_subprocess.PIPE, stderr=_subprocess.PIPE)

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

    def copy_files(self, file_names, from_dir, to_dir):
        with _m.logbook_trace("Copy files %s" % ", ".join(file_names)):
            for file_name in file_names:
                from_file = join(from_dir, file_name)
                shutil.copy(from_file, to_dir)

    def move_files(self, file_names, from_dir, to_dir):
        with _m.logbook_trace("Move files %s" % ", ".join(file_names)):
            if not os.path.exists(to_dir):
                os.mkdir(to_dir)
            for file_name in file_names:
                from_file = join(from_dir, file_name)
                all_files = _glob.glob(from_file)
                for path in all_files:
                    shutil.move(from_file, to_dir)

    def delete_files(self, file_names, directory):
        with _m.logbook_trace("Delete files %s" % ", ".join(file_names)):
            for file_name in file_names:
                from_file = join(directory, file_name)
                all_files = _glob.glob(from_file)
                for path in all_files:
                    os.path.remove(from_file, directory)

    def check_for_fatal(self, file_name, error_msg):
        with open(file_name, 'r') as f:
            for line in f:
                if "FATAL" in line:
                    raise Exception(error_msg)

    def create_transit_database(self, base_scenario):
        desktop = _m.Modeller().desktop
        data_explorer = desktop.data_explorer()
        project_dir = os.path.dirname(desktop.project.path)

        base_network = base_scenario.get_network()
        zone_network = _network.Network()
        for node in base_network.nodes():
            if node["@tap_id"] > 0:
                centroid = zone_network.create_node(node["@tap_id"], is_centroid=True)
                centroid.x = node.x
                centroid.y = node.y
        transit_db_dir = join(project_dir, "Database_transit")
        transit_db_path = join(transit_db_dir, "emmebank")
        if os.path.exists(transit_db_dir):
            if os.path.exists(transit_db_path):
                emmebank = _eb.Emmebank(transit_db_path)
                emmebank.dispose()
            shutil.rmtree(transit_db_dir)
        os.mkdir(transit_db_dir)

        dimensions = base_scenario.emmebank.dimensions
        dimensions["centroids"] = len(list(zone_network.centroids()))
        dimensions["scenarios"] = 7
        transit_eb = _eb.create(transit_db_path, dimensions)
        transit_eb.title = "Transit DB " + base_scenario.emmebank.title[:65]
        zone_scenario = transit_eb.create_scenario(1)
        zone_scenario.title = "Scenario with transit zones only"
        zone_scenario.publish_network(zone_network)

        transit_db_ref = self.set_active(transit_eb)
        if not transit_db_ref:
            transit_db_ref = data_explorer.add_database(transit_eb.path)
        return zone_scenario

    def set_active(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if os.path.normpath(db.path) == os.path.normpath(emmebank.path):
                db.open()
                return db
        return None

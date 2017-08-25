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

TOOLBOX_ORDER = 1


import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import traceback as _traceback
import glob as _glob
import subprocess as _subprocess
import ctypes as _ctypes
import json as _json
import shutil as _shutil
import os
import sys
import tempfile

join = os.path.join


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")
props_utils = _m.Modeller().module("sandag.utilities.properties")


class MasterRun(_m.Tool(), gen_utils.Snapshot, props_utils.PropertiesSetter):
    main_directory = _m.Attribute(unicode)
    scenario_id = _m.Attribute(int)
    scenario_title = _m.Attribute(unicode)
    emmebank_title = _m.Attribute(unicode)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.properties_path = os.path.join(
            os.path.dirname(project_dir), "conf", "sandag_abm.properties")
        self.scenario_id = 100
        self.scenario_title = ""
        self.emmebank_title = ""
        self.num_processors = "MAX-1"
        self.attributes = ["main_directory", "scenario_id", "scenario_title", "emmebank_title", "num_processors"]
        self._properties = None

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Master run ABM"
        pb.description = """Runs the SANDAG ABM, assignments, and other demand model tools."""
        pb.branding_text = "- SANDAG - Model"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory','directory',
                           title='Select main ABM directory', note='')
        pb.add_text_box('scenario_id', title="Scenario ID:")
        pb.add_text_box('scenario_title', title="Scenario title:", size=80)
        pb.add_text_box('emmebank_title', title="Emmebank title:", size=60)
        dem_utils.add_select_processors("num_processors", pb, self)
        
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        properties_path = os.path.join(
            os.path.dirname(project_dir), "conf", "sandag_abm.properties")
        if os.path.exists(properties_path):
            self.load_properties()
            
        # defined in properties utilities
        self.add_properties_interface(pb, disclosure=True)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self.save_properties()
            self(self.main_directory, self.scenario_id, self.scenario_title, self.emmebank_title,
                 self.num_processors)
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
    def __call__(self, main_directory, scenario_id, scenario_title, emmebank_title, num_processors):
        attributes = {
            "main_directory": main_directory, 
            "scenario_id": scenario_id, 
            "scenario_title": scenario_title,
            "emmebank_title": emmebank_title,
            "num_processors": num_processors
        }
        gen_utils.log_snapshot("Master run model", str(self), attributes)

        modeller = _m.Modeller()
        copy_scenario = modeller.tool("inro.emme.data.scenario.copy_scenario")
        import_network = modeller.tool("sandag.import.import_network")
        init_transit_db = modeller.tool("sandag.initialize.initialize_transit_database")
        init_matrices = modeller.tool("sandag.initialize.initialize_matrices")
        import_demand  = modeller.tool("sandag.import.import_seed_demand")
        traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
        build_transit_scen  = modeller.tool("sandag.assignment.build_transit_scenario")
        transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
        run_truck = modeller.tool("sandag.model.truck.run_truck_model")
        run_commercial_veh = modeller.tool("sandag.model.commercial_vehicle.run_commercial_vehicle_model")
        external_internal = modeller.tool("sandag.model.external_internal")
        external_external = modeller.tool("sandag.model.external_external")
        import_auto_demand = modeller.tool("sandag.import.import_auto_demand")
        import_transit_demand = modeller.tool("sandag.import.import_transit_demand")
        export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
        export_transit_skims = modeller.tool("sandag.export.export_transit_skims")
        export_network_data = modeller.tool("sandag.export.export_data_loader_network")
        export_matrix_data = modeller.tool("sandag.export.export_data_loader_matrices")

        utils = modeller.module('sandag.utilities.demand')
        load_properties = modeller.tool('sandag.utilities.properties')

        self._path = main_directory
        drive, path_no_drive = os.path.splitdrive(main_directory)
        path_forward_slash =  path_no_drive.replace("\\", "/")
        input_dir = join(main_directory, "input")
        input_truck_dir = join(main_directory, "input_truck")
        output_dir = join(main_directory, "output")
        main_emmebank = _eb.Emmebank(join(main_directory, "emme_project", "Database", "emmebank"))
        main_emmebank.title = emmebank_title
        external_zones = "1-12"

        props = load_properties(join(main_directory, "conf", "sandag_abm.properties"))

        scenarioYear = props["scenarioYear"]
        startFromIteration =  props["RunModel.startFromIteration"]
        precision = props["RunModel.MatrixPrecision"]
        minSpaceOnC = props["RunModel.minSpaceOnC"]
        sample_rate = props["sample_rates"]
        end_iteration = len(sample_rate)
        periods = ["EA", "AM", "MD", "PM", "EV"]
        period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

        skipInitialization = props["RunModel.skipInitialization"]
        skipCopyWarmupTripTables = props["RunModel.skipCopyWarmupTripTables"]
        skipCopyBikeLogsum = props["RunModel.skipCopyBikeLogsum"]
        skipCopyWalkImpedance= props["RunModel.skipCopyWalkImpedance"]
        skipWalkLogsums= props["RunModel.skipWalkLogsums"]
        skipBikeLogsums= props["RunModel.skipBikeLogsums"]
        skipBuildNetwork = props["RunModel.skipBuildNetwork"]
        skipHighwayAssignment = props["RunModel.skipHighwayAssignment"]
        skipHighwaySkimming = props["RunModel.skipHighwaySkimming"]
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
        skipFinalHighwaySkimming = props["RunModel.skipFinalHighwaySkimming"]
        skipFinalTransitSkimming = props["RunModel.skipFinalTransitSkimming"]
        skipLUZSkimCreation = props["RunModel.skipLUZSkimCreation"]
        skipDataExport = props["RunModel.skipDataExport"]
        skipDataLoadRequest = props["RunModel.skipDataLoadRequest"]
        skipDeleteIntermediateFiles = props["RunModel.skipDeleteIntermediateFiles"]

        with _m.logbook_trace("Setup and initialization"):
            # Swap Server Configurations
            self.run_proc("serverswap.bat", [drive, path_no_drive, path_forward_slash], "Run ServerSwap")
            self.check_for_fatal(join(main_directory, "logFiles", "serverswap.log"), 
                "ServerSwap failed! Open logFiles/serverswap.log for details.")
            # Update year specific properties
            # Not used in example abm_run, file not available
            # self.run_proc("updateYearSpecificProps.bat", [drive, path_no_drive, path_forward_slash],
                # "Update Year Specific Properties")
            self.check_free_space(minSpaceOnC)
            self.run_proc("checkAtTransitNetworkConsistency.cmd", [drive, path_forward_slash],
                "Checking if AT and Transit Networks are consistent")
            self.check_for_fatal(join(main_directory, "logFiles", "AtTransitCheck_event.log"), 
                "AT and Transit network consistency checking failed! Open AtTransitCheck_event.log for details.")

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

            self.set_active(main_emmebank)
            if not skipBuildNetwork:
                base_scenario = import_network(
                    source=input_dir,
                    merged_scenario_id=scenario_id, 
                    title=scenario_title,
                    data_table_name=str(scenarioYear),
                    overwrite=True,
                    emmebank=main_emmebank)
                # initialize per time-period scenarios
                for number, period in period_ids:
                    title = "%s- %s assign" % (base_scenario.title, period)
                    copy_scenario(base_scenario, number, title, overwrite=True)
            else:
                base_scenario = main_emmebank.scenario(scenario_id)
            

            if not skipInitialization:
                # initialize traffic demand, skims, truck, CV, EI, EE matrices
                traffic_components = [
                    "traffic_demand", "traffic_skims",
                    "truck_model", "commercial_vehicle_model",
                    "external_internal_model", "external_external_model"]
                init_matrices(traffic_components, periods, base_scenario)
                # import seed auto demand and seed truck demand
                transit_scenario = init_transit_db(base_scenario)
                transit_emmebank = transit_scenario.emmebank
                init_matrices(["transit_demand", "transit_skims"], periods, transit_scenario)
            else:
                transit_emmebank = _eb.Emmebank(join(main_directory, "emme_project", "Database_transit", "emmebank"))
                transit_scenario = transit_emmebank.scenario(base_scenario.number)
            
            if not skipCopyWarmupTripTables:
                for period in periods:
                    omx_file = os.path.join(input_dir, "trip_%s.omx" % period)
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
                        relative_gap = props["convergence"]
                        max_assign_iterations = 1000
                        for number, period in period_ids:
                            period_scenario = main_emmebank.scenario(number)
                            traffic_assign(
                                period, 
                                msa_iteration, 
                                relative_gap, 
                                max_assign_iterations, 
                                num_processors, 
                                period_scenario)
                            omx_file = join(output_dir, "traffic_skims_%s.omx" % period)   
                            export_traffic_skims(period, omx_file, base_scenario)
                    self.run_proc("CreateD2TAccessFile.bat", [drive, path_forward_slash],
                                  "Create drive to transit access file", capture_output=True)

                if not skipTransitSkimming[iteration]:
                    # run transit assignment
                    # export transit skims
                    with _m.logbook_trace("Transit assignments and skims"):
                        for number, period in period_ids:
                            src_period_scenario = main_emmebank.scenario(number)
                            timed_xfers = "%s_timed_xfer" % scenarioYear if period == "AM" else None
                            transit_assign_scen = build_transit_scen(
                                period=period, base_scenario=src_period_scenario, transit_emmebank=transit_emmebank, 
                                scenario_id=src_period_scenario.id, 
                                scenario_title="%s %s transit assign" % (base_scenario.title, period), 
                                timed_xfers_table=timed_xfers, overwrite=True)
                            transit_assign(
                                period=period, scenario=transit_assign_scen,
                                skims_only=True, num_processors=num_processors)

                        omx_file = os.path.join(output_dir, "transit_skims.omx")   
                        export_transit_skims(omx_file, transit_scenario)

                # move some trip matrices so run will stop if ctramp model
                # doesn't produced csv/omx (mtx) files for assignment
                # Note: is this needed otherwise? there might be a better approach
                if (msa_iteration > startFromIteration):
                    self.move_files(
                        ["auto*.mtx", "tran*.mtx", "nmot*.mtx", "othr*.mtx", 
                         "trip*.mtx", "*Trips.csv", "*airport_out.csv"],
                        output_dir, join(output_dir, "iter%s" % (iteration)))

                if not skipCoreABM[iteration]:
                    self.run_proc(
                        "runSandagAbm_SDRM.cmd", 
                        [drive, drive + path_forward_slash, sample_rate[iteration], msa_iteration],
                        "Java-Run CT-RAMP", capture_output=True)
                if not skipOtherSimulateModel[iteration]:
                    self.run_proc(
                        "runSandagAbm_SMM.cmd", 
                        [drive, drive + path_forward_slash, sample_rate[iteration], msa_iteration],
                        "Java-Run airport model, visitor model, cross-border model", capture_output=True)
                        
                if not skipCTM[iteration]:
                    run_generation = (msa_iteration == startFromIteration)
                    run_commercial_veh(run_generation, input_dir, base_scenario)
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
        if not skipFinalHighwayAssignment or not skipFinalHighwaySkimming:
            with _m.logbook_trace("Final traffic assignments"):
                relative_gap = props["convergence"]
                max_assign_iterations = 1000  
                for number, period in period_ids:
                    period_scenario = main_emmebank.scenario(number)
                    traffic_assign(period, msa_iteration, relative_gap, max_assign_iterations, num_processors, period_scenario)
                    if not skipFinalHighwaySkimming:
                        omx_file = os.path.join(output_dir, "traffic_skims_%s.omx" % period)
                        export_traffic_skims(period, omx_file, base_scenario)

        if not skipFinalTransitAssignment or not skipFinalTransitSkimming:
            import_transit_demand(output_dir, transit_scenario)
            with _m.logbook_trace("Final transit assignments"):
                for number, period in period_ids:
                    src_period_scenario = main_emmebank.scenario(number)
                    timed_xfers = "%s_timed_xfer" % scenarioYear if period == "AM" else None
                    transit_assign_scen = build_transit_scen(
                        period=period, base_scenario=src_period_scenario, transit_emmebank=transit_emmebank, 
                        scenario_id=src_period_scenario.id, scenario_title="%s- %s transit assign" % (base_scenario.title, period), 
                        timed_xfers_table=timed_xfers, overwrite=True)
                    transit_assign(
                        period=period, scenario=transit_assign_scen,
                        skims_only=False, num_processors=num_processors)
                if not skipFinalTransitSkimming:
                    omx_file = os.path.join(output_dir, "transit_skims.omx")   
                    export_transit_skims(omx_file, transit_scenario)

        if not skipDataExport:
            export_network_data(main_directory, scenario_id, main_emmebank, transit_emmebank, num_processors)
            export_matrix_data(output_dir, base_scenario, transit_scenario)
            # export core ABM data
            self.run_proc("DataExporter.bat", [drive, path_no_drive], "Export core ABM data", capture_output=True)
        if not skipDataLoadRequest:
            self.run_proc("DataLoadRequest.bat", 
                [drive, path_no_drive, end_iteration, scenarioYear, sample_rate[end_iteration]], 
                "Data load request")

        # delete trip table files in iteration sub folder if model finishes without crashing
        if not skipDeleteIntermediateFiles:
            for msa_iteration in range(startFromIteration, end_iteration + 1):
                self.delete_files(
                    ["auto*.mtx", "tran*.mtx", "nmot*.mtx", "othr*.mtx", "trip*.mtx"],
                    join(output_dir, "iter%s" % (msa_iteration)))

    def run_proc(self, name, arguments, log_message, capture_output=False):
        path = join(self._path, "bin", name)
        if not os.path.exists(path):
            raise Exception("No command / batch file '%s'" % path)
        command = path + " " + " ".join([str(x) for x in arguments])
        attrs = {"command": command, "name": name, "arguments": arguments}
        with _m.logbook_trace(log_message, attributes=attrs):
            if capture_output:
                report = _m.PageBuilder(title="Process run %s" % name)
                report.add_html('Command:<br><br><div class="preformat">%s</div><br>' % command)
                # temporary file to capture output error messages generated by Java
                err_file_ref, err_file_path = tempfile.mkstemp(suffix='.log')
                err_file = os.fdopen(err_file_ref, "w")
                try:
                    output = _subprocess.check_output(command, stderr=err_file, shell=True)
                    report.add_html('Output:<br><br><div class="preformat">%s</div>' % output)
                    _m.logbook_write("Process run %s report" % name, report.render())
                except _subprocess.CalledProcessError as error:
                    report.add_html('Output:<br><br><div class="preformat">%s</div>' % error.output)
                    raise
                finally:
                    err_file.close()
                    with open(err_file_path, 'r') as f:
                        error_msg = f.read()
                    os.remove(err_file_path)
                    if error_msg:
                        report.add_html('Error message:<br><br><div class="preformat">%s</div>' % error_msg)
                    _m.logbook_write("Process run %s report" % name, report.render())
            else:
                _subprocess.check_call(command, shell=True)

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

    def copy_files(self, file_names, from_dir, to_dir):
        with _m.logbook_trace("Copy files %s" % ", ".join(file_names)):
            for file_name in file_names:
                from_file = join(from_dir, file_name)
                _shutil.copy(from_file, to_dir)

    def move_files(self, file_names, from_dir, to_dir):
        try:
            with _m.logbook_trace("Move files %s" % ", ".join(file_names)):
                if not os.path.exists(to_dir):
                    os.mkdir(to_dir)
                for file_name in file_names:
                    from_file = join(from_dir, file_name)
                    all_files = _glob.glob(from_file)
                    for path in all_files:
                        dst_file = join(to_dir, os.path.basename(path))
                        if os.path.exists(dst_file):
                            os.remove(dst_file)
                        _shutil.move(path, to_dir)
        except:
            pass

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

    def set_active(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if os.path.normpath(db.path) == os.path.normpath(unicode(emmebank)):
                db.open()
                return db
        return None

##//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#////  model/run_emme_components.py                                         ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 32

#Steps:
#   import network 
#   initialize traffic demand, skims, truck, CV, EI, EE matrices
#   import auto demand
#   import truck demand
#   run traffic assignment
#   export traffic skims
    
#   create transit database
#   create transit scenarios
#   initialize transit demand and skims
#   import transit demand
#   run transit assignment
#   export transit skims
    
#   run truck model (generate truck trips)
#   run CV model
#   run EI model
#   run EE model
#   add CV trips to auto demand
#   add EE and EI trips to auto demand

import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.network as _network
import multiprocessing as _multiprocessing
import shutil
import os


class MasterRun(_m.Tool()):
    scenario_id = _m.Attribute(int)
    description = _m.Attribute(unicode)
    main_directory = _m.Attribute(unicode)
    network_source = _m.Attribute(unicode)
    num_processors = _m.Attribute(int)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self._max_processors = _multiprocessing.cpu_count()
        self.num_processors = "MAX-1"

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Run all Emme components"
        pb.description = """
        A test script to run the Emme assignments and model components.
        """
        pb.branding_text = "- SANDAG - Model"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_text_box("scenario_id", size=6, title="Base scenario ID:")
        pb.add_text_box("description", size=80, title="Scenario description:")
        pb.add_select_file('network_source','directory',
                           title='Select directory for network files')
        pb.add_select_file('main_directory','directory',
                           title='Select input directory')

        options = [("MAX-1", "Maximum available - 1"), ("MAX", "Maximum available")]
        options.extend([(n, "%s processors" % n) for n in range(1, self._max_processors + 1) ])
        pb.add_select("num_processors", options, title="Number of processors:")
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            emmebank = _m.Modeller().emmebank
            self(self.scenario_id, self.description, 
                self.network_source, self.main_directory, 
                self.num_processors, emmebank)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Master run", save_arguments=True)
    def __call__(self, scenario_id, description, 
                 network_source, main_directory, 
                 num_processors, main_emmebank):

        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        project = desktop.project
        project_path = os.path.dirname(project.path)

        input_directory = os.path.join(main_directory, "input")
        input_truck_directory = os.path.join(main_directory, "input_truck")
        output_directory = os.path.join(main_directory, "output")

        self.set_active(main_emmebank)
        
        modeller.refresh_tools()        
        utils = _m.Modeller().module('sandag.utilities.demand')
        props = utils.Properties(os.path.join(input_directory, "sandag_abm.properties"))

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

        import_network(
            source=network_source,
            merged_scenario_id=scenario_id, 
            description="2012 base scenario",
            coaster_node_ids="""{"sorrento_valley": 6866}""",
            data_table_name="2012",
            overwrite=True)
        base_scenario = main_emmebank.scenario(scenario_id)
        data_explorer.replace_primary_scenario(base_scenario)

        periods = ["EA", "AM", "MD", "PM", "EV"]
        initialize(["traffic_demand",
                    "traffic_skims",
                    "truck_model",
                    "external_internal_model",
                    "external_external_model",
                    "commercial_vehicle_model"], 
                   periods,
                   base_scenario)
        for period in periods:
            omx_file = os.path.join(input_directory, "trip_%s.omx" % period)
            import_demand(omx_file, "AUTO", period, base_scenario)
            import_demand(omx_file, "TRUCK", period, base_scenario)

        transit_scenario = self.create_transit_database(base_scenario)
        # TODO: the special transit scenario should be unnecessary with CT-Ramp output
        # TODO: initial import of transit demand is not needed in full run
        self.modify_zone_scenario(transit_scenario)
        data_explorer.replace_primary_scenario(transit_scenario)
        initialize(["transit_demand", "transit_skims"], periods, transit_scenario)
        for period in periods:
            omx_file = os.path.join(input_directory, "tranTotalTrips_%s.omx" % period)        
            import_demand(omx_file, "TRANSIT", period, transit_scenario)

        self.set_active(main_emmebank)

        traffic_assign._skim_classes_separately = True
        relative_gap = props["convergence"]
        max_iterations = 1000  # TODO: double check max iterations - the hwyassign.rsc uses 1000 ....
        for number, period in enumerate(periods, start=base_scenario.number + 10):
            title = "%s- %s assign" % (base_scenario.title, period)
            period_scenario = copy_scenario(base_scenario, number, title, overwrite=True)
            traffic_assign(period, relative_gap, max_iterations, num_processors, period_scenario)

            #TODO: previously separated "cars" and "trucks", may need to revert
            omx_file = os.path.join(output_directory, "traffic_skims_%s.omx" % period)   
            export_traffic_skims(period, omx_file, base_scenario)

        self.set_active(transit_scenario.emmebank)
        for number, period in enumerate(periods, start=base_scenario.number + 10):
            src_period_scenario = main_emmebank.scenario(number)
            transit_assign(
                period=period,
                scenario_id=src_period_scenario.id, 
                emmebank=transit_scenario.emmebank, 
                base_scenario=src_period_scenario,
                timed_xfers_table="2012_timed_xfer" if period == "AM" else None,
                scenario_title="%s- %s transit assign" % (base_scenario.title, period),
                overwrite=True, 
                num_processors=num_processors)

        omx_file = os.path.join(output_directory, "transit_skims.omx")   
        export_transit_skims(omx_file, transit_scenario)

        self.set_active(main_emmebank)
        run_generation = True
        external_zones = "1-12"
        run_truck(run_generation, input_directory, input_truck_directory, base_scenario)
        run_commercial_veh(run_generation, input_directory, base_scenario)
        external_internal(input_directory, base_scenario)
        external_external(input_directory, external_zones, base_scenario)
        add_demand(base_scenario)

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
        transit_db_dir = os.path.join(project_dir, "Database_transit")
        transit_db_path = os.path.join(transit_db_dir, "emmebank")
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
                
    def modify_zone_scenario(self, zone_scenario):
        # generate scenario with zones removed that do not exist in the OMX file for some reason
        emmebank = zone_scenario.emmebank
        network = zone_scenario.get_network()
        missing_zones = [
            7, 28, 37, 38, 42, 44, 54, 93, 145, 353, 728, 729, 737, 
            767, 820, 844, 1046, 1084, 1158, 1882, 2171, 2478
        ]
        for number in missing_zones:
            network.delete_node(number, cascade=True)
        zone_scenario.title = "Temp special scenario with 1744 zones"
        zone_scenario.publish_network(network)
        return

    def set_active(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if os.path.normpath(db.path) == os.path.normpath(emmebank.path):
                db.open()
                return db
        return None

#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// initialize_transit_databse.py                                         ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# 
# Coordinates the initialization of all matrices.
# The matrix names are listed for each of the model components / steps,
# and the matrix IDs are assigned consistently from the set of matrices.
# In each of the model steps the matrices are only referenced by name,
# never by ID.
#
#
# Inputs:
#    components: A list of the model components / steps for which to initialize matrices
#                One or more of "traffic_demand", "transit_demand", 
#                "traffic_skims", "transit_skims",  "external_internal_model", 
#                "external_external_model", "truck_model", "commercial_vehicle_model"
#    periods: A list of periods for which to initialize matrices, "EA", "AM", "MD", "PM", "EV"
#    scenario: scenario to use for reference zone system and the emmebank in which 
#              the matrices will be created. Defaults to the current primary scenario.
#
# Script example:
"""
    import os
    import inro.emme.database.emmebank as _eb
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    main_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    base_scenario = main_emmebank.scenario(100)
    initialize_transit_db = modeller.tool("sandag.initialize.initialize_transit_database")
    initialize_transit_db(base_scenario)
"""
TOOLBOX_ORDER = 8


import inro.modeller as _m
import inro.emme.network as _network
import inro.emme.database.emmebank as _eb
from inro.emme.desktop.exception import AddDatabaseError
import traceback as _traceback
import shutil as _shutil
import time
import os

join = os.path.join


gen_utils = _m.Modeller().module("sandag.utilities.general")


class InitializeTransitDatabase(_m.Tool(), gen_utils.Snapshot):

    base_scenario =  _m.Attribute(object)
    period = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.base_scenario = _m.Modeller().scenario
        self.period = _m.Attribute(str)
        self.attributes = ["base_scenario", "period"]

    def from_snapshot(self, snapshot):
        super(InitializeTransitDatabase, self).from_snapshot(snapshot)
        # custom from_snapshot to load scenario object
        self.base_scenario = _m.Modeller().emmebank.scenario(self.base_scenario)
        return self

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Initialize transit database"
        pb.description = """Create and setup database for transit assignments under 'Database_transit' directory. 
            Will overwrite an existing database."""
        pb.branding_text = "- SANDAG"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_scenario("base_scenario", 
            title="Base scenario:", note="Base traffic and transit scenario with TAZs.")
        
        options = [("EA","Early AM"),
                   ("AM","AM peak"),
                   ("MD","Mid-day"),
                   ("PM","PM peak"),
                   ("EV","Evening")]

        pb.add_select("period", options, title="Period:")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.base_scenario, self.period)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Initialize transit database', save_arguments=True)
    def __call__(self, base_scenario, period, add_database=True):
        attributes = {"base_scenario": base_scenario.id}
        gen_utils.log_snapshot("Initialize transit database", str(self), attributes)
        create_function = _m.Modeller().tool("inro.emme.data.function.create_function")
        build_transit_scen  = _m.Modeller().tool("sandag.assignment.build_transit_scenario")
        load_properties = _m.Modeller().tool('sandag.utilities.properties')

        base_eb = base_scenario.emmebank
        project_dir = os.path.dirname(os.path.dirname(base_eb.path))
        main_directory = os.path.dirname(project_dir)
        props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
        scenarioYear = props["scenarioYear"]

        transit_db_dir = join(project_dir, "Database_transit_" + period)
        transit_db_path = join(transit_db_dir, "emmebank")
        network = base_scenario.get_partial_network(["NODE"], include_attributes=True)
        #num_zones = sum([1 for n in network.nodes() if n["isZone"] > 0])
        dimensions = base_eb.dimensions
        #dimensions["centroids"] = num_zones
        dimensions["scenarios"] = 10
        if not os.path.exists(transit_db_dir):
            os.mkdir(transit_db_dir)
        if os.path.exists(transit_db_path):
            transit_eb = _eb.Emmebank(transit_db_path)
            for scenario in transit_eb.scenarios():
                transit_eb.delete_scenario(scenario.id)
            for function in transit_eb.functions():
                transit_eb.delete_function(function.id)
            if transit_eb.dimensions != dimensions:
                _eb.change_dimensions(transit_db_path, dimensions, keep_backup=False)
        else:
            transit_eb = _eb.create(transit_db_path, dimensions)

        transit_eb.title = base_eb.title[:65] + "-transit-" + period
        transit_eb.coord_unit_length = base_eb.coord_unit_length
        transit_eb.unit_of_length = base_eb.unit_of_length
        transit_eb.unit_of_cost = base_eb.unit_of_cost
        transit_eb.unit_of_energy = base_eb.unit_of_energy
        transit_eb.use_engineering_notation = base_eb.use_engineering_notation
        transit_eb.node_number_digits = base_eb.node_number_digits

        zone_scenario = build_transit_scen(
            period="AM", base_scenario=base_scenario, transit_emmebank=transit_eb, 
            scenario_id=base_scenario.id, scenario_title="%s transit zones" % (base_scenario.title), 
            data_table_name=scenarioYear, overwrite=True)
        for function in base_scenario.emmebank.functions():
            create_function(function.id, function.expression, transit_eb)
        if add_database:
            self.add_database(transit_eb)
        return zone_scenario

    def add_database(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if os.path.normpath(db.path) == os.path.normpath(emmebank.path):
                return 
        try:
            data_explorer.add_database(emmebank.path)
        except AddDatabaseError:
            pass  # database has already been added to the project

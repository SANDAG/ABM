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

TOOLBOX_ORDER = 112


import inro.modeller as _m
import inro.emme.network as _network
import inro.emme.database.emmebank as _eb
import traceback as _traceback
import shutil as _shutil
import time
import os

join = os.path.join


gen_utils = _m.Modeller().module("sandag.utilities.general")


class InitializeTransitDatabase(_m.Tool(), gen_utils.Snapshot):

    base_scenario =  _m.Attribute(_m.InstanceType)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.base_scenario = _m.Modeller().scenario
        self.attributes = ["base_scenario"]

    def from_snapshot(self, snapshot):
        super(InitializeTransitDatabase, self).from_snapshot(snapshot)
        # custom from_snapshot to load scenario object
        self.base_scenario = _m.Modeller().emmebank.scenario(self.base_scenario)
        return self

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Initialize transit database"
        pb.description = """Create and setup database for transit assignments under 'Database_transit' directory. 
            Will overwrite an existing database. The TAZs will be removed and TAP nodes converted to zones."""
        pb.branding_text = "- SANDAG"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_scenario("base_scenario", 
            title="Base scenario:", note="Base traffic and transit scenario with TAZs.")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.base_scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Initialize transit database', save_arguments=True)
    def __call__(self, base_scenario):
        attributes = {"base_scenario": base_scenario.id}
        gen_utils.log_snapshot("Initialize transit database", str(self), attributes)
        create_function = _m.Modeller().tool("inro.emme.data.function.create_function")
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
            _shutil.rmtree(transit_db_dir)
            time.sleep(10)  # wait 10 seconds - avoid potential race condition to remove the file in Windows
        os.mkdir(transit_db_dir)

        dimensions = base_scenario.emmebank.dimensions
        dimensions["centroids"] = len(list(zone_network.centroids()))
        dimensions["scenarios"] = 10
        transit_eb = _eb.create(transit_db_path, dimensions)
        transit_eb.title = "Transit DB " + base_scenario.emmebank.title[:65]
        zone_scenario = transit_eb.create_scenario(base_scenario.number)
        zone_scenario.title = "Scenario with transit zones only"
        zone_scenario.publish_network(zone_network)
        for function in base_scenario.emmebank.functions():
            create_function(function.id, function.expression, transit_eb)

        self.add_database(transit_eb)
        return zone_scenario

    def add_database(self, emmebank):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        data_explorer = desktop.data_explorer()
        for db in data_explorer.databases():
            if os.path.normpath(db.path) == os.path.normpath(emmebank.path):
                return 
        data_explorer.add_database(emmebank.path)

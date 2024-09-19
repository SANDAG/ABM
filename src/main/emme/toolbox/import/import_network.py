#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/import_network.py                                              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Imports the network from the input network files.
#
#
# Inputs:
#    source: path to the location of the input network geodatabase
#    traffic_scenario_id: optional scenario to store the imported network from the traffic files only
#    transit_scenario_id: optional scenario to store the imported network from the transit files only
#    merged_scenario_id: scenario to store the combined traffic and transit data from all network files
#    title: the title to use for the imported scenario
#    save_data_tables: if checked, create a data table for each reference file for viewing in the Emme Desktop
#    data_table_name: prefix to use to identify all data tables
#    overwrite: check to overwrite any existing data tables or scenarios with the same ID or name
#    emmebank: the Emme database in which to create the scenario. Default is the current open database
#    create_time_periods: if True (default), also create per-time period scenarios (required to run assignments)
#
# Files referenced:
#
#    *.gdb: A Geodatabase file with the network data for both highway and transit. The following tables are used
#       - TNED_HwyNet
#       - TNED_HwyNodes
#       - TNED_RailNet
#       - TNED_RailNodes
#       - Turns
#    The following files are also used (in the same directory as the *.gdb)
#
#    trrt.csv: header data for the transit lines
#    trlink.csv: sequence of links (routing) of transit lines
#    trstop.csv: stop data for the transit lines
#    mode5tod.csv: global (per-mode) transit cost and perception attributes
#    timexfer_<period>.csv (optional): table of timed transfer pairs of lines, by period
#    special_fares.txt (optional): table listing special fares in terms of boarding and incremental in-vehicle costs.
#    off_peak_toll_factors.csv (optional): factors to calculate the toll for EA, MD, and EV periods from the OP toll input for specified facilities
#    vehicle_class_toll_factors.csv (optional): factors to adjust the toll cost by facility name and class (DA, S2, S3, TRK_L, TRK_M, TRK_H)
#
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    source_file = os.path.join(main_directory, "input", "EMMEOutputs.gdb")
    title = "Base 2012 scenario"
    import_network = modeller.tool("sandag.import.import_network")
    import_network(source_file, merged_scenario_id=100, title=title,
        data_table_name="2012_base", overwrite=True)
"""


TOOLBOX_ORDER = 11


import inro.modeller as _m
import inro.emme.datatable as _dt
import inro.emme.network as _network
from inro.emme.core.exception import Error as _NetworkError

from itertools import izip as _izip
from collections import defaultdict as _defaultdict, OrderedDict
from contextlib import contextmanager as _context
import fiona as _fiona

from math import ceil as _ceiling
from math import floor as _floor
from copy import deepcopy as _copy
import numpy as _np
import heapq as _heapq
import pandas as pd

import traceback as _traceback
import os

_join = os.path.join
_dir = os.path.dirname


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

FILE_NAMES = {
    "FARES": "special_fares.txt",
    "TIMEXFER": "timexfer_%s.csv",
    "OFF_PEAK": "off_peak_toll_factors.csv",
    "VEHICLE_CLASS": "vehicle_class_toll_factors.csv",
    "MODE5TOD": "MODE5TOD.csv",
}


class ImportNetwork(_m.Tool(), gen_utils.Snapshot):

    source = _m.Attribute(unicode)
    scenario_id = _m.Attribute(int)
    overwrite = _m.Attribute(bool)
    title = _m.Attribute(unicode)
    save_data_tables = _m.Attribute(bool)
    data_table_name = _m.Attribute(unicode)
    create_time_periods = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self._log = []
        self._error = []
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.source = _join(_dir(project_dir), "input")
        self.overwrite = False
        self.title = ""
        self.data_table_name = ""
        self.create_time_periods = True
        self.attributes = [
            "source", "scenario_id", "overwrite", "title", "save_data_tables", "data_table_name", "create_time_periods"
        ]

    def page(self):
        if not self.data_table_name:
            try:
                load_properties = _m.Modeller().tool('sandag.utilities.properties')
                props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
                self.data_table_name = props["scenarioYear"]
            except:
                pass

        pb = _m.ToolPageBuilder(self)
        pb.title = "Import network"
        pb.description = """
        <div style="text-align:left">
            Create an Emme network from TNED geodatabase (*.gdb) and associated files.
            <br>
            <br>
            The following layers in the gdb are used:
            <ul>
                <li>TNED_HwyNet</li>
                <li>TNED_HwyNodes</li>
                <li>TNED_RailNet</li>
                <li>TNED_RailNodes</li>
                <li>Turns</li>
            </ul>
            The following files are also used (in the same directory as the *.gdb):
            <ul>
                <li>trrt.csv</li>
                <li>trlink.csv</li>
                <li>trstop.csv</li>
                <li>mode5tod.csv</li>
                <li>timexfer_<period>.csv (optional)</li>
                <li>special_fares.txt (optional)</li>
                <li>off_peak_toll_factors.csv (optional)</li>
                <li>vehicle_class_toll_factors.csv (optional)</li>
            </ul>
        </div>
        """
        pb.branding_text = "- SANDAG - Import"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file("source", window_type="directory", file_filter="",
                           title="Source gdb:",)

        pb.add_text_box("scenario_id", size=6, title="Scenario ID for imported network:")
        pb.add_text_box("title", size=80, title="Scenario title:")
        pb.add_checkbox("save_data_tables", title=" ", label="Save reference data tables of file data")
        pb.add_text_box("data_table_name", size=80, title="Name for data tables:",
            note="Prefix name to use for all saved data tables")
        pb.add_checkbox("overwrite", title=" ", label="Overwrite existing scenarios and data tables")
        pb.add_checkbox("create_time_periods", title=" ", label="Copy base scenario to all time periods and set modes (required for assignments)")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self.emmebank = _m.Modeller().emmebank
            with self.setup():
                self.execute()
            run_msg = "Network import complete"
            if self._error:
                run_msg += " with %s non-fatal errors. See logbook for details" % len(self._error)
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc())
            raise

    def __call__(self, source, scenario_id,
                 title="", save_data_tables=False, data_table_name="", overwrite=False,
                 emmebank=None, create_time_periods=True):

        self.source = source
        self.scenario_id = scenario_id
        self.title = title
        self.save_data_tables = save_data_tables
        self.data_table_name = data_table_name
        self.overwrite = overwrite
        if not emmebank:
            self.emmebank = _m.Modeller().emmebank
        else:
            self.emmebank = emmebank
        self.create_time_periods = create_time_periods

        with self.setup():
            self.execute()

        return self.emmebank.scenario(scenario_id)

    @_context
    def setup(self):
        self._log = []
        self._error = []
        fatal_error = False
        attributes = OrderedDict([
            ("self", str(self)),
            ("source", self.source),
            ("scenario_id", self.scenario_id),
            ("title", self.title),
            ("save_data_tables", self.save_data_tables),
            ("data_table_name", self.data_table_name),
            ("overwrite", self.overwrite),
            ("create_time_periods", self.create_time_periods)
        ])
        self._log = [{
            "content": attributes.items(),
            "type": "table", "header": ["name", "value"],
            "title": "Tool input values"
        }]
        with _m.logbook_trace("Import network", attributes=attributes) as trace:
            gen_utils.log_snapshot("Import network", str(self), attributes)
            load_properties = _m.Modeller().tool('sandag.utilities.properties')
            self._props = load_properties(_join(_dir(_dir(self.source)), "conf", "sandag_abm.properties"))
            try:
                yield
            except Exception as error:
                self._log.append({"type": "text", "content": error})
                trace_text = _traceback.format_exc().replace("\n", "<br>")
                self._log.append({"type": "text", "content": trace_text})
                self._error.append(error)
                fatal_error = True
                raise
            finally:
                self._props = None
                self.log_report()
                self._auto_mode_lookup = None
                self._transit_mode_lookup = None
                if self._error:
                    if fatal_error:
                         trace.write("Import network failed (%s errors)" % len(self._error), attributes=attributes)
                    else:
                        trace.write("Import network completed (%s non-fatal errors)" % len(self._error), attributes=attributes)

    def execute(self):
        attr_map = {
            "NODE": OrderedDict([
                ("HNODE",       ("@hnode",        "BOTH",    "EXTRA", "HNODE label from TNED" )),
                ("TAP",         ("@tap_id",       "BOTH",    "EXTRA", "TAP number")),
                ("PARK",        ("@park",         "BOTH",    "EXTRA", "parking indicator" )),
                ("STOPTYPE",    ("@stoptype",     "BOTH",    "EXTRA", "stop type indicator" )),
                ("ELEV",        ("@elev",         "BOTH",    "EXTRA", "station/stop elevation in feet")),
                ("interchange", ("@interchange",  "DERIVED", "EXTRA", "is interchange node")),
            ]),
            "LINK": OrderedDict([
                ("HWYCOV0_ID",("@tcov_id",             "TWO_WAY",     "EXTRA", "SANDAG-assigned link ID")),
                ("SPHERE",    ("@sphere",              "HWY_TWO_WAY", "EXTRA", "Jurisdiction sphere of influence")),
                ("HWYSegGUID",("#hwyseg_guid",         "TWO_WAY",     "STRING", "HWYSegGUID")),
                ("NM",        ("#name",                "TWO_WAY",     "STRING", "Street name")),
                ("FXNM",      ("#name_from",           "TWO_WAY",     "STRING", "Cross street at the FROM end")),
                ("TXNM",      ("#name_to",             "TWO_WAY",     "STRING", "Cross street name at the TO end")),
                ("DIR",       ("@direction_cardinal",  "TWO_WAY",     "EXTRA", "Link direction")),
                ("ASPD",      ("@speed_adjusted",      "HWY_TWO_WAY", "EXTRA", "Adjusted link speed (miles/hr)")),
                ("YR",        ("@year_open_traffic",   "HWY_TWO_WAY", "EXTRA", "The year the link opened to traffic")),
                ("PROJ",      ("@project_code",        "HWY_TWO_WAY", "EXTRA", "Project number for use with hwyproj.xls")),
                ("FC",        ("type",                 "TWO_WAY",     "STANDARD", "")),
                ("HOV",       ("@hov",                 "TWO_WAY",     "EXTRA", "Link operation type")),
                ("MINMODE",   ("@minmode",             "TWO_WAY",     "EXTRA", "Transit mode type")),
                ("EATRUCK",   ("@truck_ea",            "HWY_TWO_WAY", "EXTRA", "Early AM truck restriction code ")),
                ("AMTRUCK",   ("@truck_am",            "HWY_TWO_WAY", "EXTRA", "AM Peak truck restriction code ")),
                ("MDTRUCK",   ("@truck_md",            "HWY_TWO_WAY", "EXTRA", "Mid-day truck restriction code ")),
                ("PMTRUCK",   ("@truck_pm",            "HWY_TWO_WAY", "EXTRA", "PM Peak truck restriction code ")),
                ("EVTRUCK",   ("@truck_ev",            "HWY_TWO_WAY", "EXTRA", "Evening truck restriction code ")),
                ("TOLLEA",    ("@toll_ea",             "HWY_TWO_WAY", "EXTRA", "Early AM toll cost (cent)")),
                ("TOLLA",     ("@toll_am",             "HWY_TWO_WAY", "EXTRA", "AM Peak toll cost (cent)")),
                ("TOLLMD",    ("@toll_md",             "HWY_TWO_WAY", "EXTRA", "Mid-day toll cost (cent)")),
                ("TOLLP",     ("@toll_pm",             "HWY_TWO_WAY", "EXTRA", "PM Peak toll cost (cent)")),
                ("TOLLEV",    ("@toll_ev",             "HWY_TWO_WAY", "EXTRA", "Evening toll cost (cent)")),

                ("SPD",       ("@speed_posted",        "HWY_TWO_WAY", "EXTRA", "Posted speed limit   (mph)")),
                ("MED",       ("@median",              "TWO_WAY",     "EXTRA", "Median type")),
                ("AU",        ("@lane_auxiliary",      "HWY_ONE_WAY", "EXTRA", "Number of auxiliary lanes")),
                ("CNT",       ("@traffic_control",     "HWY_ONE_WAY", "EXTRA", "Intersection control type")),
                ("TL",        ("@turn_thru",           "HWY_ONE_WAY", "EXTRA", "Intersection approach through lanes")),
                ("RL",        ("@turn_right",          "HWY_ONE_WAY", "EXTRA", "Intersection approach right-turn lanes")),
                ("LL",        ("@turn_left",           "HWY_ONE_WAY", "EXTRA", "Intersection approach left-turn lanes")),
                ("GC",        ("@green_to_cycle_init", "HWY_ONE_WAY", "EXTRA", "Initial green-to-cycle ratio")),
                ("WAY",       ("way",                  "HWY_TWO_WAY", "INTERNAL", "")),
                ("TRANSIT_MODES", ("transit_modes",    "DERIVED", "INTERNAL", "")),
                ("@cost_operating", ("@cost_operating", "DERIVED", "EXTRA",    "Fuel and maintenance cost")),
                ("INTDIST_UP",      ("@intdist_up",     "DERIVED", "EXTRA",    "Upstream major intersection distance")),
                ("INTDIST_DOWN",    ("@intdist_down",   "DERIVED", "EXTRA",    "Downstream major intersection distance")),

                ("TMO",       ("@trtime",               "RAIL_TWO_WAY", "EXTRA", "link time in minutes")),
                ("OSPD",      ("@speed_observed",       "RAIL_TWO_WAY", "EXTRA", "Observed speed")),

            ]),
            "TRANSIT_LINE": OrderedDict([
                ("AM_Headway",     ("@headway_am",       "TRRT",     "EXTRA",    "AM Peak actual headway")),
                ("PM_Headway",     ("@headway_pm",       "TRRT",     "EXTRA",    "PM Peak actual headway")),
                ("Midday_Headway", ("@headway_md",       "TRRT",     "EXTRA",    "Midday actual headway")),
                ("Evening_Headway",("@headway_ev",       "TRRT",     "EXTRA",    "Evening actual headway")),
                ("EarlyAM_Headway",("@headway_ea",       "TRRT",     "EXTRA",    "Early AM actual headway")),
                ("AM_Headway_rev", ("@headway_rev_am",   "DERIVED",  "EXTRA",    "AM Peak revised headway")),
                ("PM_Headway_rev", ("@headway_rev_pm",   "DERIVED",  "EXTRA",    "PM Peak revised headway")),
                ("MD_Headway_rev", ("@headway_rev_md",   "DERIVED",  "EXTRA",    "Midday revised headway")),
                ("EV_Headway_rev", ("@headway_rev_ev",   "DERIVED",  "EXTRA",    "Evening revised headway")),
                ("EA_Headway_rev", ("@headway_rev_ea",   "DERIVED",  "EXTRA",    "Early AM revised headway")),
                ("WT_IVTPK",       ("@vehicle_per_pk",   "MODE5TOD", "EXTRA",    "Peak in-vehicle perception factor")),
                ("WT_IVTOP",       ("@vehicle_per_op",   "MODE5TOD", "EXTRA",    "Off-Peak in-vehicle perception factor")),
                ("WT_FAREPK",      ("@fare_per_pk",      "MODE5TOD", "EXTRA",    "Peak fare perception factor")),
                ("WT_FAREOP",      ("@fare_per_op",      "MODE5TOD", "EXTRA",    "Off-Peak fare perception factor")),
                ("DWELLTIME",      ("default_dwell_time", "MODE5TOD", "INTERNAL", "")),
                ("Fare",           ("@fare",             "TRRT",     "EXTRA",    "Boarding fare ($)")),
                ("@transfer_penalty",("@transfer_penalty","DERIVED", "EXTRA",    "Transfer penalty (min)")),
                ("Route_ID",       ("@route_id",         "TRRT",     "EXTRA",    "Transit line internal ID")),
                ("EarlyAM_Hours",  ("@hours_ea",         "TRRT",     "EXTRA",    "Early AM hours")),
                ("Evening_Hours",  ("@hours_ev",         "TRRT",     "EXTRA",    "Evening hours")),
                ("Config",         ("@config",           "TRRT",     "EXTRA",    "Config ID (same as route name)")),
            ]),
            "TRANSIT_SEGMENT": OrderedDict([
                ("Stop_ID",       ("@stop_id",       "TRSTOP", "EXTRA", "Stop ID from trcov")),
                ("Pass_Count",    ("@pass_count",    "TRSTOP", "EXTRA", "Number of times this stop is passed")),
                ("Milepost",      ("@milepost",      "TRSTOP", "EXTRA", "Distance from start of line")),
                ("StopName",      ("#stop_name",     "TRSTOP", "STRING", "Name of stop")),
                ("@coaster_fare_board", ("@coaster_fare_board",   "DERIVED",  "EXTRA", "Boarding fare for coaster")),
                ("@coaster_fare_inveh", ("@coaster_fare_inveh",   "DERIVED",  "EXTRA", "Incremental fare for Coaster")),
            ])
        }

        time_name = {
            "_ea": "Early AM ", "_am": "AM Peak ", "_md": "Mid-day ", "_pm": "PM Peak ", "_ev": "Evening "
        }
        time_name_dst = ["_ea", "_am", "_md", "_pm", "_ev"]
        time_name_src = ["EA", "A", "MD", "P", "EV"]
        time_period_attrs = [
            ("CP",   "@capacity_link",    "mid-link capacity"),
            ("CX",   "@capacity_inter",   "approach capacity"),
            ("CH",   "@capacity_hourly",  "hourly mid-link capacity"),
            ("LN",   "@lane",             "number of lanes"),
            ("TM",   "@time_link",        "link time in minutes"),
            ("TX",   "@time_inter",       "intersection delay time"),
        ]
        for src_attr, dst_attr, desc_tmplt in time_period_attrs:
            for time_s, time_d in zip(time_name_src, time_name_dst):
                attr_map["LINK"][src_attr + time_s] = \
                    (dst_attr + time_d, "HWY_ONE_WAY", "EXTRA", time_name[time_d] + desc_tmplt)
        derived_period_attrs = [
            ("@cost_auto",         "toll + cost autos"),
            ("@cost_hov2",         "toll (non-mngd) + cost HOV2"),
            ("@cost_hov3",         "toll (non-mngd) + cost HOV3+"),
            ("@cost_lgt_truck",    "toll + cost light trucks"),
            ("@cost_med_truck",    "toll + cost medium trucks"),
            ("@cost_hvy_truck",    "toll + cost heavy trucks"),
            ("@cycle",             "cycle length (minutes)"),
            ("@green_to_cycle",    "green to cycle ratio"),
            ("@sta_reliability",   "static reliability")
        ]
        for attr, desc_tmplt in derived_period_attrs:
            for time in time_name_dst:
                attr_map["LINK"][attr + time] = \
                    (attr + time, "DERIVED", "EXTRA", time_name[time] + desc_tmplt)

        create_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.create_scenario")

        title = self.title
        if not title:
            existing_scenario = self.emmebank.scenario(self.scenario_id)
            if existing_scenario:
                title = existing_scenario.title

        scenario = create_scenario(self.scenario_id, title, overwrite=self.overwrite, emmebank=self.emmebank)
        scenarios = [scenario]
        if self.create_time_periods:
            periods=["EA", "AM", "MD", "PM", "EV"]
            period_ids = list(enumerate(periods, start=int(self.scenario_id) + 1))
            for ident, period in period_ids:
                scenarios.append(create_scenario(ident, "%s - %s assign" % (title, period),
                                                 overwrite=self.overwrite, emmebank=self.emmebank))
        # create attributes in scenario
        for elem_type, mapping in attr_map.iteritems():
            for name, _tcoved_type, emme_type, desc in mapping.values():
                if emme_type == "EXTRA":
                    for s in scenarios:
                        if not s.extra_attribute(name):
                            xatt = s.create_extra_attribute(elem_type, name)
                            xatt.description =  desc
                elif emme_type == "STRING":
                    for s in scenarios:
                        if not s.network_field(elem_type, name):
                            s.create_network_field(elem_type, name, 'STRING', description=desc)

            log_content = []
            for k, v in mapping.iteritems():
                if v[3] == "DERIVED":
                    k = "--"
                log_content.append([k] + list(v))
            self._log.append({
                "content": log_content,
                "type": "table",
                "header": ["TNED", "Emme", "Source", "Type", "Description"],
                "title": "Network %s attributes" % elem_type.lower().replace("_", " "),
                "disclosure": True
            })

        network = _network.Network()
        for elem_type, mapping in attr_map.iteritems():
            for field, (attr, tcoved_type, emme_type, desc) in mapping.iteritems():
                if emme_type == "STANDARD":
                    continue
                default = "" if emme_type == "STRING" else 0
                network.create_attribute(elem_type, attr, default)
        try:
            self.create_modes(network)
            self.create_road_base(network, attr_map)
            self.create_turns(network)
            self.calc_traffic_attributes(network)
            self.check_zone_access(network, network.mode("d"))
            self.create_rail_base(network, attr_map)
            self.create_transit_lines(network, attr_map)
            self.calc_transit_attributes(network)
        finally:
            # TAP connectors included in network, fix type setting and renumber node IDs
            for link in network.links():
                if link.type <= 0:
                    link.type = 99
            self.renumber_base_nodes(network)
            scenario.publish_network(network, resolve_attributes=True)

        self.set_functions(scenario)
        self.check_connectivity(scenario)

        if "modify_network.py" in os.listdir(os.getcwd()):
            try:
                with _m.logbook_trace("Modify network script"):
                    import modify_network
                    reload(modify_network)
                    modify_network.run(base_scenario)
            except ImportError as e:
                pass
            network = base_scenario.get_network()
            network.create_attribute("LINK", "transit_modes")

        if self.create_time_periods:
            for link in network.links():
                link.transit_modes = link.modes
            for ident, period in period_ids:
                self.set_auto_modes(network, period)
                scenario = self.emmebank.scenario(ident)
                scenario.publish_network(network, resolve_attributes=True)

    def create_modes(self, network):
        # combined traffic and transit mode creation
        mode_table = {
            "AUTO": [("d", "dummy auto")],
            "AUX_AUTO": [
                ("s", "SOV"),
                ("h", "HOV2"),
                ("i", "HOV3+"),
                ("t", "TRKL"),
                ("m", "TRKM"),
                ("v", "TRKH"),
                ("S", "SOV TOLL"),
                ("H", "HOV2 TOLL"),
                ("I", "HOV3+ TOLL"),
                ("T", "TRKL TOLL"),
                ("M", "TRKM TOLL"),
                ("V", "TRKH TOLL"),
            ],
            "TRANSIT": [
                ("b", "BUS" ),         # (vehicle type 100, PCE=3.0)
                ("e", "EXP BUS"),      # (vehicle type 90 , PCE=3.0)
                ("p", "LTDEXP BUS"),   # (vehicle type 80 , PCE=3.0)
                ("l", "LRT"),          # (vehicle type 50)
                ("y", "BRT YEL"),      # (vehicle type 60 , PCE=3.0)
                ("r", "BRT RED"),      # (vehicle type 70 , PCE=3.0)
                ("c", "CMR"),          # (vehicle type 40)
                ("o", "TIER1"),        # (vehicle type 45)
            ],
            "AUX_TRANSIT": [
                ("a", "ACCESS", 3),
                ("x", "TRANSFER", 3),
                ("w", "WALK", 3),
                ("u", "ACCESS_WLK", 3),
                ("k", "EGRESS_WLK", 3),
                ("f", "ACCESS_PNR", 25),
                ("g", "EGRESS_PNR", 25),
                ("q", "ACCESS_KNR", 25),
                ("j", "EGRESS_KNR", 25),
                ("Q", "ACCESS_TNC", 25),
                ("J", "EGRESS_TNC", 25),
            ],
        }
        for mode_type, modes in mode_table.iteritems():
            for mode_info in modes:
                mode = network.create_mode(mode_type, mode_info[0])
                mode.description = mode_info[1]
                if len(mode_info) == 3:
                    mode.speed = mode_info[2]
        self._transit_mode_lookup = {
            0:    set([]),
            1:    set([network.mode(m_id) for m_id in "x"]),      # 1  = special transfer walk links between certain nearby stops
            2:    set([network.mode(m_id) for m_id in "w"]),      # 2  = walk links in the downtown area
            3:    set([network.mode(m_id) for m_id in "a"]),      # 3  = the special TAP connectors
            400:  set([network.mode(m_id) for m_id in "c"]),      # 4  = Coaster Rail Line
            500:  set([network.mode(m_id) for m_id in "l"]),      # 5  = Trolley & Light Rail Transit (LRT)
            600:  set([network.mode(m_id) for m_id in "bpeyr"]),  # 6  = Yellow Car Bus Rapid Transit (BRT)
            700:  set([network.mode(m_id) for m_id in "bpeyr"]),  # 7  = Red Car Bus Rapid Transit (BRT)
            800:  set([network.mode(m_id) for m_id in "bpe"]),    # 8  = Limited Express Bus
            900:  set([network.mode(m_id) for m_id in "bpe"]),    # 9  = Express Bus
            1000: set([network.mode(m_id) for m_id in "bpe"]),    # 10 = Local Bus
            11:   set([network.mode(m_id) for m_id in "u"]),      #    = access walk links
            12:   set([network.mode(m_id) for m_id in "k"]),      #    = egress walk links
            13:   set([network.mode(m_id) for m_id in "f"]),      #    = access PNR links
            14:   set([network.mode(m_id) for m_id in "g"]),      #    = egress PNR links
            15:   set([network.mode(m_id) for m_id in "q"]),      #    = access KNR links
            16:   set([network.mode(m_id) for m_id in "j"]),      #    = egress KNR links
            17:   set([network.mode(m_id) for m_id in "Q"]),      #    = access TNC links
            18:   set([network.mode(m_id) for m_id in "J"]),      #    = egress TNC links
        }
        modes_gp_lanes = {
            0: set([]),
            1: set([network.mode(m_id) for m_id in "dvmtshiVMTSHI"]),  # all modes
            2: set([network.mode(m_id) for m_id in "dmtshiMTSHI"]),    # no heavy truck
            3: set([network.mode(m_id) for m_id in "dtshiTSHI"]),      # no heavy or medium truck
            4: set([network.mode(m_id) for m_id in "dshiSHI"]),        # no truck
            5: set([network.mode(m_id) for m_id in "dvV"]),            # only heavy trucks
            6: set([network.mode(m_id) for m_id in "dvmVM"]),          # heavy and medium trucks
            7: set([network.mode(m_id) for m_id in "dvmtVMT"]),        # all trucks only (no passenger cars)
        }
        non_toll_modes = set([network.mode(m_id) for m_id in "vmtshi"])
        self._auto_mode_lookup = {
            "GP": modes_gp_lanes,
            "TOLL": dict((k, v - non_toll_modes) for k, v in modes_gp_lanes.iteritems()),
            "HOV2": set([network.mode(m_id) for m_id in "dhiHI"]),
            "HOV3": set([network.mode(m_id) for m_id in "diI"]),
        }

    def set_auto_modes(self, network, period):
        # time periods
        # need to update the modes from the XTRUCK for their time of day
        # Note: only truck types 1, 3, 4, and 7 found in 2012 base network
        truck = "@truck_%s" % period.lower()
        toll = "@toll_%s" % period.lower()
        lookup = self._auto_mode_lookup
        for link in network.links():
            auto_modes = set([])
            if link.type == 10:  # connector
                auto_modes = lookup["GP"][link[truck]]
            elif link.type in [11, 12]:
                pass  # no auto modes, rail only (11) or bus only (12)
            elif link["@hov"] == 1:
                auto_modes = lookup["GP"][link[truck]]
            elif link["@hov"] in [2, 3]:
                # managed lanes, free for HOV2 and HOV3+, tolls for SOV
                if link[toll] > 0:
                    auto_modes =  lookup["TOLL"][link[truck]]
                # special case of I-15 managed lanes base year and 2020, no build
                elif link.type == 1 and link["@project_code"] in [41, 42, 486, 373, 711]:
                    auto_modes =  lookup["TOLL"][link[truck]]
                elif link.type == 8 or link.type == 9:
                    auto_modes =  lookup["TOLL"][link[truck]]
                if link["@hov"] == 2:
                    auto_modes = auto_modes | lookup["HOV2"]
                else:
                    auto_modes = auto_modes | lookup["HOV3"]
            elif link["@hov"] == 4:
                auto_modes =  lookup["TOLL"][link[truck]]
            link.modes = link.transit_modes | auto_modes

    def create_road_base(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import roadway base network from TNED_HwyNet %s" % self.source})
        hwy_data = gen_utils.DataTableProc("TNED_HwyNet", self.source)
        # TEMP workaround: BN field is string
        bn_index = hwy_data._attr_names.index("BN")
        hwy_data._values[bn_index] = hwy_data._values[bn_index].astype(int)

        if self.save_data_tables:
            hwy_data.save("%s_TNED_HwyNet" % self.data_table_name, self.overwrite)

        is_centroid = lambda arc, node : (arc["FC"] == 10)  and (node == "AN")
        link_attr_map = {}
        for field, (name, tcoved_type, emme_type, desc) in attr_map["LINK"].iteritems():
            if tcoved_type in ("TWO_WAY", "HWY_TWO_WAY", "ONE_WAY", "HWY_ONE_WAY"):
                link_attr_map[field] = (name, tcoved_type.replace("HWY_", ""), emme_type, desc)
        
        auto_mode = network.mode("d")
        
        def define_modes(arc):
            vehicle_index = int(arc["MINMODE"] / 100)*100
            aux_index = int(arc["MINMODE"] % 100)
            veh_modes = self._transit_mode_lookup.get(vehicle_index, set([]))
            aux_modes = self._transit_mode_lookup.get(aux_index, set([]))
            modes = veh_modes | aux_modes
            if arc["FC"] not in [11, 12, 99] and arc["HOV"] != 0:
                modes |= set([auto_mode])
            return modes

        self._create_base_net(
            hwy_data, network, mode_callback=define_modes, centroid_callback=is_centroid, link_attr_map=link_attr_map)

        hwy_node_data = gen_utils.DataTableProc("TNED_HwyNodes", self.source)
        node_attrs = [(k, v[0]) for k, v in attr_map["NODE"].iteritems()
                      if v[1] in ("BOTH", "HWY")]
        for record in hwy_node_data:
            node = network.node(record["HNODE"])
            if node:
                for src, dst in node_attrs:
                    node[dst] = record[src]
            else:
                self._log.append({"type": "text", "content": "Cannot find node %s" % record["HNODE"]})
        self._log.append({"type": "text", "content": "Import traffic base network complete"})

    def create_rail_base(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import rail base network from TNED_RailNet %s" % self.source})
        transit_data = gen_utils.DataTableProc("TNED_RailNet", self.source)

        if self.save_data_tables:
            transit_data.save("%s_TNED_RailNet" % self.data_table_name, self.overwrite)

        link_attr_map = {}
        for field, (name, tcoved_type, emme_type, desc) in attr_map["LINK"].iteritems():
            if tcoved_type in ("TWO_WAY", "RAIL_TWO_WAY", "ONE_WAY", "RAIL_ONE_WAY"):
                link_attr_map[field] = (name, tcoved_type.replace("RAIL_", ""), emme_type, desc)

        tier1_modes = set([network.mode(m_id) for m_id in "o"])
        tier1_rail_link_name = self._props["transit.newMode"]

        def define_modes(arc):
            if arc["NM"] == tier1_rail_link_name:
                return tier1_modes
            vehicle_index = int(arc["MINMODE"] / 100)*100
            aux_index = int(arc["MINMODE"] % 100)
            return self._transit_mode_lookup[vehicle_index] | self._transit_mode_lookup[aux_index]

        self._create_base_net(
            transit_data, network, mode_callback=define_modes, link_attr_map=link_attr_map)

        transit_node_data = gen_utils.DataTableProc("TNED_RailNodes", self.source)
        # Load PARK, elevation, stop type data onto transit nodes
        node_attrs = [(k, v[0]) for k, v in attr_map["NODE"].iteritems()
                      if v[1] in ("BOTH", "RAIL")]
        for record in transit_node_data:
            node = network.node(record["HNODE"])
            if node:
                for src, dst in node_attrs:
                    node[dst] = record[src]
            else:
                self._log.append({"type": "text", "content": "Cannot find node %s" % record["HNODE"]})

        self._log.append({"type": "text", "content": "Import transit base network complete"})

    def _create_base_net(self, data, network, link_attr_map, mode_callback, centroid_callback=None):
        forward_attr_map = {}
        reverse_attr_map = {}
        arc_id_name = "HWYCOV0_ID"
        arc_guid_name = "HWYSegGUID"
        for field, (name, tcoved_type, emme_type, desc) in link_attr_map.iteritems():
            if field in [arc_id_name, arc_guid_name, "DIR"]:
                # these attributes are special cases for reverse link
                forward_attr_map[field] = name
            elif tcoved_type in "TWO_WAY":
                forward_attr_map[field] = name
                reverse_attr_map[field] = name
            elif tcoved_type in "ONE_WAY":
                forward_attr_map["AB" + field] = name
                reverse_attr_map["BA" + field] = name

        emme_id_name = forward_attr_map[arc_id_name]
        emme_guid_name = forward_attr_map[arc_guid_name]
        dir_name =  forward_attr_map["DIR"]
        reverse_dir_map = {1: 3, 3: 1, 2: 4, 4: 2, 0: 0}
        new_node_id = max(data.values("AN").max(), data.values("BN").max()) + 1

        if centroid_callback is None:
            centroid_callback = lambda a,n: False

        # Create nodes and links
        for arc in data:
            if float(arc["AN"]) == 0 or float(arc["BN"]) == 0:
                self._log.append({"type": "text",
                    "content": "Node ID 0 in AN (%s) or BN (%s) for link GUID/ID %s/%s." %
                    (arc["AN"], arc["BN"], arc[arc_guid_name], arc[arc_id_name])})
                continue
            coordinates = arc["geo_coordinates"]
            i_node = get_node(network, arc['AN'], coordinates[0], centroid_callback(arc, "AN"))
            j_node = get_node(network, arc['BN'], coordinates[-1], centroid_callback(arc, "BN"))
            link = network.link(i_node, j_node)
            if link:
                msg = "Duplicate link between AN %s and BN %s. Link GUID/IDs %s/%s and %s/%s." % \
                    (arc["AN"], arc["BN"], link[emme_guid_name], link[emme_id_name], arc[arc_guid_name], arc[arc_id_name])
                self._log.append({"type": "text", "content": msg})
                if link[emme_guid_name] == arc[arc_guid_name]:
                    self._log.append({"type": "text", "content": "... but GUIDs match (not an error)"})
                else:
                    self._error.append(msg)
            else:
                modes = mode_callback(arc)
                link = network.create_link(i_node, j_node, modes)
                link.length = arc["LENGTH"]
                if len(coordinates) > 2:
                    link.vertices = coordinates[1:-1]
            for field, attr in forward_attr_map.iteritems():
                link[attr] = arc[field]
            if arc["WAY"] == 2 or arc["WAY"] == 0:
                reverse_link = network.link(j_node, i_node)
                if not reverse_link:
                    reverse_link = network.create_link(j_node, i_node, modes)
                    reverse_link.length = link.length
                    reverse_link.vertices = list(reversed(link.vertices))
                for field, attr in reverse_attr_map.iteritems():
                    reverse_link[attr] = arc[field]
                reverse_link[emme_id_name] = -1*arc[arc_id_name]
                reverse_link[emme_guid_name] = "-" + arc[arc_guid_name]
                reverse_link[dir_name] = reverse_dir_map[arc["DIR"]]

    def create_transit_lines(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import transit lines"})
        fatal_errors = 0
        # Route_ID,Route_Name,Mode,AM_Headway,PM_Headway,Midday_Headway,Evening_Headway,EarlyAM_Headway,Night_Headway,Night_Hours,Config,Fare
        #transit_line_data = gen_utils.DataTableProc("trrt", self.source)
        transit_line_data = gen_utils.DataTableProc("trrt", _join(_dir(self.source), "trrt.csv"))
        # Route_ID,Link_ID,Link_GUID,Direction
        #transit_link_data = gen_utils.DataTableProc("trlink", self.source)
        transit_link_data = gen_utils.DataTableProc("trlink", _join(_dir(self.source), "trlink.csv"))
        # Stop_ID,Route_ID,Link_ID,Pass_Count,Milepost,Longitude, Latitude,HwyNode,TrnNode,StopName
        #transit_stop_data = gen_utils.DataTableProc("trstop", self.source)
        transit_stop_data = gen_utils.DataTableProc("trstop", _join(_dir(self.source), "trstop.csv"))
        # From_line,To_line,Board_stop,Wait_time
        # Note: Board_stop is not used
        #   Timed xfer data
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        timed_xfer_data = {}
        for period in periods:
            file_path = _join(_dir(self.source), FILE_NAMES["TIMEXFER"] % period)
            if os.path.exists(file_path):
                timed_xfer_data[period] = gen_utils.DataTableProc("timexfer_"+period, file_path)
            else:
                timed_xfer_data[period] = []

        mode_properties = gen_utils.DataTableProc("MODE5TOD", _join(_dir(self.source), FILE_NAMES["MODE5TOD"]), convert_numeric=True)
        mode_details = {}
        for record in mode_properties:
            mode_details[int(record["MODE_ID"])] = record

        if self.save_data_tables:
            transit_link_data.save("%s_trlink" % self.data_table_name, self.overwrite)
            transit_line_data.save("%s_trrt" % self.data_table_name, self.overwrite)
            transit_stop_data.save("%s_trstop" % self.data_table_name, self.overwrite)
            mode_properties.save("%s_MODE5TOD" % self.data_table_name, self.overwrite)

        coaster = network.create_transit_vehicle(40, 'c')       # 4  coaster
        trolley = network.create_transit_vehicle(50, 'l')       # 5  sprinter/trolley
        brt_yellow  = network.create_transit_vehicle(60, 'y')   # 6 BRT yellow line (future line)
        brt_red = network.create_transit_vehicle(70, 'r')       # 7 BRT red line (future line)
        premium_bus = network.create_transit_vehicle(80, 'p')   # 8  prem express
        express_bus = network.create_transit_vehicle(90, 'e')   # 9  regular express
        local_bus = network.create_transit_vehicle(100, 'b')    # 10 local bus
        tier1 = network.create_transit_vehicle(45, 'o')         # 11 Tier 1

        brt_yellow.auto_equivalent = 3.0
        brt_red.auto_equivalent = 3.0
        premium_bus.auto_equivalent = 3.0
        express_bus.auto_equivalent = 3.0
        local_bus.auto_equivalent = 3.0

        # Capacities - for reference / post-assignment analysis
        tier1.seated_capacity, tier1.total_capacity = 7 * 142, 7 * 276
        trolley.seated_capacity, trolley.total_capacity = 4 * 64, 4 * 200
        brt_yellow.seated_capacity, brt_yellow.total_capacity = 32, 70
        brt_red.seated_capacity, brt_red.total_capacity = 32, 70
        premium_bus.seated_capacity, premium_bus.total_capacity = 32, 70
        express_bus.seated_capacity, express_bus.total_capacity = 32, 70
        local_bus.seated_capacity, local_bus.total_capacity = 32, 70

        trrt_attrs = []
        mode5tod_attrs = []
        for elem_type in "TRANSIT_LINE", "TRANSIT_SEGMENT":
            mapping = attr_map[elem_type]
            for field, (attr, tcoved_type, emme_type, desc) in mapping.iteritems():
                if tcoved_type == "TRRT":
                    trrt_attrs.append((field, attr))
                elif tcoved_type == "MODE5TOD":
                    mode5tod_attrs.append((field, attr))
        network.create_attribute("TRANSIT_SEGMENT", "milepost")

        # Pre-process transit line (trrt) to know the route names for errors / warnings
        transit_line_records = list(transit_line_data)
        line_names = {}
        for record in transit_line_records:
            line_names[int(record["Route_ID"])] = str(record["Route_Name"])

        links = dict((link["#hwyseg_guid"], link) for link in network.links())
        transit_routes = _defaultdict(lambda: [])
        for record in transit_link_data:
            line_ref = line_names.get(int(record["Route_ID"]), record["Route_ID"])
            link_id = record["Link_GUID"]
            if "-" in record["Direction"]:
                link_id = "-" + link_id
            link = links.get(link_id)
            if not link:
                if "-" in record["Direction"]:
                    reverse_link = links.get("-" + link_id)
                else:
                    reverse_link = links.get(link_id[1:])
                if reverse_link:
                    link = network.create_link(reverse_link.j_node, reverse_link.i_node, reverse_link.modes)
                    link.vertices = list(reversed(reverse_link.vertices))
                    for attr in network.attributes("LINK"):
                        if attr not in set(["vertices"]):
                            link[attr] = reverse_link[attr]
                    link["@tcov_id"] = -1 * reverse_link["@tcov_id"]
                    link["#hwyseg_guid"] = link_id
                    links[link_id] = link
                    msg = "Transit line %s : Missing reverse link with ID %s (%s) (reverse link created)" % (
                        line_ref, record["Link_GUID"], link)
                    self._log.append({"type": "text", "content": msg})
                    self._error.append("Transit route import: " + msg)
                link = reverse_link
            if not link:
                msg = "Transit line %s : No link with GUID %s, routing may not be correct" % (
                    line_ref, record["Link_GUID"])
                self._log.append({"type": "text", "content": msg})
                self._error.append("Transit route import: " + msg)
                fatal_errors += 1
                continue

            transit_routes[int(record["Route_ID"])].append(link)

        # lookup list of special tier 1 mode route names
        tier1_rail_route_names = [str(n) for n in self._props["transit.newMode.route"]]
        dummy_links = set([])
        transit_lines = {}
        auto_mode = network.mode("d")
        for record in transit_line_records:
            try:
                route = transit_routes[int(record["Route_ID"])]
                # Find if name matches one of the names listed in transit.newMode.route and convert to tier 1 rail
                is_tier1_rail = False
                for name in tier1_rail_route_names:
                    if str(record["Route_Name"]).startswith(name):
                        is_tier1_rail = True
                        break
                if is_tier1_rail:
                    vehicle_type = 45
                    mode = network.transit_vehicle(vehicle_type).mode
                else:
                    vehicle_type = int(record["Mode"]) * 10
                mode = network.transit_vehicle(vehicle_type).mode
                prev_link = route[0]
                itinerary = [prev_link]
                for link in route[1:]:
                    if prev_link.j_node != link.i_node:  # filling in the missing gap
                        msg = "Transit line %s (index %s): Links not adjacent, shortest path interpolation used (%s and %s)" % (
                            record["Route_Name"], record["Route_ID"], prev_link["#hwyseg_guid"], link["#hwyseg_guid"])
                        log_record = {"type": "text", "content": msg}
                        self._log.append(log_record)
                        sub_path = find_path(prev_link, link, mode)
                        itinerary.extend(sub_path)
                        log_record["content"] = log_record["content"] + " through %s links" % (len(sub_path))
                    itinerary.append(link)
                    prev_link = link

                node_itinerary = [itinerary[0].i_node] +  [l.j_node for l in itinerary]
                missing_mode = 0
                for link in itinerary:
                    if mode not in link.modes:
                        link.modes |= set([mode])
                        missing_mode += 1
                if missing_mode:
                    msg = "Transit line %s (index %s): missing mode added to %s link(s)" % (
                        str(record["Route_Name"]), record["Route_ID"], missing_mode)
                    self._log.append({"type": "text", "content": msg})
                tline = network.create_transit_line(
                    str(record["Route_Name"]), vehicle_type, node_itinerary)

                for field, attr in trrt_attrs:
                    tline[attr] = float(record[field])
                if is_tier1_rail:
                    line_details = mode_details[11]
                else:
                    line_details = mode_details[int(record["Mode"])]
                for field, attr in mode5tod_attrs:
                    tline[attr] = float(line_details[field])
                #"XFERPENTM": "Transfer penalty time: "
                #"WTXFERTM":  "Transfer perception:"
                # NOTE: an additional transfer penalty perception factor of 5.0 is included
                #       in assignment
                tline["@transfer_penalty"] = float(line_details["XFERPENTM"]) * float(line_details["WTXFERTM"])
                tline.headway = tline["@headway_am"] if tline["@headway_am"] > 0 else 999
                tline.layover_time = 5

                transit_lines[int(record["Route_ID"])] = tline
                milepost = 0
                for segment in tline.segments():
                    segment.milepost = milepost
                    milepost += segment.link.length
                    segment.allow_boardings = False
                    segment.allow_alightings = False
                    if auto_mode in segment.link.modes:
                        # segments on links with auto mode are ft1 = timau
                        segment.transit_time_func = 1
                    else:
                        # ft2 = ul2 -> copied @trtime (fixed speed)
                        segment.transit_time_func = 2
            except Exception as error:
                msg = "Transit line %s: %s %s" % (record["Route_Name"], type(error), error)
                self._log.append({"type": "text", "content": msg})
                trace_text = _traceback.format_exc().replace("\n", "<br>")
                self._log.append({"type": "text", "content": trace_text})
                self._error.append("Transit route import: line %s not created" % record["Route_Name"])
                fatal_errors += 1
        for link in dummy_links:
            network.delete_link(link.i_node, link.j_node)

        line_stops = _defaultdict(lambda: [])
        for record in transit_stop_data:
            try:
                line_name = line_names[int(record["Route_ID"])]
                line_stops[line_name].append(record)
            except KeyError:
                self._log.append(
                    {"type": "text",
                     "content": "Stop %s: could not find transit line by ID %s (link GUID %s)" % (
                        record["Stop_ID"], record["Route_ID"], record["Link_GUID"])})
        for stops in line_stops.itervalues():
            stops.sort(key=lambda stop: float(stop["Milepost"]))

        seg_float_attr_map = []
        seg_string_attr_map = []
        for field, (attr, t_type, e_type, desc) in attr_map["TRANSIT_SEGMENT"].iteritems():
            if t_type == "TRSTOP":
                if e_type == "STRING":
                    seg_string_attr_map.append([field, attr])
                else:
                    seg_float_attr_map.append([field, attr])

        for line_name, stops in line_stops.iteritems():
            tline = network.transit_line(line_name)
            if not tline:
                continue
            itinerary = tline.segments(include_hidden=True)
            segment = prev_segment = itinerary.next()
            for stop in stops:
                if "DUMMY" in stop["StopName"]:
                    continue
                stop_link_id = stop['Link_GUID']
                stop_node_id = int(stop['Node'])
                while segment.link and segment.link["#hwyseg_guid"].lstrip("-") != stop_link_id:
                    segment = itinerary.next()

                if stop_node_id == segment.i_node.number:
                    pass
                elif segment.j_node and stop_node_id == segment.j_node.number:
                    # if matches the J-node then the stop is on the next segment
                    segment = itinerary.next()
                else:
                    next_segment = None
                    if segment.j_node:
                        next_segment = itinerary.next()
                    if next_segment and next_segment.link["#hwyseg_guid"].lstrip("-") == stop_link_id and \
                            stop_node_id == next_segment.j_node.number:
                        # split link case, where stop is at the end of the next segment
                        segment = next_segment
                    else:
                        if segment.link and segment.link["#hwyseg_guid"].lstrip("-") == stop_link_id:
                            msg = "Transit line %s (index %s): found GUID %s (segment %s) but node ID %s does not match I or J node" % (
                                line_name, stop["Route_ID"], segment, stop_link_id, stop_node_id)
                        else:
                            msg = "Transit line %s (index %s): did not found GUID %s for stop node ID %s" % (
                                line_name, stop["Route_ID"], stop_link_id, stop_node_id)
                        self._log.append({"type": "text", "content": msg})
                        self._error.append(msg)
                        fatal_errors += 1
                        # reset iterator to start back from previous segment
                        itinerary = tline.segments(include_hidden=True)
                        segment = itinerary.next()
                        while segment.id != prev_segment.id:
                            segment = itinerary.next()
                        continue
                segment.allow_boardings = True
                segment.allow_alightings = True
                segment.dwell_time = min(tline.default_dwell_time, 99.99)
                for field, attr in seg_string_attr_map:
                    segment[attr] = stop[field]
                for field, attr in seg_float_attr_map:
                    segment[attr] = float(stop[field])
                prev_segment = segment

        def lookup_line(ident):
            line = network.transit_line(ident)
            if line:
                return line.id
            line = transit_lines.get(int(ident))
            if line:
                return line.id
            raise Exception("'%s' is not a route name or route ID" % ident)

        # Normalizing the case of the headers as different examples have been seen
        for period, data in timed_xfer_data.iteritems():
            norm_data = []
            for record in data:
                norm_record = {}
                for key, val in record.iteritems():
                    norm_record[key.lower()] = val
                norm_data.append(norm_record)

            from_line, to_line, wait_time = [], [], []
            for i, record in enumerate(norm_data, start=2):
                try:
                    from_line.append(lookup_line(record["from_line"]))
                    to_line.append(lookup_line(record["to_line"]))
                    wait_time.append(float(record["wait_time"]))
                except Exception as error:
                    msg = "Error processing timexfer_%s.csv on file line %s: %s" % (period, i, error)
                    self._log.append({"type": "text", "content": msg})
                    self._error.append(msg)
                    fatal_errors += 1

            timed_xfer = _dt.Data()
            timed_xfer.add_attribute(_dt.Attribute("from_line", _np.array(from_line).astype("O")))
            timed_xfer.add_attribute(_dt.Attribute("to_line", _np.array(to_line).astype("O")))
            timed_xfer.add_attribute(_dt.Attribute("wait_time", _np.array(wait_time)))
            # Creates and saves the new table
            gen_utils.DataTableProc("%s_timed_xfer_%s" % (self.data_table_name, period), data=timed_xfer)

        if fatal_errors > 0:
            raise Exception("Import of transit lines: %s fatal errors found" % fatal_errors)
        self._log.append({"type": "text", "content": "Import transit lines complete"})

    def calc_transit_attributes(self, network):
        self._log.append({"type": "header", "content": "Calculate derived transit line attributes"})
        # ON TRANSIT LINES
        # Set 3-period headway based on revised headway calculation
        for line in network.transit_lines():
            for period in ["ea", "am", "md", "pm", "ev"]:
                # Update EA and EV headways based on hours
                num_hours_period = 0
                if period == "ea":
                    num_hours_period = 3 # Caution hard-coded number of hours in period
                elif period == "ev": 
                    num_hours_period = 8 # Caution hard-coded number of hours in period
                if period in ["ea", "ev"]:
                    if line["@headway_" + period] > 0:
                        num_runs = line["@hours_" + period]*60/line["@headway_" + period]
                        headway_adj = _floor(num_hours_period*60/num_runs)
                        if headway_adj > 999:
                            headway_adj = 999
                        line["@headway_" + period] = headway_adj
                line["@headway_rev_" + period] = revised_headway(line["@headway_" + period])
                
        self._log.append({"type": "text", "content": "Revised headway calculation complete"})

        fares_file_name = FILE_NAMES["FARES"]
        special_fare_path = _join(_dir(self.source), fares_file_name)
        if not os.path.isfile(special_fare_path):
            self._log.append({"type": "text", "content": "Special fares file %s not found" % fares_file_name})
            return

        def get_line(line_id):
            line = network.transit_line(line_id)
            if line is None:
                raise Exception("%s: line does not exist: %s" % (fares_file_name, line_id))
            return line

        # Special incremental boarding and in-vehicle fares
        # to recreate the coaster zone fares
        self._log.append({"type": "header", "content": "Apply special_fares to transit lines"})
        with open(special_fare_path) as fare_file:
            self._log.append({"type": "text", "content": "Using fare details (for coaster) from %s" % fares_file_name})
            special_fares = None
            yaml_installed = True
            try:
                import yaml
                special_fares = yaml.load(fare_file)
                self._log.append({"type": "text", "content": yaml.dump(special_fares).replace("\n", "<br>")})
            except ImportError:
                yaml_installed = False
            except:
                pass
            if special_fares is None:
                try:
                    import json
                    special_fares = json.load(fare_file)
                    self._log.append({"type": "text", "content": json.dumps(special_fares, indent=4).replace("\n", "<br>")})
                except:
                    pass
            if special_fares is None:
                msg = "YAML or JSON" if yaml_installed else "JSON (YAML parser not installed)"
                raise Exception(fares_file_name + ": file could not be parsed as " + msg)


        for record in special_fares["boarding_cost"]["base"]:
            line = get_line(record["line"])
            line["@fare"] = 0
            for seg in line.segments():
                seg["@coaster_fare_board"] = record["cost"]
        for record in special_fares["boarding_cost"].get("stop_increment", []):
            line = get_line(record["line"])
            for seg in line.segments(True):
                if record["stop"] in seg["#stop_name"]:
                    seg["@coaster_fare_board"] += record["cost"]
                    break
        for record in special_fares["in_vehicle_cost"]:
            line = get_line(record["line"])
            for seg in line.segments(True):
                if record["from"] in seg["#stop_name"]:
                    seg["@coaster_fare_inveh"] = record["cost"]
                    break
        pass_cost_keys = ['day_pass', 'regional_pass']
        pass_costs = []
        for key in pass_cost_keys:
            cost = special_fares.get(key)
            if cost is None:
                raise Exception("key '%s' missing from %s" % (key, fares_file_name))
            pass_costs.append(cost)
        pass_values = _dt.Data()
        pass_values.add_attribute(_dt.Attribute("pass_type", _np.array(pass_cost_keys).astype("O")))
        pass_values.add_attribute(_dt.Attribute("cost", _np.array(pass_costs).astype("f8")))
        gen_utils.DataTableProc("%s_transit_passes" % self.data_table_name, data=pass_values)
        self._log.append({"type": "text", "content": "Apply special_fares to transit lines complete"})

    def renumber_base_nodes(self, network):
        tracker = gen_utils.AvailableNodeIDTracker(network)
        nodes = [n for n in network.nodes() if n.number > 999999]
        nodes = sorted(nodes, key=lambda x: x.number, reverse=True)
        if nodes:
            self._log.append({"type": "text", "content": "Renumbered %s nodes" % len(nodes)})
        for n in nodes:
            old_number = n.number
            n.number = tracker.get_id()
            self._log.append({"type": "text", "content": " - renumbered %s to %s " % (old_number, n.number)})

    def create_turns(self, network):
        self._log.append({"type": "header", "content": "Import turns and turn restrictions"})
        self._log.append({"type": "text", "content": "Process turns for turn prohibited by ID"})
        turn_data = gen_utils.DataTableProc("Turns", self.source)
        if self.save_data_tables:
            turn_data.save("%s_turns"  % self.data_table_name, self.overwrite)
        # Process turns.csv for prohibited turns  penalty
        for i, record in enumerate(turn_data):
            from_node_id, to_node_id, at_node_id = record["FromNode"], record["ToNode"], record["MidNode"]
            at_node = network.node(at_node_id)
            if at_node and not at_node.is_intersection:
                try:
                    network.create_intersection(at_node)
                except Exception as error:
                    text = ("record %s turn from %s, at %s, to %s: cannot create intersection" %
                        (i, from_node_id, at_node_id, to_node_id))
                    self._log.append({"type": "text", "content": text})
                    trace_text = _traceback.format_exc().replace("\n", "<br>")
                    self._log.append({"type": "text", "content": trace_text})
                    self._error.append(text)
                    continue
            turn = network.turn(from_node_id, at_node_id, to_node_id)
            if at_node is None:
                text = ("record %s turn from %s, at %s, to %s: at node does not exist" %
                        (i, from_node_id, at_node_id, to_node_id))
                self._log.append({"type": "text", "content": text})
                self._error.append(text)
            elif turn is None:
                text = ("record %s turn from %s, at %s, to %s: does not form a turn" %
                        (i, from_node_id, at_node_id, to_node_id))
                self._log.append({"type": "text", "content": text})
                self._error.append(text)
            else:
                turn.penalty_func = 0  # prohibit turn
                # NOTE: could support penalty value
                # turn.penalty_func = 1
                # turn.data1 = float(record["penalty"])
        self._log.append({"type": "text", "content": "Import turns and turn prohibitions complete"})

    def calc_traffic_attributes(self, network):
        self._log.append({"type": "header", "content": "Calculate derived traffic attributes"})
        # "COST":       "@cost_operating"
        # "ITOLL":      "@toll_flag"       # ITOLL  - Toll + 100 *[0,1] if managed lane (I-15 tolls)
        #               Note: toll_flag is no longer used
        # "ITOLL2":     "@toll"            # ITOLL2 - Toll
        # "ITOLL3":     "@cost_auto"       # ITOLL3 - Toll + AOC
        #               "@cost_hov"
        # "ITOLL4":     "@cost_med_truck"  # ITOLL4 - Toll * 1.03 + AOC
        # "ITOLL5":     "@cost_hvy_truck"  # ITOLL5 - Toll * 2.33 + AOC
        fatal_errors = 0
        try:
            aoc = float(self._props["aoc.fuel"]) + float(self._props["aoc.maintenance"])
        except ValueError:
            raise Exception("Error during float conversion for aoc.fuel or aoc.maintenance from sandag_abm.properties file")
        scenario_year = int(self._props["scenarioYear"])
        periods = ["EA", "AM", "MD", "PM", "EV"]
        time_periods = ["_ea", "_am", "_md", "_pm", "_ev"]
        src_time_periods = ["_op", "_am", "_op", "_pm", "_op"]
        mode_d = network.mode('d')

        # Calculate upstream and downstream interchange distance
        # First, label the intersection nodes as nodes with type 1 links (freeway) and
        #        type 8 links (freeway-to-freeway ramp)
        network.create_attribute("NODE", "is_interchange")
        interchange_points = []
        for node in network.nodes():
            adj_links = list(node.incoming_links()) + list(node.outgoing_links())
            has_freeway_links = bool(
                [l for l in adj_links
                 if l.type == 1 and mode_d in l.modes])
            has_ramp_links = bool(
                [l for l in adj_links
                 if l.type == 8 and mode_d in l.modes and not "HOV" in l["#name"]])
            if has_freeway_links and has_ramp_links:
                node.is_interchange = True
                interchange_points.append(node)
            else:
                node.is_interchange = False
        for node in network.nodes():
            node["@interchange"] = node.is_interchange

        for link in network.links():
            if link.type == 1 and mode_d in link.modes:
                link["@intdist_down"] = interchange_distance(link, "DOWNSTREAM")
                link["@intdist_up"] = interchange_distance(link, "UPSTREAM")
        self._log.append({"type": "text", "content": "Calculate of nearest interchange distance complete"})

        # Static reliability parameters
        # freeway coefficients
        freeway_rel = {
            "intercept": 0.1078,
            "speed>70": 0.01393,
            "upstream": 0.011,
            "downstream": 0.0005445,
        }
        # arterial/ramp/other coefficients
        road_rel = {
            "intercept": 0.0546552,
            "lanes": {
                1: 0.0,
                2: 0.0103589,
                3: 0.0361211,
                4: 0.0446958,
                5: 0.0
            },
            "speed":  {
                "<35": 0,
                35: 0.0075674,
                40: 0.0091012,
                45: 0.0080996,
                50: -0.0022938,
                ">50": -0.0046211
            },
            "control": {
                0: 0,           # Uncontrolled
                1: 0.0030973,   # Signal
                2: -0.0063281,  # Stop
                3: -0.0063281,  # Stop
                4: 0.0127692,   # Other, Railway, etc.
            }
        }
        for link in network.links():
            # Change SR125 toll speed to 70MPH
            if link["@hov"] == 4 and link.type == 1:
                link["@speed_posted"] = 70
            link["@cost_operating"] = link.length * aoc
            for time in time_periods:
                # add link delay (30 sec=0.5mins) to HOV connectors to discourage travel
                if link.type == 8 and (link["@hov"] == 2 or link["@hov"] == 3):
                    link["@time_link" + time] = link["@time_link" + time] + 0.375

		        # make speed on HOV lanes (70mph) the same as parallel GP lanes (65mph)
                # - set speed back to posted speed - increase travel time by (speed_adj/speed_posted)
                if link.type == 1 and (link["@hov"] == 2 or link["@hov"] == 3):
                    speed_adj = link["@speed_adjusted"]
                    speed_posted = link["@speed_posted"]
                    if speed_adj>0:
                        link["@time_link" + time] = (speed_adj/(speed_posted*1.0)) * link["@time_link" + time]

        # Required file
        vehicle_class_factor_file = FILE_NAMES["VEHICLE_CLASS"]
        facility_factors = _defaultdict(lambda: {})
        facility_factors["DEFAULT_FACTORS"] = {
            "ALL": {
                "auto": 1.0,
                "hov2": 1.0,
                "hov3": 1.0,
                "lgt_truck": 1.0,
                "med_truck": 1.03,
                "hvy_truck": 2.03
            },
            "count": 0
        }
        if os.path.exists(_join(self.source, vehicle_class_factor_file)):
            msg = "Adjusting tolls based on factors from %s" % vehicle_class_factor_file
            self._log.append({"type": "text", "content": msg})
            # NOTE: CSV Reader sets the field names to UPPERCASE for consistency
            with gen_utils.CSVReader(_join(self.source, vehicle_class_factor_file)) as r:
                for row in r:
                    if "YEAR" in r.fields and int(row["YEAR"]) != scenario_year:  # optional year column
                        continue
                    name = row["FACILITY_NAME"]
                    # optional time-of-day entry, default to ALL if no column or blank
                    fac_time = row.get("TIME_OF_DAY")
                    if fac_time is None:
                        fac_time = "ALL"
                    facility_factors[name][fac_time] = {
                        "auto": float(row["DA_FACTOR"]),
                        "hov2": float(row["S2_FACTOR"]),
                        "hov3": float(row["S3_FACTOR"]),
                        "lgt_truck": float(row["TRK_L_FACTOR"]),
                        "med_truck": float(row["TRK_M_FACTOR"]),
                        "hvy_truck": float(row["TRK_H_FACTOR"])
                    }
                    facility_factors[name]["count"] = 0

            # validate ToD entry, either list EA, AM, MD, PM and EV, or ALL, but not both
            for name, factors in facility_factors.iteritems():
                # default keys should be "ALL" and "count"
                if "ALL" in factors:
                    if len(factors) > 2:
                        fatal_errors += 1
                        msg = ("Individual time periods and 'ALL' (or blank) listed under "
                               "TIME_OF_DAY column in {} for facility {}").format(vehicle_class_factor_file, name)
                        self._log.append({"type": "text", "content": msg})
                        self._error.append(msg)
                elif set(periods + ["count"]) != set(factors.keys()):
                    fatal_errors += 1
                    msg = ("Missing time periods {} under TIME_OF_DAY column in {} for facility {}").format(
                        (set(periods) - set(factors.keys())), vehicle_class_factor_file, name)
                    self._log.append({"type": "text", "content": msg})
                    self._error.append(msg)

        def lookup_link_name(link):
            for attr_name in ["#name", "#name_from", "#name_to"]:
                for name, _factors in facility_factors.iteritems():
                    if name in link[attr_name]:
                        return _factors
            return facility_factors["DEFAULT_FACTORS"]

        def match_facility_factors(link):
            factors = lookup_link_name(link)
            factors["count"] += 1
            factors = _copy(factors)
            del factors["count"]
            # @hov = 2 or 3 overrides hov2 and hov3 costs
            if link["@hov"] == 2:
                for _, time_factors in factors.iteritems():
                    time_factors["hov2"] = 0.0
                    time_factors["hov3"] = 0.0
            elif link["@hov"] == 3:
                for _, time_factors in factors.iteritems():
                    time_factors["hov3"] = 0.0
            return factors

        vehicle_classes = ["auto", "hov2", "hov3", "lgt_truck", "med_truck", "hvy_truck"]
        for link in network.links():
            if sum(link["@toll" + time] for time in time_periods) > 0:
                factors = match_facility_factors(link)
                for time, period in zip(time_periods, periods):
                    time_factors = factors.get(period, factors.get("ALL"))
                    for name in vehicle_classes:
                        link["@cost_" + name + time] = time_factors[name] * link["@toll" + time] + link["@cost_operating"]
            else:
                for time in time_periods:
                    for name in vehicle_classes:
                        link["@cost_" + name + time] = link["@cost_operating"]
        for name, class_factors in facility_factors.iteritems():
            msg = "Facility name '%s' matched to %s links." % (name, class_factors["count"])
            self._log.append({"type": "text2", "content": msg})

        self._log.append({
            "type": "text",
            "content": "Calculation and time period expansion of costs, tolls, capacities and times complete"})

        # calculate static reliability
        for link in network.links():
            for time in time_periods:
                sta_reliability = "@sta_reliability" + time
                # if freeway apply freeway parameters to this link
                if link["type"] == 1 and link["@lane" + time] > 0:
                    high_speed_factor = freeway_rel["speed>70"] if link["@speed_posted"] >= 70 else 0.0
                    upstream_factor = freeway_rel["upstream"] * 1 / link["@intdist_up"]
                    downstream_factor = freeway_rel["downstream"] * 1 / link["@intdist_down"]
                    link[sta_reliability] = (
                        freeway_rel["intercept"] + high_speed_factor + upstream_factor + downstream_factor)
                # arterial/ramp/other apply road parameters
                elif link["type"] <= 9 and link["@lane" + time] > 0:
                    lane_factor = road_rel["lanes"].get(link["@lane" + time], 0.0)
                    speed_bin = int(link["@speed_posted"] / 5) * 5  # truncate to multiple of 5
                    if speed_bin < 35:
                        speed_bin = "<35"
                    elif speed_bin > 50:
                        speed_bin = ">50"
                    speed_factor = road_rel["speed"][speed_bin]
                    control_bin = min(max(link["@traffic_control"], 0), 4)
                    control_factor = road_rel["control"][control_bin]
                    link[sta_reliability] = road_rel["intercept"] + lane_factor + speed_factor + control_factor
                else:
                    link[sta_reliability] = 0.0
        self._log.append({"type": "text", "content": "Calculate of link static reliability factors complete"})

        # Cycle length matrix
        #       Intersecting Link
        # Approach Link           2     3     4     5     6     7     8      9
        # FC   Description
        # 2     Prime Arterial    2.5   2     2     2     2     2     2      2
        # 3     Major Arterial    2     2     2     2     2     2     2      2
        # 4     Collector         2     2     1.5   1.5   1.5   1.5   1.5    1.5
        # 5     Local Collector   2     2     1.5   1.25  1.25  1.25  1.25   1.25
        # 6     Rural Collector   2     2     1.5   1.25  1.25  1.25  1.25   1.25
        # 7     Local Road        2     2     1.5   1.25  1.25  1.25  1.25   1.25
        # 8     Freeway connector 2     2     1.5   1.25  1.25  1.25  1.25   1.25
        # 9     Local Ramp        2     2     1.5   1.25  1.25  1.25  1.25   1.25

        # Volume-delay functions
        # fd10: freeway node approach
        # fd11: non-intersection node approach
        # fd20: cycle length 1.25
        # fd21: cycle length 1.5
        # fd22: cycle length 2.0
        # fd23: cycle length 2.5
        # fd24: cycle length 2.5 and metered ramp
        # fd25: freeway node approach AM and PM only
        network.create_attribute("LINK", "green_to_cycle")
        network.create_attribute("LINK", "cycle")
        vdf_cycle_map = {1.25: 20, 1.5: 21, 2.0: 22, 2.5: 23}
        for node in network.nodes():
            incoming = list(node.incoming_links())
            outgoing = list(node.outgoing_links())
            is_signal = False
            for link in incoming:
                if link["@green_to_cycle_init"] > 0:
                    is_signal = True
                    break
            if is_signal:
                lcs = [link.type for link in incoming + outgoing]
                min_lc = max(lcs)  # Note: minimum class is actually the HIGHEST value,
                max_lc = min(lcs)  #       and maximum is the LOWEST

            for link in incoming:
                # Metered ramps
                if link["@traffic_control"] in [4, 5]:
                    link["cycle"] = 2.5
                    link["green_to_cycle"] = 0.42
                    link.volume_delay_func = 24
                # Stops
                elif link["@traffic_control"] in [2, 3]:
                    link["cycle"] = 1.25
                    link["green_to_cycle"] = 0.42
                    link.volume_delay_func = 20
                elif link["@green_to_cycle_init"] > 0 and is_signal:
                    if link.type == 2:
                        c_len = 2.5 if min_lc == 2 else 2.0
                    elif link.type == 3:
                        c_len = 2.0       # Major arterial & anything
                    elif link.type == 4:
                        c_len = 1.5 if max_lc > 2 else 2.0
                    elif link.type > 4:
                        if max_lc > 4:
                            c_len = 1.25
                        elif max_lc == 4:
                            c_len = 1.5
                        else:
                            c_len = 2.0
                    if link["@green_to_cycle_init"] > 10:
                        link["green_to_cycle"] = link["@green_to_cycle_init"] / 100.0
                    if link["green_to_cycle"] > 1.0:
                        link["green_to_cycle"] = 1.0
                    link["cycle"] = c_len
                    link.volume_delay_func = vdf_cycle_map[c_len]
                elif link.type == 1:
                    link.volume_delay_func = 10  # freeway
                else:
                    link.volume_delay_func = 11  # non-controlled approach
        self._log.append({"type": "text", "content": "Derive cycle, green_to_cycle, and VDF by approach node complete"})

        for link in network.links():
            if link.volume_delay_func in [10, 11]:
                continue
            if link["@traffic_control"] in [4, 5]:
                # Ramp meter controlled links are only enabled during the peak periods
                for time in ["_am", "_pm"]:
                    link["@cycle" + time] = link["cycle"]
                    link["@green_to_cycle" + time] = link["green_to_cycle"]
            else:
                for time in time_periods:
                    link["@cycle" + time] = link["cycle"]
                    link["@green_to_cycle" + time] = link["green_to_cycle"]
        self._log.append({"type": "text", "content": "Setting of time period @cycle and @green_to_cycle complete"})

        network.delete_attribute("LINK", "green_to_cycle")
        network.delete_attribute("LINK", "cycle")
        network.delete_attribute("NODE", "is_interchange")
        self._log.append({"type": "text", "content": "Calculate derived traffic attributes complete"})
        if fatal_errors > 0:
            raise Exception("%s fatal errors during calculation of traffic attributes" % fatal_errors)
        return

    def check_zone_access(self, network, mode):
        # Verify that every centroid has at least one available
        # access and egress connector
        for centroid in network.centroids():
            access = egress = False
            for link in centroid.outgoing_links():
                if mode in link.modes:
                    if link.j_node.is_intersection:
                        for turn in link.outgoing_turns():
                            if turn.i_node != turn.k_node and turn.penalty_func != 0:
                                egress = True
                    else:
                        egress = True
            if not egress:
                raise Exception("No egress permitted from zone %s" % centroid.id)
            for link in centroid.incoming_links():
                if mode in link.modes:
                    if link.j_node.is_intersection:
                        for turn in link.incoming_turns():
                            if turn.i_node != turn.k_node and turn.penalty_func != 0:
                                access = True
                    else:
                        access = True
            if not access:
                raise Exception("No access permitted to zone %s" % centroid.id)

    @_m.logbook_trace("Set database functions (VDF, TPF and TTF)")
    def set_functions(self, scenario):
        create_function = _m.Modeller().tool(
            "inro.emme.data.function.create_function")
        set_extra_function_params = _m.Modeller().tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        emmebank = self.emmebank
        for f_id in ["fd10", "fd11", "fd20", "fd21", "fd22", "fd23", "fd24", "fd25",
                     "fp1", "ft1", "ft2", "ft3", "ft4"]:
            function = emmebank.function(f_id)
            if function:
                emmebank.delete_function(function)

        smartSignalf_CL = self._props["smartSignal.factor.LC"]
        smartSignalf_MA = self._props["smartSignal.factor.MA"]
        smartSignalf_PA = self._props["smartSignal.factor.PA"]
        atdmf = self._props["atdm.factor"]

        reliability_tmplt = (
            "* (1 + el2 + {0}*(".format(atdmf)+
            "( {factor[LOS_C]} * ( put(get(1).min.1.5) - {threshold[LOS_C]} + 0.01 ) ) * (get(1) .gt. {threshold[LOS_C]})"
            "+ ( {factor[LOS_D]} * ( get(2) - {threshold[LOS_D]} + 0.01 )  ) * (get(1) .gt. {threshold[LOS_D]})"
            "+ ( {factor[LOS_E]} * ( get(2) - {threshold[LOS_E]} + 0.01 )  ) * (get(1) .gt. {threshold[LOS_E]})"
            "+ ( {factor[LOS_FL]} * ( get(2) - {threshold[LOS_FL]} + 0.01 )  ) * (get(1) .gt. {threshold[LOS_FL]})"
            "+ ( {factor[LOS_FH]} * ( get(2) - {threshold[LOS_FH]} + 0.01 )  ) * (get(1) .gt. {threshold[LOS_FH]})"
            "))")
        parameters = {
            "freeway": {
                "factor": {
                    "LOS_C": 0.2429, "LOS_D": 0.1705, "LOS_E": -0.2278, "LOS_FL": -0.1983, "LOS_FH": 1.022
                },
                "threshold": {
                    "LOS_C": 0.7, "LOS_D": 0.8,  "LOS_E": 0.9, "LOS_FL": 1.0, "LOS_FH": 1.2
                },
            },
            "road": {   # for arterials, ramps, collectors, local roads, etc.
                "factor": {
                    "LOS_C": 0.1561, "LOS_D": 0.0, "LOS_E": 0.0, "LOS_FL": -0.449, "LOS_FH": 0.0
                },
                "threshold": {
                    "LOS_C": 0.7, "LOS_D": 0.8,  "LOS_E": 0.9, "LOS_FL": 1.0, "LOS_FH": 1.2
                },
            }
        }
        # freeway fd10
        create_function(
            "fd10",
            "(ul1 * (1.0 + 0.24 * put((volau + volad) / ul3) ** 5.5))"
            + reliability_tmplt.format(**parameters["freeway"]),
            emmebank=emmebank)
        # non-freeway link which is not an intersection approach fd11
        create_function(
            "fd11",
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0))"
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        create_function(
            "fd20",  # Local collector and lower intersection and stop controlled approaches
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0) +"
            "1.25 / 2 * (1-el1) ** 2 * (1.0 + 4.5 * ( (volau + volad) / el3 ) ** 2.0))"
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        create_function(
            "fd21",  # Collector intersection approaches
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0) +"
            "{0} * 1.5/ 2 * (1-el1) ** 2 * (1.0 + 4.5 * ( (volau + volad) / el3 ) ** 2.0))".format(smartSignalf_CL)
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        create_function(
            "fd22",  # Major arterial and major or prime arterial intersection approaches
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0) +"
            "{0} * 2.0 / 2 * (1-el1) ** 2 * (1.0 + 4.5 * ( (volau + volad) / el3 ) ** 2.0))".format(smartSignalf_MA)
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        create_function(
            "fd23",  # Primary arterial intersection approaches
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0) +"
            "{0} * 2.5/ 2 * (1-el1) ** 2 * (1.0 + 4.5 * ( (volau + volad) / el3 ) ** 2.0))".format(smartSignalf_PA)
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        create_function(
            "fd24",  # Metered ramps
            "(ul1 * (1.0 + 0.8 * put((volau + volad) / ul3) ** 4.0) +"
            "2.5/ 2 * (1-el1) ** 2 * (1.0 + 6.0 * ( (volau + volad) / el3 ) ** 2.0))"
            + reliability_tmplt.format(**parameters["road"]),
            emmebank=emmebank)
        # freeway fd25 (AM and PM only)
        create_function(
            "fd25",
            "(ul1 * (1.0 + 0.6 * put((volau + volad) / ul3) ** 4))"
            + reliability_tmplt.format(**parameters["freeway"]),
            emmebank=emmebank)

        set_extra_function_params(
            el1="@green_to_cycle", el2="@sta_reliability", el3="@capacity_inter_am",
            emmebank=emmebank)

        create_function("fp1", "up1", emmebank=emmebank)  # fixed cost turns stored in turn data 1 (up1)

        # buses in mixed traffic, use auto time
        create_function("ft1", "ul1", emmebank=emmebank)
        # fixed speed for separate guideway operations
        create_function("ft2", "ul2", emmebank=emmebank)
        # special 0-cost segments for prohibition of walk to different stop from centroid
        create_function("ft3", "0", emmebank=emmebank)
        # fixed guideway systems according to vehicle speed (not used at the moment)
        create_function("ft4", "60 * length / speed", emmebank=emmebank)

    @_m.logbook_trace("Traffic zone connectivity check")
    def check_connectivity(self, scenario):
        modeller = _m.Modeller()
        sola_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        set_extra_function_para = modeller.tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        create_matrix = _m.Modeller().tool(
            "inro.emme.data.matrix.create_matrix")
        net_calc = gen_utils.NetworkCalculator(scenario)

        emmebank = scenario.emmebank
        zone_index = dict(enumerate(scenario.zone_numbers))
        num_processors = dem_utils.parse_num_processors("MAX-1")

        # Note matrix is also created in initialize_matrices
        create_matrix("ms1", "zero", "zero", scenario=scenario, overwrite=True)
        with gen_utils.temp_matrices(emmebank, "FULL", 1) as (result_matrix,):
            result_matrix.name = "TEMP_AUTO_TRAVEL_TIME"
            set_extra_function_para(
                el1="@green_to_cycle_am",
                el2="@sta_reliability_am",
                el3="@capacity_inter_am", emmebank=emmebank)
            net_calc("ul1", "@time_link_am", "modes=d")
            net_calc("ul3", "@capacity_link_am", "modes=d")
            net_calc("lanes", "@lane_am", "modes=d")
            spec = {
                "type": "SOLA_TRAFFIC_ASSIGNMENT",
                "background_traffic": None,
                "classes": [
                    {
                        "mode": "d",
                        "demand": 'ms"zero"',
                        "generalized_cost": None,
                        "results": {
                            "od_travel_times": {"shortest_paths": result_matrix.named_id}
                        }
                    }
                ],
                "stopping_criteria": {
                    "max_iterations": 0, "best_relative_gap": 0.0,
                    "relative_gap": 0.0, "normalized_gap": 0.0
                },
                "performance_settings": {"number_of_processors": num_processors},
            }
            sola_assign(spec, scenario=scenario)
            travel_time = result_matrix.get_numpy_data(scenario)

        is_disconnected = (travel_time == 1e20)
        disconnected_pairs = is_disconnected.sum()
        if disconnected_pairs > 0:
            error_msg = "Connectivity error(s) between %s O-D pairs" % disconnected_pairs
            self._log.append({"type": "header", "content": error_msg})
            count_disconnects = []
            for axis, term in [(0, "from"), (1, "to")]:
                axis_totals = is_disconnected.sum(axis=axis)
                for i, v in enumerate(axis_totals):
                    if v > 0:
                        count_disconnects.append((zone_index[i], term, v))
            count_disconnects.sort(key=lambda x: x[2], reverse=True)
            for z, direction, count in count_disconnects[:50]:
                msg ="Zone %s disconnected %s %d other zones" % (z, direction, count)
                self._log.append({"type": "text", "content": msg})
            if disconnected_pairs > 50:
                self._log.append({"type": "text", "content": "[List truncated]"})
            raise Exception(error_msg)
        self._log.append({"type": "header", "content":
                          "Zone connectivity verified for AM period on SOV toll ('S') mode"})
        scenario.has_traffic_results = False

    def log_report(self):
        report = _m.PageBuilder(title="Import network from TNED files report")
        try:
            if self._error:
                report.add_html("<div style='margin-left:10px'>Errors detected during import: %s</div>" % len(self._error))
                error_msg = ["<ul style='margin-left:10px'>"]
                for error in self._error:
                    error_msg.append("<li>%s</li>"  % error)
                error_msg.append("</ul>")
                report.add_html("".join(error_msg))
            else:
                report.add_html("<br><br><nbsp><nbsp>No errors detected during import :-)")

            for item in self._log:
                if item["type"] == "text":
                    report.add_html("<div style='margin-left:20px'>%s</div>" % item["content"])
                if item["type"] == "text2":
                    report.add_html("<div style='margin-left:30px'>%s</div>" % item["content"])
                elif item["type"] == "header":
                    report.add_html("<h3 style='margin-left:10px'>%s</h3>" % item["content"])
                elif item["type"] == "table":
                    table_msg = ["<div style='margin-left:20px'><table>", "<h3>%s</h3>" % item["title"]]
                    if "header" in item:
                        table_msg.append("<tr>")
                        for label in item["header"]:
                            table_msg.append("<th>%s</th>" % label)
                        table_msg.append("</tr>")
                    for row in item["content"]:
                        table_msg.append("<tr>")
                        for cell in row:
                            table_msg.append("<td>%s</td>" % cell)
                        table_msg.append("</tr>")
                    table_msg.append("</table></div>")
                    report.add_html("".join(table_msg))

        except Exception as error:
            # no raise during report to avoid masking real error
            report.add_html("Error generating report")
            report.add_html(unicode(error))
            report.add_html(_traceback.format_exc())

        _m.logbook_write("Import network report", report.render())


def get_node(network, number, coordinates, is_centroid=False):
    node = network.node(number)
    if not node:
        node = network.create_node(number, is_centroid)
        node.x, node.y = coordinates
    return node


# shortest path interpolation
def find_path(orig_link, dest_link, mode):
    visited = set([])
    visited_add = visited.add
    back_links = {}
    heap = []

    for link in orig_link.j_node.outgoing_links():
        if mode in link.modes:
            back_links[link] = None
            _heapq.heappush(heap, (link["length"], link))

    link_found = False
    try:
        while not link_found:
            link_cost, link = _heapq.heappop(heap)
            if link in visited:
                continue
            visited_add(link)
            for outgoing in link.j_node.outgoing_links():
                if mode not in outgoing.modes:
                    continue
                if outgoing in visited:
                    continue
                back_links[outgoing] = link
                if outgoing == dest_link:
                    link_found = True
                    break
                outgoing_cost = link_cost + link["length"]
                _heapq.heappush(heap, (outgoing_cost, outgoing))
    except IndexError:
        pass  # IndexError if heap is empty
    if not link_found:
        raise NoPathException(
            "no path found between links with trcov_id %s and %s (Emme IDs %s and %s)" % (
            orig_link["@tcov_id"], dest_link["@tcov_id"], orig_link, dest_link))

    prev_link = back_links[dest_link]
    route = []
    while prev_link:
        route.append(prev_link)
        prev_link = back_links[prev_link]
    return list(reversed(route))


class NoPathException(Exception):
    pass


def revised_headway(headway):
    # CALCULATE REVISED HEADWAY
    # new headway calculation is less aggressive; also only being used for initial wait
    # It uses a negative exponential formula to calculate headway
    #
    if headway <= 10:
        rev_headway = headway
    else:
        rev_headway = headway * (0.275 + 0.788 * _np.exp(-0.011*headway))
    return rev_headway


def interchange_distance(orig_link, direction):
    visited = set([])
    visited_add = visited.add
    back_links = {}
    heap = []
    if direction == "DOWNSTREAM":
        get_links = lambda l: l.j_node.outgoing_links()
        check_far_node = lambda l: l.j_node.is_interchange
    elif direction == "UPSTREAM":
        get_links = lambda l: l.i_node.incoming_links()
        check_far_node = lambda l: l.i_node.is_interchange
    # Shortest path search for nearest interchange node along freeway
    for link in get_links(orig_link):
        _heapq.heappush(heap, (link["length"], link))
    interchange_found = False
    try:
        while not interchange_found:
            link_cost, link = _heapq.heappop(heap)
            if link in visited:
                continue
            visited_add(link)
            if check_far_node(link):
                interchange_found = True
                break
            for next_link in get_links(link):
                if next_link in visited:
                    continue
                next_cost = link_cost + link["length"]
                _heapq.heappush(heap, (next_cost, next_link))
    except IndexError:
        # IndexError if heap is empty
        # case where start / end of highway, dist = 99
        return 99
    return orig_link["length"] / 2.0 + link_cost
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
#    source: path to the location of the input network files
#    traffic_scenario_id: optional scenario to store the imported network from the traffic files only
#    transit_scenario_id: optional scenario to store the imported network from the transit files only
#    merged_scenario_id: scenario to store the combined traffic and transit data from all network files
#    title: the title to use for the imported scenario
#    save_data_tables: if checked, create a data table for each reference file for viewing in the Emme Desktop
#    data_table_name: prefix to use to identify all data tables
#    overwrite: check to overwrite any existing data tables or scenarios with the same ID or name
#    emmebank: the Emme database in which to create the scenario. Default is the current open database
#
# Files referenced:
#    hwycov.e00: base nodes and links for traffic network with traffic attributes in ESRI input exchange format
#    linktypeturns.dbf: fixed turn travel times by to/from link type (field IFC) pairs
#    turns.csv: turn bans and fixed costs by link from/to ID (field HWYCOV-ID)
#    trcov.e00: base nodes and links for transit network in ESRI input exchange format
#    trrt.csv: transit routes and their attributes
#    trlink.csv: itineraries for each route as sequence of link IDs (TRCOV-ID field)
#    trstop.csv: transit stop attributes
#    timexfer_period.csv: table of timed transfer pairs of lines, by period
#    mode5tod.csv: global (per-mode) transit cost and perception attributes
#    special_fares.txt: table listing special fares in terms of boarding and incremental in-vehicle costs.
#    off_peak_toll_factors.csv (optional): factors to calculate the toll for EA, MD, and EV periods from the OP toll input for specified facilities
#    vehicle_class_toll_factors.csv (optional): factors to adjust the toll cost by facility name and class (DA, S2, S3, TRK_L, TRK_M, TRK_H)
#
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    source_dir = os.path.join(main_directory, "input")
    title = "Base 2012 scenario"
    import_network = modeller.tool("sandag.import.import_network")
    import_network(output_dir, merged_scenario_id=100, title=title,
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
from copy import deepcopy as _copy
import numpy as _np
import heapq as _heapq

import traceback as _traceback
import os
import yaml #for network editor yaml file

_join = os.path.join
_dir = os.path.dirname


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

FILE_NAMES = {
    "FARES": "special_fares.txt",
    "OFF_PEAK": "off_peak_toll_factors.csv",
    "VEHICLE_CLASS": "vehicle_class_toll_factors.csv",
    "NETWORK_EDITS": "network_edits.yaml" #prepare the network ymal file
}


class ImportNetwork(_m.Tool(), gen_utils.Snapshot):

    source = _m.Attribute(unicode)
    traffic_scenario_id = _m.Attribute(int)
    transit_scenario_id = _m.Attribute(int)
    merged_scenario_id = _m.Attribute(int)
    overwrite = _m.Attribute(bool)
    title = _m.Attribute(unicode)
    save_data_tables = _m.Attribute(bool)
    data_table_name = _m.Attribute(unicode)

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
        self.attributes = [
            "source", "traffic_scenario_id", "transit_scenario_id", "merged_scenario_id",
            "overwrite", "title", "save_data_tables", "data_table_name"]

    def page(self):
        if not self.data_table_name:
            load_properties = _m.Modeller().tool('sandag.utilities.properties')
            props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
            self.data_table_name = props["scenarioYear"]

        pb = _m.ToolPageBuilder(self)
        pb.title = "Import network"
        pb.description = """
        Create an Emme network from the E00 and associated files
        generated from TCOVED.
        The timed transfer is stored in data tables with the suffix "_timed_xfers_<i>period</i>".
        <br>
        <br>
        <div style="text-align:left">
            The following files are used:
            <ul>
                <li>hwycov.e00</li>
                <li>LINKTYPETURNS.DBF</li>
                <li>turns.csv</li>
                <li>trcov.e00</li>
                <li>trrt.csv</li>
                <li>trlink.csv</li>
                <li>trstop.csv</li>
                <li>timexfer_<i>period</i>.csv, where <i>period</i> = EA,AM,MD,PM,EV</li>
                <li>MODE5TOD.csv</li>
                <li>special_fares.txt</li>
                <li>off_peak_toll_factors.csv (optional)</li>
                <li>vehicle_class_toll_factors.csv (optional)</li>
            </ul>
        </div>
        """
        pb.branding_text = "- SANDAG - Import"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file("source", window_type="directory", file_filter="",
                           title="Source directory:",)

        pb.add_text_box("traffic_scenario_id", size=6, title="Scenario ID for traffic (optional):")
        pb.add_text_box("transit_scenario_id", size=6, title="Scenario ID for transit (optional):")
        pb.add_text_box("merged_scenario_id", size=6, title="Scenario ID for merged network:")
        pb.add_text_box("title", size=80, title="Scenario title:")
        pb.add_checkbox("save_data_tables", title=" ", label="Save reference data tables of file data")
        pb.add_text_box("data_table_name", size=80, title="Name for data tables:",
            note="Prefix name to use for all saved data tables")
        pb.add_checkbox("overwrite", title=" ", label="Overwrite existing scenarios and data tables")

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

    def __call__(self, source,
                 traffic_scenario_id=None, transit_scenario_id=None, merged_scenario_id=None,
                 title="", save_data_tables=False, data_table_name="", overwrite=False,
                 emmebank=None):

        self.source = source
        self.traffic_scenario_id = traffic_scenario_id
        self.transit_scenario_id = transit_scenario_id
        self.merged_scenario_id = merged_scenario_id
        self.title = title
        self.save_data_tables = save_data_tables
        self.data_table_name = data_table_name
        self.overwrite = overwrite
        if not emmebank:
            self.emmebank = _m.Modeller().emmebank
        else:
            self.emmebank = emmebank

        with self.setup():
            self.execute()

        return self.emmebank.scenario(merged_scenario_id)

    @_context
    def setup(self):
        self._log = []
        self._error = []
        fatal_error = False
        attributes = OrderedDict([
            ("self", str(self)),
            ("source", self.source),
            ("traffic_scenario_id", self.traffic_scenario_id),
            ("transit_scenario_id", self.transit_scenario_id),
            ("merged_scenario_id", self.merged_scenario_id),
            ("title", self.title),
            ("save_data_tables", self.save_data_tables),
            ("data_table_name", self.data_table_name),
            ("overwrite", self.overwrite),
        ])
        self._log = [{
            "content": attributes.items(),
            "type": "table", "header": ["name", "value"],
            "title": "Tool input values"
        }]
        with _m.logbook_trace("Import network", attributes=attributes) as trace:
            gen_utils.log_snapshot("Import network", str(self), attributes)
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
                self.log_report()
                if self._error:
                    if fatal_error:
                        trace.write("Import network failed (%s errors)" % len(self._error), attributes=attributes)
                    else:
                        trace.write("Import network completed (%s non-fatal errors)" % len(self._error), attributes=attributes)

    def execute(self):
        traffic_attr_map = {
            "NODE": {
                "interchange": ("@interchange", "DERIVED", "EXTRA", "is interchange node")
            },
            "LINK": OrderedDict([
                ("HWYCOV-ID", ("@tcov_id",             "TWO_WAY", "EXTRA", "SANDAG-assigned link ID")),
                ("SPHERE",    ("@sphere",              "TWO_WAY", "EXTRA", "Jurisdiction sphere of influence")),
                ("NM",        ("#name",                "TWO_WAY", "STRING", "Street name")),
                ("FXNM",      ("#name_from",           "TWO_WAY", "STRING", "Cross street at the FROM end")),
                ("TXNM",      ("#name_to",             "TWO_WAY", "STRING", "Cross street name at the TO end")),
                ("DIR",       ("@direction_cardinal",  "TWO_WAY", "EXTRA", "Link direction")),
                ("ASPD",      ("@speed_adjusted",      "TWO_WAY", "EXTRA", "Adjusted link speed (miles/hr)")),
                ("IYR",       ("@year_open_traffic",   "TWO_WAY", "EXTRA", "The year the link opened to traffic")),
                ("IPROJ",     ("@project_code",        "TWO_WAY", "EXTRA", "Project number for use with hwyproj.xls")),
                ("IJUR",      ("@jurisdiction_type",   "TWO_WAY", "EXTRA", "Link jurisdiction type")),
                ("IFC",       ("type",                 "TWO_WAY", "STANDARD", "")),
                ("IHOV",      ("@lane_restriction",    "TWO_WAY", "EXTRA", "Link operation type")),
                ("ITRUCK",    ("@truck_restriction",   "TWO_WAY", "EXTRA", "Truck restriction code (ITRUCK)")),
                ("ISPD",      ("@speed_posted",        "TWO_WAY", "EXTRA", "Posted speed limit   (mph)")),
                ("IMED",      ("@median",              "TWO_WAY", "EXTRA", "Median type")),
                ("AU",        ("@lane_auxiliary",      "ONE_WAY", "EXTRA", "Number of auxiliary lanes")),
                ("CNT",       ("@traffic_control",     "ONE_WAY", "EXTRA", "Intersection control type")),
                ("TL",        ("@turn_thru",           "ONE_WAY", "EXTRA", "Intersection approach through lanes")),
                ("RL",        ("@turn_right",          "ONE_WAY", "EXTRA", "Intersection approach right-turn lanes")),
                ("LL",        ("@turn_left",           "ONE_WAY", "EXTRA", "Intersection approach left-turn lanes")),
                ("GC",        ("@green_to_cycle_init", "ONE_WAY", "EXTRA", "Initial green-to-cycle ratio")),
                ("CHO",       ("@capacity_hourly_op",  "ONE_WAY", "EXTRA", "Off-Peak hourly mid-link capacity")),
                ("CHA",       ("@capacity_hourly_am",  "ONE_WAY", "EXTRA", "AM Peak hourly mid-link capacity")),
                ("CHP",       ("@capacity_hourly_pm",  "ONE_WAY", "EXTRA", "PM Peak hourly mid-link capacity")),
                # These attributes are expanded from 3 time periods to 5
                ("ITOLLO",    ("toll_op",              "TWO_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("ITOLLA",    ("toll_am",              "TWO_WAY", "INTERNAL", "")),
                ("ITOLLP",    ("toll_pm",              "TWO_WAY", "INTERNAL", "")),
                ("LNO",       ("lane_op",              "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("LNA",       ("lane_am",              "ONE_WAY", "INTERNAL", "")),
                ("LNP",       ("lane_pm",              "ONE_WAY", "INTERNAL", "")),
                ("CPO",       ("capacity_link_op",     "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("CPA",       ("capacity_link_am",     "ONE_WAY", "INTERNAL", "")),
                ("CPP",       ("capacity_link_pm",     "ONE_WAY", "INTERNAL", "")),
                ("CXO",       ("capacity_inter_op",    "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("CXA",       ("capacity_inter_am",    "ONE_WAY", "INTERNAL", "")),
                ("CXP",       ("capacity_inter_pm",    "ONE_WAY", "INTERNAL", "")),
                ("TMO",       ("time_link_op",         "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("TMA",       ("time_link_am",         "ONE_WAY", "INTERNAL", "")),
                ("TMP",       ("time_link_pm",         "ONE_WAY", "INTERNAL", "")),
                ("TXO",       ("time_inter_op",        "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("TXA",       ("time_inter_am",        "ONE_WAY", "INTERNAL", "")),
                ("TXP",       ("time_inter_pm",        "ONE_WAY", "INTERNAL", "")),
                # These three attributes are used to cross-reference the turn directions
                ("TLB",       ("through_link",         "ONE_WAY", "INTERNAL", "")),
                ("RLB",       ("right_link",           "ONE_WAY", "INTERNAL", "")),
                ("LLB",       ("left_link",            "ONE_WAY", "INTERNAL", "")),
                ("@cost_operating", ("@cost_operating","DERIVED", "EXTRA",    "Fuel and maintenance cost")),
                ("INTDIST_UP",      ("@intdist_up",    "DERIVED", "EXTRA",    "Upstream major intersection distance")),
                ("INTDIST_DOWN",    ("@intdist_down",  "DERIVED", "EXTRA",    "Downstream major intersection distance")),
            ])
        }
        time_period_attrs = OrderedDict([
            ("@cost_auto",         "toll + cost autos"),
            ("@cost_hov2",          "toll (non-mngd) + cost HOV2"),
            ("@cost_hov3",          "toll (non-mngd) + cost HOV3+"),
            ("@cost_lgt_truck",    "toll + cost light trucks"),
            ("@cost_med_truck",    "toll + cost medium trucks"),
            ("@cost_hvy_truck",    "toll + cost heavy trucks"),
            ("@cycle",             "cycle length (minutes)"),
            ("@green_to_cycle",    "green to cycle ratio"),
            ("@capacity_link",     "mid-link capacity"),
            ("@capacity_inter",    "approach capacity"),
            ("@toll",              "toll cost (cent)"),
            ("@lane",              "number of lanes"),
            ("@time_link",         "link time in minutes"),
            ("@time_inter",        "intersection delay time"),
            ("@sta_reliability",   "static reliability")
        ])
        time_name = {
            "_ea": "Early AM ", "_am": "AM Peak ", "_md": "Mid-day ", "_pm": "PM Peak ", "_ev": "Evening "
        }
        time_periods = ["_ea", "_am", "_md", "_pm", "_ev"]
        for attr, desc_tmplt in time_period_attrs.iteritems():
            for time in time_periods:
                traffic_attr_map["LINK"][attr + time] = \
                    (attr + time, "DERIVED", "EXTRA", time_name[time] + desc_tmplt)

        transit_attr_map = {
            "NODE": OrderedDict([
                ("@tap_id",   ("@tap_id",              "DERIVED",  "EXTRA", "Transit-access point ID")),
            ]),
            "LINK": OrderedDict([
                ("TRCOV-ID",  ("@tcov_id",              "TWO_WAY", "EXTRA", "SANDAG-assigned link ID")),
                ("NM",        ("#name",                 "TWO_WAY", "STRING", "Street name")),
                ("FXNM",      ("#name_from",            "TWO_WAY", "STRING", "Cross street at the FROM end")),
                ("TXNM",      ("#name_to",              "TWO_WAY", "STRING", "Cross street name at the TO end")),
                ("DIR",       ("@direction_cardinal",   "TWO_WAY", "EXTRA",  "Link direction")),
                ("OSPD",      ("@speed_observed",       "TWO_WAY", "EXTRA", "Observed speed")),
                ("IYR",       ("@year_open_traffic",    "TWO_WAY", "EXTRA", "The year the link opened to traffic ")),
                ("IFC",       ("type",                  "TWO_WAY", "STANDARD", "")),
                ("IHOV",      ("@lane_restriction_tr",  "TWO_WAY", "EXTRA", "Link operation type")),
                ("ISPD",      ("@speed_posted_tr_l",    "TWO_WAY", "EXTRA", "Posted speed limit (mph)")),
                ("IMED",      ("@median",               "TWO_WAY", "EXTRA", "Median type")),
                ("TMO",       ("trtime_link_op",        "ONE_WAY", "INTERNAL", "Expanded to EA, MD and EV")),
                ("TMEA",      ("@trtime_link_ea",       "DERIVED", "EXTRA", "Early AM transit link time in minutes")),
                ("TMA",       ("@trtime_link_am",       "ONE_WAY", "EXTRA", "AM Peak transit link time in minutes")),
                ("TMMD",      ("@trtime_link_md",       "DERIVED", "EXTRA", "Mid-day transit link time in minutes")),
                ("TMP",       ("@trtime_link_pm",       "ONE_WAY", "EXTRA", "PM Peak transit link time in minutes")),
                ("TMEV",      ("@trtime_link_ev",       "DERIVED", "EXTRA", "Evening transit link time in minutes")),
                ("MINMODE",   ("@mode_hierarchy",       "TWO_WAY", "EXTRA", "Transit mode type")),
            ]),
            "TRANSIT_LINE": OrderedDict([
                ("AM_Headway",     ("@headway_am",        "TRRT",     "EXTRA",    "AM Peak actual headway")),
                ("PM_Headway",     ("@headway_pm",        "TRRT",     "EXTRA",    "PM Peak actual headway")),
                ("OP_Headway",     ("@headway_op",        "TRRT",     "EXTRA",    "Off-Peak actual headway")),
                ("Night_Headway",  ("@headway_night",     "TRRT",     "EXTRA",    "Night actual headway")),
                ("AM_Headway_rev", ("@headway_rev_am",    "DERIVED",  "EXTRA",    "AM Peak revised headway")),
                ("PM_Headway_rev", ("@headway_rev_pm",    "DERIVED",  "EXTRA",    "PM Peak revised headway")),
                ("OP_Headway_rev", ("@headway_rev_op",    "DERIVED",  "EXTRA",    "Off-Peak revised headway")),
                ("WT_IVTPK",       ("@vehicle_per_pk",    "MODE5TOD", "EXTRA",    "Peak in-vehicle perception factor")),
                ("WT_IVTOP",       ("@vehicle_per_op",    "MODE5TOD", "EXTRA",    "Off-Peak in-vehicle perception factor")),
                ("WT_FAREPK",      ("@fare_per_pk",       "MODE5TOD", "EXTRA",    "Peak fare perception factor")),
                ("WT_FAREOP",      ("@fare_per_op",       "MODE5TOD", "EXTRA",    "Off-Peak fare perception factor")),
                ("DWELLTIME",      ("default_dwell_time", "MODE5TOD", "INTERNAL", "")),
                ("Fare",           ("@fare",              "TRRT",     "EXTRA",    "Boarding fare ($)")),
                ("@transfer_penalty",("@transfer_penalty","DERIVED",  "EXTRA",    "Transfer penalty (min)")),
                ("Route_ID",       ("@route_id",          "TRRT",     "EXTRA",    "Transit line internal ID")),
                ("Night_Hours",    ("@night_hours",       "TRRT",     "EXTRA",    "Night hours")),
                ("Config",         ("@config",            "TRRT",     "EXTRA",    "Config ID (same as route name)")),
            ]),
            "TRANSIT_SEGMENT": OrderedDict([
                ("Stop_ID",       ("@stop_id",       "TRSTOP", "EXTRA", "Stop ID from trcov")),
                ("Pass_Count",    ("@pass_count",    "TRSTOP", "EXTRA", "Number of times this stop is passed")),
                ("Milepost",      ("@milepost",      "TRSTOP", "EXTRA", "Distance from start of line")),
                ("FareZone",      ("@fare_zone",     "TRSTOP", "EXTRA", "Fare zone ID")),
                ("StopName",      ("#stop_name",     "TRSTOP", "STRING", "Name of stop")),
                ("@coaster_fare_board", ("@coaster_fare_board",   "DERIVED",  "EXTRA", "Boarding fare for coaster")),
                ("@coaster_fare_inveh", ("@coaster_fare_inveh",   "DERIVED",  "EXTRA", "Incremental fare for Coaster")),
            ])
        }

        create_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.create_scenario")

        file_names = [
            "hwycov.e00", "LINKTYPETURNS.DBF", "turns.csv",
            "trcov.e00", "trrt.csv", "trlink.csv", "trstop.csv",
            "timexfer_EA.csv", "timexfer_AM.csv","timexfer_MD.csv",
            "timexfer_PM.csv","timexfer_EV.csv","MODE5TOD.csv",
        ]
        for name in file_names:
            file_path = _join(self.source, name)
            if not os.path.exists(file_path):
                raise Exception("missing file '%s' in directory %s" % (name, self.source))

        title = self.title
        if not title:
            existing_scenario = self.emmebank.scenario(self.merged_scenario_id)
            if existing_scenario:
                title = existing_scenario.title

        def create_attributes(scenario, attr_map):
            for elem_type, mapping in attr_map.iteritems():
                for name, _tcoved_type, emme_type, desc in mapping.values():
                    if emme_type == "EXTRA":
                        if not scenario.extra_attribute(name):
                            xatt = scenario.create_extra_attribute(elem_type, name)
                            xatt.description =  desc
                    elif emme_type == "STRING":
                        if not scenario.network_field(elem_type, name):
                            scenario.create_network_field(elem_type, name, 'STRING', description=desc)

        if self.traffic_scenario_id:
            traffic_scenario = create_scenario(
                self.traffic_scenario_id, title + " Traffic",
                overwrite=self.overwrite, emmebank=self.emmebank)
            create_attributes(traffic_scenario, traffic_attr_map)
        else:
            traffic_scenario = None
        if self.transit_scenario_id:
            transit_scenario = create_scenario(
                self.transit_scenario_id, title + " Transit",
                overwrite=self.overwrite, emmebank=self.emmebank)
            create_attributes(transit_scenario, transit_attr_map)
        else:
            transit_scenario = None
        if self.merged_scenario_id:
            scenario = create_scenario(
                self.merged_scenario_id, title,
                overwrite=self.overwrite, emmebank=self.emmebank)
            create_attributes(scenario, traffic_attr_map)
            create_attributes(scenario, transit_attr_map)
        else:
            scenario = traffic_scenario or transit_scenario

        traffic_network = _network.Network()
        transit_network = _network.Network()

        try:
            if self.traffic_scenario_id or self.merged_scenario_id:
                for elem_type, attrs in traffic_attr_map.iteritems():
                    log_content = []
                    for k, v in attrs.iteritems():
                        if v[3] == "DERIVED":
                            k = "--"
                        log_content.append([k] + list(v))
                    self._log.append({
                        "content": log_content,
                        "type": "table",
                        "header": ["TCOVED", "Emme", "Source", "Type", "Description"],
                        "title": "Traffic %s attributes" % elem_type.lower().replace("_", " "),
                        "disclosure": True
                    })
                try:
                    self.create_traffic_base(traffic_network, traffic_attr_map)
                    self.create_turns(traffic_network)
                    self.calc_traffic_attributes(traffic_network)
                    self.check_zone_access(traffic_network, traffic_network.mode("d"))
                finally:
                    if traffic_scenario:
                        traffic_scenario.publish_network(traffic_network, resolve_attributes=True)

            if self.transit_scenario_id or self.merged_scenario_id:
                for elem_type, attrs in transit_attr_map.iteritems():
                    log_content = []
                    for k, v in attrs.iteritems():
                        if v[3] == "DERIVED":
                            k = "--"
                        log_content.append([k] + list(v))
                    self._log.append({
                        "content": log_content,
                        "type": "table",
                        "header": ["TCOVED", "Emme", "Source", "Type", "Description"],
                        "title": "Transit %s attributes" % elem_type.lower().replace("_", " "),
                        "disclosure": True
                    })
                try:
                    self.create_transit_base(transit_network, transit_attr_map)
                    self.create_transit_lines(transit_network, transit_attr_map)
                    self.calc_transit_attributes(transit_network)
                finally:
                    if transit_scenario:
                        for link in transit_network.links():
                            if link.type <= 0:
                                link.type = 99
                        transit_scenario.publish_network(transit_network, resolve_attributes=True)
                    if self.merged_scenario_id:
                        self.add_transit_to_traffic(traffic_network, transit_network)
        finally:
            if self.merged_scenario_id:
                scenario.publish_network(traffic_network, resolve_attributes=True)

        self.set_functions(scenario)
        self.check_connectivity(scenario)

    def create_traffic_base(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import traffic base network from hwycov.e00"})
        hwy_data = gen_utils.DataTableProc("ARC", _join(self.source, "hwycov.e00"))

        if self.save_data_tables:
            hwy_data.save("%s_hwycov" % self.data_table_name, self.overwrite)

        for elem_type in "NODE", "TURN":
            mapping = attr_map.get(elem_type)
            if not mapping:
                continue
            for field, (attr, tcoved_type, emme_type, desc) in mapping.iteritems():
                default = "" if emme_type == "STRING" else 0
                network.create_attribute(elem_type, attr, default)

        # Create Modes
        dummy_auto = network.create_mode("AUTO", "d")
        hov2 = network.create_mode("AUX_AUTO", "h")
        hov2_toll = network.create_mode("AUX_AUTO", "H")
        hov3 = network.create_mode("AUX_AUTO", "i")
        hov3_toll = network.create_mode("AUX_AUTO", "I")
        sov = network.create_mode("AUX_AUTO", "s")
        sov_toll = network.create_mode("AUX_AUTO", "S")
        heavy_trk = network.create_mode("AUX_AUTO", "v")
        heavy_trk_toll = network.create_mode("AUX_AUTO", "V")
        medium_trk = network.create_mode("AUX_AUTO", "m")
        medium_trk_toll = network.create_mode("AUX_AUTO", "M")
        light_trk = network.create_mode("AUX_AUTO", "t")
        light_trk_toll = network.create_mode("AUX_AUTO", "T")

        dummy_auto.description = "dummy auto"
        sov.description = "SOV"
        hov2.description = "HOV2"
        hov3.description = "HOV3+"
        light_trk.description = "TRKL"
        medium_trk.description = "TRKM"
        heavy_trk.description = "TRKH"

        sov_toll.description = "SOV TOLL"
        hov2_toll.description = "HOV2 TOLL"
        hov3_toll.description = "HOV3+ TOLL"
        light_trk_toll.description = "TRKL TOLL"
        medium_trk_toll.description = "TRKM TOLL"
        heavy_trk_toll.description = "TRKH TOLL"

        is_centroid = lambda arc, node : (arc["IFC"] == 10)  and (node == "AN")

        # Note: only truck types 1, 3, 4, and 7 found in 2012 base network
        modes_gp_lanes= {
            1: set([dummy_auto, sov, hov2, hov3, light_trk, medium_trk, heavy_trk,
                    sov_toll, hov2_toll, hov3_toll, light_trk_toll, medium_trk_toll,
                    heavy_trk_toll]),
            2: set([dummy_auto, sov, hov2, hov3, light_trk, medium_trk,
                    sov_toll, hov2_toll, hov3_toll, light_trk_toll, medium_trk_toll]),
            3: set([dummy_auto, sov, hov2, hov3, light_trk, sov_toll, hov2_toll,
                    hov3_toll, light_trk_toll]),
            4: set([dummy_auto, sov, hov2, hov3, sov_toll, hov2_toll, hov3_toll]),
            5: set([dummy_auto, heavy_trk, heavy_trk_toll]),
            6: set([dummy_auto, medium_trk, heavy_trk, medium_trk_toll, heavy_trk_toll]),
            7: set([dummy_auto, light_trk, medium_trk, heavy_trk, light_trk_toll,
                    medium_trk_toll, heavy_trk_toll]),
        }
        modes_toll_lanes = {
            1: set([dummy_auto, sov_toll, hov2_toll, hov3_toll, light_trk_toll,
                    medium_trk_toll, heavy_trk_toll]),
            2: set([dummy_auto, sov_toll, hov2_toll, hov3_toll, light_trk_toll,
                    medium_trk_toll]),
            3: set([dummy_auto, sov_toll, hov2_toll, hov3_toll, light_trk_toll]),
            4: set([dummy_auto, sov_toll, hov2_toll, hov3_toll]),
            5: set([dummy_auto, heavy_trk_toll]),
            6: set([dummy_auto, medium_trk_toll, heavy_trk_toll]),
            7: set([dummy_auto, light_trk_toll, medium_trk_toll, heavy_trk_toll]),
        }
        modes_HOV2 = set([dummy_auto, hov2, hov3, hov2_toll, hov3_toll])
        modes_HOV3 = set([dummy_auto, hov3, hov3_toll])


        def define_modes(arc):
            if arc["IFC"] == 10:  # connector
                return modes_gp_lanes[1]
            elif arc["IHOV"] == 1:
                return modes_gp_lanes[arc["ITRUCK"]]
            elif arc["IHOV"] == 2:
                # managed lanes, free for HOV2 and HOV3+, tolls for SOV
                if arc["ITOLLO"] + arc["ITOLLA"] + arc["ITOLLP"] > 0:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV2
                # special case of I-15 managed lanes base year and 2020, no build
                elif arc["IFC"] == 1 and arc["IPROJ"] in [41, 42, 486, 373, 711]:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV2
                elif arc["IFC"] == 8 or arc["IFC"] == 9:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV2
                else:
                    return modes_HOV2
            elif arc["IHOV"] == 3:
                # managed lanes, free for HOV3+, tolls for SOV and HOV2
                if arc["ITOLLO"] + arc["ITOLLA"] + arc["ITOLLP"]  > 0:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV3
                # special case of I-15 managed lanes for base year and 2020, no build
                elif arc["IFC"] == 1 and arc["IPROJ"] in [41, 42, 486, 373, 711]:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV3
                elif arc["IFC"] == 8 or arc["IFC"] == 9:
                    return modes_toll_lanes[arc["ITRUCK"]] | modes_HOV3
                else:
                    return modes_HOV3
            elif arc["IHOV"] == 4:
                return modes_toll_lanes[arc["ITRUCK"]]
            else:
                return modes_gp_lanes[arc["ITRUCK"]]

        self._create_base_net(
            hwy_data, network, mode_callback=define_modes, centroid_callback=is_centroid,
            arc_id_name="HWYCOV-ID", link_attr_map=attr_map["LINK"])
        self._log.append({"type": "text", "content": "Import traffic base network complete"})

    def create_transit_base(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import transit base network from trcov.e00"})
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
        transit_data = gen_utils.DataTableProc("ARC", _join(self.source, "trcov.e00"))

        if self.save_data_tables:
            transit_data.save("%s_trcov" % self.data_table_name, self.overwrite)

        # aux mode speed is always 3 (miles/hr)
        access = network.create_mode("AUX_TRANSIT", "a")
        transfer = network.create_mode("AUX_TRANSIT", "x")
        walk = network.create_mode("AUX_TRANSIT", "w")

        bus = network.create_mode("TRANSIT", "b")
        express_bus = network.create_mode("TRANSIT", "e")
        ltdexp_bus = network.create_mode("TRANSIT", "p")
        brt_red = network.create_mode("TRANSIT", "r")
        brt_yellow = network.create_mode("TRANSIT", "y")
        lrt = network.create_mode("TRANSIT", "l")
        coaster_rail = network.create_mode("TRANSIT", "c")
        tier1 = network.create_mode("TRANSIT", "o")

        access.description = "ACCESS"
        transfer.description = "TRANSFER"
        walk.description = "WALK"
        bus.description = "BUS"                  # (vehicle type 100, PCE=3.0)
        express_bus.description = "EXP BUS"      # (vehicle type 90 , PCE=3.0)
        ltdexp_bus.description = "LTDEXP BUS"    # (vehicle type 80 , PCE=3.0)
        lrt.description = "LRT"                  # (vehicle type 50)
        brt_yellow.description = "BRT YEL"       # (vehicle type 60 , PCE=3.0)
        brt_red.description = "BRT RED"          # (vehicle type 70 , PCE=3.0)
        coaster_rail.description = "CMR"         # (vehicle type 40)
        tier1.description = "TIER1"         # (vehicle type 45)

        access.speed = 3
        transfer.speed = 3
        walk.speed = 3

        # define TAP connectors as centroids
        is_centroid = lambda arc, node: (int(arc["MINMODE"]) == 3) and (node == "BN")

        mode_setting = {
            1:  set([transfer]),                                 # 1  = special transfer walk links between certain nearby stops
            2:  set([walk]),                                     # 2  = walk links in the downtown area
            3:  set([access]),                                   # 3  = the special TAP connectors
            4:  set([coaster_rail]),                             # 4  = Coaster Rail Line
            5:  set([lrt]),                                      # 5  = Light Rail Transit (LRT) Line
            6:  set([brt_yellow, ltdexp_bus, express_bus, bus]), # 6  = Yellow Car Bus Rapid Transit (BRT)
            7:  set([brt_red, ltdexp_bus, express_bus, bus]),    # 7  = Red Car Bus Rapid Transit (BRT)
            8:  set([ltdexp_bus, express_bus, bus]),             # 8  = Limited Express Bus
            9:  set([ltdexp_bus, express_bus, bus]),             # 9  = Express Bus
            10: set([ltdexp_bus, express_bus, bus]),             # 10 = Local Bus
        }
        tier1_rail_link_name = props["transit.newMode"]

        def define_modes(arc):
            if arc["NM"] == tier1_rail_link_name:
                return set([tier1])
            return mode_setting[arc["MINMODE"]]

        arc_filter = lambda arc: (arc["MINMODE"] > 2)

        # first pass to create the main base network for vehicles, xfer links and TAPs
        self._create_base_net(
            transit_data, network, mode_callback=define_modes, centroid_callback=is_centroid,
            arc_id_name="TRCOV-ID", link_attr_map=attr_map["LINK"], arc_filter=arc_filter)

        # second pass to add special walk links / modify modes on existing links
        reverse_dir_map = {1:3, 3:1, 2:4, 4:2, 0:0}

        def set_reverse_link(link, modes):
            reverse_link = link.reverse_link
            if reverse_link:
                reverse_link.modes |= modes
            else:
                reverse_link = network.create_link(link.j_node, link.i_node, modes)
                for attr in network.attributes("LINK"):
                    reverse_link[attr] = link[attr]
                reverse_link["@direction_cardinal"] = reverse_dir_map[link["@direction_cardinal"]]
                reverse_link["@tcov_id"] = -1*link["@tcov_id"]
                reverse_link.vertices = list(reversed(link.vertices))

        def epsilon_compare(a, b, epsilon):
            return abs((a - b) / (a if abs(a) > 1 else 1)) <= epsilon

        for arc in transit_data:
            # possible improvement: snap walk nodes to nearby node if not matched and within distance
            if arc_filter(arc):
                continue
            if float(arc["AN"]) == 0 or float(arc["BN"]) == 0:
                self._log.append({"type": "text",
                    "content": "Node ID 0 in AN (%s) or BN (%s) for link ID %s." %
                    (arc["AN"], arc["BN"], arc["TRCOV-ID"])})
                continue
            coordinates = arc["geo_coordinates"]
            arc_length = arc["LENGTH"] / 5280.0  # convert feet to miles
            i_node = get_node(network, arc['AN'], coordinates[0], False)
            j_node = get_node(network, arc['BN'], coordinates[-1], False)
            modes = define_modes(arc)
            link = network.link(i_node, j_node)
            split_link_case = False
            if link:
                link.modes |= modes
            else:
                # Note: additional cases of "tunnel" walk links could be
                #       considered to optimize network matching
                # check if this a special "split" link case where
                # we do not need to add a "tunnel" walk link
                for link1 in i_node.outgoing_links():
                    if split_link_case:
                        break
                    for link2 in link1.j_node.outgoing_links():
                        if link2.j_node == j_node:
                            if epsilon_compare(link1.length + link2.length, arc_length, 10**-5):
                                self._log.append({"type": "text",
                                    "content": "Walk link AN %s BN %s matched to two links TCOV-ID %s, %s" %
                                    (arc['AN'], arc['BN'], link1["@tcov_id"], link2["@tcov_id"])})
                                link1.modes |= modes
                                link2.modes |= modes
                                set_reverse_link(link1, modes)
                                set_reverse_link(link2, modes)
                                split_link_case = True
                                break
                if not split_link_case:
                    link = network.create_link(i_node, j_node, modes)
                    link.length = arc_length
                    if len(coordinates) > 2:
                        link.vertices = coordinates[1:-1]
            if not split_link_case:
                set_reverse_link(link, modes)
        self._log.append({"type": "text", "content": "Import transit base network complete"})

    def _create_base_net(self, data, network, mode_callback, centroid_callback, arc_id_name, link_attr_map, arc_filter=None):
        forward_attr_map = {}
        reverse_attr_map = {}
        for field, (name, tcoved_type, emme_type, desc) in link_attr_map.iteritems():
            if emme_type != "STANDARD":
                default = "" if emme_type == "STRING" else 0
                network.create_attribute("LINK", name, default)

            if field in [arc_id_name, "DIR"]:
                # these attributes are special cases for reverse link
                forward_attr_map[field] = name
            elif tcoved_type == "TWO_WAY":
                forward_attr_map[field] = name
                reverse_attr_map[field] = name
            elif tcoved_type == "ONE_WAY":
                forward_attr_map["AB" + field] = name
                reverse_attr_map["BA" + field] = name

        emme_id_name = forward_attr_map[arc_id_name]
        dir_name =  forward_attr_map["DIR"]
        reverse_dir_map = {1:3, 3:1, 2:4, 4:2, 0:0}
        new_node_id = max(data.values("AN").max(), data.values("BN").max()) + 1
        if arc_filter is None:
            arc_filter = lambda arc : True

        # Check if network editor file is in inputs
        network_editor_yaml_file = FILE_NAMES["NETWORK_EDITS"]
        network_editor_bool = False
        network_editor_yaml_path = _join(self.source, network_editor_yaml_file)
        self._log.append({"type": "header", "content": "__Network Edits"})
        if os.path.exists(network_editor_yaml_path):
            network_editor_bool = True
            with open(network_editor_yaml_path, "r") as stream:
                network_editor_data = yaml.safe_load(stream)
                self._log.append({"type": "text", "content": "Successfully loaded Network Edits YAML"})
                self._log.append({"type": "text", "content": "\tYAML Path: %s" % network_editor_yaml_path})
        else:
            self._log.append({"type": "text", "content": "NO Network Edits YAML to load"})

        # Create nodes and links
        for arc in data:
            if not arc_filter(arc):
                continue
            if float(arc["AN"]) == 0 or float(arc["BN"]) == 0:
                self._log.append({"type": "text",
                    "content": "Node ID 0 in AN (%s) or BN (%s) for link ID %s." %
                    (arc["AN"], arc["BN"], arc[arc_id_name])})
                continue
            coordinates = arc["geo_coordinates"]
            i_node = get_node(network, arc['AN'], coordinates[0], centroid_callback(arc, "AN"))
            j_node = get_node(network, arc['BN'], coordinates[-1], centroid_callback(arc, "BN"))
            existing_link = network.link(i_node, j_node)
            if existing_link:
                msg = "Duplicate link between AN %s and BN %s. Link IDs %s and %s." % \
                    (arc["AN"], arc["BN"], existing_link[emme_id_name], arc[arc_id_name])
                self._log.append({"type": "text", "content": msg})
                self._error.append(msg)
                self._split_link(network, i_node, j_node, new_node_id)
                new_node_id += 1

            #arc edits to raw_network
            if network_editor_bool:
                if arc_id_name == "HWYCOV-ID":
                    for link_edits in network_editor_data.get('raw_network',[]):
                        if arc["HWYCOV-ID"] in link_edits.get("@tcov_id",[]):
                            for attribute,value in link_edits["attributes_to_edit"].items():
                                arc[attribute] = value

            modes = mode_callback(arc)
            link = network.create_link(i_node, j_node, modes)
            link.length = arc["LENGTH"] / 5280.0  # convert feet to miles
            if len(coordinates) > 2:
                link.vertices = coordinates[1:-1]
            for field, attr in forward_attr_map.iteritems():
                link[attr] = arc[field]
            if arc["IWAY"] == 2 or arc["IWAY"] == 0:
                reverse_link = network.create_link(j_node, i_node, modes)
                reverse_link.length = link.length
                reverse_link.vertices = list(reversed(link.vertices))
                for field, attr in reverse_attr_map.iteritems():
                    reverse_link[attr] = arc[field]
                reverse_link[emme_id_name] = -1*arc[arc_id_name]
                reverse_link[dir_name] = reverse_dir_map[arc["DIR"]]

    def create_transit_lines(self, network, attr_map):
        self._log.append({"type": "header", "content": "Import transit lines"})
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
        fatal_errors = 0
        # Route_ID,Route_Name,Mode,AM_Headway,PM_Headway,OP_Headway,Night_Headway,Night_Hours,Config,Fare
        transit_line_data = gen_utils.DataTableProc("trrt", _join(self.source, "trrt.csv"))
        # Route_ID,Link_ID,Direction
        transit_link_data = gen_utils.DataTableProc("trlink", _join(self.source, "trlink.csv"))
        # Stop_ID,Route_ID,Link_ID,Pass_Count,Milepost,Longitude, Latitude,HwyNode,TrnNode,FareZone,StopName
        transit_stop_data = gen_utils.DataTableProc("trstop", _join(self.source, "trstop.csv"))
        # From_line,To_line,Board_stop,Wait_time
        # Note: Board_stop is not used
        #   Timed xfer data
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        timed_xfer_data = {}
        for period in periods:
            timed_xfer_data[period] = gen_utils.DataTableProc("timexfer_"+period, _join(self.source, "timexfer_"+period+".csv"))

        mode_properties = gen_utils.DataTableProc("MODE5TOD", _join(self.source, "MODE5TOD.csv"), convert_numeric=True)
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
        tier1 = network.create_transit_vehicle(45, 'o')  # 11 Tier 1

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
        for elem_type in "TRANSIT_LINE", "TRANSIT_SEGMENT", "NODE":
            mapping = attr_map[elem_type]
            for field, (attr, tcoved_type, emme_type, desc) in mapping.iteritems():
                default = "" if emme_type == "STRING" else 0
                network.create_attribute(elem_type, attr, default)
                if tcoved_type == "TRRT":
                    trrt_attrs.append((field, attr))
                elif tcoved_type == "MODE5TOD":
                    mode5tod_attrs.append((field, attr))

        # Pre-process transit line (trrt.csv) to know the route names for errors / warnings
        transit_line_records = list(transit_line_data)
        line_names = {}
        for record in transit_line_records:
            line_names[int(record["Route_ID"])] = record["Route_Name"].strip()

        links = dict((link["@tcov_id"], link) for link in network.links())
        transit_routes = _defaultdict(lambda: [])
        for record in transit_link_data:
            line_ref = line_names.get(int(record["Route_ID"]), record["Route_ID"])
            link_id = int(record["Link_ID"])
            if "+" in record["Direction"]:
                link = links.get(link_id)
            else:
                link = links.get(-1*link_id)
                if not link:
                    link = links.get(link_id)
                    if link and not link.reverse_link:
                        reverse_link = network.create_link(link.j_node, link.i_node, link.modes)
                        reverse_link.vertices = list(reversed(link.vertices))
                        for attr in network.attributes("LINK"):
                            if attr not in set(["vertices"]):
                                reverse_link[attr] = link[attr]
                        reverse_link["@tcov_id"] = -1 * link["@tcov_id"]
                        msg = "Transit line %s : Missing reverse link with ID %s (%s) (reverse link created)" % (
                            line_ref, record["Link_ID"], link)
                        self._log.append({"type": "text", "content": msg})
                        self._error.append("Transit route import: " + msg)
                        link = reverse_link
            if not link:
                msg = "Transit line %s : No link with ID %s, line not created" % (
                    line_ref, record["Link_ID"])
                self._log.append({"type": "text", "content": msg})
                self._error.append("Transit route import: " + msg)
                fatal_errors += 1
                continue
            transit_routes[int(record["Route_ID"])].append(link)

        # lookup list of special tier 1 mode route names
        tier1_rail_route_names = [str(n) for n in props["transit.newMode.route"]]
        dummy_links = set([])
        transit_lines = {}
        for record in transit_line_records:
            try:
                route = transit_routes[int(record["Route_ID"])]
                # Find if name matches one of the names listed in transit.newMode.route and convert to tier 1 rail
                is_tier1_rail = False
                for name in tier1_rail_route_names:
                    if str(record["Route_Name"]).startswith(name):
                        print 'record["Route_Name"]2', record["Route_Name"]
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
                        msg = "line %s : Links not adjacent, shortest path interpolation used (%s and %s)" % (
                            record["Route_Name"], prev_link["@tcov_id"], link["@tcov_id"])
                        log_record = {"type": "text", "content": msg}
                        self._log.append(log_record)
                        sub_path = find_path(prev_link, link, mode)
                        itinerary.extend(sub_path)
                        log_record["content"] = log_record["content"] + " through %s links" % (len(sub_path))
                    itinerary.append(link)
                    prev_link = link

                node_itinerary = [itinerary[0].i_node] +  [l.j_node for l in itinerary]
                try:
                    tline = network.create_transit_line(
                        record["Route_Name"].strip(), vehicle_type, node_itinerary)
                except:
                    msg = "Transit line %s : missing mode added to at least one link" % (
                        record["Route_Name"])
                    self._log.append({"type": "text", "content": msg})
                    for link in itinerary:
                        link.modes |= set([mode])
                    tline = network.create_transit_line(
                        record["Route_Name"].strip(), vehicle_type, node_itinerary)

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
                for segment in tline.segments():
                    segment.allow_boardings = False
                    segment.allow_alightings = False
                    segment.transit_time_func = 2
                    # ft2 = ul2 -> copied @trtime_link_XX
                    # segments on links matched to auto network (with auto mode) are changed to ft1 = timau
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
                     "content": "Stop %s: could not find transit line by ID %s (link ID %s)" % (
                        record["Stop_ID"], record["Route_ID"], record["Link_ID"])})

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
            segment = itinerary.next()
            tcov_id = abs(segment.link["@tcov_id"])
            for stop in stops:
                if "DUMMY" in stop["StopName"]:
                    continue
                link_id = int(stop['Link_ID'])
                node_id = int(stop['TrnNode'])
                while tcov_id != link_id:
                    segment = itinerary.next()
                    if segment.link is None:
                        break
                    tcov_id = abs(segment.link["@tcov_id"])

                if node_id == segment.i_node.number:
                    pass
                elif node_id == segment.j_node.number:
                    segment = itinerary.next()  # its the next segment
                else:
                    msg = "Transit line %s: could not find stop on link ID %s at node ID %s" % (line_name, link_id, node_id)
                    self._log.append({"type": "text", "content": msg})
                    self._error.append(msg)
                    fatal_errors += 1
                    continue
                segment.allow_boardings = True
                segment.allow_alightings = True
                segment.dwell_time = tline.default_dwell_time
                for field, attr in seg_string_attr_map:
                    segment[attr] = stop[field]
                for field, attr in seg_float_attr_map:
                    segment[attr] = float(stop[field])

        def lookup_line(ident):
            line = network.transit_line(ident)
            if line:
                return line.id
            line = transit_lines.get(int(ident))
            if line:
                return line.id
            raise Exception("'%s' is not a route name or route ID" % ident)

        # Normalizing the case of the headers as different examples have been seen
        for period in periods:
            norm_data = []
            for record in timed_xfer_data[period]:
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
            raise Exception("Cannot create transit network, %s fatal errors found" % fatal_errors)
        self._log.append({"type": "text", "content": "Import transit lines complete"})

    def calc_transit_attributes(self, network):
        self._log.append({"type": "header", "content": "Calculate derived transit attributes"})
        # - TM by 5 TOD periods copied from TM for 3 time periods
        # NOTE: the values of @trtime_link_## are only used for
        #       separate guideway.
        #       Links shared with the traffic network use the
        #       assignment results in timau
        for link in network.links():
            for time in ["_ea", "_md", "_ev"]:
                link["@trtime_link" + time] = link["trtime_link_op"]
            if link.type == 0:  # walk only links have IFC ==0
                link.type = 99

        # ON TRANSIT LINES
        # Set 3-period headway based on revised headway calculation
        for line in network.transit_lines():
            for period in ["am", "pm", "op"]:
                line["@headway_rev_" + period] = revised_headway(line["@headway_" + period])

        for c in network.centroids():
            c["@tap_id"] = c.number

        # Special incremental boarding and in-vehicle fares
        # to recreate the coaster zone fares
        fares_file_name = FILE_NAMES["FARES"]
        special_fare_path = _join(self.source, fares_file_name)
        if os.path.isfile(special_fare_path):
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
        else:
            # Default coaster fare for 2012 base year
            special_fares = {
                "boarding_cost": {
                    "base": [
                        {"line": "398104", "cost" : 4.0},
                        {"line": "398204", "cost" : 4.0}
                    ],
                    "stop_increment": [
                        {"line": "398104", "stop": "SORRENTO VALLEY", "cost": 0.5},
                        {"line": "398204", "stop": "SORRENTO VALLEY", "cost": 0.5}
                    ]
                },
                "in_vehicle_cost": [
                    {"line": "398104", "from": "SOLANA BEACH", "cost": 1.0},
                    {"line": "398104", "from": "SORRENTO VALLEY", "cost": 0.5},
                    {"line": "398204", "from": "OLD TOWN", "cost": 1.0},
                    {"line": "398204", "from": "SORRENTO VALLEY", "cost": 0.5}
                ],
                "day_pass": 5.0,
                "regional_pass": 12.0
            }
            self._log.append({"type": "text", "content": "Using default coaster fare based on 2012 base year setup."})

        def get_line(line_id):
            line = network.transit_line(line_id)
            if line is None:
                raise Exception("%s: line does not exist: %s" % (fares_file_name, line_id))
            return line

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

        # Check if network editor file is in inputs
        network_editor_yaml_file = FILE_NAMES["NETWORK_EDITS"]
        network_editor_bool = False
        network_editor_yaml_path = _join(self.source, network_editor_yaml_file)
        self._log.append({"type": "header", "content": "__Network Edits"})
        if os.path.exists(network_editor_yaml_path):
            network_editor_bool = True
            with open(network_editor_yaml_path, "r") as stream:
                network_editor_data = yaml.safe_load(stream)
                self._log.append({"type": "text", "content": "Successfully loaded Network Edits YAML"})
                self._log.append({"type": "text", "content": "\tYAML Path: %s" % network_editor_yaml_path})
        else:
            self._log.append({"type": "text", "content": "NO Network Edits YAML to load"})

        if network_editor_bool:
            for link_edits in network_editor_data.get('transit',[]):
                for link in network.links():
                    if link["@tcov_id"] in link_edits.get("@tcov_id",[]):
                        for attribute,value in link_edits["attributes_to_edit"].items():
                            link[attribute] = value

        self._log.append({"type": "text", "content": "Calculate derived transit attributes complete"})
        return

    def create_turns(self, network):
        self._log.append({"type": "header", "content": "Import turns and turn restrictions"})
        self._log.append({"type": "text", "content": "Process LINKTYPETURNS.DBF for turn prohibited by type"})
        # Process LINKTYPETURNS.DBF for turn prohibited by type
        with _fiona.open(_join(self.source, "LINKTYPETURNS.DBF"), 'r') as f:
            link_type_turns = _defaultdict(lambda: {})
            for record in f:
                record = record['properties']
                link_type_turns[record["FROM"]][record["TO"]] = {
                    "LEFT": record["LEFT"],
                    "RIGHT": record["RIGHT"],
                    "STRAIGHT": record["STRAIGHT"],
                    "UTURN": record["UTURN"]
                }
        for from_link in network.links():
            if from_link.type in link_type_turns:
                to_link_turns = link_type_turns[from_link.type]
                for to_link in from_link.j_node.outgoing_links():
                    if to_link.type in to_link_turns:
                        record = to_link_turns[to_link.type]
                        if not from_link.j_node.is_intersection:
                            network.create_intersection(from_link.j_node)
                        turn = network.turn(from_link.i_node, from_link.j_node, to_link.j_node)
                        turn.penalty_func = 1
                        if to_link["@tcov_id"] == from_link["left_link"]:
                            turn.data1 = record["LEFT"]
                        elif to_link["@tcov_id"] == from_link["through_link"]:
                            turn.data1 = record["STRAIGHT"]
                        elif to_link["@tcov_id"] == from_link["right_link"]:
                            turn.data1 = record["RIGHT"]
                        else:
                            turn.data1 = record["UTURN"]

        self._log.append({"type": "text", "content": "Process turns.csv for turn prohibited by ID"})
        turn_data = gen_utils.DataTableProc("turns", _join(self.source, "turns.csv"))
        if self.save_data_tables:
            turn_data.save("%s_turns"  % self.data_table_name, self.overwrite)
        links = dict((link["@tcov_id"], link) for link in network.links())

        # Process turns.csv for prohibited turns from_id, to_id, penalty
        for i, record in enumerate(turn_data):
            from_link_id, to_link_id = int(record["from_id"]), int(record["to_id"])
            from_link, to_link = links[from_link_id], links[to_link_id]
            if from_link.j_node == to_link.i_node:
                pass
            elif from_link.j_node == to_link.j_node:
                to_link = to_link.reverse_link
            elif from_link.i_node == to_link.i_node:
                from_link = from_link.reverse_link
            elif from_link.i_node == to_link.j_node:
                from_link = from_link.reverse_link
                to_link = to_link.reverse_link
            else:
                msg = "Record %s: links are not adjacent %s - %s." % (i, from_link_id, to_link_id)
                self._log.append({"type": "text", "content": msg})
                self._error.append("Turn import: " + msg)
                continue
            if not from_link or not to_link:
                msg = "Record %s: links adjacent but in reverse direction %s - %s." % (i, from_link_id, to_link_id)
                self._log.append({"type": "text", "content": msg})
                self._error.append("Turn import: " + msg)
                continue

            node = from_link.j_node
            if not node.is_intersection:
                network.create_intersection(node)
            turn = network.turn(from_link.i_node, node, to_link.j_node)
            if not record["penalty"]:
                turn.penalty_func = 0  # prohibit turn
            else:
                turn.penalty_func = 1
                turn.data1 = float(record["penalty"])
        self._log.append({"type": "text", "content": "Import turns and turn restrictions complete"})

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
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
        try:
            aoc = float(props["aoc.fuel"]) + float(props["aoc.maintenance"])
        except ValueError:
            raise Exception("Error during float conversion for aoc.fuel or aoc.maintenance from sandag_abm.properties file")
        scenario_year = int(props["scenarioYear"])
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
            if link["@lane_restriction"] == 4 and link.type == 1:
                link["@speed_posted"] = 70

            link["@cost_operating"] = link.length * aoc

            # Expand off-peak TOD attributes, copy peak period attributes
            for time, src_time in zip(time_periods, src_time_periods):
                link["@lane" + time] = link["lane" + src_time]
                link["@time_link" + time] = link["time_link" + src_time]

                # add link delay (30 sec=0.5mins) to HOV connectors to discourage travel
                if link.type == 8 and (link["@lane_restriction"] == 2 or link["@lane_restriction"] == 3):
                    link["@time_link" + time] = link["@time_link" + time] + 0.375

		        # make speed on HOV lanes (70mph) the same as parallel GP lanes (65mph)
                # - set speed back to posted speed - increase travel time by (speed_adj/speed_posted)
                if link.type == 1 and (link["@lane_restriction"] == 2 or link["@lane_restriction"] == 3):
                    speed_adj = link["@speed_adjusted"]
                    speed_posted = link["@speed_posted"]
                    if speed_adj>0:
                        link["@time_link" + time] = (speed_adj/(speed_posted*1.0)) * link["@time_link" + time]

                link["@time_inter" + time] = link["time_inter" + src_time]
                link["@toll" + time] = link["toll" + src_time]

        # Check if network editor file is in inputs
        network_editor_yaml_file = FILE_NAMES["NETWORK_EDITS"]
        network_editor_bool = False
        network_editor_yaml_path = _join(self.source, network_editor_yaml_file)
        self._log.append({"type": "header", "content": "__Network Edits"})
        if os.path.exists(network_editor_yaml_path):
            network_editor_bool = True
            with open(network_editor_yaml_path, "r") as stream:
                network_editor_data = yaml.safe_load(stream)
                self._log.append({"type": "text", "content": "Successfully loaded Network Edits YAML"})
                self._log.append({"type": "text", "content": "\tYAML Path: %s" % network_editor_yaml_path})
        else:
            self._log.append({"type": "text", "content": "NO Network Edits YAML to load"})

        if network_editor_bool:
            for link_edits in network_editor_data.get('traffic',[]):
                for link in network.links():
                    if link["@tcov_id"] in link_edits.get("@tcov_id",[]):
                        for attribute,value in link_edits["attributes_to_edit"].items():
                            link[attribute] = value

        # if network_editor_bool:
        #     for link_edits in network_editor_data.get('traffic',[]):
        #         if link_edits.get('delete_link', False):
        #             for ij in link_edits['i-j']:
        #                 network.delete_link(ij[0], ij[1], cascade=True)
        #         else:
        #             for link in network.links():


        off_peak_factor_file = FILE_NAMES["OFF_PEAK"]
        if os.path.exists(_join(self.source, off_peak_factor_file)):
            msg = "Adjusting off-peak tolls based on factors from %s" % off_peak_factor_file
            self._log.append({"type": "text", "content": msg})
            tolled_links = list(link for link in network.links() if link["toll_op"] > 0)
            # NOTE: CSV Reader sets the field names to UPPERCASE for consistency
            with gen_utils.CSVReader(_join(self.source, off_peak_factor_file)) as r:
                for row in r:
                    name = row["FACILITY_NAME"]
                    ea_factor = float(row["OP_EA_FACTOR"])
                    md_factor = float(row["OP_MD_FACTOR"])
                    ev_factor = float(row["OP_EV_FACTOR"])
                    count = 0
                    for link in tolled_links:
                        if name in link["#name"]:
                            count += 1
                            link["@toll_ea"] = link["@toll_ea"] * ea_factor
                            link["@toll_md"] = link["@toll_md"] * md_factor
                            link["@toll_ev"] = link["@toll_ev"] * ev_factor

                    msg = "Facility name '%s' matched to %s links." % (name, count)
                    msg += " Adjusted off-peak period tolls EA: %s, MD: %s, EV: %s" % (ea_factor, md_factor, ev_factor)
                    self._log.append({"type": "text2", "content": msg})

        for link in network.links():
            factors = [(3.0/12.0), 1.0, (6.5/12.0), (3.5/3.0), (8.0/12.0)]
            for f, time, src_time in zip(factors, time_periods, src_time_periods):
                if link["capacity_link" + src_time] != 999999:
                    link["@capacity_link" + time] = f * link["capacity_link" + src_time]
                else:
                    link["@capacity_link" + time] = 999999
                if link["capacity_inter" + src_time] != 999999:
                    link["@capacity_inter" + time] = f * link["capacity_inter" + src_time]
                else:
                    link["@capacity_inter" + time] = 999999
                if link["@capacity_hourly" + src_time] != 0:
					link["@capacity_hourly" + src_time] = round(link["@capacity_hourly" + src_time])

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
            # @lane_restriction = 2 or 3 overrides hov2 and hov3 costs
            if link["@lane_restriction"] == 2:
                for _, time_factors in factors.iteritems():
                    time_factors["hov2"] = 0.0
                    time_factors["hov3"] = 0.0
            elif link["@lane_restriction"] == 3:
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

        self._log.append({"type": "text", "content": "Calculation and time period expansion of costs, tolls, capacities and times complete"})

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
                    speed_bin = link["@speed_posted"]
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
        # IFC   Description
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

    def add_transit_to_traffic(self, hwy_network, tr_network):
        if not self.merged_scenario_id or not hwy_network or not tr_network:
            return
        self._log.append({"type": "header", "content": "Merge transit network to traffic network"})
        fatal_errors = 0
        for tr_mode in tr_network.modes():
            hwy_mode = hwy_network.create_mode(tr_mode.type, tr_mode.id)
            hwy_mode.description = tr_mode.description
            hwy_mode.speed = tr_mode.speed
        for tr_veh in tr_network.transit_vehicles():
            hwy_veh = hwy_network.create_transit_vehicle(tr_veh.id, tr_veh.mode.id)
            hwy_veh.description = tr_veh.description
            hwy_veh.auto_equivalent = tr_veh.auto_equivalent
            hwy_veh.seated_capacity = tr_veh.seated_capacity
            hwy_veh.total_capacity = tr_veh.total_capacity

        for elem_type in ["NODE", "LINK", "TRANSIT_LINE", "TRANSIT_SEGMENT"]:
            for attr in tr_network.attributes(elem_type):
                if not attr in hwy_network.attributes(elem_type):
                    default = "" if attr.startswith("#") else 0
                    new_attr = hwy_network.create_attribute(elem_type, attr, default)

        hwy_link_index = dict((l["@tcov_id"], l) for l in hwy_network.links())
        hwy_node_position_index = dict(((n.x, n.y), n) for n in hwy_network.nodes())
        hwy_node_index = dict()
        not_matched_links = []
        for tr_link in tr_network.links():
            tcov_id = tr_link["@tcov_id"]
            if tcov_id == 0:
                i_node = hwy_node_position_index.get((tr_link.i_node.x, tr_link.i_node.y))
                j_node = hwy_node_position_index.get((tr_link.j_node.x, tr_link.j_node.y))
                if i_node and j_node:
                    hwy_link = hwy_network.link(i_node, j_node)
                else:
                    hwy_link = None
            else:
                hwy_link = hwy_link_index.get(tcov_id)
            if not hwy_link:
                not_matched_links.append(tr_link)
            else:
                hwy_node_index[tr_link.i_node] = hwy_link.i_node
                hwy_node_index[tr_link.j_node] = hwy_link.j_node
                hwy_link.modes |= tr_link.modes

        new_node_id = max(n.number for n in hwy_network.nodes())
        new_node_id = int(_ceiling(new_node_id / 10000.0) * 10000)
        bus_mode = tr_network.mode("b")

        def lookup_node(src_node, new_node_id):
            node = hwy_node_index.get(src_node)
            if not node:
                node = hwy_node_position_index.get((src_node.x, src_node.y))
                if not node:
                    node = hwy_network.create_regular_node(new_node_id)
                    new_node_id += 1
                    for attr in tr_network.attributes("NODE"):
                        node[attr] = src_node[attr]
                hwy_node_index[src_node] = node
            return node, new_node_id

        for tr_link in not_matched_links:
            i_node, new_node_id = lookup_node(tr_link.i_node, new_node_id)
            j_node, new_node_id = lookup_node(tr_link.j_node, new_node_id)
            # check for duplicate but different links
            # All cases to be logged and then an error raised at end
            ex_link = hwy_network.link(i_node, j_node)
            if ex_link:
                self._log.append({
                    "type": "text",
                    "content": "Duplicate links between the same nodes with different IDs in traffic/transit merge. "
                               "Traffic link ID %s, transit link ID %s." % (ex_link["@tcov_id"], tr_link["@tcov_id"])
                })
                self._error.append("Duplicate links with different IDs between traffic (%s) and transit (%s) networks" %
                                   (ex_link["@tcov_id"], tr_link["@tcov_id"]))
                self._split_link(hwy_network, i_node, j_node, new_node_id)
                new_node_id += 1
                fatal_errors += 1
            try:
                link = hwy_network.create_link(i_node, j_node, tr_link.modes)
            except Exception as error:
                self._log.append({
                    "type": "text",
                    "content": "Error creating link '%s', I-node '%s', J-node '%s'. Error message %s" %
                    (tr_link["@tcov_id"], i_node, j_node, error)
                })
                self._error.append("Cannot create transit link '%s' in traffic network" % tr_link["@tcov_id"])
                fatal_errors += 1
                continue
            hwy_link_index[tr_link["@tcov_id"]] = link
            for attr in tr_network.attributes("LINK"):
                link[attr] = tr_link[attr]
            link.vertices = tr_link.vertices

        # Create transit lines and copy segment data
        for tr_line in tr_network.transit_lines():
            itinerary = []
            for seg in tr_line.segments(True):
                itinerary.append(hwy_node_index[seg.i_node])
            try:
                hwy_line = hwy_network.create_transit_line(tr_line.id, tr_line.vehicle.id, itinerary)
            except Exception as error:
                msg = "Transit line %s, error message %s" % (tr_line.id, error)
                self._log.append({"type": "text", "content": msg})
                self._error.append("Cannot create transit line '%s' in traffic network" % tr_line.id)
                fatal_errors += 1
                continue
            for attr in hwy_network.attributes("TRANSIT_LINE"):
                hwy_line[attr] = tr_line[attr]
            for tr_seg, hwy_seg in _izip(tr_line.segments(True), hwy_line.segments(True)):
                for attr in hwy_network.attributes("TRANSIT_SEGMENT"):
                    hwy_seg[attr] = tr_seg[attr]

        # Change ttf from ft2 (fixed speed) to ft1 (congested auto time)
        auto_mode = hwy_network.mode("d")
        for hwy_link in hwy_network.links():
            if auto_mode in hwy_link.modes:
                for seg in hwy_link.segments():
                    seg.transit_time_func = 1
        if fatal_errors > 0:
            raise Exception("Cannot merge traffic and transit network, %s fatal errors found" % fatal_errors)

        self._log.append({"type": "text", "content": "Merge transit network to traffic network complete"})

    def _split_link(self, network, i_node, j_node, new_node_id):
        # Attribute types to maintain consistency for correspondence with incoming / outgoing link data
        periods = ["ea", "am", "md", "pm", "ev"]
        approach_attrs = ["@traffic_control", "@turn_thru", "@turn_right", "@turn_left",
                          "@lane_auxiliary", "@green_to_cycle_init"]
        for p_attr in ["@green_to_cycle_", "@time_inter_", "@cycle_"]:
            approach_attrs.extend([p_attr + p for p in periods])
        capacity_inter = ["@capacity_inter_" + p for p in periods]
        cost_attrs = ["@cost_operating"]
        for p_attr in ["@cost_lgt_truck_", "@cost_med_truck_", "@cost_hvy_truck_", "@cost_hov2_",
                       "@cost_hov3_", "@cost_auto_", "@time_link_", "@trtime_link_", "@toll_"]:
            cost_attrs.extend([p_attr + p for p in periods])
        approach_attrs = [a for a in approach_attrs if a in network.attributes("LINK")]
        capacity_inter = [a for a in capacity_inter if a in network.attributes("LINK")]
        cost_attrs = [a for a in cost_attrs if a in network.attributes("LINK")]

        new_node = network.split_link(i_node, j_node, new_node_id)

        # Correct attributes on the split links
        for link in new_node.incoming_links():
            link["#name_to"] = ""
            for attr in approach_attrs:
                link[attr] = 0
            for attr in capacity_inter:
                link[attr] = 999999
            for attr in cost_attrs:
                link[attr] = 0.5 * link[attr]
            link.volume_delay_func = 10
        for link in new_node.outgoing_links():
            link["#name_from"] = ""
            for attr in cost_attrs:
                link[attr] = 0.5 * link[attr]

    @_m.logbook_trace("Set database functions (VDF, TPF and TTF)")
    def set_functions(self, scenario):
        create_function = _m.Modeller().tool(
            "inro.emme.data.function.create_function")
        set_extra_function_params = _m.Modeller().tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        emmebank = self.emmebank
        for f_id in ["fd10", "fd11", "fd20", "fd21", "fd22", "fd23", "fd24", "fp1", "ft1", "ft2", "ft3", "ft4"]:
            function = emmebank.function(f_id)
            if function:
                emmebank.delete_function(function)

        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(_dir(self.source), "conf", "sandag_abm.properties"))
        smartSignalf_CL = props["smartSignal.factor.LC"]
        smartSignalf_MA = props["smartSignal.factor.MA"]
        smartSignalf_PA = props["smartSignal.factor.PA"]
        atdmf = props["atdm.factor"]

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
        create_function("ft1", "timau", emmebank=emmebank)
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
            result_matrix.name = "TEMP_SOV_TRAVEL_TIME"
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
                        "mode": "S",  # SOV toll mode
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
        report = _m.PageBuilder(title="Import network from TCOVED files report")
        try:
            if self._error:
                report.add_html("<div style='margin-left:10px'>Errors detected during import: %s</div>" % len(self._error))
                error_msg = ["<ul style='margin-left:10px'>"]
                for error in self._error:
                    error_msg.append("<li>%s</li>"  % error)
                error_msg.append("</ul>")
                report.add_html("".join(error_msg))
            else:
                report.add_html("No errors detected during import")

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


def get_node(network, number, coordinates, is_centroid):
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
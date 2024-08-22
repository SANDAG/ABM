#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// transit_assignment.py                                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# The Transit assignment tool runs the transit assignment and skims for each
# period on the current primary scenario.
#
# The Build transit network tool must be run first to prepare the scenario for
# assignment. Note that this tool must be run with the Transit database
# (under the Database_transit directory) open (as the active database in the
# Emme desktop).
#
#
# Inputs:
#   period: the time-of-day period, one of EA, AM, MD, PM, EV.
#   scenario: Transit assignment scenario
#   skims_only: Only run assignment for skim matrices, if True only two assignments
#       are run to generate the skim matrices for the BUS and ALL skim classes.
#       Otherwise, all 15 assignments are run to generate the total network flows.
#   num_processors: number of processors to use for the traffic assignments.
#
# Matrices:
#   All transit demand and skim matrices.
#   See list of matrices under report method.
#
# Script example:
"""
import inro.modeller as _m
import os
modeller = _m.Modeller()
desktop = modeller.desktop

build_transit_scen  = modeller.tool("sandag.assignment.build_transit_scenario")
transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
load_properties = modeller.tool('sandag.utilities.properties')

project_dir = os.path.dirname(desktop.project_path())
main_directory = os.path.dirname(project_dir)
props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
main_emmebank = os.path.join(project_dir, "Database", "emmebank")
scenario_id = 100
base_scenario = main_emmebank.scenario(scenario_id)

transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")

periods = ["EA", "AM", "MD", "PM", "EV"]
period_ids = list(enumerate(periods, start=int(scenario_id) + 1))
num_processors = "MAX-1"
scenarioYear = str(props["scenarioYear"])

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

omx_file = os.path.join(output_dir, "transit_skims.omx")
export_transit_skims(omx_file, periods, transit_scenario)
"""


TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict, OrderedDict
import contextlib as _context
import numpy

import os
import sys
import math


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TransitAssignment(_m.Tool(), gen_utils.Snapshot):

    period = _m.Attribute(str)
    scenario =  _m.Attribute(object)
    data_table_name = _m.Attribute(str)
    assignment_only = _m.Attribute(bool)
    skims_only = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.assignment_only = False
        self.skims_only = False
        self.scenario = _m.Modeller().scenario
        self.num_processors = "MAX-1"
        self.attributes = [
            "period", "scenario", "data_table_name", "assignment_only", "skims_only",  "num_processors"]
        self._dt_db = _m.Modeller().desktop.project.data_tables()
        self._matrix_cache = {}  # used to hold data for reporting and post-processing of skims
        self.acc_egr_walk_percep = "2"
        self.acc_egr_drive_percep = "2"
        self.acc_spd_fac = {"WALK": "3.0", "PNROUT": "25.0", "PNRIN": "3.0", "KNROUT": "25.0", "KNRIN": "3.0"}
        self.egr_spd_fac = {"WALK": "3.0", "PNROUT": "3.0", "PNRIN": "25.0", "KNROUT": "3.0", "KNRIN": "25.0"}
        

    def from_snapshot(self, snapshot):
        super(TransitAssignment, self).from_snapshot(snapshot)
        # custom from_snapshot to load scenario and database objects
        self.scenario = _m.Modeller().emmebank.scenario(self.scenario)
        return self

    def page(self):
        if not self.data_table_name:
            load_properties = _m.Modeller().tool('sandag.utilities.properties')
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            main_directory = os.path.dirname(project_dir)
            props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
            self.data_table_name = props["scenarioYear"]

        pb = _m.ToolPageBuilder(self)
        pb.title = "Transit assignment"
        pb.description = """Assign transit demand for the selected time period."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        options = [("EA", "Early AM"),
                   ("AM", "AM peak"),
                   ("MD", "Mid-day"),
                   ("PM", "PM peak"),
                   ("EV", "Evening")]
        pb.add_select("period", options, title="Period:")
        pb.add_select_scenario("scenario",
            title="Transit assignment scenario:")
        pb.add_text_box("data_table_name", title="Data table prefix name:", note="Default is the ScenarioYear")
        pb.add_checkbox("assignment_only", title=" ", label="Only assign trips (no skims)")
        pb.add_checkbox("skims_only", title=" ", label="Only run assignments relevant for skims")
        dem_utils.add_select_processors("num_processors", pb, self)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(
                self.period, self.scenario, self.data_table_name,
                self.assignment_only, self.skims_only, self.num_processors)
            run_msg = "Transit assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, scenario, data_table_name, assignment_only=False, skims_only=False,
                 num_processors="MAX-1"):
        attrs = {
            "period": period,
            "scenario": scenario.id,
            "data_table_name": data_table_name,
            "assignment_only": assignment_only,
            "skims_only": skims_only,
            "num_processors": num_processors,
            "self": str(self)
        }
        self.scenario = scenario
        # if not scenario.has_traffic_results:
        #     raise Exception("missing traffic assignment results for period %s scenario %s" % (period, scenario))
        emmebank = scenario.emmebank
        with self.setup(attrs):
            gen_utils.log_snapshot("Transit assignment", str(self), attrs)
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise Exception('period: unknown value - specify one of %s' % periods)
            num_processors = dem_utils.parse_num_processors(num_processors)
            params = self.get_perception_parameters(period)
            network = scenario.get_partial_network(
                element_types=["TRANSIT_LINE"], include_attributes=True)
            coaster_mode = network.mode("c")
            params["coaster_fare_percep"] = 0
            for line in list(network.transit_lines()):
                # get the coaster fare perception for use in journey levels
                if line.mode == coaster_mode:
                    params["coaster_fare_percep"] = line[params["fare"]]
                    break

            transit_passes = gen_utils.DataTableProc("%s_transit_passes" % data_table_name)
            transit_passes = {row["pass_type"]: row["cost"] for row in transit_passes}
            day_pass = float(transit_passes["day_pass"]) / 2.0
            regional_pass = float(transit_passes["regional_pass"]) / 2.0

            self.run_assignment(period, params, network, day_pass, skims_only, num_processors)

            if not assignment_only:

                transit_modes = ['WALK_LOC', 'PNRIN_LOC', 'KNRIN_LOC', 'PNROUT_LOC', 'KNROUT_LOC',
                                  'WALK_PRM', 'PNRIN_PRM', 'KNRIN_PRM', 'PNROUT_PRM', 'KNROUT_PRM',
                                  'WALK_MIX', 'PNRIN_MIX', 'KNRIN_MIX', 'PNROUT_MIX', 'KNROUT_MIX']

                for mode_name in transit_modes:
                    self.run_skims(period, mode_name, params, regional_pass, num_processors, network)

                # max_fare = day_pass for local bus and regional_pass for premium modes
                self.mask_allpen(period)
                self.mask_highvalues(period)
                self.report(period)

    @_context.contextmanager
    def setup(self, attrs):
        self._matrix_cache = {}  # initialize cache at beginning of run
        emmebank = self.scenario.emmebank
        period = attrs["period"]

        names = ["TEMP_IN_VEHICLE_COST", "TEMP_LAYOVER_BOARD", "TEMP_PERCEIVED_FARE"]
        for matrix in emmebank.matrices():
            if matrix.name in names:
                emmebank.delete_matrix(matrix)

        with _m.logbook_trace("Transit assignment for period %s" % period, attributes=attrs):
            with gen_utils.temp_matrices(emmebank, "FULL", 3) as matrices:
                matrices[0].name = "TEMP_IN_VEHICLE_COST"
                matrices[1].name = "TEMP_LAYOVER_BOARD"
                matrices[2].name = "TEMP_PERCEIVED_FARE"
                try:
                    yield
                finally:
                    self._matrix_cache = {}  # clear cache at end of run

    def get_perception_parameters(self, period):

        access = self.acc_egr_walk_percep
        egress = self.acc_egr_walk_percep

        # if amode in ["PNROUT", "KNROUT"]:
        #     access = self.acc_egr_drive_percep
        # elif amode in ["PNRIN", "KNRIN"]:
        #     egress = self.acc_egr_drive_percep
        perception_parameters = {
            "EA": {
                "access" : access,
                "egress": egress,
                "vot": 0.27,
                "init_wait": 1.5,
                "xfer_wait": 3.0,
                "walk": 2.0,
                "init_headway": "@headway_rev_ea",
                "xfer_headway": "@headway_ea",
                "fare": "@fare_per_op",
                "in_vehicle": "@vehicle_per_op",
                "fixed_rail_link_time": "@trtime",
                "fixed_bus_link_time": "@time_link_ea"
            },
            "AM": {
                "access" : access,
                "egress": egress,
                "vot": 0.27,
                "init_wait": 1.5,
                "xfer_wait": 3.0,
                "walk": 2.0,
                "init_headway": "@headway_rev_am",
                "xfer_headway": "@headway_am",
                "fare": "@fare_per_pk",
                "in_vehicle": "@vehicle_per_pk",
                "fixed_rail_link_time": "@trtime",
                "fixed_bus_link_time": "@time_link_am"
            },
            "MD": {
                "access" : access,
                "egress": egress,
                "vot": 0.27,
                "init_wait": 1.5,
                "xfer_wait": 3.0,
                "walk": 2.0,
                "init_headway": "@headway_rev_md",
                "xfer_headway": "@headway_md",
                "fare": "@fare_per_op",
                "in_vehicle": "@vehicle_per_op",
                "fixed_rail_link_time": "@trtime",
                "fixed_bus_link_time": "@time_link_md"
            },
            "PM": {
                "access" : access,
                "egress": egress,
                "vot": 0.27,
                "init_wait": 1.5,
                "xfer_wait": 3.0,
                "walk": 2.0,
                "init_headway": "@headway_rev_pm",
                "xfer_headway": "@headway_pm",
                "fare": "@fare_per_pk",
                "in_vehicle": "@vehicle_per_pk",
                "fixed_rail_link_time": "@trtime",
                "fixed_bus_link_time": "@time_link_pm"
            },
            "EV": {
                "access" : access,
                "egress": egress,
                "vot": 0.27,
                "init_wait": 1.5,
                "xfer_wait": 3.0,
                "walk": 2.0,
                "init_headway": "@headway_rev_ev",
                "xfer_headway": "@headway_ev",
                "fare": "@fare_per_op",
                "in_vehicle": "@vehicle_per_op",
                "fixed_rail_link_time": "@trtime",
                "fixed_bus_link_time": "@time_link_ev"
            }
        }
        return perception_parameters[period]

    def group_modes_by_fare(self, network, day_pass_cost):
        # Identify all the unique boarding fare values
        fare_set = {mode.id: _defaultdict(lambda:0)
                    for mode in network.modes()
                    if mode.type == "TRANSIT"}
        for line in network.transit_lines():
            fare_set[line.mode.id][line["@fare"]] += 1
        del fare_set['c']  # remove coaster mode, this fare is handled separately
        # group the modes relative to day_pass
        mode_groups = {
            "bus": [],       # have a bus fare, less than 1/2 day pass
            "day_pass": [],  # boarding fare is the same as 1/2 day pass
            "premium": []    # special premium services not covered by day pass
        }
        for mode_id, fares in list(fare_set.items()):
            try:
                max_fare = max(fares.keys())
            except ValueError:
                continue  # an empty set means this mode is unused in this period
            if numpy.isclose(max_fare, day_pass_cost, rtol=0.0001):
                mode_groups["day_pass"].append((mode_id, fares))
            elif max_fare < day_pass_cost:
                mode_groups["bus"].append((mode_id, fares))
            else:
                mode_groups["premium"].append((mode_id, fares))
        return mode_groups

    def all_modes_journey_levels(self, params, network, day_pass_cost):
        transfer_penalty = {"on_segments": {"penalty": "@transfer_penalty_s", "perception_factor": 5.0}}
        transfer_wait = {
            "effective_headways": "@headway_seg",
            "headway_fraction": 0.5,
            "perception_factor": params["xfer_wait"],
            "spread_factor": 1.0
        }
        mode_groups = self.group_modes_by_fare(network, day_pass_cost)

        def get_transition_rules(next_level):
            rules = []
            for name, group in list(mode_groups.items()):
                for mode_id, fares in group:
                    rules.append({"mode": mode_id, "next_journey_level": next_level[name]})
            rules.append({"mode": "c", "next_journey_level": next_level["coaster"]})
            return rules

        journey_levels = [
            {
                "description": "base",
                "destinations_reachable": False,
                "transition_rules": get_transition_rules({"bus": 1, "day_pass": 2, "premium": 3, "coaster": 4}),
                "boarding_time": {"global": {"penalty": 0, "perception_factor": 1}},
                "waiting_time": {
                    "effective_headways": params["init_headway"], "headway_fraction": 0.5,
                    "perception_factor": params["init_wait"], "spread_factor": 1.0
                },
                "boarding_cost": {
                    "on_lines": {"penalty": "@fare", "perception_factor": params["fare"]},
                    "on_segments": {"penalty": "@coaster_fare_board", "perception_factor": params["coaster_fare_percep"]},
                },
            },
            {
                "description": "boarded_bus",
                "destinations_reachable": True,
                "transition_rules": get_transition_rules({"bus": 2, "day_pass": 2, "premium": 5, "coaster": 5}),
                "boarding_time": transfer_penalty,
                "waiting_time": transfer_wait,
                "boarding_cost": {
                    # xfer from bus fare is on segments so circle lines get free transfer
                    "on_segments": {"penalty": "@xfer_from_bus", "perception_factor": params["fare"]},
                },
            },
            {
                "description": "day_pass",
                "destinations_reachable": True,
                "transition_rules": get_transition_rules({"bus": 2, "day_pass": 2, "premium": 5, "coaster": 5}),
                "boarding_time": transfer_penalty,
                "waiting_time": transfer_wait,
                "boarding_cost": {
                    "on_lines": {"penalty": "@xfer_from_day", "perception_factor": params["fare"]},
                },
            },
            {
                "description": "boarded_premium",
                "destinations_reachable": True,
                "transition_rules": get_transition_rules({"bus": 5, "day_pass": 5, "premium": 5, "coaster": 5}),
                "boarding_time": transfer_penalty,
                "waiting_time": transfer_wait,
                "boarding_cost": {
                    "on_lines": {"penalty": "@xfer_from_premium", "perception_factor": params["fare"]},
                },
            },
            {
                "description": "boarded_coaster",
                "destinations_reachable": True,
                "transition_rules": get_transition_rules({"bus": 5, "day_pass": 5, "premium": 5, "coaster": 5}),
                "boarding_time": transfer_penalty,
                "waiting_time": transfer_wait,
                "boarding_cost": {
                    "on_lines": {"penalty": "@xfer_from_coaster", "perception_factor": params["fare"]},
                },
            },
            {
                "description": "regional_pass",
                "destinations_reachable": True,
                "transition_rules": get_transition_rules({"bus": 5, "day_pass": 5, "premium": 5, "coaster": 5}),
                "boarding_time": transfer_penalty,
                "waiting_time": transfer_wait,
                "boarding_cost":  {
                    "on_lines": {"penalty": "@xfer_regional_pass", "perception_factor": params["fare"]},
                },
            }
        ]
        return journey_levels

    def filter_journey_levels_by_mode(self, modes, journey_levels):
        # remove rules for unused modes from provided journey_levels
        # (restrict to provided modes)
        journey_levels = _copy(journey_levels)
        for level in journey_levels:
            rules = level["transition_rules"]
            rules = [r for r in rules if r["mode"] in modes]
            level["transition_rules"] = rules
        # count level transition rules references to find unused levels
        num_levels = len(journey_levels)
        level_count = [0] * len(journey_levels)

        def follow_rule(next_level):
            level_count[next_level] += 1
            if level_count[next_level] > 1:
                return
            for rule in journey_levels[next_level]["transition_rules"]:
                follow_rule(rule["next_journey_level"])

        follow_rule(0)
        # remove unreachable levels
        # and find new index for transition rules for remaining levels
        level_map = {i:i for i in range(num_levels)}
        for level_id, count in reversed(list(enumerate(level_count))):
            if count == 0:
                for index in range(level_id, num_levels):
                    level_map[index] -= 1
                del journey_levels[level_id]
        # re-index remaining journey_levels
        for level in journey_levels:
            for rule in level["transition_rules"]:
                next_level = rule["next_journey_level"]
                rule["next_journey_level"] = level_map[next_level]
        return journey_levels

    @_m.logbook_trace("Transit assignment by demand set", save_arguments=True)
    def run_assignment(self, period, params, network, day_pass_cost, skims_only, num_processors):
        modeller = _m.Modeller()
        scenario = self.scenario
        emmebank = scenario.emmebank
        assign_transit = modeller.tool(
            "inro.emme.transit_assignment.extended_transit_assignment")

        walk_modes = ["w", "x", "k", "u"]
        walkIN_modes = ["w", "x", "k"]
        walkOUT_modes = ["w", "x", "u"]
        PNROUT_modes = ["k", "x", "f"]
        PNRIN_modes = ["x", "u", "g"]
        KNROUT_modes = ["k", "x", "q"]
        KNRIN_modes = ["x", "u", "j"]
        TNCOUT_modes = ["k", "x", "Q"]
        TNCIN_modes = ["x", "u", "J"]

        local_bus_mode = ["b"]
        premium_modes = ["c", "l", "e", "p", "r", "y", "o"]

        # get the generic all-modes journey levels table
        journey_levels = self.all_modes_journey_levels(params, network, day_pass_cost)
        local_bus_journey_levels = self.filter_journey_levels_by_mode(local_bus_mode, journey_levels)
        premium_modes_journey_levels = self.filter_journey_levels_by_mode(premium_modes, journey_levels)
        # All modes transfer penalty assignment uses penalty of 15 minutes
        for level in journey_levels[1:]:
            level["boarding_time"] =  {"global": {"penalty": 15, "perception_factor": 1}}

        base_spec = {
            "type": "EXTENDED_TRANSIT_ASSIGNMENT",
            "modes": [],
            "demand": "",
            "waiting_time": {
                "effective_headways": params["init_headway"], "headway_fraction": 0.5,
                "perception_factor": params["init_wait"], "spread_factor": 1.0
            },
            # Fare attributes
            "boarding_cost": {"global": {"penalty": 0, "perception_factor": 1}},
            "boarding_time": {"global": {"penalty": 0, "perception_factor": 1}},
            "in_vehicle_cost": {"penalty": "@coaster_fare_inveh",
                                "perception_factor": params["coaster_fare_percep"]},
            "in_vehicle_time": {"perception_factor": params["in_vehicle"]},
            "aux_transit_time": {"perception_factor": params["walk"]},
            "aux_transit_cost": None,
            "journey_levels": [],
            "flow_distribution_between_lines": {"consider_total_impedance": False},
            "flow_distribution_at_origins": {
                "fixed_proportions_on_connectors": None,
                "choices_at_origins": "OPTIMAL_STRATEGY"
            },
            "flow_distribution_at_regular_nodes_with_aux_transit_choices": {
                "choices_at_regular_nodes": "OPTIMAL_STRATEGY"
            },
			#"circular_lines": {
			#	"stay": True
            #},
            "connector_to_connector_path_prohibition": None,
            "od_results": {"total_impedance": None},
            "performance_settings": {"number_of_processors": num_processors}
        }

        skim_parameters = OrderedDict([
                    ("WALK_LOC", {
                        "modes": walk_modes + local_bus_mode,
                        "journey_levels": local_bus_journey_levels
                    }),

                    ("PNRIN_LOC", {
                        "modes": walkOUT_modes + local_bus_mode + PNRIN_modes,
                        "journey_levels": local_bus_journey_levels
                    }),                
                    ("KNRIN_LOC", {
                        "modes": walkOUT_modes + local_bus_mode + KNRIN_modes,
                        "journey_levels": local_bus_journey_levels
                    }),
                    ("PNROUT_LOC", {
                        "modes": walkIN_modes + local_bus_mode + PNROUT_modes,
                        "journey_levels": local_bus_journey_levels
                    }),                
                    ("KNROUT_LOC", {
                        "modes": walkIN_modes + local_bus_mode + KNROUT_modes,
                        "journey_levels": local_bus_journey_levels
                    }),
                    ("TNCIN_LOC", {
                        "modes": walkOUT_modes + local_bus_mode + TNCIN_modes,
                        "journey_levels": local_bus_journey_levels
                    }),
                    ("TNCOUT_LOC", {
                        "modes": walkIN_modes + local_bus_mode + TNCOUT_modes,
                        "journey_levels": local_bus_journey_levels
                    }),
                    ("WALK_PRM", {
                        "modes": walk_modes + premium_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),
                    ("PNRIN_PRM", {
                        "modes": walkOUT_modes + premium_modes + PNRIN_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),                
                    ("KNRIN_PRM", {
                        "modes": walkOUT_modes + premium_modes + KNRIN_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),
                    ("PNROUT_PRM", {
                        "modes": walkIN_modes + premium_modes + PNROUT_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),                
                    ("KNROUT_PRM", {
                        "modes": walkIN_modes + premium_modes + KNROUT_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),
                    ("TNCIN_PRM", {
                        "modes": walkOUT_modes + premium_modes + TNCIN_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),
                    ("TNCOUT_PRM", {
                        "modes": walkIN_modes + premium_modes + TNCOUT_modes,
                        "journey_levels": premium_modes_journey_levels
                    }),
                    ("WALK_MIX", {
                        "modes": walk_modes + premium_modes + local_bus_mode,
                        "journey_levels": journey_levels
                    }),
                    ("PNRIN_MIX", {
                        "modes": walkOUT_modes + premium_modes + local_bus_mode + PNRIN_modes,
                        "journey_levels": journey_levels
                    }),                
                    ("KNRIN_MIX", {
                        "modes": walkOUT_modes + premium_modes + local_bus_mode + KNRIN_modes,
                        "journey_levels": journey_levels
                    }),
                    ("PNROUT_MIX", {
                        "modes": walkIN_modes + premium_modes + local_bus_mode + PNROUT_modes,
                        "journey_levels": journey_levels
                    }),                
                    ("KNROUT_MIX", {
                        "modes": walkIN_modes + premium_modes + local_bus_mode + KNROUT_modes,
                        "journey_levels": journey_levels
                    }),
                    ("TNCIN_MIX", {
                        "modes": walkOUT_modes + premium_modes + local_bus_mode + TNCIN_modes,
                        "journey_levels": journey_levels
                    }),
                    ("TNCOUT_MIX", {
                        "modes": walkIN_modes + premium_modes + local_bus_mode + TNCOUT_modes,
                        "journey_levels": journey_levels
                    }),
                    
                ])

        if skims_only:
            access_modes =  ["WLK", "PNR", "KNR", "TNC"]
        else:
            access_modes = ["WLK", "PNR", "KNR", "TNC"]

        add_volumes = False

        for mode_name, skim_params in skim_parameters.items():
            _m.logbook_trace(name="Running skims for %s, using params %s" % (mode_name, skim_params))
            spec = _copy(base_spec)
            name = "%s__%s" % (mode_name, period)
            spec["modes"] = skim_params["modes"]
            spec["demand"] = '%s_%s' % (period, mode_name)
            spec["journey_levels"] = skim_params["journey_levels"]
            _m.logbook_trace(name="spec: %s" % (spec))
            assign_transit(spec, class_name=name, add_volumes=add_volumes, scenario=self.scenario)
            add_volumes = True

    @_m.logbook_trace("Extract skims", save_arguments=True)
    def run_skims(self, period, mode_name, params, max_fare, num_processors, network):
        modeller = _m.Modeller()
        scenario = self.scenario
        emmebank = scenario.emmebank
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        network_calc = modeller.tool(
            "inro.emme.network_calculation.network_calculator")
        matrix_results = modeller.tool(
            "inro.emme.transit_assignment.extended.matrix_results")
        path_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.path_based_analysis")
        strategy_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.strategy_based_analysis")

        class_name = "%s__%s" % (mode_name, period)
        # skim_name = "%s__%s" % (mode_name, period)
        self.run_skims.logbook_cursor.write(name="Extract skims for %s, using assignment class %s" % (mode_name, class_name))

        with _m.logbook_trace("First and total wait time, number of boardings, fares, total walk time, in-vehicle time"):
            # First and total wait time, number of boardings, fares, total walk time, in-vehicle time
            spec = {
                "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                "actual_first_waiting_times": 'mf"%s_FIRSTWAIT__%s"' % (mode_name, period),
                "actual_total_waiting_times": 'mf"%s_TOTALWAIT__%s"' % (mode_name, period),
                # "total_impedance": 'mf"%s_GENCOST__%s"' % (mode_name, period),
                "by_mode_subset": {
                    "modes": [mode.id for mode in network.modes() if mode.type == "TRANSIT" or mode.type == "AUX_TRANSIT"],
                    "avg_boardings": 'mf"%s_XFERS__%s"' % (mode_name, period),
                    "actual_in_vehicle_times": 'mf"%s_TOTALIVTT__%s"' % (mode_name, period),
                    "actual_in_vehicle_costs": 'mf"TEMP_IN_VEHICLE_COST"',
                    "actual_total_boarding_costs": 'mf"%s_FARE__%s"' % (mode_name, period),
                    "perceived_total_boarding_costs": 'mf"TEMP_PERCEIVED_FARE"',
                    "actual_aux_transit_times": 'mf"%s_TOTALWALK__%s"' % (mode_name, period),
                },
            }
            matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
        with _m.logbook_trace("in-vehicle time by mode"): #distance taken out for 2-zone
            mode_combinations_mix = [
                ("BUS", ["b"],      ["IVTT"]),
                ("LRT", ["l"],      ["IVTT"]),
                ("CMR", ["c"],      ["IVTT"]),
                ("EXP", ["e"],      ["IVTT"]),
                ("LTDEXP", ["p"],   ["IVTT"]),
                ("BRT", ["r", "y"], ["IVTT"]),
                #("BRTRED", ["r"],   ["IVTT"]),
                #("BRTYEL", ["y"],   ["IVTT"]),
                # ("TIER1", ["o"],    ["IVTT"]),
            ]
            mode_combinations_prm = [
                ("LRT", ["l"],      ["IVTT"]),
                ("CMR", ["c"],      ["IVTT"]),
                ("EXP", ["e"],      ["IVTT"]),
                ("LTDEXP", ["p"],   ["IVTT"]),
                ("BRT", ["r", "y"], ["IVTT"]),
                #("BRTRED", ["r"],   ["IVTT"]),
                #("BRTYEL", ["y"],   ["IVTT"]),
                # ("TIER1", ["o"],    ["IVTT"]),
            ]
            mode_combinations_loc = [
                ("BUS", ["b"],      ["IVTT"])
            ]
            if "LOC" in mode_name:
                for modename, modes, skim_types in mode_combinations_loc:
                    # dist = 'mf"%s_%sDIST"' % (skim_name, mode_name) if "DIST" in skim_types else None
                    ivtt = 'mf"%s_%sIVTT__%s"' % (mode_name, modename, period) if "IVTT" in skim_types else None
                    spec = {
                        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                        "by_mode_subset": {
                            "modes": modes,
                            # "distance": dist,
                            "actual_in_vehicle_times": ivtt,
                        },
                    }
                    matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            elif "PRM" in mode_name:
                for modename, modes, skim_types in mode_combinations_prm:
                    # dist = 'mf"%s_%sDIST"' % (skim_name, mode_name) if "DIST" in skim_types else None
                    ivtt = 'mf"%s_%sIVTT__%s"' % (mode_name, modename, period) if "IVTT" in skim_types else None
                    spec = {
                        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                        "by_mode_subset": {
                            "modes": modes,
                            # "distance": dist,
                            "actual_in_vehicle_times": ivtt,
                        },
                    }
                    matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            else:
                for modename, modes, skim_types in mode_combinations_mix:
                    # dist = 'mf"%s_%sDIST"' % (skim_name, mode_name) if "DIST" in skim_types else None
                    ivtt = 'mf"%s_%sIVTT__%s"' % (mode_name, modename, period) if "IVTT" in skim_types else None
                    spec = {
                        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                        "by_mode_subset": {
                            "modes": modes,
                            # "distance": dist,
                            "actual_in_vehicle_times": ivtt,
                        },
                    }
                    matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # Sum total distance
            # spec = {
            #     "type": "MATRIX_CALCULATION",
            #     "constraint": None,
            #     "result": 'mf"%s_TOTDIST"' % skim_name,
            #     "expression": ('mf"{0}_BUSDIST" + mf"{0}_LRTDIST" + mf"{0}_CMRDIST"'
            #                    ' + mf"{0}_EXPDIST" + mf"{0}_BRTDIST"  + mf"{0}_TIER1DIST"').format(skim_name),
            # }
            # matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # convert number of boardings to number of transfers
        # and subtract transfers to the same line at layover points
        with _m.logbook_trace("Number of transfers and total fare"):
            spec = {
                "trip_components": {"boarding": "@layover_board"},
                "sub_path_combination_operator": "+",
                "sub_strategy_combination_operator": "average",
                "selected_demand_and_transit_volumes": {
                    "sub_strategies_to_retain": "ALL",
                    "selection_threshold": {"lower": -999999, "upper": 999999}
                },
                "results": {
                    "strategy_values": 'TEMP_LAYOVER_BOARD',
                },
                "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
            }
            strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"%s_XFERS__%s"' % (mode_name, period),
                        "interval_min": 1, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"%s_XFERS__%s"' % (mode_name, period),
                "expression": '(%s_XFERS__%s - 1 - TEMP_LAYOVER_BOARD).max.0' % (mode_name, period),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # sum in-vehicle cost and boarding cost to get the fare paid
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"%s_FARE__%s"' % (mode_name, period),
                "expression": '(%s_FARE__%s + TEMP_IN_VEHICLE_COST).min.%s' % (mode_name, period, max_fare),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # walk access time - get distance and convert to time with 3 miles / hr
        with _m.logbook_trace("Walk time access, egress and xfer"):
            path_spec = {
                "portion_of_path": "ORIGIN_TO_INITIAL_BOARDING",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"%s_ACC__%s"' % (mode_name, period),
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # walk egress time - get distance and convert to time with 3 miles/ hr
            path_spec = {
                "portion_of_path": "FINAL_ALIGHTING_TO_DESTINATION",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"%s_EGR__%s"' % (mode_name, period)
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            acc_mode = mode_name.split('_')[0]

            spec_list = [
            {    # walk access time - convert to time with 3 miles/ hr
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"%s_ACC__%s"' % (mode_name, period),
                "expression": '60.0 * %s_ACC__%s / %s' % (mode_name, period, self.acc_spd_fac[acc_mode]),
            },
            {    # walk egress time - convert to time with 3 miles/ hr
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"%s_EGR__%s"' % (mode_name, period),
                "expression": '60.0 * %s_EGR__%s / %s' % (mode_name, period, self.egr_spd_fac[acc_mode]),
            },
            {   # transfer walk time = total - access - egress
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"%s_XFERWALK__%s"' % (mode_name, period),
                #"expression": '(%s_XFERWALK__%s).max.0' % (mode_name, period),
                "expression": '(%s_TOTALWALK__%s - %s_ACC__%s - %s_EGR__%s).max.0' % (mode_name, period, mode_name, period, mode_name, period),
            }]
            matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)

        # transfer wait time
        with _m.logbook_trace("Wait time - xfer"):
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"%s_TOTALWAIT__%s"' % (mode_name, period),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"%s_XFERWAIT__%s"' % (mode_name, period),
                "expression": '(%s_TOTALWAIT__%s - %s_FIRSTWAIT__%s).max.0'% (mode_name, period, mode_name, period),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        with _m.logbook_trace("Calculate dwell time"):
            with gen_utils.temp_attrs(scenario, "TRANSIT_SEGMENT", ["@dwt_for_analysis"]):
                values = scenario.get_attribute_values("TRANSIT_SEGMENT", ["dwell_time"])
                scenario.set_attribute_values("TRANSIT_SEGMENT", ["@dwt_for_analysis"], values)

                spec = {
                    "trip_components": {"in_vehicle": "@dwt_for_analysis"},
                    "sub_path_combination_operator": "+",
                    "sub_strategy_combination_operator": "average",
                    "selected_demand_and_transit_volumes": {
                        "sub_strategies_to_retain": "ALL",
                        "selection_threshold": {"lower": -999999, "upper": 999999}
                    },
                    "results": {
                        "strategy_values": 'mf"%s_DWELLTIME__%s"' % (mode_name, period),
                    },
                    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                }
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

        # expr_params = _copy(params)
        # expr_params["xfers"] = 15.0
        # expr_params["name"] = (mode_name, period)
        # spec = {
        #     "type": "MATRIX_CALCULATION",
        #     "constraint": None,
        #     "result": 'mf"%s_GENCOST__%s"' % (mode_name, period),
        #     "expression": ("{xfer_wait} * {name}_TOTALWAIT "
        #                    "- ({xfer_wait} - {init_wait}) * {name}_FIRSTWAIT "
        #                    "+ 1.0 * {name}_TOTALIVTT + 0.5 * {name}_BUSIVTT"
        #                    "+ (1 / {vot}) * (TEMP_PERCEIVED_FARE + {coaster_fare_percep} * TEMP_IN_VEHICLE_COST)"
        #                    "+ {xfers} *({name}_XFERS.max.0) "
        #                    "+ {walk} * {name}_TOTALWALK").format(**expr_params)
        # }
        # matrix_calc(spec, scenario=scenario, num_processors=num_processors)
        return

    def mask_allpen(self, period):
        # Reset skims to 0 if not both local and premium
        skims = [
            "FIRSTWAIT", "BUSIVTT", "XFERS", #"TOTALWALK",
            "LRTIVTT", "CMRIVTT", "EXPIVTT", "LTDEXPIVTT", "BRTIVTT", 
            "XFERWAIT", "FARE", "DWELLTIME",
            "ACC", "XFERWALK", "EGR", "TOTALIVTT"]
        
        with _m.logbook_trace("Reset Mix skims to 0 if not both local and premium"):

            for amode in ['WALK', 'PNRIN', 'PNROUT', 'KNRIN', 'KNROUT']:

                localivt_skim = self.get_matrix_data(amode + "_MIX_BUSIVTT" + "__" + period)
                totalivt_skim = self.get_matrix_data(amode + "_MIX_TOTALIVTT" + "__" + period)
                has_premium = numpy.greater((totalivt_skim - localivt_skim), 0)
                has_both = numpy.greater(localivt_skim, 0) * has_premium

                for skim in skims:
                    mat_name = amode + "_MIX_" + skim + "__" + period
                    data = self.get_matrix_data(mat_name)
                    self.set_matrix_data(mat_name, data * has_both)
        
    def mask_highvalues(self, period):
        
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        #Masking the skims required by ActivitySim
        skims = ['LOC_FIRSTWAIT', 'LOC_XFERWAIT', 'LOC_FARE', 'LOC_XFERS',  'LOC_ACC', 'LOC_XFERWALK', 'LOC_EGR', 'LOC_TOTALWALK',
                  'LOC_TOTALIVTT', 'LOC_DWELLTIME', 'LOC_BUSIVTT', 'PRM_FIRSTWAIT', 'PRM_XFERWAIT', 'PRM_FARE', 'PRM_XFERS', 'PRM_ACC',
                  'PRM_XFERWALK', 'PRM_EGR', 'PRM_TOTALWALK', 'PRM_TOTALIVTT', 'PRM_LRTIVTT', 'PRM_CMRIVTT', 'PRM_EXPIVTT', 
                  'PRM_LTDEXPIVTT', 'PRM_BRTIVTT', 'MIX_FIRSTWAIT', 'MIX_XFERWAIT', 'MIX_FARE', 'MIX_XFERS', 'MIX_ACC', 'MIX_XFERWALK',
                  'MIX_EGR', 'MIX_TOTALIVTT', 'MIX_BUSIVTT', 'MIX_LRTIVTT', 'MIX_CMRIVTT', 'MIX_EXPIVTT', 'MIX_LTDEXPIVTT', 'MIX_BRTIVTT']

        with _m.logbook_trace("Set high values to 0"):
            for amode in ['WALK', 'PNRIN', 'PNROUT', 'KNRIN', 'KNROUT']:
                for skim in skims:
                    name = amode + "_" + skim + "__" + period
                    # Set high values to 0
                    
                    spec = {
                        "type": "MATRIX_CALCULATION",
                        "constraint":{
                            "by_value": {
                                "od_values": name,
                                "interval_min": 0, "interval_max": 999999,
                                "condition": "EXCLUDE"},
                        },
                        "result": name,
                        "expression": '0',
                    }
                    matrix_calc(spec, scenario=self.scenario)

    def get_matrix_data(self, name):
        data = self._matrix_cache.get(name)
        if data is None:
            matrix = self.scenario.emmebank.matrix(name)
            data = matrix.get_numpy_data(self.scenario)
            self._matrix_cache[name] = data
        return data

    def set_matrix_data(self, name, data):
        matrix = self.scenario.emmebank.matrix(name)
        self._matrix_cache[name] = data
        matrix.set_numpy_data(data, self.scenario)

    def report(self, period):
        text = ['<div class="preformat">']
        init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
        matrices = init_matrices.get_matrix_names("transit_skims", [period], self.scenario)
        num_zones = len(self.scenario.zone_numbers)
        num_cells = num_zones ** 2
        text.append(
            "Number of zones: %s. Number of O-D pairs: %s. "
            "Values outside -9999999, 9999999 are masked in summaries.<br>" % (num_zones, num_cells))
        text.append("%-25s %9s %9s %9s %13s %9s" % ("name", "min", "max", "mean", "sum", "mask num"))
        for name in matrices:
            data = self.get_matrix_data(name)
            data = numpy.ma.masked_outside(data, -9999999, 9999999, copy=False)
            stats = (name, data.min(), data.max(), data.mean(), data.sum(), num_cells-data.count())
            text.append("%-25s %9.4g %9.4g %9.4g %13.7g %9d" % stats)
        text.append("</div>")
        title = 'Transit impedance summary for period %s' % period
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(title, report.render())
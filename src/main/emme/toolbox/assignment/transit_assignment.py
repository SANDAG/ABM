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

TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict
import contextlib as _context

import os
import sys
import math


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TransitAssignment(_m.Tool()):

    period = _m.Attribute(unicode)

    scenario_id = _m.Attribute(int)
    transit_database =  _m.Attribute(_m.InstanceType)
    base_scenario =  _m.Attribute(_m.InstanceType)

    skims_only = _m.Attribute(bool)
    transfer_limit = _m.Attribute(bool)
    timed_xfers_table = _m.Attribute(unicode)
    scenario_title = _m.Attribute(unicode)
    overwrite = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.skims_only = False
        self.transfer_limit = False
        self.timed_xfers_table = None
        self.base_scenario = _m.Modeller().scenario
        self.scenario_id = self.base_scenario.number + 200
        self.scenario_title = ""
        self.overwrite = False
        self.num_processors = "MAX-1"
        self._dt_db = _m.Modeller().desktop.project.data_tables()
        
    def page(self):
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

        pb.add_select_scenario("base_scenario", 
            title="Base scenario (with traffic and transit data):", allow_none=True,
            note="With period traffic results from main (traffic assignment) database")
        pb.add_select_database("transit_database", 
            title="Transit database:", allow_none=True)
        pb.add_text_box("scenario_id", title="ID for transit assignment scenario:")
        pb.add_text_box("scenario_title", title="Scenario title:", size=80)

        pb.add_checkbox("skims_only", title=" ", label="Only run assignments for skim matrices")
        pb.add_checkbox("transfer_limit", title=" ", label="Apply 3-transfer limit")        
        options = [(None, "")] + [(table.name, table.name) for table in self._dt_db.tables()]
        pb.add_select("timed_xfers_table", options, title="Timed transfer data table:",
            note="Normally used only with AM peak period assignment.")

        dem_utils.add_select_processors("num_processors", pb, self)
        pb.add_checkbox("overwrite", title=" ", label="Overwrite existing scenario")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            emmebank = self.transit_database.core_emmebank
            results = self(
                self.period, self.base_scenario, emmebank, self.scenario_id, self.scenario_title, 
                self.skims_only, self.transfer_limit, self.timed_xfers_table,
                self.num_processors, self.overwrite)
            run_msg = "Transit assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, base_scenario, emmebank, scenario_id, scenario_title="", 
                 skims_only=False, transfer_limit=False, timed_xfers_table=None,
                 num_processors="MAX-1", overwrite=False):
        modeller = _m.Modeller()
        attrs = {
            "period": period, 
            "scenario_id": scenario_id, 
            "emmebank": emmebank, 
            "base_scenario": base_scenario, 
            "skims_only": skims_only, 
            "transfer_limit": transfer_limit, 
            "timed_xfers_table": timed_xfers_table,
            "scenario_title": scenario_title, 
            "overwrite": overwrite,
            "num_processors": num_processors
        }
        with _m.logbook_trace("Transit assignment for period %s" % period, attributes=attrs):
            copy_scenario = modeller.tool(
                "inro.emme.data.scenario.copy_scenario")
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise Exception(
                    'period: unknown value - specify one of %s' % periods)
            num_processors = dem_utils.parse_num_processors(num_processors)

            if timed_xfers_table:
                timed_transfers_with_walk = list(gen_utils.DataTableProc(timed_xfers_table))

            perception_parameters = {
                "EA": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_ea"
                },
                "AM": {
                    "vot": 0.10, "init_wait": 1.5, "xfer_wait": 3.0, "walk": 1.8, 
                    "headway": "@headway_rev_am", "fare": "@fare_per_pk", "in_vehicle": "@vehicle_per_pk",
                    "fixed_link_time": "@trtime_link_am"
                },
                "MD": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_md"
                },
                "PM": {
                    "vot": 0.10, "init_wait": 1.5, "xfer_wait": 3.0, "walk": 1.8, 
                    "headway": "@headway_rev_pm", "fare": "@fare_per_pk", "in_vehicle": "@vehicle_per_pk",
                    "fixed_link_time": "@trtime_link_pm"
                },
                "EV": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_ev"
                },
            }        
            params = perception_parameters[period]
            
            if base_scenario:
                if emmebank.scenario(scenario_id):
                    if overwrite:
                        emmebank.delete_scenario(scenario_id)
                    else:
                        raise Exception("scenario_id: scenario %s already exists" % scenario_id)

                scenario = emmebank.create_scenario(scenario_id)
                scenario.title = scenario_title
                scenario.has_traffic_results = base_scenario.has_traffic_results
                scenario.has_transit_results = base_scenario.has_transit_results
                for attr in sorted(base_scenario.extra_attributes(), key=lambda x: x._id):
                    dst_attr = scenario.create_extra_attribute(attr.type, attr.name, attr.default_value)
                    dst_attr.description = attr.description
                for field in base_scenario.network_fields():
                    scenario.create_network_field(field.type, field.name, field.atype, field.description)
                network = base_scenario.get_network()
                new_attrs = ["@headway_seg", "@transfer_penalty_s", "@layover_board"]
                for attr in new_attrs:
                    scenario.create_extra_attribute("TRANSIT_SEGMENT", attr)
                    network.create_attribute("TRANSIT_SEGMENT", attr)

                self._init_node_id(network)
                coaster_mode = network.mode("c")
                for line in list(network.transit_lines()):
                    # Note: should do only if copying from base scenario, adjust fare_per by vot
                    line[params["fare"]] = line[params["fare"]] / params["vot"]
                    # remove the "unavailable" lines in this period
                    if line[params["headway"]] == 0:
                        network.delete_transit_line(line)
                    # get the coaster fare perception for use in journey levels
                    if line.mode == coaster_mode:
                        coaster_fare_percep = line[params["fare"]]
                for seg in network.transit_segments():
                    seg["@headway_seg"] = seg.line[params["headway"]]
                    seg["@transfer_penalty_s"] = seg.line["@transfer_penalty"]

                self.taps_to_centroids(network)
                if timed_xfers_table:
                    self.timed_transfers(network, timed_transfers_with_walk, period)
                self.connect_circle_lines(network)
                self.duplicate_tap_adajcent_stops(network, params["headway"])
                # The fixed guideway travel times are stored in "@trtime_link_xx"
                # and copied to data2 (ul2) for the ttf 
                # The congested auto times for mixed traffic are in "timau" 
                # (output from traffic assignment)
                values = network.get_attribute_values("LINK", [params["fixed_link_time"]])
                network.set_attribute_values("LINK", ["data2"], values)
                scenario.publish_network(network)
            else:
                # Run on specified existing scenario instead
                scenario = emmebank.scenario(scenario_id)
                if not scenario:
                    raise Exception(
                        "scenario_id: %s does not exist" % scenario_id)
                network = scenario.get_partial_network(
                    element_types=["TRANSIT_LINE"], include_attributes=True)
                coaster_mode = network.mode("c")
                for line in list(network.transit_lines()):
                    # get the coaster fare perception for use in journey levels
                    if line.mode == coaster_mode:
                        coaster_fare_percep = line[params["fare"]]

            self.run_assignment(period, skims_only, scenario, num_processors, params, coaster_fare_percep, transfer_limit)

            self.skims_local_bus(period, num_processors, scenario)
            self.skims_all_modes(period, num_processors, scenario)


    @_m.logbook_trace("Transit assignment slices by access mode and main mode", save_arguments=True)
    def run_assignment(self, period, skims_only, scenario, num_processors, params, coaster_fare_percep, transfer_limit):
        modeller = _m.Modeller()
        assign_transit = modeller.tool(
            "inro.emme.transit_assignment.extended_transit_assignment")

        all_modes = ["b", "c", "e", "l", "r", "p", "y", "a", "w", "x"]
        local_bus_modes = ["b", "a", "w", "x"]
        transfer_penalty = {"on_segments": {"penalty": "@transfer_penalty_s", "perception_factor": 5}}
        transfer_wait = {
            "effective_headways": "@headway_seg", 
            "headway_fraction": 0.5, 
            "perception_factor": params["xfer_wait"], 
            "spread_factor": 1.0
        }
        if transfer_limit:
            local_bus_journey_levels = [
                {
                    "description": "base", 
                    "destinations_reachable": False, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 1}, ],
                    "boarding_cost": None,
                    "boarding_time": None,
                    "waiting_time": None,
                }, 
                {
                    "description": "boarded-1", 
                    "destinations_reachable": True, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 2}, ],
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                },
                {
                    "description": "boarded-2", 
                    "destinations_reachable": True, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 3}, ], 
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                },
                {
                    "description": "boarded-3", 
                    "destinations_reachable": True, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 4}, ], 
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                },
                {
                    "description": "boarded-4", 
                    "destinations_reachable": True, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 5}, ], 
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                },
                {
                    "description": "boarded-5", 
                    "destinations_reachable": False,
                    "transition_rules": [{"mode": "b", "next_journey_level": 5}, ], 
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                }
            ]
            all_modes_journey_levels = _copy(local_bus_journey_levels)
            for i, level in enumerate(all_modes_journey_levels):
                next_level = i + 1 if i < 5 else i
                level["transition_rules"] = [
                    {"mode": "b", "next_journey_level": next_level}, 
                    {"mode": "c", "next_journey_level": next_level}, 
                    {"mode": "e", "next_journey_level": next_level}, 
                    {"mode": "l", "next_journey_level": next_level}, 
                    {"mode": "p", "next_journey_level": next_level}, 
                    {"mode": "r", "next_journey_level": next_level}, 
                    {"mode": "y", "next_journey_level": next_level}
                ]
        else:
            local_bus_journey_levels = [
                {
                    "description": "base", 
                    "destinations_reachable": False, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 1}, ],
                    "boarding_cost": None,
                    "boarding_time": None,
                    "waiting_time": None,
                }, 
                {
                    "description": "boarded-1", 
                    "destinations_reachable": True, 
                    "transition_rules": [{"mode": "b", "next_journey_level": 1}, ],
                    "boarding_time": transfer_penalty, 
                    "waiting_time": transfer_wait,
                    "boarding_cost": {"global": {"penalty": 0.0, "perception_factor": 1.0}},
                }
            ]

            all_modes_journey_levels = _copy(local_bus_journey_levels)
            for i, level in enumerate(all_modes_journey_levels):
                next_level = 1
                level["transition_rules"] = [
                    {"mode": "b", "next_journey_level": next_level}, 
                    {"mode": "c", "next_journey_level": next_level}, 
                    {"mode": "e", "next_journey_level": next_level}, 
                    {"mode": "l", "next_journey_level": next_level}, 
                    {"mode": "p", "next_journey_level": next_level}, 
                    {"mode": "r", "next_journey_level": next_level}, 
                    {"mode": "y", "next_journey_level": next_level}
                ]

        base_spec = {
            "type": "EXTENDED_TRANSIT_ASSIGNMENT",
            "modes": [],
            "demand": "", 
            "waiting_time": {
                "effective_headways": "@headway_seg", "headway_fraction": 0.5, 
                "perception_factor": params["init_wait"], "spread_factor": 1.0
            }, 
            # Fare attributes
            "boarding_cost": {
                "at_nodes": {"penalty": "@coaster_fare_node", "perception_factor": coaster_fare_percep}, 
                "on_lines": {"penalty": "@fare", "perception_factor": params["fare"]}, 
            }, 
            "in_vehicle_cost": {"penalty": "@coaster_fare_seg", "perception_factor": coaster_fare_percep}, 
            "in_vehicle_time": {"perception_factor": params["in_vehicle"]}, 
            "aux_transit_time": {"perception_factor": params["walk"]},     
            "boarding_time": {"global": {"penalty": 0, "perception_factor": 1}}, 
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
            "connector_to_connector_path_prohibition": None, 
            "od_results": {"total_impedance": None},
            #"performance_settings": {"number_of_processors": num_processors}
        }

        add_volumes = False
        access_modes = ["WLK"] if skims_only else ["WLK", "PNR", "KNR"]
        main_modes = ["BUS", "LRT"] if skims_only else ["BUS", "LRT", "CMR", "BRT", "EXP"]
        for access_type in access_modes:
            for main_mode in main_modes: 
                class_name = "%s_%s%s" % (period, access_type, main_mode)
                spec = _copy(base_spec)
                spec["demand"] = 'mf"%s_%s%s"' % (period, access_type, main_mode)
                if main_mode == "BUS":
                    spec["modes"] = local_bus_modes
                    spec["journey_levels"] = local_bus_journey_levels
                else:
                    spec["modes"] = all_modes
                    spec["journey_levels"] = all_modes_journey_levels
                assign_transit(spec, class_name=class_name, add_volumes=add_volumes, scenario=scenario)
                add_volumes = True

    @_m.logbook_trace("Extract skims for bus mode", save_arguments=True)
    def skims_local_bus(self, period, num_processors, scenario):
        # local cost and impedance analysis
        # access walk time - will be the walk on the connector for most O-D pairs (?)
        # Dwelling time - requires strategy analysis - don't know if I care
        # egress walk time - same as access time for nearly all O-D pairs 
        # fare - may require stategy analysis
        # transfer walk time - requires path analysis
        modeller = _m.Modeller()
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")  
        name = "%s_BUS" % period
        class_name = "%s_WLKBUS" % period
        self._run_skims(name, class_name, period, num_processors, scenario)
        spec = {
            "type": "MATRIX_CALCULATION", 
            "constraint": None,
            "result": 'mf"%s_GENCOST"' % name, 
            "expression": ("3.0 * {0}_TOTALWAIT - 1.5 * {0}_FIRSTWAIT "
                           "+ 1.5 * {0}_TOTALIVTT "
                           "+ (0.67 / 0.1) * {0}_FARE "
                           "+ 1.0 * ({0}_XFERS.max.0) "
                           "+ 1.8 * {0}_TOTALWALK").format(name),
        }
        matrix_calc(spec, scenario=scenario, num_processors=num_processors)


    @_m.logbook_trace("Extract skims for all modes", save_arguments=True)
    def skims_all_modes(self, period, num_processors, scenario):
        modeller = _m.Modeller()
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator") 

        name = "%s_ALL" % period
        class_name = "%s_WLKLRT" % period        
        self._run_skims(name, class_name, period, num_processors, scenario)

        spec = {
            "type": "MATRIX_CALCULATION", 
            "constraint": None,
            "result": 'mf"%s_GENCOST"' % name, 
            "expression": ("3.0 * {0}_TOTALWAIT - 1.5 * {0}_FIRSTWAIT "
                           "+ 1.0 * {0}_TOTALIVTT + 0.5 * {0}_BUSIVTT"
                           "+ (0.56 / 0.1) * {0}_FARE "
                           "+ 1.0 *({0}_XFERS.max.0) "
                           "+ 1.8 * {0}_TOTALWALK").format(name),
        }
        matrix_calc(spec, scenario=scenario, num_processors=num_processors)

    def _run_skims(self, name, class_name, period, num_processors, scenario):
        modeller = _m.Modeller()
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

        with gen_utils.temp_matrices(emmebank, "FULL", 2) as matrices:
            matrices[0].name = "TEMP_IN_VEHICLE_COST"
            matrices[1].name = "TEMP_LAYOVER_BOARD"

            with _m.logbook_trace("First and total wait time, number of boardings, fares, total walk time, in-vehicle time"):
                # First and total wait time, number of boardings, fares, total walk time, in-vehicle time
                spec = {
                    "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                    "actual_first_waiting_times": 'mf"%s_FIRSTWAIT"' % name,
                    "actual_total_waiting_times": 'mf"%s_TOTALWAIT"' % name,
                    "total_impedance": 'mf"%s_GENCOST"' % name,
                    "by_mode_subset": {
                        "modes": ["b", "e", "p", "r", "y", "l", "c", "a", "w", "x"],  
                        "avg_boardings": 'mf"%s_XFERS"' % name,
                        "actual_in_vehicle_times": 'mf"%s_TOTALIVTT"' % name,
                        "actual_in_vehicle_costs": 'mf"TEMP_IN_VEHICLE_COST"',
                        "actual_total_boarding_costs": 'mf"%s_FARE"' % name,  
                        "actual_aux_transit_times": 'mf"%s_TOTALWALK"' % name,
                    },
                }
                matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # convert number of boardings to number of transfers
            # subtrack transfers to the same line at layover points
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
                            "od_values": 'mf"%s_XFERS"' % name, 
                            "interval_min": 1, "interval_max": 9999999, 
                            "condition": "INCLUDE"},
                    },
                    "result": 'mf"%s_XFERS"' % name, 
                    "expression": '(%s_XFERS - 1 - TEMP_LAYOVER_BOARD).max.0' % name,
                }
                matrix_calc(spec, scenario=scenario, num_processors=num_processors)

                # sum in-vehicle cost and boarding cost to get the fare paid
                spec = {
                    "type": "MATRIX_CALCULATION", 
                    "constraint": None,
                    "result": 'mf"%s_FARE"' % name, 
                    "expression": '%s_FARE + TEMP_IN_VEHICLE_COST' % name,
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
                    "aggregated_path_values": 'mf"%s_ACCWALK"' % name,
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)        
            spec = {
                "type": "MATRIX_CALCULATION", 
                "constraint": None,
                "result": 'mf"%s_ACCWALK"' % name, "expression": '60.0 * %s_ACCWALK / 3.0' % name,
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # walk egress time - get distance and convert to time with 3 miles/ hr
            path_spec = {
                "portion_of_path": "FINAL_ALIGHTING_TO_DESTINATION",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"%s_EGRWALK"' % name
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
            spec = {
                "type": "MATRIX_CALCULATION", 
                "constraint": None,
                "result": 'mf"%s_EGRWALK"' % name, 
                "expression": '60.0 * %s_EGRWALK / 3.0' % name,
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # transfer walk time = total - access - egress
            spec = {
                "type": "MATRIX_CALCULATION", 
                "constraint": None,
                "result": 'mf"%s_XFERWALK"' % name, 
                "expression": '(%s_TOTALWALK - %s_ACCWALK - %s_EGRWALK).max.0' % (name, name, name),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # transfer wait time
        with _m.logbook_trace("Wait time - xfer"):
            spec = {
                "type": "MATRIX_CALCULATION", 
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"%s_TOTALWAIT"' % name, 
                        "interval_min": 0, "interval_max": 9999999, 
                        "condition": "INCLUDE"},
                },
                "result": 'mf"%s_XFERWAIT"' % name, 
                "expression": '(%s_TOTALWAIT - %s_FIRSTWAIT).max.0' % (name, name),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        with _m.logbook_trace("In-vehicle time breakdown - dwell time and by main mode(s)"):
            with gen_utils.temp_attrs(scenario, "TRANSIT_SEGMENT", ["@dwt_for_analysis", "@tm_for_analysis"]):
                values = scenario.get_attribute_values(
                    "TRANSIT_SEGMENT", ["dwell_time"])
                scenario.set_attribute_values(
                    "TRANSIT_SEGMENT", ["@dwt_for_analysis"], values)

                spec = {
                    "trip_components": {"in_vehicle": "@dwt_for_analysis"},
                    "sub_path_combination_operator": "+",
                    "sub_strategy_combination_operator": "average",
                    "selected_demand_and_transit_volumes": {
                        "sub_strategies_to_retain": "ALL",
                        "selection_threshold": {"lower": -999999, "upper": 999999}
                    },
                    "results": {
                        "strategy_values": 'mf"%s_DWELLTIME"' % name,
                    },
                    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                }
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

                if name.endswith("ALL"):
                    strat_analysis_spec = {
                        "trip_components": {"in_vehicle": "@tm_for_analysis"},
                        "sub_path_combination_operator": "+",
                        "sub_strategy_combination_operator": "average",
                        "selected_demand_and_transit_volumes": {
                            "sub_strategies_to_retain": "ALL",
                            "selection_threshold": {"lower": -999999, "upper": 999999}
                        },
                        "results": {"strategy_values": None},
                        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                    }
                    network_calc_spec = {
                        "result": "@tm_for_analysis",
                        "expression": "timtr - dwtn",
                        "selections": {"transit_line": "all", "link": "all"},
                        "aggregation": None,
                        "type": "NETWORK_CALCULATION"
                    }
                    mode_names = [
                        ("mode=c", "CMRIVTT"), 
                        ("mode=e", "EXPIVTT"), 
                        ("mode=l", "LRTIVTT"), 
                        ("mode=p", "LTDEXPIVTT"), 
                        ("mode=b", "BUSIVTT"),
                        ("mode=y", "BRTYELIVTT"),
                        ("mode=r", "BRTREDIVTT"),
                    ] 
                    for selection, m_name in mode_names:
                        scenario.extra_attribute("@tm_for_analysis").initialize(0)
                        network_calc_spec["selections"]["transit_line"] = selection 
                        network_calc(network_calc_spec, scenario=scenario)
                        strat_analysis_spec["results"]["strategy_values"] = '%s_%s' % (name, m_name)
                        strategy_analysis(strat_analysis_spec, class_name=class_name, 
                                          scenario=scenario, num_processors=num_processors)
        return

    @_m.logbook_trace("Convert TAP nodes to centroids")
    def taps_to_centroids(self, network):
        # delete existing traffic centroids
        for centroid in list(network.centroids()):
            network.delete_node(centroid, cascade=True)

        node_attrs = network.attributes("NODE")
        link_attrs = network.attributes("LINK")
        for node in list(network.nodes()):
            if node["@tap_id"] > 0:
                centroid = network.create_node(node["@tap_id"], is_centroid=True)
                for attr in node_attrs:
                    centroid[attr] = node[attr]
                for link in node.outgoing_links():
                    connector = network.create_link(centroid, link.j_node, link.modes)
                    connector.vertices = link.vertices
                    for attr in link_attrs:
                        connector[attr] = link[attr]
                for link in node.incoming_links():
                    connector = network.create_link(link.i_node, centroid, link.modes)
                    connector.vertices = link.vertices
                    for attr in link_attrs:
                        connector[attr] = link[attr]
                network.delete_node(node, cascade=True)

    @_m.logbook_trace("Duplicate TAP access and transfer access stops")
    def duplicate_tap_adajcent_stops(self, network, headway):
        # Expand network by duplicating TAP adjacent stops
        network.create_attribute("NODE", "tap_stop", False)
        all_transit_modes = set([network.mode(m) for m in ["b", "e", "p", "r", "y", "l", "c"]])
        access_mode = set([network.mode("a")])
        transfer_mode =  network.mode("x")
        walk_mode =  network.mode("w")

        # Mark TAP adjacent stops and split TAP connectors
        new_node_id = max(n.number for n in network.nodes())
        new_node_id = math.ceil(new_node_id / 10000.0) * 10000
        for centroid in network.centroids():
            out_links = list(centroid.outgoing_links())
            in_links = list(centroid.incoming_links())
            for link in out_links + in_links:
                link.length = 0.0005  # setting length so that connector access time = 0.01
            for link in out_links:
                real_stop = link.j_node
                has_adjacent_transfer_links = False
                has_adjacent_walk_links = False
                for stop_link in real_stop.outgoing_links():
                    if stop_link == link.reverse_link:
                        continue
                    if transfer_mode in stop_link.modes :
                        has_adjacent_transfer_links = True
                    if walk_mode in stop_link.modes :
                        has_adjacent_walk_links = True
                        
                if has_adjacent_transfer_links and not has_adjacent_walk_links:
                    length = link.length
                    tap_stop = network.split_link(centroid, real_stop, self._get_node_id(), include_reverse=True)
                    real_stop.tap_stop = tap_stop
                    transit_access_link = network.link(real_stop, tap_stop)
                    for link in transit_access_link, transit_access_link.reverse_link:
                        link.modes = all_transit_modes
                        link.length = 0
                        for p in ["ea", "am", "md", "pm", "ev"]:
                            link["@time_link_" + p] = 0
                    access_link = network.link(tap_stop, centroid)
                    access_link.modes = access_mode
                    access_link.reverse_link.modes = access_mode
                    access_link.length = length
                    access_link.reverse_link.length = length
            
        line_attributes = network.attributes("TRANSIT_LINE")
        seg_attributes = network.attributes("TRANSIT_SEGMENT")

        for line in network.transit_lines():
            # store line and segment data for re-routing
            line_data = dict((k, line[k]) for k in line_attributes)
            line_data["id"] = line.id
            line_data["vehicle"] = line.vehicle
            
            seg_data = {}
            itinerary = []
            tap_adjacent_stops = []

            for seg in line.segments(include_hidden=True):
                seg_data[(seg.i_node, seg.j_node, seg.loop_index)] = \
                    dict((k, seg[k]) for k in seg_attributes)
                itinerary.append(seg.i_node.number)
                if seg.i_node.tap_stop and seg.allow_boardings:
                    # insert tap_stop, real_stop loop after tap_stop
                    real_stop = seg.i_node
                    tap_stop = real_stop.tap_stop
                    itinerary.extend([tap_stop.number, real_stop.number])
                    tap_adjacent_stops.append(len(itinerary) - 1)  # index of "real" stop in itinerary
            
            if tap_adjacent_stops:
                network.delete_transit_line(line)
                new_line = network.create_transit_line(
                    line_data.pop("id"), 
                    line_data.pop("vehicle"), 
                    itinerary)
                for k, v in line_data.iteritems():
                    new_line[k] = v

                for seg in new_line.segments(include_hidden=True):
                    data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                    for k, v in data.iteritems():
                        seg[k] = v
                for index in tap_adjacent_stops:
                    access_seg = new_line.segment(index - 2)
                    egress_seg = new_line.segment(index - 1)
                    real_seg = new_line.segment(index)
                    for seg in access_seg, egress_seg, real_seg:
                        seg["@headway_seg"] = new_line[headway]
                        seg["@transfer_penalty_s"] = new_line["@transfer_penalty"]
                    access_seg.allow_boardings = False
                    access_seg.allow_alightings = True
                    access_seg.transit_time_func = 3
                    access_seg.dwell_time = real_seg.dwell_time
                    egress_seg.allow_boardings = True
                    egress_seg.allow_alightings = True
                    egress_seg.transit_time_func = 3
                    egress_seg.dwell_time = 0
                    real_seg.allow_boardings = True
                    real_seg.allow_alightings = False
                    real_seg.dwell_time = 0

        network.delete_attribute("NODE", "tap_stop")

    @_m.logbook_trace("Add timed-transfer links", save_arguments=True)
    def timed_transfers(self, network, timed_transfers_with_walk, period):
        def find_walk_link(from_line, to_line):
            to_nodes = set([s.i_node for s in to_line.segments(True)])
            for seg in from_line.segments(True):
                for link in seg.i_node.outgoing_links():
                    if link.j_node in to_nodes:
                        return link
            raise Exception("no walk link from line %s to %s" % (from_line, to_line))
            
        def link_on_line(line, node):
            node = network.node(node)
            for seg in line.segments():
                if seg.i_node == node:
                    return seg.link
            raise Exception(node_not_found_error % (node, from_line))

        # Group parallel transfers together (same pair of alighting-boarding nodes)
        walk_transfers = _defaultdict(lambda: {"from_lines": [], "to_lines": [], "walk_link": None})
        waits = {}
        for i, transfer in enumerate(timed_transfers_with_walk):
            try:
                from_line = network.transit_line(transfer["from_line"])
                if not from_line:
                    raise Exception("from_line %s does not exist" % transfer["from_line"])
                to_line = network.transit_line(transfer["to_line"])
                if not to_line:
                    raise Exception("to_line %s does not exist" % transfer["to_line"])
                walk_link = find_walk_link(from_line, to_line)        
                from_link = link_on_line(from_line, walk_link.i_node)
                to_link = link_on_line(to_line, walk_link.j_node)        
                walk_transfers[(from_link, to_link)]["from_lines"].append(from_line)
                walk_transfers[(from_link, to_link)]["to_lines"].append(to_line)
                walk_transfers[(from_link, to_link)]["walk_link"] = walk_link
                waits[to_line] = transfer["wait_time"]
            except Exception as error:
                new_message = "Timed transfer[%s]: %s" % (i, error.message)
                raise type(error), type(error)(new_message), sys.exc_info()[2]

        def split_link(link, node_id, lines, stop_attr, split_links, waits=None):
            i_node, j_node = link.i_node, link.j_node
            if link in split_links:
                new_node = split_links[link]
                in_link = network.link(i_node, new_node)
                out_link = network.link(new_node, j_node)
            else:
                length = link.length
                proportion = min(0.006 / length, 0.2)    
                new_node = network.split_link(i_node, j_node, node_id, False, proportion)
                in_link = network.link(i_node, new_node)
                out_link = network.link(new_node, j_node)
                out_link.length = length
                in_link.length = 0
                for p in ["ea", "am", "md", "pm", "ev"]:
                    in_link["@time_link_" + p] = 0
                split_links[link] = new_node
                
            for seg in in_link.segments():
                seg.transit_time_func = 3
            for seg in out_link.segments():
                seg.allow_alightings = seg.allow_boardings = False            
                seg.dwell_time = 0
                if seg.line in lines:
                    seg[stop_attr] = True
                    if stop_attr == "allow_boardings":
                        seg["@headway_seg"] = waits[seg.line] * 2
            return new_node

        # process the transfer points, split links and set attributes
        split_links = {}
        for (from_link, to_link), transfer in walk_transfers.iteritems():
            new_alight_node = split_link(
                from_link, self._get_node_id(), transfer["from_lines"], 
                "allow_alightings", split_links)
            new_board_node = split_link(
                to_link, self._get_node_id(), transfer["to_lines"], 
                "allow_boardings", split_links, waits)
            walk_link = transfer["walk_link"]
            transfer_link = network.create_link(
                new_alight_node, new_board_node, [network.mode("x")])
            for attr in network.attributes("LINK"):
                transfer_link[attr] = walk_link[attr]

    @_m.logbook_trace("Add circle line free layover transfers")
    def connect_circle_lines(self, network):
        network.create_attribute("NODE", "circle_lines")
        line_attributes = network.attributes("TRANSIT_LINE")
        seg_attributes = network.attributes("TRANSIT_SEGMENT")

        def offset_coords(node):
            rho = math.sqrt(5000)
            phi = 3 * math.pi / 4 + node.circle_lines * math.pi / 12
            x = node.x + rho * math.cos(phi)
            y = node.y + rho * math.sin(phi)
            node.circle_lines += 1
            return(x, y)

        transit_lines = list(network.transit_lines())
        for line in transit_lines:
            if line.segment(0).i_node == line.segment(-1).i_node:
                # Add new node, offset from existing node
                start_node = line.segment(0).i_node
                xfer_node = network.create_node(self._get_node_id(), False)
                xfer_node.x, xfer_node.y = offset_coords(start_node)        
                network.create_link(start_node, xfer_node, [line.vehicle.mode])
                network.create_link(xfer_node, start_node, [line.vehicle.mode])
                
                # copy transit line data, re-route itinerary to and from new node
                line_data = dict((k, line[k]) for k in line_attributes)
                line_data["id"] = line.id
                line_data["vehicle"] = line.vehicle
                seg_data = {
                    (xfer_node, start_node, 1): {
                        "allow_boardings": True, "allow_alightings": True, 
                        "@headway_seg": 0.01, "dwell_time": 0, "transit_time_func": 3,
                        "@transfer_penalty_s": 0,
                        "@layover_board": 1
                    },
                    (xfer_node, None, 1): {
                        "allow_boardings": True, "allow_alightings": True, 
                        "@headway_seg": 0.01, 
                        "dwell_time": 4.7,  
                        # incremental dwell time for layover of 5 min (0.3 already included)
                        # Note: some lines seem to have a layover of 0, most of 5 mins
                        "transit_time_func": 3
                    },
                }
                itinerary = [xfer_node.number]
                for seg in line.segments():
                    seg_data[(seg.i_node, seg.j_node, seg.loop_index)] = dict((k, seg[k]) for k in seg_attributes)
                    itinerary.append(seg.i_node.number)
                last_seg = line.segment(-1)
                seg_data[(last_seg.i_node, xfer_node, 1)] = dict((k, last_seg[k]) for k in seg_attributes)
                seg_data[(last_seg.i_node, xfer_node, 1)]["transit_time_func"] = 3
                itinerary.extend([last_seg.i_node.number, xfer_node.number])
                
                network.delete_transit_line(line)        
                new_line = network.create_transit_line(
                    line_data.pop("id"), line_data.pop("vehicle"), itinerary)
                for k, v in line_data.iteritems():
                    new_line[k] = v
                for seg in new_line.segments(include_hidden=True):
                    data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                    for k, v in data.iteritems():
                        seg[k] = v
                
        network.delete_attribute("NODE", "circle_lines")

    def _init_node_id(self, network):
        new_node_id = max(n.number for n in network.nodes())
        self._new_node_id = math.ceil(new_node_id / 10000.0) * 10000

    def _get_node_id(self):
        self._new_node_id += 1
        return self._new_node_id

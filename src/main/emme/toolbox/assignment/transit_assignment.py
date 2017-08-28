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
import numpy

import os
import sys
import math


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TransitAssignment(_m.Tool(), gen_utils.Snapshot):

    period = _m.Attribute(unicode)
    scenario =  _m.Attribute(_m.InstanceType)
    skims_only = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.skims_only = False
        self.scenario = _m.Modeller().scenario
        self.num_processors = "MAX-1"
        self.attributes = [
            "period", "scenario", "skims_only",  "num_processors"]
        self._dt_db = _m.Modeller().desktop.project.data_tables()
        
    def from_snapshot(self, snapshot):
        super(TransitAssignment, self).from_snapshot(snapshot)
        # custom from_snapshot to load scenario and database objects
        self.scenario = _m.Modeller().emmebank.scenario(self.scenario)
        return self

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

        pb.add_select_scenario("scenario", 
            title="Transit assignment scenario:")

        pb.add_checkbox("skims_only", title=" ", label="Only run assignments for skim matrices")

        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(
                self.period, self.scenario, self.skims_only, self.num_processors)
            run_msg = "Transit assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, scenario, skims_only=False, num_processors="MAX-1"):
        modeller = _m.Modeller()
        attrs = {
            "period": period,
            "scenario": scenario.id,
            "emmebank": scenario.emmebank.path,
            "skims_only": skims_only,
            "num_processors": num_processors,
            "self": str(self)
        }
        with _m.logbook_trace("Transit assignment for period %s" % period, attributes=attrs):
            gen_utils.log_snapshot("Transit assignment", str(self), attrs)
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise Exception(
                    'period: unknown value - specify one of %s' % periods)
            num_processors = dem_utils.parse_num_processors(num_processors)

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
        
            network = scenario.get_partial_network(
                element_types=["TRANSIT_LINE"], include_attributes=True)
            coaster_mode = network.mode("c")
            for line in list(network.transit_lines()):
                # get the coaster fare perception for use in journey levels
                if line.mode == coaster_mode:
                    coaster_fare_percep = line[params["fare"]]

            self.run_assignment(period, skims_only, scenario, num_processors, params, coaster_fare_percep)

            self.skims_local_bus(period, num_processors, scenario)
            self.skims_all_modes(period, num_processors, scenario)
            self.report(period, scenario)

    @_m.logbook_trace("Transit assignment slices by access mode and main mode", save_arguments=True)
    def run_assignment(self, period, skims_only, scenario, num_processors, params, coaster_fare_percep):
        modeller = _m.Modeller()
        assign_transit = modeller.tool(
            "inro.emme.transit_assignment.extended_transit_assignment")

        all_modes = ["b", "c", "e", "l", "r", "p", "y", "a", "w", "x"]
        local_bus_modes = ["b", "a", "w", "x"]
        #transfer_penalty = {"on_segments": {"penalty": "@transfer_penalty_s", "perception_factor": 5}}
        transfer_penalty = {"on_lines": {"penalty": "@transfer_penalty", "perception_factor": 5}}
        transfer_wait = {
            "effective_headways": "@headway_seg", 
            "headway_fraction": 0.5, 
            "perception_factor": params["xfer_wait"], 
            "spread_factor": 1.0
        }
        local_bus_journey_levels = [
            {
                "description": "base", 
                "destinations_reachable": False, 
                "transition_rules": [{"mode": "b", "next_journey_level": 1}, ],
                "boarding_time": None, 
                "waiting_time": None,
                "boarding_cost": None, 
            }, 
            {
                "description": "boarded_bus", 
                "destinations_reachable": True, 
                "transition_rules": [{"mode": "b", "next_journey_level": 2}, ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_from_bus", "perception_factor": params["fare"]}},
            },
            {
                "description": "day_pass", 
                "destinations_reachable": True, 
                "transition_rules": [{"mode": "b", "next_journey_level": 2}, ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_regional_pass", "perception_factor": params["fare"]}},
            }
        ]
        all_modes_journey_levels = [
            {
                "description": "base", 
                "destinations_reachable": False, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 1}, 
                    {"mode": "l", "next_journey_level": 2}, 
                    {"mode": "e", "next_journey_level": 2}, 
                    {"mode": "r", "next_journey_level": 2}, 
                    {"mode": "y", "next_journey_level": 2},
                    {"mode": "p", "next_journey_level": 3}, 
                    {"mode": "c", "next_journey_level": 4}, 
                ],
                "boarding_time": None, 
                "waiting_time": None,
                "boarding_cost": None,
            }, 
            {
                "description": "boarded_bus", 
                "destinations_reachable": True, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 2}, 
                    {"mode": "l", "next_journey_level": 2}, 
                    {"mode": "e", "next_journey_level": 2}, 
                    {"mode": "r", "next_journey_level": 2}, 
                    {"mode": "y", "next_journey_level": 2},
                    {"mode": "p", "next_journey_level": 5}, 
                    {"mode": "c", "next_journey_level": 5}, 
                ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_from_bus", "perception_factor": params["fare"]}},
            },
            {
                "description": "day_pass", 
                "destinations_reachable": True, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 2}, 
                    {"mode": "l", "next_journey_level": 2}, 
                    {"mode": "e", "next_journey_level": 2}, 
                    {"mode": "r", "next_journey_level": 2}, 
                    {"mode": "y", "next_journey_level": 2},
                    {"mode": "p", "next_journey_level": 5}, 
                    {"mode": "c", "next_journey_level": 5}, 
                ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_from_day", "perception_factor": params["fare"]}},
            },
            {
                "description": "boarded_premium", 
                "destinations_reachable": True, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 5}, 
                    {"mode": "l", "next_journey_level": 5}, 
                    {"mode": "e", "next_journey_level": 5}, 
                    {"mode": "r", "next_journey_level": 5}, 
                    {"mode": "y", "next_journey_level": 5},
                    {"mode": "p", "next_journey_level": 5}, 
                    {"mode": "c", "next_journey_level": 5}, 
                ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_from_premium", "perception_factor": params["fare"]}},
            },
            {
                "description": "boarded_coaster", 
                "destinations_reachable": True, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 5}, 
                    {"mode": "l", "next_journey_level": 5}, 
                    {"mode": "e", "next_journey_level": 5}, 
                    {"mode": "r", "next_journey_level": 5}, 
                    {"mode": "y", "next_journey_level": 5},
                    {"mode": "p", "next_journey_level": 5}, 
                    {"mode": "c", "next_journey_level": 5}, 
                ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost": {"on_lines": {"penalty": "@xfer_from_coaster", "perception_factor": params["fare"]}},
            },
            {
                "description": "regional_pass", 
                "destinations_reachable": True, 
                "transition_rules": [
                    {"mode": "b", "next_journey_level": 5}, 
                    {"mode": "l", "next_journey_level": 5}, 
                    {"mode": "e", "next_journey_level": 5}, 
                    {"mode": "r", "next_journey_level": 5}, 
                    {"mode": "y", "next_journey_level": 5},
                    {"mode": "p", "next_journey_level": 5}, 
                    {"mode": "c", "next_journey_level": 5}, 
                ],
                "boarding_time": transfer_penalty, 
                "waiting_time": transfer_wait,
                "boarding_cost":  {"on_lines": {"penalty": "@xfer_regional_pass", "perception_factor": params["fare"]}},
            }
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
                "on_lines": {"penalty": "@fare", "perception_factor": params["fare"]}, 
                "on_segments": {"penalty": "@coaster_fare_board", "perception_factor": coaster_fare_percep}, 
            }, 
            "boarding_time": {"global": {"penalty": 0, "perception_factor": 1}}, 
            "in_vehicle_cost": {"penalty": "@coaster_fare_inveh", "perception_factor": coaster_fare_percep}, 
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
            "connector_to_connector_path_prohibition": None, 
            "od_results": {"total_impedance": None},
            "performance_settings": {"number_of_processors": num_processors}
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
            with _m.logbook_trace("Distance by mode"):
                if name.endswith("ALL"):
                    mode_combinations = [
                        ("BUS", ["b"]),
                        ("LRT", ["l"]),
                        ("CMR", ["c"]),
                        ("EXP", ["e", "p"]),
                        ("BRT", ["r", "y"]),
                    ]
                else:
                    mode_combinations = [("", ["b"])]

                for mode_name, modes in mode_combinations:
                    spec = {
                        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                        "by_mode_subset": {
                            "modes": modes,  
                            "distance": 'mf"%s_%sDIST"' % (name, mode_name),
                        },
                    }
                    matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # convert number of boardings to number of transfers
            # subtract transfers to the same line at layover points
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
                max_fare = 6.0 if name.endswith("ALL") else 2.50
                spec = {
                    "type": "MATRIX_CALCULATION", 
                    "constraint": None,
                    "result": 'mf"%s_FARE"' % name, 
                    "expression": '(%s_FARE + TEMP_IN_VEHICLE_COST).min.%s' % (name, max_fare),
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
                
                spec = {
                    "type": "MATRIX_CALCULATION", 
                    "constraint":{
                        "by_value": {
                            "od_values": 'mf"%s_TOTALIVTT"' % name, 
                            "interval_min": 0, "interval_max": 9999999, 
                            "condition": "INCLUDE"},
                    },
                    "result": 'mf"%s_TOTALIVTT"' % name, 
                    "expression": '(%s_TOTALIVTT - %s_DWELLTIME).max.0' % (name, name),
                }
                matrix_calc(spec, scenario=scenario, num_processors=num_processors)

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

    def _init_node_id(self, network):
        new_node_id = max(n.number for n in network.nodes())
        self._new_node_id = math.ceil(new_node_id / 10000.0) * 10000

    def _get_node_id(self):
        self._new_node_id += 1
        return self._new_node_id

    def report(self, period, scenario):
        emmebank = scenario.emmebank
        text = ['<div class="preformat">']
        matrices = [
            "BUS_GENCOST", 
            "BUS_TOTALWAIT",
            "BUS_TOTALWALK",  
            "BUS_TOTALIVTT",
            "BUS_DWELLTIME",   
            "BUS_FIRSTWAIT",
            "BUS_XFERWAIT",
            "BUS_FARE",      
            "BUS_XFERS",     
            "BUS_ACCWALK",     
            "BUS_XFERWALK", 
            "BUS_EGRWALK",     
            "ALL_GENCOST",  
            "ALL_TOTALWAIT",
            "ALL_TOTALWALK",
            "ALL_TOTALIVTT",
            "ALL_FIRSTWAIT",
            "ALL_XFERWAIT",
            "ALL_FARE",      
            "ALL_XFERS",     
            "ALL_ACCWALK",     
            "ALL_XFERWALK", 
            "ALL_EGRWALK",     
            "ALL_DWELLTIME",     
            "ALL_BUSIVTT", 
            "ALL_LRTIVTT",       
            "ALL_CMRIVTT",      
            "ALL_EXPIVTT",   
            "ALL_LTDEXPIVTT",   
            "ALL_BRTREDIVTT",   
            "ALL_BRTYELIVTT",
        ]
        num_cells = len(scenario.zone_numbers) ** 2
        text.append("Number of O-D pairs: %s. Values outside -9999999, 9999999 are masked in summaries.<br>" % num_cells)
        text.append("%-25s %9s %9s %9s %13s %9s" % ("name", "min", "max", "mean", "sum", "mask num"))
        for name in matrices:
            name = period + "_" + name
            matrix = emmebank.matrix(name)
            data = matrix.get_numpy_data(scenario)
            data = numpy.ma.masked_outside(data, -9999999, 9999999, copy=False)
            stats = (name, data.min(), data.max(), data.mean(), data.sum(), num_cells-data.count())
            text.append("%-25s %9.4g %9.4g %9.4g %13.7g %9d" % stats)
        text.append("</div>")
        title = 'Transit impedance summary for period %s' % period
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(title, report.render())
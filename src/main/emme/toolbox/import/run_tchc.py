#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright RSG, 2026.                                                  ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/run_tchc.py                                                    ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Recomputes TCHC roadway capacity, travel time and intersection delay for the
# links of the TNED highway network and writes the results back into the input
# geodatabase. Runs after run4Ds and before import_network, so that
# import_network reads refreshed capacities.
#
# The capacity calculation engine, a Python port of the FORTRAN TCHC procedure,
# is included below ahead of the geodatabase driver.
#
# Links are processed when any of the period capacity, time or delay fields is
# empty, or when the link is listed in the file given by link_id_file. GC and
# PLC are inputs as well as outputs, so they do not take part in that test.
#
# Inputs:
#    path: scenario directory, containing conf/ and input/
#    source: input network geodatabase. Default is the single *.gdb in <path>/input
#    station_file: count station hourly percentages, CSV or fixed width, one row
#        per (station ID, direction) with 24 hourly columns
#    gc_file: green-to-cycle ratio lookup table (gc.csv)
#    link_id_file: optional list of HWYCOV0_ID to recompute unconditionally
#    year: analysis year. Default is scenarioYear from the properties file
#    aoc: auto operating cost in cents/mile. Default is aoc.fuel + aoc.maintenance
#    managed_lane_capacity_rate: capacity multiplier for HOV3+ and managed lanes
#    freeway_capacity_rate: capacity multiplier for general purpose freeways
#    ramp_meter_file: optional ramp meter direction by traffic count ID
#    hov_freeway_pairs_file: optional managed lane to parallel freeway link ID
#    external_zone_delay_file: optional external station delay in cents by node
#    jurisdiction_field: TNED field holding the 1-6 jurisdiction code
#    traffic_count_field: TNED field holding the ADT link ID, if present
#    recompute_all: recompute every link instead of only those missing outputs
#    treat_zero_as_missing: treat a stored zero as a missing value
#    dry_run: compute and report without writing to the geodatabase
#    report_file: optional CSV of computed and previously stored values
#
# Files referenced:
#    <source>: TNED_HwyNet and TNED_HwyNodes layers
#    conf/sandag_abm.properties
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    run_tchc = modeller.tool("sandag.import.run_tchc")
    run_tchc(path=main_directory, dry_run=True, report_file="tchc_report.csv")
"""


TOOLBOX_ORDER = 11


import inro.modeller as _m

from osgeo import ogr as _ogr

from collections import OrderedDict
from dataclasses import dataclass, field
from typing import Dict, List
import csv
import glob as _glob
import pandas as pd
import re
import traceback as _traceback
import os

_join = os.path.join
_dir = os.path.dirname

gen_utils = _m.Modeller().module("sandag.utilities.general")


# ==================================================================
# Capacity calculation engine
# ==================================================================

# Fallback speed (mph) by functional class, 0-indexed: FC 1..14
DEFAULT_SPEED_BY_FUNCTIONAL_CLASS = [65, 45, 40, 35, 30, 40, 35, 65, 30, 30, 50, 25, 25, 30]

# Turn-lane capacity per lane (veh/hr) by functional class, 0-indexed: FC 1..10
TURN_CAPACITY_BY_FUNCTIONAL_CLASS = [250, 250, 150, 100, 100, 100, 100, 100, 100, 0]

# Fallback jurisdiction (1-6) by functional class; the FORTRAN mjur table
DEFAULT_JURISDICTION_BY_FUNCTIONAL_CLASS = [1, 5, 5, 6, 6, 6, 6, 1, 1, 6, 6, 6, 6, 1]


@dataclass
class TCHCLink:
    # identifiers
    link_identifier: int
    link_name: str
    length_feet: float
    functional_class: int              # functional class
    high_occupancy_vehicle_class: int  # 1=mix, 2=hov2, 3=hov3, 4=toll
    jurisdiction: int                  # jurisdiction
    median_type: int                   # median type
    directionality: int                # 1=one-way, 2=two-way
    traffic_count_identifier: int
    station_identifier: int
    project_identifier: int

    # nodes
    from_node_identifier: int
    to_node_identifier: int

    # speed (coded or default from mspd[fc])
    speed: int = 35

    # lanes: [period][dir]
    lane_count_by_period_and_direction: List[List[int]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    auxiliary_lane_count_by_direction: List[int] = field(default_factory=lambda: [0, 0])

    # per-link capacity per direction (aatplc)
    planned_lane_capacity_by_direction: List[int] = field(default_factory=lambda: [0, 0])

    # cross-street functional class per direction (aatxfc, default 7)
    cross_street_functional_class_by_direction: List[int] = field(default_factory=lambda: [7, 7])

    # control / turn lanes
    control_type_by_direction: List[int] = field(default_factory=lambda: [0, 0])
    through_lane_count_by_direction: List[int] = field(default_factory=lambda: [0, 0])
    right_turn_lane_count_by_direction: List[int] = field(default_factory=lambda: [0, 0])
    left_turn_lane_count_by_direction: List[int] = field(default_factory=lambda: [0, 0])
    green_cycle_value_by_direction: List[int] = field(default_factory=lambda: [0, 0])

    # per-mile tolls -> converted in place
    toll_cost_by_period: List[int] = field(default_factory=lambda: [0, 0, 0])

    # external zone delay cost (extcst, for zone connectors)
    external_zone_delay_cost: float = 0.0

    # outputs
    link_travel_time_minutes_by_period_and_direction: List[List[float]] = field(default_factory=lambda: [[999, 999], [999, 999], [999, 999]])
    intersection_delay_minutes_by_period_and_direction: List[List[float]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    hourly_capacity_by_period_and_direction: List[List[float]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    period_capacity_by_period_and_direction: List[List[float]] = field(default_factory=lambda: [[999999, 999999], [999999, 999999], [999999, 999999]])
    intersection_capacity_by_period_and_direction: List[List[float]] = field(default_factory=lambda: [[999999, 999999], [999999, 999999], [999999, 999999]])
    generalized_cost_by_direction: List[float] = field(default_factory=lambda: [999999, 999999])

    # resolved from the lookup tables and written back; None where nothing was resolved
    resolved_green_cycle_by_direction: List = field(default_factory=lambda: [None, None])
    resolved_per_lane_capacity_by_direction: List = field(default_factory=lambda: [None, None])

    auto_operating_cost: float = 0.0


@dataclass
class TCHCContext:
    """
    Scenario-level parameters and lookup tables consumed by ``apply_tchc``.

    These are typically loaded once from external data sources (count station
    files, green/cycle tables, ramp meter lists, HOV-freeway mappings, border
    delay tables) and shared across all links in a single model run.
    """

    auto_operating_cost_per_mile: float  # cents/mile
    managed_lane_capacity_rate: float    # multiplier for HOV3+/managed lanes (typically 1.0)
    freeway_capacity_rate: float         # multiplier for GP freeway/FC8 capacity (typically 1.0)
    analysis_year: int                   # enables TSM features when > 2015

    # node_id -> approach count (2-4): non-connector links touching each node
    approach_count: Dict[int, int]
    # adt_id -> direction code for ramp metering (1=SB,2=EB,3=NB,4=WB,9=both)
    ramp_meter_direction_by_traffic_count_identifier: Dict[int, int]
    # [period][direction][station_id] -> peak-period expansion factor
    station_peak_period_factor: List[List[List[float]]]

    # jurisdiction (1-6) -> capacity multiplier for signalized intersections
    roadway_safety_adjustment_factor_by_jurisdiction: Dict[int, float]

    # GC ratio lookup tables (integer percentages):
    #   signal:     [approach_count-1][fc-1][cross_fc-1]  (4x9x9)
    #   4-way stop: [fc-1][cross_fc-1]                    (9x9)
    #   2-way stop: [cross_fc-1]                          (9,)
    signal_green_cycle_lookup: List[List[List[int]]]
    four_way_stop_green_cycle_lookup: List[List[int]]
    two_way_stop_green_cycle_lookup: List[int]

    # [crossing_index][period][border_direction] -> delay in minutes
    # 5 crossings: San Ysidro(0), Otay(1), East(2), Tecate(3), Jacumba(4)
    # 2 directions: SB/EB(0), NB(1)
    border_delay_minutes_lookup: List[List[List[float]]]

    time_period_adjustments: bool = True
    # HOV link_id -> adjacent GP freeway link_id (for station resolution)
    managed_lane_to_freeway_identifier: Dict[int, int] = field(default_factory=dict)
    # freeway link_id -> count station_id
    freeway_identifier_to_station_identifier: Dict[int, int] = field(default_factory=dict)
    # node_id -> raw sphere code (divide by 100 for sphere group)
    node_sphere_by_id: Dict[int, int] = field(default_factory=dict)


def miles(ft):
    return ft / 5280.0


def direction_from_name(name):
    if "SB" in name: return 1
    if "EB" in name: return 2
    if "NB" in name: return 3
    if "WB" in name: return 4
    return 0


def compute_safety_factors(analysis_year):
    """Roadway safety adjustment factor by jurisdiction (FORTRAN rsafac)."""
    factors = dict((j, 1.0) for j in range(1, 7))
    if analysis_year > 2015:
        factor = 1.0 + (min(analysis_year, 2020) - 2010) * 0.01
        for jurisdiction in range(1, 5):
            factors[jurisdiction] = factor
    return factors


def _max_lanes_for_direction(link, direction_index):
    """Compute aatmln(idir): max lane count across all periods for one direction."""
    maximum_lane_count = 0
    for period_index in range(3):
        lane_count = link.lane_count_by_period_and_direction[period_index][direction_index]
        if 1 <= lane_count <= 8:
            maximum_lane_count = max(maximum_lane_count, lane_count)
    return maximum_lane_count


def apply_tchc(link, ctx, remaining_toll=None):
    """Compute capacity, travel time, and generalized cost for one link.

    Mutates ``link`` in place, populating all output fields.  Returns the
    ``remaining_toll`` carry-forward list (3 floats) for the next link in
    the route sequence.

    Args:
        link: Link to process.  Output fields are overwritten.
        ctx: Shared scenario parameters and lookup tables.
        remaining_toll: Fractional toll cents carried from the previous
            link.  Pass ``None`` for the first link or standalone use.
    """
    if remaining_toll is None:
        remaining_toll = [0.0, 0.0, 0.0]

    distance_miles = miles(link.length_feet)
    use_traffic_system_management = ctx.analysis_year > 2015

    # ---- toll conversion: per-mile rate -> absolute cents, with carry-forward
    # Tolls are coded as per-mile rates. Multiply by distance, accumulate
    # fractional cents in remaining_toll to avoid rounding loss across links.
    for period_index in range(3):
        raw_toll_value = link.toll_cost_by_period[period_index] * distance_miles + remaining_toll[period_index]
        rounded_toll_value = int(round(raw_toll_value))
        remaining_toll[period_index] = raw_toll_value - rounded_toll_value
        if link.toll_cost_by_period[period_index] > 0 and rounded_toll_value == 0:
            rounded_toll_value = 1
        link.toll_cost_by_period[period_index] = rounded_toll_value

    # auto operating cost
    link.auto_operating_cost = distance_miles * ctx.auto_operating_cost_per_mile

    # link speed (use coded speed, or FC default)
    speed_miles_per_hour = link.speed
    if speed_miles_per_hour < 1 or speed_miles_per_hour > 75:
        speed_miles_per_hour = (
            DEFAULT_SPEED_BY_FUNCTIONAL_CLASS[link.functional_class - 1]
            if 1 <= link.functional_class <= len(DEFAULT_SPEED_BY_FUNCTIONAL_CLASS)
            else 35
        )
    travel_time_minutes = distance_miles * 60.0 / float(speed_miles_per_hour)

    # ---- station resolution ----
    # HOV lanes share count stations with the adjacent GP freeway.
    # Chain: HOV link_id -> freeway link_id -> station_id.
    # Non-freeway links and unresolved stations default to station 1.
    station_id = link.station_identifier
    if link.high_occupancy_vehicle_class in (2, 3):
        station_id = 0
        freeway_id = ctx.managed_lane_to_freeway_identifier.get(link.link_identifier, 0)
        if freeway_id > 0:
            station_id = ctx.freeway_identifier_to_station_identifier.get(freeway_id, 0)
    if station_id < 1 or (link.functional_class != 1):
        station_id = 1

    # ---- direction loop: AB (dir 0) then BA (dir 1) if two-way ----
    for direction_index in range(2):
        if link.directionality == 1 and direction_index == 1:
            continue

        # TNED codes AB* intersection fields at the TO (B) end, so the approach
        # node is the link's downstream end for that direction.
        node_id = link.to_node_identifier if direction_index == 0 else link.from_node_identifier
        approach_count = ctx.approach_count.get(node_id, 3)

        for period_index in range(3):
            lane_count = link.lane_count_by_period_and_direction[period_index][direction_index]
            if lane_count == 9:
                continue

            link.link_travel_time_minutes_by_period_and_direction[period_index][direction_index] = travel_time_minutes

            # fc==10 (zone connector): only gets travel time, no capacity
            if link.functional_class == 10:
                continue

            # ---- peak-period factor ----
            # Converts hourly capacity to period capacity. For freeways,
            # direction is inferred from the link name (NB/WB = reverse)
            # rather than the loop index.
            if link.functional_class == 1:
                peak_factor_direction_index = 1 if ("NB" in link.link_name or "WB" in link.link_name) else 0
            else:
                peak_factor_direction_index = direction_index

            peak_period_factor = ctx.station_peak_period_factor[period_index][peak_factor_direction_index][station_id]

            # PPF validation (FORTRAN: must be in [1.0, 15.0], else fallback to station 1)
            if peak_period_factor < 1.0 or peak_period_factor > 15.0:
                peak_period_factor = ctx.station_peak_period_factor[period_index][peak_factor_direction_index][1]

            # ---- base mid-block capacity by facility type ----
            if link.functional_class == 1:
                # freeway capacity from per-link field, bounded [1900, 2100];
                # the AB value is used for both directions
                freeway_capacity_per_lane = 2000.0
                if 1600 <= link.planned_lane_capacity_by_direction[0] <= 2400:
                    freeway_capacity_per_lane = float(link.planned_lane_capacity_by_direction[0])
                freeway_capacity_per_lane = min(freeway_capacity_per_lane, 2100.0)
                freeway_capacity_per_lane = max(freeway_capacity_per_lane, 1900.0)
                link.resolved_per_lane_capacity_by_direction[direction_index] = freeway_capacity_per_lane

                directional_capacity = lane_count * freeway_capacity_per_lane + link.auxiliary_lane_count_by_direction[direction_index] * 1200.0
                if link.high_occupancy_vehicle_class == 1:
                    directional_capacity *= ctx.freeway_capacity_rate
                if link.high_occupancy_vehicle_class in (2, 3):
                    directional_capacity = lane_count * 2000.0
                if link.high_occupancy_vehicle_class == 3:
                    directional_capacity *= ctx.managed_lane_capacity_rate
                if link.project_identifier in (613, 614):
                    directional_capacity *= ctx.managed_lane_capacity_rate

                if use_traffic_system_management:
                    ramp_meter_direction = ctx.ramp_meter_direction_by_traffic_count_identifier.get(link.traffic_count_identifier, 0)
                    if ramp_meter_direction == 9 or ramp_meter_direction == direction_from_name(link.link_name):
                        directional_capacity *= 1.10

            elif link.functional_class == 8:
                # fwy-fwy connector: check for ACCESS special case
                if "ACCESS" in link.link_name:
                    link.hourly_capacity_by_period_and_direction[period_index][direction_index] = 9999.0
                    link.period_capacity_by_period_and_direction[period_index][direction_index] = 999999.0
                    continue
                directional_capacity = lane_count * 1800.0
                if link.high_occupancy_vehicle_class > 1:
                    directional_capacity *= ctx.managed_lane_capacity_rate
                elif link.high_occupancy_vehicle_class == 1:
                    directional_capacity *= ctx.freeway_capacity_rate

            elif link.functional_class == 9:
                directional_capacity = lane_count * 1200.0

            else:
                # arterials (fc 2-7): check plc==950 override
                if link.planned_lane_capacity_by_direction[direction_index] == 950 and lane_count < 2:
                    directional_capacity = 950.0
                else:
                    directional_capacity = lane_count * 1800.0 - 300.0
                    if link.median_type < 2:
                        directional_capacity -= 200.0

            link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity
            link.period_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor

            # ---- turn-lane sanitization ----
            # Values >7 are invalid (zeroed); 7 is a special code meaning
            # "1 lane present but not explicitly counted".
            through_lane_count = link.through_lane_count_by_direction[direction_index]
            right_turn_lane_count = link.right_turn_lane_count_by_direction[direction_index]
            left_turn_lane_count = link.left_turn_lane_count_by_direction[direction_index]

            if through_lane_count > 7:
                through_lane_count = 0
            if right_turn_lane_count > 7:
                right_turn_lane_count = 0
            if left_turn_lane_count > 7:
                left_turn_lane_count = 0
            if through_lane_count == 7:
                through_lane_count = 1
            if right_turn_lane_count == 7:
                right_turn_lane_count = 1
            if left_turn_lane_count == 7:
                left_turn_lane_count = 1

            # fallback: if no through lanes, promote highest turn lane
            if through_lane_count < 1:
                approach_count = 3
                if right_turn_lane_count > left_turn_lane_count:
                    through_lane_count = right_turn_lane_count
                    right_turn_lane_count = 0
                else:
                    through_lane_count = left_turn_lane_count
                    left_turn_lane_count = 0

            # ---- intersection capacity by control type ----
            control_type = link.control_type_by_direction[direction_index]
            cross_street_functional_class = link.cross_street_functional_class_by_direction[direction_index]
            # clamp cross_fc to valid index (1-based in FORTRAN, 0-based here)
            cross_street_functional_class_index = max(0, min(cross_street_functional_class - 1, 8))

            if control_type == 1:  # signal (FORTRAN 610)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.17
                # gc override: use lookup only if coded gc < 10
                green_cycle_value = link.green_cycle_value_by_direction[direction_index]
                if green_cycle_value < 10:
                    green_cycle_value = ctx.signal_green_cycle_lookup[min(approach_count, 4) - 1][link.functional_class - 1][cross_street_functional_class_index]
                link.resolved_green_cycle_by_direction[direction_index] = green_cycle_value
                green_cycle_factor = green_cycle_value / 100.0
                turn_capacity_per_lane = (
                    TURN_CAPACITY_BY_FUNCTIONAL_CLASS[link.functional_class - 1]
                    if link.functional_class <= len(TURN_CAPACITY_BY_FUNCTIONAL_CLASS)
                    else 100
                )
                directional_capacity = (
                    through_lane_count * 1800.0 * green_cycle_factor
                    + (right_turn_lane_count + left_turn_lane_count) * turn_capacity_per_lane
                )
                if directional_capacity < 1000.0:
                    directional_capacity = 1000.0
                directional_capacity *= ctx.roadway_safety_adjustment_factor_by_jurisdiction.get(link.jurisdiction, 1.0)
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity

            elif control_type == 2:  # 4-way stop (FORTRAN 620)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.20
                green_cycle_value = link.green_cycle_value_by_direction[direction_index]
                if green_cycle_value < 1:
                    green_cycle_value = ctx.four_way_stop_green_cycle_lookup[link.functional_class - 1][cross_street_functional_class_index]
                link.resolved_green_cycle_by_direction[direction_index] = green_cycle_value
                green_cycle_factor = green_cycle_value / 100.0
                turn_capacity_per_lane = (
                    TURN_CAPACITY_BY_FUNCTIONAL_CLASS[link.functional_class - 1]
                    if link.functional_class <= len(TURN_CAPACITY_BY_FUNCTIONAL_CLASS)
                    else 100
                )
                directional_capacity = (
                    through_lane_count * 1800.0 * green_cycle_factor
                    + (right_turn_lane_count + left_turn_lane_count) * turn_capacity_per_lane
                )
                if directional_capacity < 500.0:
                    directional_capacity = 500.0
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity

            elif control_type == 3:  # 2-way stop (FORTRAN 630)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.20
                green_cycle_value = ctx.two_way_stop_green_cycle_lookup[cross_street_functional_class_index]
                link.resolved_green_cycle_by_direction[direction_index] = green_cycle_value
                green_cycle_through_factor = green_cycle_value / 100.0
                green_cycle_right_factor = green_cycle_value / 100.0
                green_cycle_left_factor = green_cycle_value / 100.0
                # FORTRAN checks the free-right code before sanitization, so re-read the raw value
                if link.right_turn_lane_count_by_direction[direction_index] == 7:
                    green_cycle_right_factor = 1.0
                    right_turn_lane_count = 1
                directional_capacity = (
                    through_lane_count * 500.0 * green_cycle_through_factor
                    + right_turn_lane_count * 500.0 * green_cycle_right_factor
                    + left_turn_lane_count * 500.0 * green_cycle_left_factor
                )
                if directional_capacity < 500.0:
                    directional_capacity = 500.0
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity

            elif control_type == 4 and period_index > 0:  # ramp meter off-peak (FORTRAN 640)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.50
                directional_capacity = 1000.0
                if link.green_cycle_value_by_direction[direction_index] >= 1:
                    green_cycle_through_factor = link.green_cycle_value_by_direction[direction_index] / 100.0
                    directional_capacity *= green_cycle_through_factor
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity

            elif control_type == 5 and period_index > 0:  # ramp meter on-peak (FORTRAN 650)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.50
                directional_capacity = 1000.0
                if link.green_cycle_value_by_direction[direction_index] >= 1:
                    green_cycle_through_factor = link.green_cycle_value_by_direction[direction_index] / 100.0
                    directional_capacity *= green_cycle_through_factor
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity

            elif control_type == 6:  # rail crossing (FORTRAN 660)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.02

            elif control_type == 7:  # toll / border (FORTRAN 670)
                # use max lanes across periods as floor for through lanes
                max_lane_count = _max_lanes_for_direction(link, direction_index)
                through_lane_count = max(through_lane_count, max_lane_count)
                directional_capacity = through_lane_count * 500.0
                link.hourly_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity
                link.intersection_capacity_by_period_and_direction[period_index][direction_index] = directional_capacity * peak_period_factor
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 1.0

        # generalized cost (FORTRAN 1232)
        peak_period_index = 0
        link.generalized_cost_by_direction[direction_index] = (
            link.external_zone_delay_cost
            + link.auto_operating_cost
            + (
                link.link_travel_time_minutes_by_period_and_direction[peak_period_index][direction_index]
                + link.intersection_delay_minutes_by_period_and_direction[peak_period_index][direction_index]
            ) * 35.0
            + (link.toll_cost_by_period[0] + link.toll_cost_by_period[1]) / 2.0
        )
        link.generalized_cost_by_direction[direction_index] = min(link.generalized_cost_by_direction[direction_index], 999999.0)

    return remaining_toll


# ---------------------------------------------------------------------------
# Green/cycle ratio lookup tables (gc.csv)
# ---------------------------------------------------------------------------

GREEN_CYCLE_CLASS_COUNT = 9  # functional classes 1-9 in both table dimensions


@dataclass
class GreenCycleLookups:
    """The three green/cycle tables consumed by ``TCHCContext``.

    Values are integer percentages (G/C x 100), matching what ``apply_tchc``
    divides by 100 and compares against its coded-value thresholds.
    """

    signal: List[List[List[int]]]   # [approach_count-1][fc-1][cross_fc-1]
    four_way_stop: List[List[int]]  # [fc-1][cross_fc-1]
    two_way_stop: List[int]         # [cross_fc-1]


def _normalize_control_label(label):
    return re.sub(r"[^a-z0-9]", "", label.strip().lower())


def _empty_class_table():
    return [[0] * GREEN_CYCLE_CLASS_COUNT for _ in range(GREEN_CYCLE_CLASS_COUNT)]


def load_green_cycle_lookups(path, two_way_stop_roadway_class=7):
    """Read green/cycle ratios from a gc.csv-style file.

    Expected layout: column 1 is the intersection control type
    (``Signal - 4 Leg``, ``Signal - 3 Leg``, ``Signal - 2 Leg``,
    ``4-Way Stop``, and optionally ``2-Way Stop``), column 2 is the roadway
    functional class, and the remaining columns are the crossroad functional
    classes named in the header.

    Ratios coded as fractions (0.35) are rescaled to percentages (35); files
    already in percent are left alone.
    """
    with open(path, newline="") as handle:
        rows = list(csv.reader(handle))
    if len(rows) < 2:
        raise Exception("%s: no data rows" % path)

    header = rows[0]
    cross_classes = []
    for column in header[2:]:
        column = column.strip()
        cross_classes.append(int(column) if column else 0)

    # {normalized control label: {road_fc: {cross_fc: ratio}}}
    blocks = {}
    largest_ratio = 0.0
    for line_number, row in enumerate(rows[1:], start=2):
        if not row or not row[0].strip():
            continue
        label = _normalize_control_label(row[0])
        try:
            road_class = int(row[1])
        except (IndexError, ValueError):
            raise Exception("%s line %s: bad roadway class %s" % (path, line_number, row[1:2]))
        block = blocks.setdefault(label, {})
        entries = block.setdefault(road_class, {})
        for cross_class, cell in zip(cross_classes, row[2:]):
            cell = cell.strip()
            if not cell or cross_class < 1:
                continue
            ratio = float(cell)
            entries[cross_class] = ratio
            largest_ratio = max(largest_ratio, ratio)

    # gc.csv codes ratios as fractions; apply_tchc works in percent.
    scale = 100.0 if largest_ratio <= 1.5 else 1.0

    def to_table(block):
        table = _empty_class_table()
        for road_class, entries in block.items():
            if not 1 <= road_class <= GREEN_CYCLE_CLASS_COUNT:
                continue
            for cross_class, ratio in entries.items():
                if 1 <= cross_class <= GREEN_CYCLE_CLASS_COUNT:
                    table[road_class - 1][cross_class - 1] = int(round(ratio * scale))
        return table

    signal = [_empty_class_table() for _ in range(4)]
    populated = []
    for label, block in blocks.items():
        if not label.startswith("signal"):
            continue
        legs = re.search(r"(\d)", label[len("signal"):])
        if not legs:
            raise Exception("%s: cannot read a leg count from signal block '%s'" % (path, label))
        index = int(legs.group(1)) - 1
        if not 0 <= index < 4:
            raise Exception("%s: signal leg count %s out of range 1-4" % (path, legs.group(1)))
        signal[index] = to_table(block)
        populated.append(index)
    if not populated:
        raise Exception("%s: no 'Signal - N Leg' blocks found" % path)
    # apply_tchc indexes signal[min(approach_count, 4) - 1]; fill leg counts
    # the file does not supply (typically 1 leg) from the nearest one it does.
    for index in range(4):
        if index not in populated:
            nearest = min(populated, key=lambda other: abs(other - index))
            signal[index] = [row[:] for row in signal[nearest]]

    four_way_block = blocks.get("4waystop") or blocks.get("fourwaystop")
    if four_way_block is None:
        raise Exception("%s: no '4-Way Stop' block found" % path)
    four_way_stop = to_table(four_way_block)

    two_way_block = blocks.get("2waystop") or blocks.get("twowaystop")
    if two_way_block is not None:
        two_way_source = to_table(two_way_block)
    else:
        # No 2-way stop block: reuse the 4-way stop row for the stopped
        # (minor) approach, since apply_tchc indexes this table by cross
        # street only.
        two_way_source = four_way_stop
    two_way_stop = two_way_source[max(1, min(two_way_stop_roadway_class, GREEN_CYCLE_CLASS_COUNT)) - 1]

    return GreenCycleLookups(
        signal=signal,
        four_way_stop=four_way_stop,
        two_way_stop=list(two_way_stop),
    )


# ==================================================================
# Geodatabase driver
# ==================================================================

LINK_LAYER = "TNED_HwyNet"
NODE_LAYER = "TNED_HwyNodes"

# DataTableProc appends geo_coordinates alongside the raw geometry column
GEOMETRY_COLUMNS = ("geometry", "geo_coordinates")

FEET_PER_MILE = 5280.0

# TCHC computes three periods; TNED stores five
TCHC_PERIOD_TARGETS = (("A",), ("EA", "MD", "EV"), ("P",))

CAPACITY_FACTOR_BY_PERIOD_SUFFIX = (
    ("EA", 1.0 / 4.0),
    ("MD", 6.5 / 12.0),
    ("EV", 2.0 / 3.0),
    ("A", 1.0),
    ("P", 3.5 / 3.0),
)
CAPACITY_SENTINEL = 999999

# TCHCLink output attribute -> TNED field stem, by period and direction
OUTPUT_STEMS = (
    ("period_capacity_by_period_and_direction", "CP"),
    ("intersection_capacity_by_period_and_direction", "CX"),
    ("hourly_capacity_by_period_and_direction", "CH"),
    ("link_travel_time_minutes_by_period_and_direction", "TM"),
    ("intersection_delay_minutes_by_period_and_direction", "TX"),
)

# TCHCLink output attribute -> TNED field stem, by direction only
DIRECTIONAL_OUTPUT_STEMS = (
    ("resolved_green_cycle_by_direction", "GC"),
    ("resolved_per_lane_capacity_by_direction", "PLC"),
)

DIRECTION_PREFIXES = ("AB", "BA")

DEFAULT_AM_HOURS = (6, 7, 8)
DEFAULT_PM_HOURS = (15, 16, 17)

HOURS_PER_DAY = 24
STATION_KEY_COLUMNS = 2
STATION_FIXED_WIDTH = 5

# must match the bounds and fallback that apply_tchc applies to the factor it reads
FALLBACK_STATION = 1
MIN_PEAK_PERIOD_FACTOR = 1.0
MAX_PEAK_PERIOD_FACTOR = 15.0

REQUIRED_COLUMNS = (
    "HWYCOV0_ID", "NM", "AN", "BN", "FC", "HOV", "MED", "WAY", "COSTAT", "PROJ", "SPD",
    "ABLNA", "BALNA", "ABLNMD", "BALNMD", "ABLNP", "BALNP",
    "ABAU", "BAAU", "ABPLC", "BAPLC",
    "ABCNT", "BACNT", "ABTL", "BATL", "ABRL", "BARL", "ABLL", "BALL", "ABGC", "BAGC",
    "TOLLA", "TOLLMD", "TOLLP",
)

# TNED codes WAY 0 the same as 2; import_network makes both two-way
TWO_WAY_CODES = (0, 2)


def output_fields():
    """[(attribute, period index, direction index, TNED field name)] for all 50 outputs."""
    fields = []
    for attribute, stem in OUTPUT_STEMS:
        for period_index, suffixes in enumerate(TCHC_PERIOD_TARGETS):
            for direction_index, prefix in enumerate(DIRECTION_PREFIXES):
                for suffix in suffixes:
                    fields.append((attribute, period_index, direction_index, prefix + stem + suffix))
    return fields


def directional_output_fields():
    """[(attribute, direction index, TNED field name)] for the per-direction outputs."""
    fields = []
    for attribute, stem in DIRECTIONAL_OUTPUT_STEMS:
        for direction_index, prefix in enumerate(DIRECTION_PREFIXES):
            fields.append((attribute, direction_index, prefix + stem))
    return fields


OUTPUT_FIELDS = output_fields()
DIRECTIONAL_OUTPUT_FIELDS = directional_output_fields()
# GC and PLC are inputs as well as outputs, so selection tests the period fields only
OUTPUT_FIELDS_BY_DIRECTION = tuple(
    [name for _a, _p, direction, name in OUTPUT_FIELDS if direction == index]
    for index in range(len(DIRECTION_PREFIXES))
)


def adjusted_capacity(value, field_name, enabled=True):
    """Scale a populated CP/CX value for its five-period TNED target."""
    if not enabled or value is None or pd.isna(value) or value == CAPACITY_SENTINEL:
        return value
    for suffix, factor in CAPACITY_FACTOR_BY_PERIOD_SUFFIX:
        if field_name.endswith(suffix):
            return value * factor
    raise Exception("Cannot determine the time period for capacity field %s" % field_name)


# ------------------------------------------------------------------
# Input files
# ------------------------------------------------------------------

def resolve_path(base, value):
    if not value:
        return ""
    return value if os.path.isabs(value) else _join(base, value)


def read_station_table(path):
    """Station hourly percentages: station ID, direction, then 24 hourly columns."""
    if os.path.splitext(path)[1].lower() in (".csv", ".txt"):
        table = pd.read_csv(path)
    else:
        table = pd.read_fwf(path, widths=[STATION_FIXED_WIDTH] * (STATION_KEY_COLUMNS + HOURS_PER_DAY))
    expected = STATION_KEY_COLUMNS + HOURS_PER_DAY
    if len(table.columns) < expected:
        raise Exception(
            "%s: expected at least %s columns (station, direction, 24 hours), found %s"
            % (path, expected, len(table.columns)))
    return table


def load_station_peak_period_factors(path, station_count, am_hours, pm_hours):
    """[period][direction][station] -> factor converting hourly to period capacity."""
    table = read_station_table(path)
    columns = list(table.columns)
    stations = pd.to_numeric(table[columns[0]], errors="coerce")
    directions = pd.to_numeric(table[columns[1]], errors="coerce")
    hourly = table[columns[STATION_KEY_COLUMNS:STATION_KEY_COLUMNS + HOURS_PER_DAY]].apply(
        pd.to_numeric, errors="coerce")

    peak_hours = set(am_hours) | set(pm_hours)
    period_hours = (
        list(am_hours),
        [hour for hour in range(HOURS_PER_DAY) if hour not in peak_hours],
        list(pm_hours),
    )

    size = max(station_count, int(stations.max()) + 1)
    factors = [[[0.0] * size, [0.0] * size] for _ in range(len(period_hours))]
    for period_index, hours in enumerate(period_hours):
        shares = hourly.iloc[:, hours].max(axis=1) / 100.0
        for station, direction, share in zip(stations, directions, shares):
            if pd.isna(station) or pd.isna(direction) or pd.isna(share) or share <= 0:
                continue
            direction_index = int(direction) - 1
            station_id = int(station)
            if direction_index not in (0, 1) or not 0 <= station_id < size:
                continue
            factors[period_index][direction_index][station_id] = 1.0 / share

    for period_index in range(len(period_hours)):
        for direction_index in range(2):
            fallback = factors[period_index][direction_index][FALLBACK_STATION]
            if not MIN_PEAK_PERIOD_FACTOR <= fallback <= MAX_PEAK_PERIOD_FACTOR:
                raise Exception(
                    "%s: station %s period %s direction %s factor %s is outside [%s, %s]; "
                    "every unmatched link falls back to it"
                    % (path, FALLBACK_STATION, period_index, direction_index + 1, fallback,
                       MIN_PEAK_PERIOD_FACTOR, MAX_PEAK_PERIOD_FACTOR))
    return factors


def load_lookup(path, cast=int):
    """Two column CSV -> dict, using the first two columns whatever they are named."""
    if not path:
        return {}
    table = pd.read_csv(path)
    if len(table.columns) < 2:
        raise Exception("%s: expected at least two columns" % path)
    keys = pd.to_numeric(table.iloc[:, 0], errors="coerce")
    values = pd.to_numeric(table.iloc[:, 1], errors="coerce")
    lookup = {}
    for key, value in zip(keys, values):
        if not pd.isna(key) and not pd.isna(value):
            lookup[int(key)] = cast(value)
    return lookup


def load_link_ids(path):
    if not path:
        return set()
    table = pd.read_csv(path)
    values = pd.to_numeric(table.iloc[:, 0], errors="coerce").dropna()
    return set(int(value) for value in values)


# ------------------------------------------------------------------
# Geodatabase access
# ------------------------------------------------------------------

def read_layer(source, layer_name):
    """Read a geodatabase layer through the same path import_network uses."""
    data = gen_utils.DataTableProc(layer_name, source)
    columns = OrderedDict()
    for name, values in zip(data._attr_names, data._values):
        if name not in GEOMETRY_COLUMNS:
            columns[name] = values
    return pd.DataFrame(columns)


def read_links(source):
    links = read_layer(source, LINK_LAYER)
    missing = [name for name in REQUIRED_COLUMNS if name not in links.columns]
    if missing:
        raise Exception("%s in %s is missing required fields: %s" % (LINK_LAYER, source, ", ".join(missing)))
    missing = [name for _a, _p, _d, name in OUTPUT_FIELDS if name not in links.columns]
    if missing:
        raise Exception("%s in %s is missing TCHC output fields: %s" % (LINK_LAYER, source, ", ".join(missing)))
    # import_network applies the same cast; the TNED export types BN as string
    for column in ("AN", "BN", "FC", "WAY", "HWYCOV0_ID"):
        links[column] = pd.to_numeric(links[column], errors="coerce").fillna(0).astype("int64")
    return links


def length_feet(links):
    if "SHAPE_Length" in links.columns:
        return pd.to_numeric(links["SHAPE_Length"], errors="coerce").fillna(0.0)
    if "LENGTH" in links.columns:
        return pd.to_numeric(links["LENGTH"], errors="coerce").fillna(0.0) * FEET_PER_MILE
    raise Exception("%s has neither SHAPE_Length nor LENGTH" % LINK_LAYER)


def write_results(source, results, log):
    """Patch the TCHC output fields of the matched features, leaving all else alone."""
    data_source = _ogr.Open(source, 1)
    if data_source is None:
        raise Exception("Cannot open %s for update" % source)
    try:
        layer = data_source.GetLayerByName(LINK_LAYER)
        if layer is None:
            raise Exception("%s has no layer %s" % (source, LINK_LAYER))
        if not layer.TestCapability(_ogr.OLCRandomWrite):
            raise Exception(
                "The GDAL driver for %s does not support updating features. "
                "GDAL 3.6 or later is required to write a file geodatabase." % source)

        definition = layer.GetLayerDefn()
        field_types = {}
        for index in range(definition.GetFieldCount()):
            field = definition.GetFieldDefn(index)
            field_types[field.GetName()] = field.GetType()
        integer_types = (_ogr.OFTInteger, _ogr.OFTInteger64)

        feature_ids = {}
        layer.ResetReading()
        for feature in layer:
            feature_ids[feature.GetField("HWYCOV0_ID")] = feature.GetFID()

        updated = 0
        layer.StartTransaction()
        try:
            for link_id, values in results.items():
                feature_id = feature_ids.get(link_id)
                if feature_id is None:
                    continue
                feature = layer.GetFeature(feature_id)
                for name, value in values.items():
                    if value is None or pd.isna(value):
                        continue
                    if field_types[name] in integer_types:
                        feature.SetField(name, int(round(value)))
                    else:
                        feature.SetField(name, float(value))
                layer.SetFeature(feature)
                updated += 1
            layer.CommitTransaction()
        except Exception:
            layer.RollbackTransaction()
            raise
        log.append({"type": "text", "content": "Updated %s features in %s" % (updated, LINK_LAYER)})
        return updated
    finally:
        data_source = None


# ------------------------------------------------------------------
# Network topology
# ------------------------------------------------------------------

def two_way_mask(links):
    return links["WAY"].isin(TWO_WAY_CODES)


def build_approach_counts(links):
    """Approaches per node; a link approaches the node at its downstream end."""
    roads = links[(links["FC"] >= 1) & (links["FC"] <= 9)]
    approaches = pd.concat([roads["BN"], roads.loc[two_way_mask(roads), "AN"]])
    counts = approaches.value_counts().clip(2, 4)
    return dict((int(node), int(count)) for node, count in counts.items())


def build_cross_street_classes(links):
    """Per node, the two highest class cross streets, so a link can exclude itself.

    Highest class is the lowest functional class number in 2-7.
    """
    eligible = links[(links["FC"] >= 2) & (links["FC"] <= 7)]
    incidence = pd.concat([
        pd.DataFrame({"node": eligible["AN"], "link": eligible["HWYCOV0_ID"], "fc": eligible["FC"]}),
        pd.DataFrame({"node": eligible["BN"], "link": eligible["HWYCOV0_ID"], "fc": eligible["FC"]}),
    ], ignore_index=True)
    incidence = incidence.drop_duplicates(["node", "link"]).sort_values(["node", "fc"])
    incidence["rank"] = incidence.groupby("node").cumcount()

    best = incidence[incidence["rank"] == 0].set_index("node")
    runner = incidence[incidence["rank"] == 1].set_index("node")
    return {
        "best_class": dict((int(k), int(v)) for k, v in best["fc"].items()),
        "best_link": dict((int(k), int(v)) for k, v in best["link"].items()),
        "runner_class": dict((int(k), int(v)) for k, v in runner["fc"].items()),
    }


def cross_street_class(tables, node_id, link_id, default=7):
    functional_class = tables["best_class"].get(node_id)
    if functional_class is None:
        return default
    if tables["best_link"].get(node_id) == link_id:
        functional_class = tables["runner_class"].get(node_id)
        if functional_class is None:
            return default
    return functional_class


# ------------------------------------------------------------------
# Link projection
# ------------------------------------------------------------------

def build_context(links, analysis_year, auto_operating_cost_per_mile, station_peak_period_factor,
                  green_cycle, managed_lane_capacity_rate, freeway_capacity_rate,
                  time_period_adjustments, ramp_meter_direction, managed_lane_to_freeway):
    freeway_to_station = dict(
        (int(link_id), int(station))
        for link_id, station in zip(links["HWYCOV0_ID"], links["COSTAT"])
        if not pd.isna(station))
    return TCHCContext(
        auto_operating_cost_per_mile=auto_operating_cost_per_mile,
        managed_lane_capacity_rate=managed_lane_capacity_rate,
        freeway_capacity_rate=freeway_capacity_rate,
        analysis_year=analysis_year,
        approach_count=build_approach_counts(links),
        ramp_meter_direction_by_traffic_count_identifier=ramp_meter_direction,
        station_peak_period_factor=station_peak_period_factor,
        roadway_safety_adjustment_factor_by_jurisdiction=compute_safety_factors(analysis_year),
        signal_green_cycle_lookup=green_cycle.signal,
        four_way_stop_green_cycle_lookup=green_cycle.four_way_stop,
        two_way_stop_green_cycle_lookup=green_cycle.two_way_stop,
        border_delay_minutes_lookup=[],
        time_period_adjustments=time_period_adjustments,
        managed_lane_to_freeway_identifier=managed_lane_to_freeway,
        freeway_identifier_to_station_identifier=freeway_to_station,
    )


def integer(value, default=0):
    if value is None or pd.isna(value):
        return default
    return int(value)


def build_link(row, cross_tables, external_zone_delay_by_node):
    functional_class = integer(row.FC)
    from_node = integer(row.AN)
    to_node = integer(row.BN)

    speed = integer(row.SPD)
    if not 1 <= speed <= 75:
        speed = integer(row.ASPD, speed)

    external_delay = 0.0
    if functional_class == 10:
        external_delay = external_zone_delay_by_node.get(from_node, 0.0)

    return TCHCLink(
        link_identifier=integer(row.HWYCOV0_ID),
        link_name=str(row.NM or ""),
        length_feet=float(row.length_feet),
        functional_class=functional_class,
        high_occupancy_vehicle_class=integer(row.HOV, 1),
        jurisdiction=integer(row.jurisdiction),
        median_type=integer(row.MED, 1),
        directionality=2 if row.WAY in TWO_WAY_CODES else 1,
        traffic_count_identifier=integer(row.traffic_count),
        station_identifier=integer(row.COSTAT),
        project_identifier=integer(row.PROJ),
        from_node_identifier=from_node,
        to_node_identifier=to_node,
        speed=speed,
        lane_count_by_period_and_direction=[
            [integer(row.ABLNA), integer(row.BALNA)],
            [integer(row.ABLNMD), integer(row.BALNMD)],
            [integer(row.ABLNP), integer(row.BALNP)],
        ],
        auxiliary_lane_count_by_direction=[integer(row.ABAU), integer(row.BAAU)],
        planned_lane_capacity_by_direction=[integer(row.ABPLC), integer(row.BAPLC)],
        cross_street_functional_class_by_direction=[
            cross_street_class(cross_tables, to_node, integer(row.HWYCOV0_ID)),
            cross_street_class(cross_tables, from_node, integer(row.HWYCOV0_ID)),
        ],
        control_type_by_direction=[integer(row.ABCNT), integer(row.BACNT)],
        through_lane_count_by_direction=[integer(row.ABTL), integer(row.BATL)],
        right_turn_lane_count_by_direction=[integer(row.ABRL), integer(row.BARL)],
        left_turn_lane_count_by_direction=[integer(row.ABLL), integer(row.BALL)],
        green_cycle_value_by_direction=[integer(row.ABGC), integer(row.BAGC)],
        toll_cost_by_period=[integer(row.TOLLA), integer(row.TOLLMD), integer(row.TOLLP)],
        external_zone_delay_cost=external_delay,
    )


def select_links(links, link_ids, recompute_all, treat_zero_as_missing):
    in_domain = (links["FC"] >= 1) & (links["FC"] <= 10)
    if recompute_all:
        return in_domain

    two_way = two_way_mask(links)
    forward, reverse = OUTPUT_FIELDS_BY_DIRECTION
    selected = links[list(forward)].isna().any(axis=1)
    selected |= two_way & links[list(reverse)].isna().any(axis=1)
    if treat_zero_as_missing:
        selected |= (links[list(forward)] == 0).any(axis=1)
        selected |= two_way & (links[list(reverse)] == 0).any(axis=1)
    if link_ids:
        selected |= links["HWYCOV0_ID"].isin(link_ids)
    return in_domain & selected


def compute(links, selected, context, cross_tables, external_zone_delay_by_node):
    """Run apply_tchc over the selected links, returning one record per link."""
    records = []
    for row in links[selected].itertuples(index=False):
        link = build_link(row, cross_tables, external_zone_delay_by_node)
        apply_tchc(link, context)
        record = {"HWYCOV0_ID": link.link_identifier}
        for attribute, period_index, direction_index, name in OUTPUT_FIELDS:
            # one-way links never populate direction 1, so leave those fields alone
            if direction_index == 1 and link.directionality == 1:
                continue
            value = getattr(link, attribute)[period_index][direction_index]
            if attribute in (
                "period_capacity_by_period_and_direction",
                "intersection_capacity_by_period_and_direction",
            ):
                value = adjusted_capacity(value, name, context.time_period_adjustments)
            record[name] = value
        for attribute, direction_index, name in DIRECTIONAL_OUTPUT_FIELDS:
            if direction_index == 1 and link.directionality == 1:
                continue
            value = getattr(link, attribute)[direction_index]
            # unresolved for controls with no lookup, and for per-lane capacity off freeways
            if value is not None:
                record[name] = value
        records.append(record)
    return pd.DataFrame(records)


def write_report(path, results, links):
    stored = links.set_index("HWYCOV0_ID")
    columns = [name for name in results.columns if name != "HWYCOV0_ID"]
    report = results.set_index("HWYCOV0_ID")
    report = report.join(stored.loc[report.index, columns], rsuffix="_prev")
    report.to_csv(path)


# ------------------------------------------------------------------
# Tool
# ------------------------------------------------------------------

class RunTCHC(_m.Tool(), gen_utils.Snapshot):

    path = _m.Attribute(str)
    source = _m.Attribute(str)
    station_file = _m.Attribute(str)
    gc_file = _m.Attribute(str)
    link_id_file = _m.Attribute(str)
    ramp_meter_file = _m.Attribute(str)
    hov_freeway_pairs_file = _m.Attribute(str)
    external_zone_delay_file = _m.Attribute(str)
    report_file = _m.Attribute(str)
    year = _m.Attribute(int)
    aoc = _m.Attribute(float)
    managed_lane_capacity_rate = _m.Attribute(float)
    freeway_capacity_rate = _m.Attribute(float)
    time_period_adjustments = _m.Attribute(bool)
    jurisdiction_field = _m.Attribute(str)
    traffic_count_field = _m.Attribute(str)
    recompute_all = _m.Attribute(bool)
    treat_zero_as_missing = _m.Attribute(bool)
    dry_run = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self._log = []
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.path = _dir(project_dir)
        self.source = ""
        self.station_file = ""
        self.gc_file = ""
        self.link_id_file = ""
        self.ramp_meter_file = ""
        self.hov_freeway_pairs_file = ""
        self.external_zone_delay_file = ""
        self.report_file = ""
        self.year = 0
        self.aoc = 0.0
        self.managed_lane_capacity_rate = 1.0
        self.freeway_capacity_rate = 1.0
        self.time_period_adjustments = True
        self.jurisdiction_field = "JUR"
        self.traffic_count_field = ""
        self.recompute_all = False
        self.treat_zero_as_missing = False
        self.dry_run = False
        self.attributes = [
            "path", "source", "station_file", "gc_file", "link_id_file", "ramp_meter_file",
            "hov_freeway_pairs_file", "external_zone_delay_file", "report_file", "year", "aoc",
            "managed_lane_capacity_rate", "freeway_capacity_rate", "time_period_adjustments", "jurisdiction_field",
            "traffic_count_field", "recompute_all", "treat_zero_as_missing", "dry_run",
        ]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Run TCHC"
        pb.description = """
        <div style="text-align:left">
            Recompute roadway capacity, travel time and intersection delay for the
            TNED highway network and write the results back into the input geodatabase.
            <br><br>
            Links are processed when any TCHC output field is empty, or when the link
            is listed in the link ID file. The following fields are updated, for each of
            the five time periods and both directions:
            <ul>
                <li>CP - mid-link period capacity</li>
                <li>CX - intersection approach capacity</li>
                <li>CH - hourly mid-link capacity</li>
                <li>TM - link time in minutes</li>
                <li>TX - intersection delay time</li>
            </ul>
            GC (green-to-cycle ratio) and PLC (per-lane capacity) are also written, per
            direction, wherever TCHC resolves them from its lookup tables.
            <br><br>
            Run this before Import network.
        </div>
        """
        pb.branding_text = "- SANDAG - Import"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file("path", window_type="directory", file_filter="",
                           title="Scenario directory:")
        pb.add_select_file("source", window_type="directory", file_filter="",
                           title="Source gdb:", note="Default is the single *.gdb in the input directory")
        pb.add_select_file("station_file", window_type="file", file_filter="",
                           title="Count station hourly percentage file:")
        pb.add_select_file("gc_file", window_type="file", file_filter="*.csv",
                           title="Green-to-cycle ratio file:")
        pb.add_select_file("link_id_file", window_type="file", file_filter="*.csv",
                           title="Link ID list (optional):",
                           note="HWYCOV0_ID values to recompute regardless of stored values")
        pb.add_select_file("ramp_meter_file", window_type="file", file_filter="*.csv",
                           title="Ramp meter direction file (optional):")
        pb.add_select_file("hov_freeway_pairs_file", window_type="file", file_filter="*.csv",
                           title="Managed lane to freeway link file (optional):")
        pb.add_select_file("external_zone_delay_file", window_type="file", file_filter="*.csv",
                           title="External zone delay file (optional):")

        pb.add_text_box("year", size=6, title="Analysis year:")
        pb.add_text_box("aoc", size=8, title="Auto operating cost (cents/mile):")
        pb.add_text_box("managed_lane_capacity_rate", size=8, title="Managed lane capacity rate:")
        pb.add_text_box("freeway_capacity_rate", size=8, title="Freeway capacity rate:")
        pb.add_checkbox("time_period_adjustments", title=" ", label="Apply time period capacity adjustments")
        pb.add_text_box("jurisdiction_field", size=20, title="Jurisdiction field:")
        pb.add_text_box("traffic_count_field", size=20, title="Traffic count ID field (optional):")
        pb.add_text_box("report_file", size=80, title="Report file (optional):")

        pb.add_checkbox("recompute_all", title=" ", label="Recompute every link")
        pb.add_checkbox("treat_zero_as_missing", title=" ", label="Treat stored zeroes as missing values")
        pb.add_checkbox("dry_run", title=" ", label="Do not write results to the geodatabase")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(path=self.path, source=self.source, station_file=self.station_file,
                 gc_file=self.gc_file, link_id_file=self.link_id_file,
                 ramp_meter_file=self.ramp_meter_file,
                 hov_freeway_pairs_file=self.hov_freeway_pairs_file,
                 external_zone_delay_file=self.external_zone_delay_file,
                 report_file=self.report_file, year=self.year, aoc=self.aoc,
                 managed_lane_capacity_rate=self.managed_lane_capacity_rate,
                 freeway_capacity_rate=self.freeway_capacity_rate,
                 time_period_adjustments=self.time_period_adjustments,
                 jurisdiction_field=self.jurisdiction_field,
                 traffic_count_field=self.traffic_count_field,
                 recompute_all=self.recompute_all,
                 treat_zero_as_missing=self.treat_zero_as_missing,
                 dry_run=self.dry_run)
            self.tool_run_msg = _m.PageBuilder.format_info("Run TCHC complete", escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(error, _traceback.format_exc())
            raise

    def __call__(self, path="", source="", station_file="", gc_file="", link_id_file="",
                 ramp_meter_file="", hov_freeway_pairs_file="", external_zone_delay_file="",
                 report_file="", year=0, aoc=0.0,
                 managed_lane_capacity_rate=0.0, freeway_capacity_rate=0.0,
                 time_period_adjustments=None,
                 jurisdiction_field="", traffic_count_field="",
                 am_hours=DEFAULT_AM_HOURS, pm_hours=DEFAULT_PM_HOURS,
                 recompute_all=False, treat_zero_as_missing=False, dry_run=False):
        self._log = []
        self.path = path or self.path
        self.recompute_all = recompute_all
        self.treat_zero_as_missing = treat_zero_as_missing
        self.dry_run = dry_run

        load_properties = _m.Modeller().tool("sandag.utilities.properties")
        props = load_properties(_join(self.path, "conf", "sandag_abm.properties"))

        self.source = source or self._default_source()
        self.station_file = resolve_path(self.path, station_file or props.get("tchc.station.file", ""))
        self.gc_file = resolve_path(self.path, gc_file or props.get("tchc.gc.file", _join("input", "gc.csv")))
        self.link_id_file = resolve_path(self.path, link_id_file or props.get("tchc.link.list.file", ""))
        self.ramp_meter_file = resolve_path(self.path, ramp_meter_file or props.get("tchc.ramp.meter.file", ""))
        self.hov_freeway_pairs_file = resolve_path(
            self.path, hov_freeway_pairs_file or props.get("tchc.hov.freeway.pairs.file", ""))
        self.external_zone_delay_file = resolve_path(
            self.path, external_zone_delay_file or props.get("tchc.external.zone.delay.file", ""))
        self.report_file = resolve_path(self.path, report_file)
        self.year = int(year or props["scenarioYear"])
        self.aoc = float(aoc or (props["aoc.fuel"] + props["aoc.maintenance"]))
        self.managed_lane_capacity_rate = float(
            managed_lane_capacity_rate or props.get("tchc.managed.lane.capacity.rate", 1.0))
        self.freeway_capacity_rate = float(
            freeway_capacity_rate or props.get("tchc.freeway.capacity.rate", 1.0))
        self.time_period_adjustments = bool(
            props.get("tchc.time.period.adjustments", True)
            if time_period_adjustments is None else time_period_adjustments)
        self.jurisdiction_field = jurisdiction_field or "JUR"
        self.traffic_count_field = traffic_count_field

        if not self.station_file:
            raise Exception("No count station file given and tchc.station.file is not set")
        for name, file_path in [("count station", self.station_file), ("green-to-cycle", self.gc_file)]:
            if not os.path.exists(file_path):
                raise Exception("Missing %s file '%s'" % (name, file_path))

        attributes = OrderedDict([
            ("self", str(self)),
            ("path", self.path),
            ("source", self.source),
            ("station_file", self.station_file),
            ("gc_file", self.gc_file),
            ("link_id_file", self.link_id_file),
            ("year", self.year),
            ("aoc", self.aoc),
            ("managed_lane_capacity_rate", self.managed_lane_capacity_rate),
            ("freeway_capacity_rate", self.freeway_capacity_rate),
            ("time_period_adjustments", self.time_period_adjustments),
            ("recompute_all", self.recompute_all),
            ("dry_run", self.dry_run),
        ])
        with _m.logbook_trace("Run TCHC", attributes=attributes):
            gen_utils.log_snapshot("Run TCHC", str(self), attributes)
            result = self.execute(am_hours, pm_hours)
            self.log_report()
        return result

    def _default_source(self):
        candidates = _glob.glob(_join(self.path, "input", "*.gdb"))
        if len(candidates) > 1:
            raise Exception("Multiple *.gdb files found in the input directory")
        if not candidates:
            raise Exception("No *.gdb file found in the input directory")
        return candidates[0]

    def execute(self, am_hours, pm_hours):
        links = read_links(self.source)
        self._log.append({"type": "text", "content": "Read %s links from %s" % (len(links), LINK_LAYER)})

        nodes = read_layer(self.source, NODE_LAYER)
        unknown = set(links["AN"]) | set(links["BN"])
        if "HNODE" in nodes.columns:
            unknown -= set(pd.to_numeric(nodes["HNODE"], errors="coerce").fillna(0).astype("int64"))
        if unknown:
            self._log.append({
                "type": "text",
                "content": "%s node IDs referenced by %s are absent from %s" % (
                    len(unknown), LINK_LAYER, NODE_LAYER)})

        links["length_feet"] = length_feet(links)
        links["jurisdiction"] = self._jurisdiction(links)
        links["traffic_count"] = self._traffic_count(links)
        if "ASPD" not in links.columns:
            links["ASPD"] = 0

        green_cycle = load_green_cycle_lookups(self.gc_file)
        station_factors = load_station_peak_period_factors(
            self.station_file, int(links["COSTAT"].max() or 0) + 1, am_hours, pm_hours)
        context = build_context(
            links, self.year, self.aoc, station_factors, green_cycle,
            self.managed_lane_capacity_rate, self.freeway_capacity_rate,
            self.time_period_adjustments,
            load_lookup(self.ramp_meter_file), load_lookup(self.hov_freeway_pairs_file))
        external_zone_delay = load_lookup(self.external_zone_delay_file, cast=float)
        cross_tables = build_cross_street_classes(links)

        selected = select_links(
            links, load_link_ids(self.link_id_file), self.recompute_all, self.treat_zero_as_missing)
        self._log.append({
            "type": "text",
            "content": "Selected %s of %s links for recalculation" % (int(selected.sum()), len(links))})

        results = compute(links, selected, context, cross_tables, external_zone_delay)
        if results.empty:
            self._log.append({"type": "text", "content": "No links to update"})
            return 0

        if self.report_file:
            write_report(self.report_file, results, links)
            self._log.append({"type": "text", "content": "Wrote report to %s" % self.report_file})

        if self.dry_run:
            self._log.append({"type": "text", "content": "Dry run: the geodatabase was not modified"})
            return 0
        return write_results(self.source, results.set_index("HWYCOV0_ID").to_dict("index"), self._log)

    def _jurisdiction(self, links):
        defaults = DEFAULT_JURISDICTION_BY_FUNCTIONAL_CLASS
        fallback = links["FC"].map(
            lambda fc: defaults[fc - 1] if 1 <= fc <= len(defaults) else defaults[-1])
        if self.jurisdiction_field not in links.columns:
            self._log.append({
                "type": "text",
                "content": "Field %s not found; using the functional class jurisdiction defaults"
                           % self.jurisdiction_field})
            return fallback
        coded = pd.to_numeric(links[self.jurisdiction_field], errors="coerce")
        return coded.where(coded.between(1, 6), fallback).astype("int64")

    def _traffic_count(self, links):
        if not self.traffic_count_field:
            return pd.Series(0, index=links.index)
        if self.traffic_count_field not in links.columns:
            raise Exception("%s has no field %s" % (LINK_LAYER, self.traffic_count_field))
        return pd.to_numeric(links[self.traffic_count_field], errors="coerce").fillna(0).astype("int64")

    def log_report(self):
        report = _m.PageBuilder(title="Run TCHC report")
        for item in self._log:
            report.add_html("<div style='margin-left:20px'>%s</div>" % item["content"])
        _m.logbook_write("Run TCHC report", report.render())

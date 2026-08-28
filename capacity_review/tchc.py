"""
TCHC capacity calculation engine.

Computes roadway capacity, travel time, intersection delay, and generalized
cost for highway network links. This is a Python port of the FORTRAN TCHC
procedure used in the SANDAG transportation model.

Usage::

    remaining_toll = apply_tchc(link, context)

See README.md for full documentation of inputs and outputs.
"""
from dataclasses import dataclass, field
from typing import List, Dict, Optional
import math


# ------------------------------------------------------------------
# Constants (from FORTRAN DATA statements)
# ------------------------------------------------------------------

# Fallback speed (mph) by functional class, 0-indexed: FC 1..14
DEFAULT_SPEED_BY_FUNCTIONAL_CLASS = [65, 45, 40, 35, 30, 40, 35, 65, 30, 30, 50, 25, 25, 30]

# Turn-lane capacity per lane (veh/hr) by functional class, 0-indexed: FC 1..10
TURN_CAPACITY_BY_FUNCTIONAL_CLASS = [250, 250, 150, 100, 100, 100, 100, 100, 100, 0]


# ------------------------------------------------------------------
# Data model (direct mapping of tcov.inc aat* fields used by TCHC)
# ------------------------------------------------------------------

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

    # per‑mile tolls → converted in place
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

    auto_operating_cost: float = 0.0
    

# ------------------------------------------------------------------
# Context (what FORTRAN pulled from files & COMMON blocks)
# ------------------------------------------------------------------

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

    # node_id → approach count (2–4): non-connector links touching each node
    approach_count: Dict[int, int]
    # adt_id → direction code for ramp metering (1=SB,2=EB,3=NB,4=WB,9=both)
    ramp_meter_direction_by_traffic_count_identifier: Dict[int, int]
    # [period][direction][station_id] → peak-period expansion factor
    station_peak_period_factor: List[List[List[float]]]

    # jurisdiction (1–6) → capacity multiplier for signalized intersections
    roadway_safety_adjustment_factor_by_jurisdiction: Dict[int, float]

    # GC ratio lookup tables (integer percentages):
    #   signal:     [approach_count-1][fc-1][cross_fc-1]  (4×9×9)
    #   4-way stop: [fc-1][cross_fc-1]                   (9×9)
    #   2-way stop: [cross_fc-1]                          (9,)
    signal_green_cycle_lookup: List[List[List[int]]]
    four_way_stop_green_cycle_lookup: List[List[int]]
    two_way_stop_green_cycle_lookup: List[int]

    # [crossing_index][period][border_direction] → delay in minutes
    # 5 crossings: San Ysidro(0), Otay(1), East(2), Tecate(3), Jacumba(4)
    # 2 directions: SB/EB(0), NB(1)
    border_delay_minutes_lookup: List[List[List[float]]]

    # HOV link_id → adjacent GP freeway link_id (for station resolution)
    managed_lane_to_freeway_identifier: Dict[int, int] = field(default_factory=dict)
    # freeway link_id → count station_id
    freeway_identifier_to_station_identifier: Dict[int, int] = field(default_factory=dict)
    # node_id → raw sphere code (divide by 100 for sphere group)
    node_sphere_by_id: Dict[int, int] = field(default_factory=dict)


# ------------------------------------------------------------------
# Utility helpers
# ------------------------------------------------------------------

def miles(ft: float) -> float:
    return ft / 5280.0


def direction_from_name(name: str) -> int:
    if "SB" in name: return 1
    if "EB" in name: return 2
    if "NB" in name: return 3
    if "WB" in name: return 4
    return 0


def _max_lanes_for_direction(link: TCHCLink, direction_index: int) -> int:
    """Compute aatmln(idir): max lane count across all periods for one direction."""
    maximum_lane_count = 0
    for period_index in range(3):
        lane_count = link.lane_count_by_period_and_direction[period_index][direction_index]
        if 1 <= lane_count <= 8:
            maximum_lane_count = max(maximum_lane_count, lane_count)
    return maximum_lane_count


# ------------------------------------------------------------------
# Full TCHC computational procedure
# ------------------------------------------------------------------

def apply_tchc(link: TCHCLink, ctx: TCHCContext, remaining_toll=None):
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

    # ---- toll conversion: per-mile rate → absolute cents, with carry-forward
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
    # Chain: HOV link_id → freeway link_id → station_id.
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

        node_id = link.from_node_identifier if direction_index == 0 else link.to_node_identifier
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
                # freeway capacity from per-link field, bounded [1900, 2100]
                freeway_capacity_per_lane = 2000.0
                if 1600 <= link.planned_lane_capacity_by_direction[0] <= 2400:
                    freeway_capacity_per_lane = float(link.planned_lane_capacity_by_direction[0])
                freeway_capacity_per_lane = min(freeway_capacity_per_lane, 2100.0)
                freeway_capacity_per_lane = max(freeway_capacity_per_lane, 1900.0)

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
                # arterials (fc 2–7): check plc==950 override
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

            elif control_type == 2:  # 4‑way stop (FORTRAN 620)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.20
                green_cycle_value = link.green_cycle_value_by_direction[direction_index]
                if green_cycle_value < 1:
                    green_cycle_value = ctx.four_way_stop_green_cycle_lookup[link.functional_class - 1][cross_street_functional_class_index]
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

            elif control_type == 3:  # 2‑way stop (FORTRAN 630)
                link.intersection_delay_minutes_by_period_and_direction[period_index][direction_index] = 0.20
                green_cycle_value = ctx.two_way_stop_green_cycle_lookup[cross_street_functional_class_index]
                green_cycle_through_factor = green_cycle_value / 100.0
                green_cycle_right_factor = green_cycle_value / 100.0
                green_cycle_left_factor = green_cycle_value / 100.0
                # special case: irt==7 was already sanitized above,
                # but in FORTRAN this check happens before sanitization
                # of values >=7. Re-check original value for this case.
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

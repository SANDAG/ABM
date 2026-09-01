"""
Adapter between an Emme network and the TCHC capacity engine.

Reads the link/node attributes created by
``src/main/emme/toolbox/import/import_network.py`` into :class:`tchc.TCHCLink`
objects, runs :func:`tchc.apply_tchc`, and writes the results back onto the
corresponding Emme extra attributes.

Two structural differences between the Emme network and the TNED/aat network
that TCHC was written for drive the design of this module:

1. **Emme links are directed.** A two-way TNED arc is imported as two Emme
   links carrying ``@tcov_id`` and ``-@tcov_id``.  Every one-way TNED field
   (``ABCNT``/``BACNT``, ``ABTL``/``BATL``, ...) therefore already sits on the
   correct directed link.  Each Emme link is read as a *one-way* TCHCLink and
   only direction index 0 is used.

2. **Emme has five time periods, TCHC has three.**  TCHC period 0 (AM) reads
   and writes ``_am``; period 1 (midday/off-peak) reads ``_md`` and writes
   ``_ea``, ``_md`` and ``_ev``; period 2 (PM) reads and writes ``_pm``.

See the "Emme network adapter" section of ``README.md`` for the full attribute
mapping, the TCHC inputs ``import_network.py`` does not carry into Emme, and
the Emme attributes that must not be fed back in as inputs.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Tuple

from tchc import TCHCContext, TCHCLink, apply_tchc


# ------------------------------------------------------------------
# Period mapping
# ------------------------------------------------------------------

EMME_TIME_PERIODS: Tuple[str, ...] = ("ea", "am", "md", "pm", "ev")

# TCHC period index -> Emme period suffix used when *reading* inputs
TCHC_PERIOD_SOURCE: Tuple[str, ...] = ("am", "md", "pm")

# TCHC period index -> Emme period suffixes written with that result
TCHC_PERIOD_TARGETS: Tuple[Tuple[str, ...], ...] = (("am",), ("ea", "md", "ev"), ("pm",))

FEET_PER_MILE = 5280.0

# Fallback jurisdiction (1-6) by functional class; mirrors the FORTRAN
# ``mjur`` DATA statement used when the coded value is out of range.
DEFAULT_JURISDICTION_BY_FUNCTIONAL_CLASS = [1, 5, 5, 6, 6, 6, 6, 1, 1, 6, 6, 6, 6, 1]


# ------------------------------------------------------------------
# Attribute name configuration
# ------------------------------------------------------------------

@dataclass(frozen=True)
class EmmeAttributeNames:
    """Emme attribute names consumed and produced by the adapter.

    Defaults match the names created by ``import_network.py``.  Names ending
    in ``_{p}`` are templates expanded with an Emme period suffix.

    The four ``optional_*`` entries are *not* created by ``import_network.py``;
    they are read when present so a customised import can supply them.
    """

    # --- inputs (created by import_network.py) ---
    link_identifier: str = "@tcov_id"
    link_name: str = "#name"
    hov_class: str = "@hov"
    median_type: str = "@median"
    project_code: str = "@project_code"
    speed_posted: str = "@speed_posted"
    speed_adjusted: str = "@speed_adjusted"
    auxiliary_lanes: str = "@lane_auxiliary"
    traffic_control: str = "@traffic_control"
    turn_thru: str = "@turn_thru"
    turn_right: str = "@turn_right"
    turn_left: str = "@turn_left"
    green_to_cycle_init: str = "@green_to_cycle_init"
    sphere: str = "@sphere"
    lane: str = "@lane_{p}"

    # --- inputs missing from import_network.py (read if the user adds them) ---
    optional_jurisdiction: str = "@jurisdiction"
    optional_station: str = "@count_station"
    optional_traffic_count: str = "@adt_id"
    optional_per_lane_capacity: str = "@plc"

    # --- outputs ---
    toll: str = "@toll_{p}"
    time_link: str = "@time_link_{p}"
    time_inter: str = "@time_inter_{p}"
    capacity_hourly: str = "@capacity_hourly_{p}"
    capacity_link: str = "@capacity_link_{p}"
    capacity_inter: str = "@capacity_inter_{p}"
    cost_operating: str = "@cost_operating"
    generalized_cost: str = "@tchc_gencost"

    def periodic(self, template: str, period: str) -> str:
        return template.format(p=period)


DEFAULT_ATTRIBUTES = EmmeAttributeNames()


# ------------------------------------------------------------------
# Reader
# ------------------------------------------------------------------

def _is_road_link(link) -> bool:
    """True for links TCHC understands (functional class 1-10)."""
    return 1 <= link.type <= 10


class EmmeNetworkReader:
    """Projects Emme links onto :class:`tchc.TCHCLink` objects.

    Topology-derived lookups (cross-street class, approach counts, node
    spheres) are computed once at construction.
    """

    def __init__(
        self,
        network,
        attributes: EmmeAttributeNames = DEFAULT_ATTRIBUTES,
        toll_units: str = "absolute",
        length_units_per_mile: float = 1.0,
        external_zone_delay_by_zone: Optional[Dict[int, float]] = None,
    ):
        if toll_units not in ("absolute", "per_mile"):
            raise ValueError("toll_units must be 'absolute' or 'per_mile'")
        self.network = network
        self.attributes = attributes
        self.toll_units = toll_units
        self.length_units_per_mile = float(length_units_per_mile)
        self.external_zone_delay_by_zone = external_zone_delay_by_zone or {}

        self.available = set(network.attributes("LINK"))
        self.approach_count: Dict[int, int] = {}
        self.node_sphere_by_id: Dict[int, int] = {}
        self._cross_street_class: Dict[Tuple[int, int], int] = {}
        self._build_topology_lookups()

    # -- setup ---------------------------------------------------------

    def _build_topology_lookups(self) -> None:
        a = self.attributes
        sphere_available = a.sphere in self.available
        outgoing: Dict[int, int] = {}
        incident: Dict[int, List[Tuple[int, int]]] = {}
        sphere: Dict[int, int] = {}

        for link in self.network.links():
            if not _is_road_link(link):
                continue
            i_id = link.i_node.number
            j_id = link.j_node.number
            if 1 <= link.type <= 9:
                outgoing[i_id] = outgoing.get(i_id, 0) + 1
            entry = (int(self._raw(link, a.link_identifier, 0)), link.type)
            incident.setdefault(i_id, []).append(entry)
            incident.setdefault(j_id, []).append(entry)
            if sphere_available:
                sphere_value = int(self._raw(link, a.sphere, 0))
                for node_id in (i_id, j_id):
                    if sphere_value > sphere.get(node_id, 0):
                        sphere[node_id] = sphere_value

        self.approach_count = {n: max(2, min(4, c)) for n, c in outgoing.items()}
        self.node_sphere_by_id = sphere

        # cross-street class: lowest type in 2..7 at the approach node,
        # ignoring this link and its own reverse (same |@tcov_id|)
        for node_id, entries in incident.items():
            for link_id, _link_type in entries:
                key = (node_id, abs(link_id))
                if key in self._cross_street_class:
                    continue
                best = 10
                for other_id, other_type in entries:
                    if abs(other_id) == abs(link_id):
                        continue
                    if other_type < best:
                        best = other_type
                self._cross_street_class[key] = best if 2 <= best <= 7 else 7

    # -- attribute access ----------------------------------------------

    def _raw(self, link, name: str, default):
        if name not in self.available:
            return default
        value = link[name]
        return default if value is None else value

    def _number(self, link, name: str, default: float = 0.0) -> float:
        try:
            return float(self._raw(link, name, default))
        except (TypeError, ValueError):
            return float(default)

    def _integer(self, link, name: str, default: int = 0) -> int:
        return int(round(self._number(link, name, default)))

    # -- projection ----------------------------------------------------

    def length_feet(self, link) -> float:
        return float(link.length) / self.length_units_per_mile * FEET_PER_MILE

    def cross_street_class(self, link) -> int:
        key = (link.i_node.number, abs(self._integer(link, self.attributes.link_identifier)))
        return self._cross_street_class.get(key, 7)

    def jurisdiction(self, link) -> int:
        a = self.attributes
        coded = self._integer(link, a.optional_jurisdiction, 0)
        if 1 <= coded <= 6:
            return coded
        index = link.type - 1
        if 0 <= index < len(DEFAULT_JURISDICTION_BY_FUNCTIONAL_CLASS):
            return DEFAULT_JURISDICTION_BY_FUNCTIONAL_CLASS[index]
        return 6

    def tolls_per_mile(self, link) -> List[int]:
        """Emme @toll_* are absolute cents; apply_tchc expects a per-mile rate."""
        a = self.attributes
        tolls = [self._number(link, a.periodic(a.toll, p), 0.0) for p in TCHC_PERIOD_SOURCE]
        if self.toll_units == "per_mile":
            return [int(round(t)) for t in tolls]
        distance_miles = self.length_feet(link) / FEET_PER_MILE
        if distance_miles <= 0.0:
            return [int(round(t)) for t in tolls]
        return [int(round(t / distance_miles)) for t in tolls]

    def to_tchc_link(self, link) -> TCHCLink:
        a = self.attributes
        speed = self._integer(link, a.speed_posted, 0)
        if speed < 1 or speed > 75:
            speed = self._integer(link, a.speed_adjusted, 0)

        lanes = [
            [self._integer(link, a.periodic(a.lane, p), 0), 0]
            for p in TCHC_PERIOD_SOURCE
        ]
        cross_class = self.cross_street_class(link)

        external_delay = 0.0
        if link.type == 10:
            for node in (link.i_node, link.j_node):
                if getattr(node, "is_centroid", False):
                    external_delay = self.external_zone_delay_by_zone.get(node.number, 0.0)
                    break

        return TCHCLink(
            link_identifier=self._integer(link, a.link_identifier, 0),
            link_name=str(self._raw(link, a.link_name, "")),
            length_feet=self.length_feet(link),
            functional_class=link.type,
            high_occupancy_vehicle_class=self._integer(link, a.hov_class, 1) or 1,
            jurisdiction=self.jurisdiction(link),
            median_type=self._integer(link, a.median_type, 1) or 1,
            directionality=1,  # Emme links are already directed
            traffic_count_identifier=self._integer(link, a.optional_traffic_count, 0),
            station_identifier=self._integer(link, a.optional_station, 0),
            project_identifier=self._integer(link, a.project_code, 0),
            from_node_identifier=link.i_node.number,
            to_node_identifier=link.j_node.number,
            speed=speed,
            lane_count_by_period_and_direction=lanes,
            auxiliary_lane_count_by_direction=[self._integer(link, a.auxiliary_lanes, 0), 0],
            planned_lane_capacity_by_direction=[
                self._integer(link, a.optional_per_lane_capacity, 0), 0
            ],
            cross_street_functional_class_by_direction=[cross_class, cross_class],
            control_type_by_direction=[self._integer(link, a.traffic_control, 0), 0],
            through_lane_count_by_direction=[self._integer(link, a.turn_thru, 0), 0],
            right_turn_lane_count_by_direction=[self._integer(link, a.turn_right, 0), 0],
            left_turn_lane_count_by_direction=[self._integer(link, a.turn_left, 0), 0],
            green_cycle_value_by_direction=[self._integer(link, a.green_to_cycle_init, 0), 0],
            toll_cost_by_period=self.tolls_per_mile(link),
            external_zone_delay_cost=external_delay,
        )

    def road_links(self) -> Iterable:
        for link in self.network.links():
            if _is_road_link(link):
                yield link


# ------------------------------------------------------------------
# Context
# ------------------------------------------------------------------

def compute_safety_factors(analysis_year: int) -> Dict[int, float]:
    """Roadway safety adjustment factor by jurisdiction (FORTRAN rsafac)."""
    factors = {j: 1.0 for j in range(1, 7)}
    if analysis_year > 2015:
        capped_year = min(analysis_year, 2020)
        factor = 1.0 + (capped_year - 2010) * 0.01
        for jurisdiction in range(1, 5):
            factors[jurisdiction] = factor
    return factors


def build_context(
    reader: EmmeNetworkReader,
    analysis_year: int,
    auto_operating_cost_per_mile: float,
    station_peak_period_factor,
    signal_green_cycle_lookup,
    four_way_stop_green_cycle_lookup,
    two_way_stop_green_cycle_lookup,
    managed_lane_capacity_rate: float = 1.0,
    freeway_capacity_rate: float = 1.0,
    ramp_meter_direction_by_traffic_count_identifier: Optional[Dict[int, int]] = None,
    border_delay_minutes_lookup=None,
    managed_lane_to_freeway_identifier: Optional[Dict[int, int]] = None,
    freeway_identifier_to_station_identifier: Optional[Dict[int, int]] = None,
    roadway_safety_adjustment_factor_by_jurisdiction: Optional[Dict[int, float]] = None,
) -> TCHCContext:
    """Assemble a TCHCContext, deriving from the network what Emme can supply.

    ``approach_count`` and ``node_sphere_by_id`` come from the network
    topology; every other lookup is external data the caller must provide.
    """
    return TCHCContext(
        auto_operating_cost_per_mile=auto_operating_cost_per_mile,
        managed_lane_capacity_rate=managed_lane_capacity_rate,
        freeway_capacity_rate=freeway_capacity_rate,
        analysis_year=analysis_year,
        approach_count=reader.approach_count,
        ramp_meter_direction_by_traffic_count_identifier=(
            ramp_meter_direction_by_traffic_count_identifier or {}
        ),
        station_peak_period_factor=station_peak_period_factor,
        roadway_safety_adjustment_factor_by_jurisdiction=(
            roadway_safety_adjustment_factor_by_jurisdiction
            if roadway_safety_adjustment_factor_by_jurisdiction is not None
            else compute_safety_factors(analysis_year)
        ),
        signal_green_cycle_lookup=signal_green_cycle_lookup,
        four_way_stop_green_cycle_lookup=four_way_stop_green_cycle_lookup,
        two_way_stop_green_cycle_lookup=two_way_stop_green_cycle_lookup,
        border_delay_minutes_lookup=border_delay_minutes_lookup or [[[0.0]]],
        managed_lane_to_freeway_identifier=managed_lane_to_freeway_identifier or {},
        freeway_identifier_to_station_identifier=freeway_identifier_to_station_identifier or {},
        node_sphere_by_id=reader.node_sphere_by_id,
    )


# ------------------------------------------------------------------
# Writer
# ------------------------------------------------------------------

class EmmeNetworkWriter:
    """Writes TCHC results onto the Emme link they were read from."""

    def __init__(
        self,
        network,
        attributes: EmmeAttributeNames = DEFAULT_ATTRIBUTES,
        write_closed_periods: bool = False,
    ):
        self.network = network
        self.attributes = attributes
        self.write_closed_periods = write_closed_periods
        self.available = set(network.attributes("LINK"))

    def _set(self, link, name: str, value) -> None:
        if name in self.available:
            link[name] = value

    def write(self, link, tchc_link: TCHCLink) -> None:
        a = self.attributes
        self._set(link, a.cost_operating, tchc_link.auto_operating_cost)
        self._set(link, a.generalized_cost, tchc_link.generalized_cost_by_direction[0])

        for tchc_period, emme_periods in enumerate(TCHC_PERIOD_TARGETS):
            lane_count = tchc_link.lane_count_by_period_and_direction[tchc_period][0]
            if lane_count == 9 and not self.write_closed_periods:
                # apply_tchc skips closed lane configurations; leave the
                # existing Emme values rather than writing 999/999999.
                continue
            travel_time = tchc_link.link_travel_time_minutes_by_period_and_direction[tchc_period][0]
            delay = tchc_link.intersection_delay_minutes_by_period_and_direction[tchc_period][0]
            hourly = tchc_link.hourly_capacity_by_period_and_direction[tchc_period][0]
            period_capacity = tchc_link.period_capacity_by_period_and_direction[tchc_period][0]
            intersection_capacity = tchc_link.intersection_capacity_by_period_and_direction[tchc_period][0]
            toll = tchc_link.toll_cost_by_period[tchc_period]
            for period in emme_periods:
                self._set(link, a.periodic(a.time_link, period), travel_time)
                self._set(link, a.periodic(a.time_inter, period), delay)
                self._set(link, a.periodic(a.capacity_hourly, period), hourly)
                self._set(link, a.periodic(a.capacity_link, period), period_capacity)
                self._set(link, a.periodic(a.capacity_inter, period), intersection_capacity)
                self._set(link, a.periodic(a.toll, period), toll)


def ensure_output_attributes(scenario, attributes: EmmeAttributeNames = DEFAULT_ATTRIBUTES) -> List[str]:
    """Create the extra attributes the adapter writes but the import does not.

    Returns the names that were created.
    """
    created = []
    for name, description in ((attributes.generalized_cost, "TCHC generalized cost (cents)"),):
        if not scenario.extra_attribute(name):
            scenario.create_extra_attribute("LINK", name).description = description
            created.append(name)
    return created


# ------------------------------------------------------------------
# Orchestration
# ------------------------------------------------------------------

@dataclass
class TCHCRunResult:
    links_processed: int = 0
    links_skipped: int = 0
    tchc_links: Dict[int, TCHCLink] = field(default_factory=dict)


def apply_tchc_to_network(
    network,
    context: TCHCContext,
    reader: EmmeNetworkReader,
    writer: Optional[EmmeNetworkWriter] = None,
    keep_links: bool = False,
) -> TCHCRunResult:
    """Run TCHC over every road link in ``network``.

    Links are evaluated independently, so the per-mile toll carry-forward
    (``remaining_toll``) is not chained: Emme links are not in route order.
    """
    result = TCHCRunResult()
    for link in network.links():
        if not _is_road_link(link):
            result.links_skipped += 1
            continue
        tchc_link = reader.to_tchc_link(link)
        apply_tchc(tchc_link, context)
        if writer is not None:
            writer.write(link, tchc_link)
        if keep_links:
            result.tchc_links[tchc_link.link_identifier] = tchc_link
        result.links_processed += 1
    return result


def apply_tchc_to_scenario(
    scenario,
    analysis_year: int,
    auto_operating_cost_per_mile: float,
    station_peak_period_factor,
    signal_green_cycle_lookup,
    four_way_stop_green_cycle_lookup,
    two_way_stop_green_cycle_lookup,
    attributes: EmmeAttributeNames = DEFAULT_ATTRIBUTES,
    toll_units: str = "absolute",
    length_units_per_mile: float = 1.0,
    external_zone_delay_by_zone: Optional[Dict[int, float]] = None,
    publish: bool = True,
    keep_links: bool = False,
    **context_kwargs,
) -> TCHCRunResult:
    """Read a scenario's network, run TCHC, and publish the updated network."""
    ensure_output_attributes(scenario, attributes)
    network = scenario.get_network()
    reader = EmmeNetworkReader(
        network,
        attributes=attributes,
        toll_units=toll_units,
        length_units_per_mile=length_units_per_mile,
        external_zone_delay_by_zone=external_zone_delay_by_zone,
    )
    context = build_context(
        reader,
        analysis_year=analysis_year,
        auto_operating_cost_per_mile=auto_operating_cost_per_mile,
        station_peak_period_factor=station_peak_period_factor,
        signal_green_cycle_lookup=signal_green_cycle_lookup,
        four_way_stop_green_cycle_lookup=four_way_stop_green_cycle_lookup,
        two_way_stop_green_cycle_lookup=two_way_stop_green_cycle_lookup,
        **context_kwargs,
    )
    writer = EmmeNetworkWriter(network, attributes=attributes)
    result = apply_tchc_to_network(network, context, reader, writer, keep_links=keep_links)
    if publish:
        scenario.publish_network(network, resolve_attributes=True)
    return result

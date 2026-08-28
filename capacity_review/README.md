# TCHC Capacity Calculation

Python implementation of the SANDAG TCHC (Transportation Coverage Highway Capacity) procedure, which computes roadway capacity, travel time, intersection delay, and generalized cost for every link in the highway network.

## Overview

The TCHC procedure takes a highway network of directed links and computes five core outputs for each link, by time period and direction:

1. **Hourly capacity** (HCAP) — the sustainable throughput in vehicles/hour
2. **Period capacity** — hourly capacity scaled by a peak-period factor derived from count-station data
3. **Intersection capacity** — capacity as constrained by downstream intersection control (signals, stops, meters, toll booths)
4. **Link travel time** — free-flow travel time in minutes, derived from link length and coded speed
5. **Generalized cost** — a composite impedance combining travel time, intersection delay, operating cost, and tolls

These outputs feed the static traffic assignment step of the activity-based travel model.

## Dimensions

All capacity and time outputs are indexed by **3 time periods** and **2 directions**:

| Period index | Meaning |
|---|---|
| 0 | AM peak |
| 1 | Midday / off-peak |
| 2 | PM peak |

| Direction index | Meaning |
|---|---|
| 0 | AB (from-node → to-node) |
| 1 | BA (to-node → from-node, two-way links only) |

One-way links (`directionality=1`) skip direction index 1 entirely.

## Entry Point

```python
from tchc import TCHCLink, TCHCContext, apply_tchc

remaining_toll = apply_tchc(link, context)
```

`apply_tchc` mutates the `link` object in place, populating all output fields. It returns a `remaining_toll` list (3 floats) representing fractional toll cents carried forward to the next link in a route sequence. Pass this value to the next call when processing links in route order; pass `None` or omit it for standalone evaluation.

---

## Input: `TCHCLink`

A `TCHCLink` represents a single road segment with all attributes needed for the capacity calculation.

### Identifiers

| Field | Type | Description |
|---|---|---|
| `link_identifier` | `int` | Unique numeric ID for this link |
| `link_name` | `str` | Street name. Parsed for directional substrings (`NB`, `SB`, `EB`, `WB`) and special-case names (`ACCESS`, `BORDER`, `YSIDRO`, `OTAY`, etc.) |
| `length_feet` | `float` | Link length in feet |
| `from_node_identifier` | `int` | Node ID at the A-end of the link |
| `to_node_identifier` | `int` | Node ID at the B-end of the link |

### Classification

| Field | Type | Valid range | Description |
|---|---|---|---|
| `functional_class` | `int` | 1–10 | Determines which capacity formula applies. See table below |
| `high_occupancy_vehicle_class` | `int` | 1–4 | 1=general purpose, 2=HOV2+, 3=HOV3+, 4=toll facility |
| `jurisdiction` | `int` | 1–6 | Owning agency. Used to look up the roadway safety adjustment factor for signalized intersections |
| `median_type` | `int` | 1–3 | 1=none/undivided, 2=raised median, 3=center turn lane. Values ≥2 are treated as "divided" |
| `directionality` | `int` | 1–2 | 1=one-way (AB only), 2=two-way (AB and BA) |

#### Functional class definitions

| FC | Facility type | Base capacity formula |
|---|---|---|
| 1 | Freeway | `lanes × PLC + aux_lanes × 1200`, with per-lane capacity clamped to [1900, 2100] |
| 2 | Prime arterial | `lanes × 1800 − median_adj`, intersection-constrained by GC ratio |
| 3 | Major arterial | Same as FC 2 |
| 4 | Collector | Same as FC 2 |
| 5 | Local collector | Same as FC 2 |
| 6 | Rural collector | Same as FC 2 |
| 7 | Local street | Same as FC 2 |
| 8 | Freeway-to-freeway connector | `lanes × 1800`; links named `ACCESS` get uncapped capacity (9999) |
| 9 | Ramp | `lanes × 1200` |
| 10 | Zone connector | No capacity computed; receives travel time only |

### Speed and station data

| Field | Type | Description |
|---|---|---|
| `speed` | `int` | Coded free-flow speed in mph. If outside [1, 75], defaults to a per-FC lookup (e.g. 65 for freeways, 35 for collectors) |
| `station_identifier` | `int` | Count station ID. Used to look up the peak-period factor for freeways. Certain station IDs (935, 980, 999) trigger per-lane-capacity overrides. Station 936 is exempt from the 1900 veh/hr/lane floor |
| `traffic_count_identifier` | `int` | ADT link identifier. Used for ramp metering direction lookup and station-specific PLC overrides (ADT 552, 553) |
| `project_identifier` | `int` | Project number. IDs 613 and 614 trigger a managed-lane capacity rate multiplier |

### Lane configuration

All lane fields use the sentinel value **9** to indicate a closed/unavailable lane configuration for that period or direction. The procedure skips capacity computation entirely when the lane count is 9.

| Field | Type | Shape | Description |
|---|---|---|---|
| `lane_count_by_period_and_direction` | `List[List[int]]` | [3][2] | Through-lanes by [period][direction]. Values 1–8 are valid lane counts |
| `auxiliary_lane_count_by_direction` | `List[int]` | [2] | Auxiliary (weaving/acceleration) lanes. Only used for freeway capacity, at 1200 veh/hr/lane |
| `planned_lane_capacity_by_direction` | `List[int]` | [2] | Per-lane capacity override (PLC). For freeways, values in [1600, 2400] replace the default 2000 veh/hr/lane. The special value 950 triggers a rural single-lane arterial override |

### Intersection control

These fields describe the downstream intersection for each direction. Direction 0 (AB) uses the from-node; direction 1 (BA) uses the to-node.

| Field | Type | Shape | Description |
|---|---|---|---|
| `control_type_by_direction` | `List[int]` | [2] | Intersection control. See table below |
| `through_lane_count_by_direction` | `List[int]` | [2] | Through lanes at the intersection approach |
| `right_turn_lane_count_by_direction` | `List[int]` | [2] | Dedicated right-turn lanes |
| `left_turn_lane_count_by_direction` | `List[int]` | [2] | Dedicated left-turn lanes |
| `green_cycle_value_by_direction` | `List[int]` | [2] | Green/cycle ratio × 100 (i.e. 50 = 0.50 G/C). If below a threshold, overridden by a lookup table |
| `cross_street_functional_class_by_direction` | `List[int]` | [2] | FC of the highest-class cross street (2–7). Defaults to 7. Used to index green/cycle lookup tables |

#### Control types

| Code | Type | Delay (min) | Capacity formula |
|---|---|---|---|
| 0 | No control | 0.0 | Mid-block capacity only (no intersection constraint) |
| 1 | Signal | 0.17 | `through × 1800 × GC + turn_lanes × TLC`, min 1000. Scaled by jurisdiction safety factor |
| 2 | 4-way stop | 0.20 | `through × 1800 × GC + turn_lanes × TLC`, min 500 |
| 3 | 2-way stop | 0.20 | `through × 500 × GC + right × 500 × GC + left × 500 × GC`, min 500 |
| 4 | Ramp meter (off-peak active) | 0.50 | `1000 × GC`, off-peak periods only |
| 5 | Ramp meter (peak active) | 0.50 | `1000 × GC`, off-peak periods only |
| 6 | Rail crossing | 0.02 | No capacity override; mid-block capacity preserved |
| 7 | Toll booth / border | 1.0 or lookup | `max(through, max_lanes) × 500`. Border crossings use a delay lookup table; domestic toll booths add a fixed cost to operating cost |

Turn lane counts go through a sanitization step: values >7 are zeroed, values of exactly 7 are set to 1, and if no through lanes remain, the largest turn-lane count is promoted to through.

### Tolls and costs

| Field | Type | Shape | Description |
|---|---|---|---|
| `toll_cost_by_period` | `List[int]` | [3] | Per-mile toll rate in cents. Converted in-place to total link toll (rounded to nearest cent, minimum 1¢ if nonzero). Fractional remainders carry to the next link via `remaining_toll` |
| `external_zone_delay_cost` | `float` | scalar | Extra impedance cost (in cents) for zone connectors at external stations. Added directly to generalized cost |

---

## Input: `TCHCContext`

Global parameters and lookup tables shared across all links.

### Scalar parameters

| Field | Type | Description |
|---|---|---|
| `auto_operating_cost_per_mile` | `float` | Vehicle operating cost in cents/mile. Multiplied by link distance to get `auto_operating_cost` |
| `managed_lane_capacity_rate` | `float` | Multiplier applied to HOV3+ lane capacity and certain project-specific links. Typically 1.0 |
| `freeway_capacity_rate` | `float` | Multiplier applied to general-purpose freeway and FC=8 capacity. Typically 1.0 |
| `analysis_year` | `int` | Scenario year. Years > 2015 enable traffic system management (TSM) features including ramp metering capacity bonuses and station-specific PLC overrides |

### Node-level lookups

| Field | Type | Key → Value | Description |
|---|---|---|---|
| `approach_count` | `Dict[int, int]` | node_id → count (2–4) | Number of non-connector link approaches at each node. Clamped to [2, 4]. Used to index the signal green/cycle lookup |
| `node_sphere_by_id` | `Dict[int, int]` | node_id → sphere code | Geographic sphere of each node (raw value). Divided by 100 to get sphere group. Sphere groups 3 (Coronado) and 14 (City of SD) affect toll booth cost surcharges |

### Station data

| Field | Type | Shape | Description |
|---|---|---|---|
| `station_peak_period_factor` | `List[List[List[float]]]` | [3][2][n_stations] | Peak-period expansion factor indexed by `[period][direction][station_id]`. Converts hourly capacity to period capacity. Valid range [1.0, 15.0]; out-of-range values fall back to station 1. For freeways, direction is determined by link name (NB/WB → index 1, else → index 0) rather than the loop direction |
| `ramp_meter_direction_by_traffic_count_identifier` | `Dict[int, int]` | adt_id → direction | Ramp metering direction code. Value 9 means both directions; values 1–4 correspond to SB/EB/NB/WB. When a metered freeway link matches, its capacity gets a 1.10× bonus |

### HOV/managed lane mappings

| Field | Type | Key → Value | Description |
|---|---|---|---|
| `managed_lane_to_freeway_identifier` | `Dict[int, int]` | hov_link_id → freeway_link_id | Maps HOV lane link IDs to their adjacent general-purpose freeway link IDs. Used to resolve station IDs for HOV links, which don't have their own count stations |
| `freeway_identifier_to_station_identifier` | `Dict[int, int]` | freeway_link_id → station_id | Maps freeway link IDs to count station IDs. Chained with the above to resolve HOV station data |

### Intersection green/cycle lookup tables

These tables provide default green/cycle ratios (as integer percentages) when the coded value on the link is below a threshold.

| Field | Type | Shape | Lookup key | Used for |
|---|---|---|---|---|
| `signal_green_cycle_lookup` | `List[List[List[int]]]` | [4][9][9] | [approach_count−1] [functional_class−1] [cross_fc−1] | Signals (control type 1). Coded GC values ≥ 10 are used as-is |
| `four_way_stop_green_cycle_lookup` | `List[List[int]]` | [9][9] | [functional_class−1] [cross_fc−1] | 4-way stops (control type 2). Coded GC values ≥ 1 are used as-is |
| `two_way_stop_green_cycle_lookup` | `List[int]` | [9] | [cross_fc−1] | 2-way stops (control type 3). Always overrides coded value |

### Safety and border parameters

| Field | Type | Description |
|---|---|---|
| `roadway_safety_adjustment_factor_by_jurisdiction` | `Dict[int, float]` | Multiplier on signalized intersection capacity, keyed by jurisdiction (1–6). For analysis years > 2015, jurisdictions 1–4 get `1.0 + (min(year, 2020) − 2010) × 0.01`, giving values from 1.06 to 1.10. Jurisdictions 5–6 remain 1.0 |
| `border_delay_minutes_lookup` | `List[List[List[float]]]` | Border crossing delay in minutes, indexed `[crossing][period][direction]`. 5 crossings (San Ysidro, Otay Mesa, East Otay, Tecate, Jacumba) × 3 periods × 2 directions (SB/EB=0, NB=1) |

---

## Output: fields populated on `TCHCLink`

After `apply_tchc` returns, the following fields on the link are populated:

| Field | Shape | Description |
|---|---|---|
| `link_travel_time_minutes_by_period_and_direction` | [3][2] | Free-flow travel time. For border crossings, includes the period-0 border delay added to all periods |
| `intersection_delay_minutes_by_period_and_direction` | [3][2] | Delay at the downstream intersection. The value depends on control type (see table above) |
| `hourly_capacity_by_period_and_direction` | [3][2] | Sustainable throughput in veh/hr. For links with intersection control, this is the intersection-constrained value |
| `period_capacity_by_period_and_direction` | [3][2] | Hourly capacity × peak-period factor |
| `intersection_capacity_by_period_and_direction` | [3][2] | Intersection-constrained capacity × peak-period factor. Only differs from period capacity for signalized/stop-controlled links |
| `generalized_cost_by_direction` | [2] | Composite impedance (see formula below). Capped at 999,999 |
| `auto_operating_cost` | scalar | `distance_miles × auto_operating_cost_per_mile`, plus toll booth surcharge (25¢ or 50¢) if applicable |
| `toll_cost_by_period` | [3] | Converted from per-mile rate to absolute cents for this link (mutated in place) |
| `computed_freeway_capacity_per_lane_by_direction` | [2] | Final per-lane capacity for freeways (after TSM adjustment). Used for binary writeback only |

### Generalized cost formula

$$
GC = C_{\text{ext}} + C_{\text{aoc}} + (T_{\text{link}}^{AM} + T_{\text{delay}}^{AM}) \times 35 + \frac{\text{toll}_{AM} + \text{toll}_{MD}}{2}
$$

Where:
- $C_{\text{ext}}$ = external zone delay cost (zone connectors at external stations only)
- $C_{\text{aoc}}$ = auto operating cost (distance × per-mile rate + toll booth surcharge)
- $T_{\text{link}}^{AM}$ = AM peak link travel time in minutes
- $T_{\text{delay}}^{AM}$ = AM peak intersection delay in minutes
- 35 = value of time conversion factor (cents per minute)
- $\text{toll}_{AM}$, $\text{toll}_{MD}$ = converted toll costs for periods 0 and 1

## Computation flow

```
for each link:
│
├─ Convert per-mile tolls to absolute cents (carry remainder to next link)
├─ Compute auto operating cost = distance × cents/mile
├─ Resolve speed (coded value or FC default)
├─ Resolve station ID (for HOV, chain through freeway adjacency)
│
└─ for each direction (AB, then BA if two-way):
   │
   ├─ Look up approach count at downstream node
   │
   └─ for each period (AM, MD, PM):
      │
      ├─ Skip if lane_count == 9 (closed)
      ├─ Set link travel time = distance / speed × 60
      ├─ Skip capacity if FC == 10 (zone connector)
      │
      ├─ Resolve peak-period factor from station data
      │
      ├─ Compute base capacity by facility type:
      │   ├─ FC 1: freeway formula with PLC overrides, HOV/TSM adjustments
      │   ├─ FC 8: connector formula with ACCESS special case
      │   ├─ FC 9: ramp formula
      │   └─ FC 2–7: arterial formula with median adjustment
      │
      ├─ Set hourly_capacity and period_capacity
      │
      ├─ Sanitize turn-lane counts (clamp, fallback)
      │
      └─ Apply intersection control (if any):
          ├─ Signal: GC lookup → through×1800×GC + turns×TLC, min 1000, × safety factor
          ├─ 4-way stop: GC lookup → through×1800×GC + turns×TLC, min 500
          ├─ 2-way stop: GC lookup → all_lanes×500×GC, min 500
          ├─ Ramp meter: 1000×GC (off-peak only)
          ├─ Rail crossing: delay only (0.02 min)
          └─ Toll/border: through×500, with border delay lookup or toll surcharge
   │
   ├─ Border adjustment: add period-0 delay to all periods' travel time
   ├─ Freeway PLC writeback
   └─ Compute generalized cost
```

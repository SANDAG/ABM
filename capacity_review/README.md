# TCHC network capacity recalculation

TCHC (Transportation Coverage Highway Capacity) computes roadway capacity, travel
time, intersection delay and generalized cost for every highway link. Those
values feed the static traffic assignment step of the activity-based model.

`src/main/emme/toolbox/import/run_tchc.py` is an Emme Modeller tool that
re-derives those values from the TNED network and writes them back into the
input file geodatabase, so that `import_network.py` imports refreshed
capacities. It lets you re-derive capacities after editing the network, instead
of going back through the TNED ETL.

The tool contains both the capacity engine (a Python port of the FORTRAN TCHC
procedure) and the geodatabase driver in a single file, because the Modeller
toolbox is built in consolidated mode and a helper module would appear as a
second, non-runnable toolbox entry.

## What the process produces

Five outputs per link, per direction, per time period:

1. **Hourly capacity** (`CH`) — sustainable throughput in vehicles/hour
2. **Period capacity** (`CP`) — hourly capacity scaled by a peak-period factor derived from count-station data
3. **Intersection capacity** (`CX`) — capacity as constrained by the downstream intersection control (signals, stops, meters, toll booths)
4. **Link travel time** (`TM`) — free-flow travel time in minutes, from link length and coded speed
5. **Intersection delay** (`TX`) — delay in minutes at the downstream intersection

And two per link, per direction, which TCHC resolves from its own lookup tables:

6. **Green-to-cycle ratio** (`GC`) — the ratio actually used at the intersection, which is the coded value where it is usable and a table lookup otherwise
7. **Per-lane capacity** (`PLC`) — the freeway per-lane capacity after the coded value is validated and clamped

Each is written back to the `TNED_HwyNet` field the network import already
reads — see [What gets written back](#what-gets-written-back).

---

## Where it runs

| | |
|---|---|
| Tool namespace | `sandag.import.run_tchc` |
| Toolbox position | `TOOLBOX_ORDER = 11`, between `run4Ds` (10) and `import_network` (12) |
| Skip flag | `RunModel.skipTCHC` |

`master_run.py` calls it after `run4Ds` and before `import_network`, against the
single `*.gdb` found in the scenario `input` directory.

## Running it

From the Modeller toolbox, open **Import → Run TCHC** and fill in the page.

From the Emme Python shell or a Modeller notebook:

```python
import os
modeller = inro.modeller.Modeller()
main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
run_tchc = modeller.tool("sandag.import.run_tchc")
run_tchc(path=main_directory)
```

Preview without touching the geodatabase, and write a CSV of computed against
stored values:

```python
run_tchc(path=main_directory, dry_run=True, report_file="tchc_report.csv")
```

Recompute a specific set of links regardless of whether their stored values are
populated, by listing `HWYCOV0_ID` values in the first column of a CSV:

```python
run_tchc(path=main_directory, link_id_file="input/tchc_links.csv")
```

### Which links are processed

A link is recomputed when **either** of these holds, and its functional class is
in 1–10:

- any of the 50 period-and-direction output fields is empty — the `AB` fields
  always, plus the `BA` fields when `WAY` is 2 or 0
- its `HWYCOV0_ID` appears in `link_id_file`

`GC` and `PLC` are deliberately excluded from that test, because they are inputs
as well as outputs: an empty `GC` legitimately means "no signal", so testing it
would select almost every link.

`recompute_all=True` processes every link in the functional class range.
`treat_zero_as_missing=True` additionally treats a stored zero as empty.

Links with functional class 11 (rail), 12 (bus) and 99 (transfer walk) are always
skipped: they are outside TCHC's domain and would index past the end of the
green/cycle lookup tables.

---

## Inputs

### Files

| Input | Tool argument | Properties key | Default |
|---|---|---|---|
| Network geodatabase | `source` | — | the single `*.gdb` in `<path>/input` |
| Count-station hourly percentages | `station_file` | `tchc.station.file` | `input/sta.hrpct` |
| Green/cycle ratios | `gc_file` | `tchc.gc.file` | `input/gc.csv` |
| Forced link list (optional) | `link_id_file` | `tchc.link.list.file` | none |
| Ramp meter directions (optional) | `ramp_meter_file` | `tchc.ramp.meter.file` | none |
| Managed lane ↔ freeway pairs (optional) | `hov_freeway_pairs_file` | `tchc.hov.freeway.pairs.file` | none |
| External zone delay (optional) | `external_zone_delay_file` | `tchc.external.zone.delay.file` | none |

Relative paths are resolved against `path`, the scenario directory.

### Parameters

| Parameter | Tool argument | Properties key | Default |
|---|---|---|---|
| Analysis year | `year` | `scenarioYear` | — |
| Auto operating cost (cents/mile) | `aoc` | `aoc.fuel` + `aoc.maintenance` | — |
| Managed lane capacity rate | `managed_lane_capacity_rate` | `tchc.managed.lane.capacity.rate` | 1.0 |
| Freeway capacity rate | `freeway_capacity_rate` | `tchc.freeway.capacity.rate` | 1.0 |
| Apply time-period capacity adjustments | `time_period_adjustments` | `tchc.time.period.adjustments` | `true` |
| Jurisdiction field | `jurisdiction_field` | — | `JUR` |
| ADT link ID field | `traffic_count_field` | — | unset |
| AM peak hours | `am_hours` | — | 6, 7, 8 |
| PM peak hours | `pm_hours` | — | 15, 16, 17 |

Years after 2015 enable traffic system management features and raise the
jurisdiction safety factor.

The two capacity rates are plain values in `sandag_abm.properties`. To vary them
by year instead, add columns of the same name to `parametersByYears.csv` —
`set_year_specific_properties` overrides any key whose name matches a column, so
no change to the properties template is needed.

### Count-station file

CSV or fixed width (26 fields of 5 characters, the legacy `sta.hrpct` layout).
The first two columns are the station ID and the direction (1 or 2); the
remaining 24 are that station's hourly share of daily traffic, as percentages,
for hours 0 to 23. Column names are ignored — only position matters, which
accommodates the legacy file's `a%6` / `o%0` / `p%15` header.

The peak-period factor is `1 / (max hourly share in the period / 100)`, taken
over the AM hours, the PM hours, and all remaining hours for the off-peak
period.

Station 1 is the fallback for every link that cannot resolve its own station, so
the tool refuses to run if station 1's factor falls outside [1.0, 15.0] in any
period or direction.

### Green/cycle file

`gc.csv` gives default G/C ratios used when the value coded on the link is below
a threshold. Column 1 is the intersection control type, column 2 the roadway
functional class, and the remaining columns the crossroad functional classes
named in the header.

- Recognised control types are `Signal - 1/2/3/4 Leg`, `4-Way Stop` and,
  optionally, `2-Way Stop`. Label matching ignores case, spaces and punctuation.
- Each `Signal - N Leg` block lands at signal table index `N - 1`. Leg counts
  absent from the file — usually 1 — are copied from the nearest one present.
- Ratios coded as fractions (`0.35`) are rescaled to percentages (`35`); a file
  already in percent is left alone.
- If there is no `2-Way Stop` block, it is taken from the `4-Way Stop` row for
  the stopped (minor) approach, functional class 7 by default.

### Optional lookup files

Each is a CSV whose first two columns are read as key and value, whatever they
are named.

| File | Key → value | Effect when absent |
|---|---|---|
| `ramp_meter_file` | ADT link ID → direction code (1=SB, 2=EB, 3=NB, 4=WB, 9=both) | The 1.10× TSM ramp-meter bonus never applies |
| `hov_freeway_pairs_file` | managed lane link ID → parallel freeway link ID | HOV links fall back to station 1 |
| `external_zone_delay_file` | node ID → delay cost in cents | Generalized cost at external-station connectors omits the delay term |

---

## Inputs the geodatabase cannot supply

The tool falls back for each of these. The fallbacks are safe but not exact.

| TCHC input | Why it is missing | Fallback |
|---|---|---|
| `traffic_count_identifier` | `ADT` is not in the documented `TNED_HwyNet` schema. Set `traffic_count_field` if your export has it | 0 |
| `ramp_meter_direction_by_traffic_count_identifier` | A ramp-meter list, not network data | empty — no 1.10× bonus, and inert anyway without an ADT field |
| `managed_lane_to_freeway_identifier` | HOV↔GP pairing is not in TNED | empty — HOV links resolve to station 1 |
| `external_zone_delay_cost` | A model parameter, not network data | 0.0 |
| `cross_street_functional_class_by_direction` | TCHC derived it from the aat turn tables, which TNED does not carry | Derived from topology, see below |
| `approach_count` | A derived count, never stored | Derived from topology, see below |
| `border_delay_minutes_lookup` | Border delay table | Never read by the engine; supplying it has no effect |
| `node_sphere_by_id` | The toll-booth sphere surcharge is not implemented | Never read by the engine |

`freeway_identifier_to_station_identifier` is built from the geodatabase itself,
mapping `HWYCOV0_ID` to `COSTAT`.

`roadway_safety_adjustment_factor_by_jurisdiction` is computed from the analysis
year: for years after 2015, jurisdictions 1–4 get
`1.0 + (min(year, 2020) − 2010) × 0.01`, giving 1.06 to 1.10; jurisdictions 5–6
stay at 1.0.

### Derived from topology

**Approach count.** A link approaches a node at its *downstream* end, so for
each link of functional class 1–9 the count is incremented at `BN`, and also at
`AN` when the link is two-way. The result is clamped to [2, 4] and selects the
leg-count dimension of the signal green/cycle table.

**Cross-street functional class.** At each node, the highest-class cross street
— the lowest functional class number in 2–7 — among the links incident to that
node other than the link being evaluated. Defaults to 7. Direction 0 (AB) uses
the class at `BN`; direction 1 (BA) uses the class at `AN`.

---

## How the network is read and written

Reading uses `gen_utils.DataTableProc`, the same
`inro.emme.datatable.DataSource` path `import_network.py` uses, so both tools see
the geodatabase identically — including the `BN`-arrives-as-string quirk, which
is cast the same way. The layer is loaded into a pandas DataFrame straight from
the numpy column arrays.

Writing uses `osgeo.ogr` in update mode. Only the TCHC output fields of the
matched features are patched, inside a transaction; geometry, `OBJECTID`, field
aliases, domains and subtypes are untouched. The Emme data table API has no
write path back to a geodatabase — `DataTableProc.save()` writes to the Emme
project's data tables, not the source — so reading and writing necessarily use
different libraries. Both are already dependencies of the toolbox.

Updating a file geodatabase in place requires GDAL 3.6 or later. The tool checks
the layer's random-write capability and fails with a clear message otherwise.

### Structural notes

- **TNED arcs carry both directions.** One `TNED_HwyNet` record holds the `AB`
  and `BA` field pairs, and `WAY` says whether the reverse direction exists.
  Direction index 0 is AB, index 1 is BA. `WAY` of 0 is treated as two-way, as
  `import_network.py` does.
- **TNED has five time periods, TCHC has three.** TCHC period 0 (AM) writes
  `A`; period 1 (midday/off-peak) writes `EA`, `MD` and `EV`; period 2 (PM)
  writes `P`.
- Periods whose lane count is 9 (closed) are skipped.
- One-way links never populate direction 1, so their `BA` fields are left as
  they were rather than being overwritten with the engine's sentinels.

### Input mapping

| `TCHCLink` field | TNED field | Notes |
|---|---|---|
| `link_identifier` | `HWYCOV0_ID` | |
| `link_name` | `NM` | Parsed for `NB`/`SB`/`EB`/`WB` and `ACCESS` |
| `length_feet` | `SHAPE_Length` | Falls back to `LENGTH` × 5280 |
| `functional_class` | `FC` | Restricted to 1–10 |
| `high_occupancy_vehicle_class` | `HOV` | |
| `jurisdiction` | `JUR` | 1–6. **Not** `COJUR`, which is a 1–20 count jurisdiction. Values outside 1–6 fall back to a per-FC default table |
| `median_type` | `MED` | |
| `directionality` | `WAY` | **Not** `DIR`, which is a compass heading |
| `station_identifier` | `COSTAT` | |
| `project_identifier` | `PROJ` | |
| `from`/`to_node_identifier` | `AN` / `BN` | |
| `speed` | `SPD` | Falls back to `ASPD` when outside [1, 75] |
| `lane_count_by_period_and_direction` | `ABLNA`/`BALNA`, `ABLNMD`/`BALNMD`, `ABLNP`/`BALNP` | |
| `auxiliary_lane_count_by_direction` | `ABAU` / `BAAU` | |
| `planned_lane_capacity_by_direction` | `ABPLC` / `BAPLC` | |
| `control_type_by_direction` | `ABCNT` / `BACNT` | |
| `through`/`right`/`left_turn_lane_count` | `ABTL`/`BATL`, `ABRL`/`BARL`, `ABLL`/`BALL` | |
| `green_cycle_value_by_direction` | `ABGC` / `BAGC` | Already coded as G/C × 100 |
| `toll_cost_by_period` | `TOLLA`, `TOLLMD`, `TOLLP` | Per-mile rates in cents |
| `cross_street_functional_class_by_direction` | *derived from topology* | |
| `traffic_count_identifier` | *`traffic_count_field`, if set* | |
| `external_zone_delay_cost` | *`external_zone_delay_file`, keyed on `AN`* | Zone connectors only |

### What gets written back

| `TCHCLink` output | TNED field stem | Fields |
|---|---|---|
| `period_capacity_by_period_and_direction` | `CP` | `AB`/`BA` × `EA`, `A`, `MD`, `P`, `EV` |
| `intersection_capacity_by_period_and_direction` | `CX` | same |
| `hourly_capacity_by_period_and_direction` | `CH` | same |
| `link_travel_time_minutes_by_period_and_direction` | `TM` | same |
| `intersection_delay_minutes_by_period_and_direction` | `TX` | same |

Fifty fields in total, plus four written per direction only:

| `TCHCLink` output | TNED field | Written when |
|---|---|---|
| `resolved_green_cycle_by_direction` | `ABGC`, `BAGC` | Control type is 1, 2 or 3, where a lookup can override the coded value. Ramp meters use the coded value unchanged, and the remaining control types have no G/C at all, so nothing is written |
| `resolved_per_lane_capacity_by_direction` | `ABPLC`, `BAPLC` | Functional class is 1. Other classes have no per-lane capacity to resolve, so the coded value is left alone |

Both are left untouched wherever TCHC resolves nothing, rather than being
overwritten with a zero.

#### Why `PLC` is written only for freeways

Per-lane capacity is a *freeway* concept in this procedure. Functional class 1
is the only class where the engine turns the coded `PLC` into a number: it takes
the coded value if it falls in [1600, 2400], substitutes 2000 otherwise, then
clamps the result to [1900, 2100]. That resolved figure multiplies the lane
count, so there is a genuine derived value to report back.

Everywhere else there is nothing to derive:

| Functional class | How `PLC` is used | Capacity comes from |
|---|---|---|
| 1 | Validated and clamped into a per-lane rate | `lanes × PLC + aux × 1200` |
| 2–7 | Read only as the sentinel `950` | `950` when the sentinel matches on a single-lane link, otherwise `lanes × 1800 − median adjustment` |
| 8, 9, 10 | Not read at all | `lanes × 1800`, `lanes × 1200`, and nothing respectively |

On an arterial, `PLC` is a flag rather than a rate. When it holds 950 on a
single-lane link the whole direction gets a flat 950 veh/hr — a link capacity,
not a per-lane one. Otherwise the 1800 veh/hr/lane in the arterial formula is a
constant that was never read from `PLC` at all.

Writing anything back for those classes would mean inventing a value, and
because `PLC` is an input field the invented value would be read on the next
run. Writing 1800 to an arterial would overwrite the `950` sentinel, so a rural
single-lane link would silently fall through to the general arterial formula and
drop to `1 × 1800 − 300 = 1500` veh/hr, or 1300 if undivided.

> `PLC` for freeways is read from the AB value in both directions, which is
> carried over from the original procedure and left as-is. TNED codes freeways
> as one-way arcs in practice, so `BAPLC` is rarely reached; where it is, it
> receives a value derived from `ABPLC`.

**Deliberately not written back:**

| Output | Why |
|---|---|
| `TOLLA`, `TOLLMD`, `TOLLP` | The engine converts these from a per-mile rate to an absolute cost *in place*. Writing them back would corrupt the input on the next run |
| `generalized_cost_by_direction` | No corresponding TNED field. Available in the report |
| `auto_operating_cost` | No corresponding TNED field. `import_network.py` derives `@cost_operating` from length and the operating cost itself |

> **After the run:** `import_network.py` derives `@cost_auto_*`, `@cost_hov2_*`,
> `@cost_med_truck_*` and friends from `@toll_*` and `@cost_operating` using
> `vehicle_class_toll_factors.csv`. Running the network import after this tool
> keeps those consistent.

---

## Reference: link data fields (`TCHCLink`)

### Identifiers

| Field | Type | Description |
|---|---|---|
| `link_identifier` | `int` | Unique numeric ID for this link |
| `link_name` | `str` | Street name. Parsed for directional substrings (`NB`, `SB`, `EB`, `WB`) and the special case `ACCESS` |
| `length_feet` | `float` | Link length in feet |
| `from_node_identifier` | `int` | Node ID at the A-end of the link |
| `to_node_identifier` | `int` | Node ID at the B-end of the link |

### Classification

| Field | Valid range | Description |
|---|---|---|
| `functional_class` | 1–10 | Determines which capacity formula applies |
| `high_occupancy_vehicle_class` | 1–4 | 1=general purpose, 2=HOV2+, 3=HOV3+, 4=toll facility |
| `jurisdiction` | 1–6 | Owning agency. Used to look up the roadway safety adjustment factor for signalized intersections |
| `median_type` | 1–3 | 1=none/undivided, 2=raised median, 3=center turn lane. Values ≥2 are treated as "divided" |
| `directionality` | 1–2 | 1=one-way (AB only), 2=two-way (AB and BA) |

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

| Field | Description |
|---|---|
| `speed` | Coded free-flow speed in mph. If outside [1, 75], defaults to a per-FC lookup (65 for freeways, 35 for collectors, and so on) |
| `station_identifier` | Count station ID, used to look up the peak-period factor for freeways |
| `traffic_count_identifier` | ADT link identifier, used for ramp metering direction lookup |
| `project_identifier` | Project number. IDs 613 and 614 trigger the managed-lane capacity rate multiplier |

### Lane configuration

All lane fields use the sentinel value **9** for a closed or unavailable lane
configuration in that period or direction; capacity is not computed for it.

| Field | Shape | Description |
|---|---|---|
| `lane_count_by_period_and_direction` | [3][2] | Through-lanes by [period][direction]. Values 1–8 are valid lane counts |
| `auxiliary_lane_count_by_direction` | [2] | Auxiliary (weaving/acceleration) lanes. Freeways only, at 1200 veh/hr/lane |
| `planned_lane_capacity_by_direction` | [2] | Per-lane capacity override. For freeways, values in [1600, 2400] replace the default 2000 veh/hr/lane. The special value 950 triggers a single-lane arterial override. The value actually used is reported back in `resolved_per_lane_capacity_by_direction` |

### Intersection control

These describe the downstream intersection for each direction — direction 0 (AB)
uses the B node, direction 1 (BA) uses the A node.

| Field | Shape | Description |
|---|---|---|
| `control_type_by_direction` | [2] | Intersection control, see below |
| `through_lane_count_by_direction` | [2] | Through lanes at the intersection approach |
| `right_turn_lane_count_by_direction` | [2] | Dedicated right-turn lanes |
| `left_turn_lane_count_by_direction` | [2] | Dedicated left-turn lanes |
| `green_cycle_value_by_direction` | [2] | Green/cycle ratio × 100. Below a threshold, overridden by a lookup table. The value actually used is reported back in `resolved_green_cycle_by_direction` |
| `cross_street_functional_class_by_direction` | [2] | Functional class of the highest-class cross street (2–7), default 7 |

#### Control types

| Code | Type | Delay (min) | Capacity formula |
|---|---|---|---|
| 0 | No control | 0.0 | Mid-block capacity only |
| 1 | Signal | 0.17 | `through × 1800 × GC + turn_lanes × TLC`, min 1000, scaled by the jurisdiction safety factor |
| 2 | 4-way stop | 0.20 | `through × 1800 × GC + turn_lanes × TLC`, min 500 |
| 3 | 2-way stop | 0.20 | `through × 500 × GC + right × 500 × GC + left × 500 × GC`, min 500 |
| 4 | Ramp meter (off-peak active) | 0.50 | `1000 × GC`, all periods except AM |
| 5 | Ramp meter (peak active) | 0.50 | `1000 × GC`, all periods except AM |
| 6 | Rail crossing | 0.02 | No capacity override; mid-block capacity preserved |
| 7 | Toll booth / border | 1.0 | `max(through, max_lanes) × 500` |

Turn lane counts are sanitized first: values above 7 are zeroed, values of
exactly 7 become 1, and if no through lanes remain the largest turn-lane count is
promoted to through.

### Tolls and costs

| Field | Shape | Description |
|---|---|---|
| `toll_cost_by_period` | [3] | Per-mile toll rate in cents, converted in place to the total link toll (rounded to the nearest cent, minimum 1¢ if nonzero). Fractional remainders carry to the next link via `remaining_toll` |
| `external_zone_delay_cost` | scalar | Extra impedance in cents for zone connectors at external stations. Added directly to generalized cost |

---

## Reference: scenario data (`TCHCContext`)

### Scalar parameters

| Field | Description |
|---|---|
| `auto_operating_cost_per_mile` | Vehicle operating cost in cents/mile |
| `managed_lane_capacity_rate` | Multiplier on HOV3+ lane capacity and projects 613/614 |
| `freeway_capacity_rate` | Multiplier on general-purpose freeway and FC 8 capacity |
| `time_period_adjustments` | Whether five-period factors are applied to populated CP and CX outputs |
| `analysis_year` | Years after 2015 enable traffic system management features |

### Lookups

| Field | Key → Value | Description |
|---|---|---|
| `approach_count` | node ID → count (2–4) | Approaches at each node, clamped to [2, 4]. Indexes the signal green/cycle lookup |
| `station_peak_period_factor` | [period][direction][station] | Peak-period expansion factor. Valid range [1.0, 15.0]; out-of-range values fall back to station 1. For freeways the direction comes from the link name (NB/WB → index 1) rather than the loop direction |
| `ramp_meter_direction_by_traffic_count_identifier` | ADT ID → direction | Value 9 means both directions; 1–4 are SB/EB/NB/WB. A matching metered freeway link gets a 1.10× bonus |
| `managed_lane_to_freeway_identifier` | HOV link ID → freeway link ID | Resolves station IDs for HOV links, which have no count stations of their own |
| `freeway_identifier_to_station_identifier` | freeway link ID → station ID | Chained with the above |
| `roadway_safety_adjustment_factor_by_jurisdiction` | jurisdiction → multiplier | Applied to signalized intersection capacity only |
| `node_sphere_by_id` | node ID → sphere code | **Declared but never read** |
| `border_delay_minutes_lookup` | [crossing][period][direction] | **Declared but never read** |

### Green/cycle lookup tables

| Field | Shape | Lookup key | Used for |
|---|---|---|---|
| `signal_green_cycle_lookup` | [4][9][9] | [approach_count−1][fc−1][cross_fc−1] | Signals. Coded G/C values ≥ 10 are used as-is |
| `four_way_stop_green_cycle_lookup` | [9][9] | [fc−1][cross_fc−1] | 4-way stops. Coded G/C values ≥ 1 are used as-is |
| `two_way_stop_green_cycle_lookup` | [9] | [cross_fc−1] | 2-way stops. Always overrides the coded value |

All three are integer percentages, because the engine divides by 100 and
compares the coded link value against the thresholds above.

---

## Reference: the capacity calculation procedure

### Dimensions

| Period index | Meaning | TNED periods written |
|---|---|---|
| 0 | AM peak | `A` |
| 1 | Midday / off-peak | `EA`, `MD`, `EV` |
| 2 | PM peak | `P` |

| Direction index | Meaning |
|---|---|
| 0 | AB (from-node → to-node), downstream intersection at the B node |
| 1 | BA (to-node → from-node), downstream intersection at the A node |

One-way links skip direction index 1 entirely.

### Flow

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
   ├─ Look up approach count at the downstream node
   │
   ├─ for each period (AM, MD, PM):
   │  │
   │  ├─ Skip if lane_count == 9 (closed)
   │  ├─ Set link travel time = distance / speed × 60
   │  ├─ Skip capacity if FC == 10 (zone connector)
   │  │
   │  ├─ Resolve peak-period factor from station data
   │  │
   │  ├─ Compute base capacity by facility type:
   │  │   ├─ FC 1: freeway formula with PLC overrides, HOV/TSM adjustments
   │  │   ├─ FC 8: connector formula with ACCESS special case
   │  │   ├─ FC 9: ramp formula
   │  │   └─ FC 2–7: arterial formula with median adjustment
   │  │
   │  ├─ Set hourly_capacity and period_capacity
   │  ├─ Sanitize turn-lane counts (clamp, fallback)
   │  │
   │  └─ Apply intersection control (if any):
   │      ├─ Signal: GC lookup → through×1800×GC + turns×TLC, min 1000, × safety factor
   │      ├─ 4-way stop: GC lookup → through×1800×GC + turns×TLC, min 500
   │      ├─ 2-way stop: GC lookup → all_lanes×500×GC, min 500
   │      ├─ Ramp meter: 1000×GC (all periods except AM)
   │      ├─ Rail crossing: delay only (0.02 min)
   │      └─ Toll/border: through×500, delay 1.0 min
   │
   └─ Compute generalized cost
```

Note that `period_capacity` is the *mid-block* capacity scaled by the
peak-period factor; the intersection control overwrites `hourly_capacity` and
sets `intersection_capacity`, but leaves `period_capacity` alone.

The toll carry-forward only matters when links are processed in route order. The
tool evaluates links independently, since the TNED table is not ordered by route.

### Generalized cost formula

$$
GC = C_{\text{ext}} + C_{\text{aoc}} + (T_{\text{link}}^{AM} + T_{\text{delay}}^{AM}) \times 35 + \frac{\text{toll}_{AM} + \text{toll}_{MD}}{2}
$$

Where:
- $C_{\text{ext}}$ = external zone delay cost (zone connectors at external stations only)
- $C_{\text{aoc}}$ = auto operating cost (distance × per-mile rate)
- $T_{\text{link}}^{AM}$ = AM peak link travel time in minutes
- $T_{\text{delay}}^{AM}$ = AM peak intersection delay in minutes
- 35 = value of time conversion factor (cents per minute)
- $\text{toll}_{AM}$, $\text{toll}_{MD}$ = converted toll costs for periods 0 and 1

Capped at 999,999.

---

## Differences from the original FORTRAN

### Intentional correction

The FORTRAN port took the *from* node as the approach node for direction AB.
TNED documents `ABCNT`, `ABTL`, `ABRL`, `ABLL` and `ABGC` as the intersection at
the **TO (B) end** of the link — the downstream intersection for AB traffic — so
the approach node is now the link's downstream end in each direction. This
affects the leg-count dimension of the signal green/cycle lookup, and therefore
signalized capacity where the two nodes have different leg counts.

### Not implemented

`TCHCContext` declares two fields the engine never reads. Supplying them has no
effect:

- `border_delay_minutes_lookup` — control type 7 applies a flat 1.0 minute delay
  instead of a per-crossing, per-period border delay.
- `node_sphere_by_id` — the toll-booth operating-cost surcharge for sphere
  groups 3 (Coronado) and 14 (City of San Diego) is not applied.

`HwyETL_vs_TCHC.md` documents the further differences between this port and the
FME HwyETL workbench.

---

## This folder

`capacity_review` holds the review material for the port, not the running code.

| File | Role |
|---|---|
| `README.md` | This document |
| `tchc.py` | A standalone copy of the capacity engine, kept for review and for the notebook. Behaviourally identical to the engine section of `run_tchc.py` |
| `tchc_run.ipynb` | Drives the engine from the TNED geodatabase for a single link and compares the result against the stored values |
| `gc.csv`, `gc.txt` | The green/cycle lookup table, and its original fixed-width form. `gc.csv` is also shipped as `input/model/gc.csv` |
| `HwyETL_final.md` | The FME HwyETL workbench logic, transcribed |
| `HwyETL_vs_TCHC.md` | Differences between this port and the FME workbench |

The engine section of `run_tchc.py` is kept textually identical to
`capacity_review/tchc.py` so the two can be diffed directly. Keep it that way —
in particular, do not replace the engine's literal constants with the driver's
named equivalents.

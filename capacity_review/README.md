# TCHC network capacity recalculation

TCHC (Transportation Coverage Highway Capacity) computes roadway capacity, travel
time, and intersection delay for every highway link. Those
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

The production tool finalizes CP/CX after hourly capacity and control overrides:

- `CP = round(final CH × station period factor, 3)`.
- Controls 1–6 use the same value for CX. Uncontrolled links retain CX=999999.
- Toll booths use hourly roadway capacity `lanes × max(min_PLC, PLC) + aux × 1200` for CH/CP,
  and `round(current-period lanes × 500 × station period factor, 3)` for CX.
  The period factor is applied once; the FME transcription's toll units need
  verification against the original workbench.
- Closed periods (lanes=9) have CH=CP=CX=0. FC8 ACCESS has CH=9999, scaled CP
  and unconstrained CX, bypassing intersection controls. When selected, open
  FC10 links receive TM but retain engine defaults CH=0 and CP=CX=999999.
- EA, MD and EV share the off-peak result by default. Optional extra CP/CX
  multipliers are available through `time_period_adjustments`, the toolbox
  checkbox, or `tchc.time.period.adjustments`. They are **disabled by default**.
  When enabled, the factors are EA=1/4, AM=1, MD=6.5/12, PM=3.5/3 and EV=2/3.
  Scaled outputs are rounded to three decimals and unconstrained sentinels are
  preserved. CH, TM and TX are not scaled.

This aligns the period conversion; remaining differences in hourly formulas
are tracked in [the FME review](run_tchc_FME_TODO.md). The standalone review
engine `tchc.py` and its adapter are older implementations, not the production
tool. The focused production-capacity test suite can be invoked with:

```powershell
python -m unittest discover -s capacity_review -p test_run_tchc_capacities.py -v
```

The suite is currently stale and does not validate the current implementation:
the verification run reported 27 errors across 13 tests. Its context fixture
passes the removed `border_delay_minutes_lookup` argument, and the toll test
also references the removed `resolved_per_lane_capacity_by_direction` attribute.
The toll fixture additionally needs to supply `link.is_rural` to reach the
capacity calculation. Update these fixtures and assertions before using the
suite to validate production results.

And two direction-only values, resolved or passed through by the engine:

6. **Green-to-cycle ratio** (`GC`) — the ratio actually used at the intersection, which is the coded value where it is usable and a table lookup otherwise
7. **Per-lane capacity** (`PLC`) — the direction's planned lane capacity, clamped to [1900, 2100] on open freeway periods and otherwise passed through

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

`RunTCHC.__init__(self)` takes no caller-supplied arguments. It initializes the
tool's page attributes; the options below belong to `RunTCHC.__call__`, invoked
by calling the tool returned by `modeller.tool(...)`. Use keyword arguments to
make overrides explicit.

From the Emme Python shell or a Modeller notebook:

```python
import os
import inro.modeller
modeller = inro.modeller.Modeller()
main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
run_tchc = modeller.tool("sandag.import.run_tchc")
run_tchc(path=main_directory)
```

Preview without touching the geodatabase, and write a CSV of computed against
stored values:

```python
run_tchc(path=main_directory, dry_run=True)
```

Recompute a specific set of links regardless of whether their stored values are
populated, by providing a path to a file containing `HWYCOV0_ID` entries
(one per line):

```python
run_tchc(path=main_directory, link_id_file="input/tchc_links.csv")
```

### Caller signature and defaults

```python
run_tchc(
    path="",
    source="",
    station_file="",
    gc_file="",
    link_id_file="",
    managed_lane_capacity_rate=0.0,
    freeway_capacity_rate=0.0,
    time_period_adjustments=None,
    am_hours=(6, 7, 8),
    pm_hours=(15, 16, 17),
    recompute_all=False,
    treat_zero_as_missing=False,
    dry_run=False,
)
```

The defaults above are the Python call defaults. An empty string or zero often
means "resolve from scenario properties", rather than an empty input or a zero
parameter. Every call loads `<path>/conf/sandag_abm.properties`, even when all
inputs are supplied explicitly.

#### Argument definitions and resolution

See [Files](#files) for input arguments, property keys, and configured defaults;
[Parameters](#parameters) for calculation parameters and their property keys;
and [Scalar parameters](#scalar-parameters) for their effects. File formats are
specified under [Count-station file](#count-station-file) and
[Green/cycle file](#greencycle-file).

The following details supplement those definitions for scripted calls:

- `path` is the scenario root. An empty value retains the tool's current path,
  initially two directory levels above `modeller.desktop.project.path`. Prefer
  an absolute path.
- File and directory arguments are strings. An explicit `source` is passed
  through unchanged, unlike the relative paths described under [Files](#files);
  use an absolute geodatabase path or join it to `main_directory`. Automatic
  source discovery raises an error if zero or multiple geodatabases match.
- Empty input-file arguments use the properties listed under [Files](#files).
  `station_file` has no hard-coded fallback: its listed default comes from the
  properties template. `gc_file` falls back to `input/gc.csv` only when its
  property key is absent. Both files are required. A configured link-list file
  must also be readable; passing `""` does not disable it, so clear its property
  to omit it.
- `link_id_file` reads IDs from a text file containing one `HWYCOV0_ID` per row
  and ignores nonnumeric entries. Its selection behavior is defined under
  [Which links are processed](#which-links-are-processed).
- For numeric capacity rates, zero requests the property fallback in
  [Parameters](#parameters); it cannot override a configured value with a
  literal zero. `year`, `aoc` and `traffic_count_field` are no longer accepted.
- `time_period_adjustments=None` uses the scenario property, which defaults to
  `False` when absent. Explicit `False` disables the extra multipliers even if
  an older scenario property enables them; explicit `True` enables them.
- `am_hours` and `pm_hours` supply positional hourly-column indices. Use
  nonempty tuples or lists of distinct integer hours in 0–23, avoid overlap,
  and leave hours for the off-peak period. The loader does not explicitly
  validate these constraints. They have no properties fallback or toolbox
  page controls.


#### Selection, reports, and return value

See [Which links are processed](#which-links-are-processed) for `recompute_all`
and `treat_zero_as_missing`. Use `recompute_all=True` after changing inputs or
parameters when populated outputs also need refreshing. Legitimate zero delays
can substantially expand selection with `treat_zero_as_missing=True`; that
option has no additional effect with `recompute_all=True`.

`dry_run=True` computes results without updating the geodatabase, as in the
preview example above. A report and Modeller logbook entries will be
written. The report contains computed [writeback fields](#what-gets-written-back)
and stored counterparts in interleaved rows, switching between the `computed` and
`original` values as reflected in the `status` column, keyed by `HWYCOV0_ID`. The
report is stored in the scenario root as `tchc_report_YYYY-MM-DD_HHMMSS.csv`,
with `original` before `computed` for each ID. No CSV is written when no links
are selected or for a normal write run.

All three switches default to `False` and have no properties fallback.

Every successful call currently returns `0`, including a normal write run;
the updated-feature count remains a FIXME in the implementation. Inspect the
Modeller logbook for the selected-link count and the dry-run CSV for comparisons.
See [How the network is read and written](#how-the-network-is-read-and-written)
for write behavior.

### Constructor and toolbox page behavior

The no-argument constructor initializes `path` from the open project; all other
path/file attributes to `""`; both capacity rates to `1.0`;
`time_period_adjustments` to `False`;
and the three selection/execution switches to `False`.

The toolbox's `run()` method takes no arguments and forwards the page attributes
to `__call__`. Consequently, the initial page values `1.0` override
the capacity-rate properties. A scripted call that
omits those arguments instead uses the properties. On the page, setting a
capacity rate to `0.0` requests its property fallback. The page uses the default
AM/PM hours; custom hours require a scripted call.

The extra-period-multiplier checkbox starts unchecked. The page forwards that
explicit value; scripted calls with `time_period_adjustments=None` use the
scenario property instead.

For scripted calls, omitted arguments use the signature defaults and resolution
rules above, rather than retaining previous page or call values. `path` is the
exception: omitting it retains the tool's current scenario directory.

### Which links are processed

A link must have functional class 1–10 to be selected. Within that domain:

- FC 2–9 is selected when any period output field is null: `AB` fields always,
  plus `BA` fields when `WAY` is 2 or 0.
- FC 1 uses the same test excluding `TX` (intersection delay). A nonzero
  `ABCNT` also selects a freeway even when all outputs are populated; a nonzero
  `BACNT` does the same for a two-way freeway.
- FC 10 is excluded from automatic missing-value selection.
- An ID in `link_id_file` forces selection, including FC 10.

`GC` and `PLC` are deliberately excluded from that test, because they are inputs
as well as outputs: an empty `GC` legitimately means "no signal", so testing it
would select almost every link.

`recompute_all=True` processes every link in the functional class range.
`treat_zero_as_missing=True` additionally treats a stored zero as missing in
the same fields, still excluding freeway `TX` and FC 10.

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

Relative paths are resolved against `path`, the scenario directory.

### Parameters

| Parameter | Tool argument | Properties key | Default |
|---|---|---|---|
| Managed lane capacity rate | `managed_lane_capacity_rate` | `tchc.managed.lane.capacity.rate` | 1.0 |
| Freeway capacity rate | `freeway_capacity_rate` | `tchc.freeway.capacity.rate` | 1.0 |
| Extra period capacity multipliers | `time_period_adjustments` | `tchc.time.period.adjustments` | `false` |
| AM peak hours | `am_hours` | — | 6, 7, 8 |
| PM peak hours | `pm_hours` | — | 15, 16, 17 |

The two capacity rates are plain values in `sandag_abm.properties`. To vary them
by year instead, add columns of the same name to `parametersByYears.csv` —
`set_year_specific_properties` overrides any key whose name matches a column, so
no change to the properties template is needed.

### Count-station file

Files ending in `.csv` or `.txt` are parsed as CSV; other extensions use fixed
width (26 fields of 5 characters, the legacy `sta.hrpct` layout). A header row
is expected in either format.
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
- An explicit `2-Way Stop` block uses its roadway-class-7 row by default.
  Without that block, the hard-coded percentages for crossroad classes 1–9
  are `[50, 50, 75, 100, 125, 125, 150, 150, 100]`.


---

## Inputs derived by the driver

The driver derives intersection topology from the highway links rather than
reading the original TCHC turn tables. It also reads `TNED_HwyNodes` to log
referenced node IDs absent from its `HNODE` field, and `RuralZone` to calculate
whether each link is fully covered by the union of rural polygons.

There are no external HOV/freeway adjacency, border-delay or node-sphere lookup
inputs in the current tool. HOV2/HOV3 links use count station 1; other freeways
use `COSTAT` when positive, and non-freeway links use station 1.

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

Reading uses `geopandas.read_file(source, layer=...)` for `TNED_HwyNet`,
`TNED_HwyNodes` and `RuralZone`. The highway layer must contain all required
input fields and all 50 period output fields; missing fields raise an error.
`AN`, `BN`, `FC`, `WAY` and `HWYCOV0_ID` are converted to integers, with
nonnumeric or missing values becoming 0. Length uses `SHAPE_Length` if present,
otherwise `LENGTH × 5280`; missing values within the chosen column become 0.

`compute()` copies the full GeoDataFrame and changes TCHC fields only on selected
rows, preserving other values and geometry in memory. The driver adds
`is_rural`, `length_feet` and, if absent, `ASPD` columns before computation.

A normal run with selected links currently writes the **entire** result using
`results.set_index("HWYCOV0_ID").to_file(self.source)`. It does not specify a
layer, driver, append mode or transaction, nor patch features by ID. Actual
output-layer and metadata behavior therefore depends on the GeoPandas writer
and its geodatabase driver; the implementation does not guarantee preservation
of `OBJECTID`, aliases, domains or subtypes. It has no explicit GDAL version or
random-write capability check. A dry run skips this write and creates the
comparison CSV instead.

### Current rural-input limitation

The driver computes `is_rural`, but `build_link()` does not attach it to the
`TCHCLink`, and the dataclass does not declare it. When processing reaches the
control-7 branch, it reads `link.is_rural` and raises `AttributeError`, including
in a dry run. Closed periods (`lane_count == 9`), FC10 links and FC8 `ACCESS`
links bypass that branch. A control-7 direction with all periods closed therefore
avoids this error; an eligible open period still triggers it. The engine's
missing-PLC default branch also reads that attribute, but the driver converts
missing PLC values to 0, bypassing that branch. These are implementation
limitations, not working rural defaults.

### Structural notes

- **TNED arcs carry both directions.** One `TNED_HwyNet` record holds the `AB`
  and `BA` field pairs, and `WAY` says whether the reverse direction exists.
  Direction index 0 is AB, index 1 is BA. `WAY` of 0 is treated as two-way, as
  `import_network.py` does.
- **TNED has five time periods, TCHC has three.** TCHC period 0 (AM) writes
  `A`; period 1 (midday/off-peak) writes `EA`, `MD` and `EV`; period 2 (PM)
  writes `P`.
- Periods whose lane count is 9 (closed) receive zero CH, CP and CX.
  TM remains at its engine default 999 minutes and TX at 0.
- One-way links never populate direction 1, so their `BA` fields are left as
  they were rather than being overwritten with the engine's sentinels.

### Input mapping

| `TCHCLink` field | TNED field | Notes |
|---|---|---|
| `link_identifier` | `HWYCOV0_ID` | |
| `link_name` | `NM` | Case-sensitive tests for `NB`/`WB` and `ACCESS` |
| `length_feet` | `SHAPE_Length` | Falls back to `LENGTH` × 5280 |
| `functional_class` | `FC` | Restricted to 1–10 |
| `high_occupancy_vehicle_class` | `HOV` | |
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
| `cross_street_functional_class_by_direction` | *derived from topology* | |

### What gets written back

| `TCHCLink` output | TNED field stem | Fields |
|---|---|---|
| `period_capacity_by_period_and_direction` | `CP` | `AB`/`BA` × `EA`, `A`, `MD`, `P`, `EV` |
| `intersection_capacity_by_period_and_direction` | `CX` | same |
| `hourly_capacity_by_period_and_direction` | `CH` | same |
| `link_travel_time_minutes_by_period_and_direction` | `TM` | same |
| `intersection_delay_minutes_by_period_and_direction` | `TX` | same |

Fifty fields in total, plus four direction-only fields (GC and PLC for AB/BA):

| `TCHCLink` output | TNED field | Written when |
|---|---|---|
| `resolved_green_cycle_by_direction` | `ABGC`, `BAGC` | Control type is 1, 2 or 3, where a lookup can override the coded value. Ramp meters use the coded value unchanged, and the remaining control types have no G/C at all, so nothing is written |
| `planned_lane_capacity_by_direction` | `ABPLC`, `BAPLC` | Every processed direction with a non-`None` value. Freeways clamp the direction's own PLC to [1900, 2100] in open periods; other classes pass the value through |

GC is left untouched where no value is resolved. PLC uses `ABPLC` for AB and
`BAPLC` for BA; it is not a separate resolved-output attribute. Missing input
PLC values become 0 in `build_link()`, and a freeway then clamps 0 to 1900.
There is no longer a [1600, 2400] validity check with a 2000 fallback.

The engine has a default-PLC branch for direct callers supplying `None`: 2000
for FC 1, 1800 for FC 2–5/7/8, 950 for FC 6 and 1200 for FC 9. Its rural override
uses 1150 when any period has at least two lanes, otherwise 950, for non-freeways.
This branch requires a caller-supplied `link.is_rural`; it is not reached for
missing geodatabase PLC values under the current driver.

For FC 2–7, PLC=950 with fewer than two lanes yields a flat 950 veh/hr. Otherwise
capacity is `lanes × 1800 − 300`, with a further 200 reduction when `MED < 2`.
PLC is also used for the toll/border roadway-capacity override, whose minimum
is 950 on rural links, 1200 on urban FC 8/9, and 1800 on other urban links
(subject to the rural-input limitation above).

---

## Reference: link data fields (`TCHCLink`)

### Identifiers

| Field | Type | Description |
|---|---|---|
| `link_identifier` | `int` | Unique numeric ID for this link |
| `link_name` | `str` | Street name. Case-sensitive tests for `NB`/`WB` and the special case `ACCESS` |
| `length_feet` | `float` | Link length in feet |
| `from_node_identifier` | `int` | Node ID at the A-end of the link |
| `to_node_identifier` | `int` | Node ID at the B-end of the link |

### Classification

| Field | Valid range | Description |
|---|---|---|
| `functional_class` | 1–10 | Determines which capacity formula applies |
| `high_occupancy_vehicle_class` | 1–4 | 1=general purpose, 2=HOV2+, 3=HOV3+, 4=toll facility |
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
| `project_identifier` | Project number. IDs 613 and 614 trigger the managed-lane capacity rate multiplier |

### Lane configuration

Only `lane_count_by_period_and_direction` uses **9** as the closure sentinel.
For that period and direction, CH, CP and CX are set to 0 and the remaining
calculation is skipped. Auxiliary-lane count 9 contributes capacity normally;
it does not close the period. Turn-lane fields have their own sanitization rules,
described under [Control types](#control-types).

| Field | Shape | Description |
|---|---|---|
| `lane_count_by_period_and_direction` | [3][2] | Through-lanes by [period][direction]. Values 1–8 are valid lane counts |
| `auxiliary_lane_count_by_direction` | [2] | Auxiliary lanes at 1200 veh/hr/lane in the freeway base formula and toll/border override |
| `planned_lane_capacity_by_direction` | [2] | Direction-specific PLC input and output. Freeways clamp it to [1900, 2100]. Missing geodatabase values become 0 before clamping. PLC=950 triggers the arterial override when lanes < 2; toll/border controls use PLC with a minimum |

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
| 1 | Signal | 0.17 | `through × 1800 × GC + turn_lanes × TLC`, min 1000 |
| 2 | 4-way stop | 0.20 | `through × 1800 × GC + turn_lanes × TLC`, min 500 |
| 3 | 2-way stop | 0.20 | `through × 500 × GC + right × 500 × GC + left × 500 × GC`, min 500; raw right-lane code 7 uses right-turn GC=1 |
| 4 | Ramp meter (off-peak active) | 0.50 outside AM; 0 in AM | 1000, multiplied by coded GC/100 only when GC ≥ 1; MD/PM only |
| 5 | Ramp meter (peak active) | 0.50 outside AM; 0 in AM | Same as control 4 in the implementation |
| 6 | Rail crossing | 0.02 | No capacity override; mid-block capacity preserved |
| 7 | Toll booth / border | 1.0 | Roadway CH uses PLC with a minimum; intersection hourly rate is current-period `lanes × 500` |

Turn lane counts are sanitized first: values above 7 are zeroed, values of
exactly 7 become 1, and if no through lanes remain the largest turn-lane count is
promoted to through, and the signal approach count is reset to 3.
In this table GC means a fraction (stored percentage divided by 100). In AM,
controls 4/5 retain roadway CH but still finalize CX to the same period capacity.

---

## Reference: scenario data (`TCHCContext`)

### Scalar parameters

| Field | Description |
|---|---|
| `managed_lane_capacity_rate` | Multiplier on HOV3+ lane capacity and projects 613/614 |
| `freeway_capacity_rate` | Multiplier on general-purpose freeway and FC 8 capacity |
| `time_period_adjustments` | Optional extra CP/CX scaling for the five TNED periods; defaults to `False` |

For FC 1, HOV=1 applies the freeway rate to `lanes × PLC + aux × 1200`.
HOV=2/3 instead use `lanes × 2000`, with the managed rate additionally applied
for HOV=3. Projects 613/614 apply the managed rate again, so an HOV=3 link on
one of those projects receives it twice. FC 8 applies the managed rate when
HOV > 1, otherwise the freeway rate when HOV=1; `ACCESS` bypasses both.
Intersection-control overrides replace these base hourly capacities.

### Lookups

| Field | Key → Value | Description |
|---|---|---|
| `approach_count` | node ID → count (2–4) | Approaches at each node, clamped to [2, 4]. Indexes the signal green/cycle lookup |
| `station_peak_period_factor` | [period][direction][station] | Peak-period expansion factor. Valid range [1.0, 15.0]; out-of-range values fall back to station 1. For freeways the direction comes from the link name (NB/WB → index 1) rather than the loop direction |

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
├─ Resolve speed (coded value or FC default)
├─ Resolve station ID (HOV and non-freeways use station 1)
│
└─ for each direction (AB, then BA if two-way):
   │
   ├─ Look up approach count at the downstream node
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
      │   ├─ FC 1: freeway formula with directional PLC clamping and HOV/project multipliers
      │   ├─ FC 8: connector formula with ACCESS special case
      │   ├─ FC 9: ramp formula
      │   └─ FC 2–7: arterial formula with median adjustment
      │
      ├─ Set initial hourly_capacity
      ├─ Sanitize turn-lane counts (clamp, fallback)
      │
      └─ Apply intersection control (if any):
          ├─ Signal: GC lookup → through×1800×GC + turns×TLC, min 1000
          ├─ 4-way stop: GC lookup → through×1800×GC + turns×TLC, min 500
          ├─ 2-way stop: GC lookup → all_lanes×500×GC, min 500
          ├─ Ramp meter: 1000, optionally × coded GC/100 (MD/PM only)
          ├─ Rail crossing: delay only (0.02 min)
          └─ Toll/border: roadway CH and lanes×500 intersection rate, delay 1.0 min
    
```

After these control branches, finalization derives `period_capacity` from the
final `hourly_capacity` and the station factor. Controlled CX uses that same
hourly rate except for the separate toll-booth rate. Both period outputs are
rounded to three decimals; unconstrained CX remains 999999.

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

The current context has no analysis-year, HOV/freeway adjacency, border-delay
or node-sphere fields. Year-dependent TSM and signal safety adjustments are not
applied. Control type 7 uses a flat 1.0 minute delay instead of a per-crossing,
per-period border delay, and no toll-booth operating-cost surcharge is applied
for sphere groups 3 (Coronado) and 14 (City of San Diego).

The current driver also has the rural-input and geodatabase-write limitations
listed under [How the network is read and written](#how-the-network-is-read-and-written).

`HwyETL_vs_TCHC.md` and `run_tchc_FME_TODO.md` contain review comparisons with
the FME HwyETL workbench; use the production Python file as the authority for
current behavior.

---

## This folder

`capacity_review` holds the review material for the port, not the running code.

| File | Role |
|---|---|
| `README.md` | This document |
| `tchc.py` | An older standalone engine used by the review notebook; differs from the production tool |
| `tchc_run.ipynb` | Drives the engine from the TNED geodatabase for a single link and compares the result against the stored values |
| `gc.csv`, `gc.txt` | The green/cycle lookup table, and its original fixed-width form. `gc.csv` is also shipped as `input/model/gc.csv` |
| `HwyETL_final.md` | The FME HwyETL workbench logic, transcribed |
| `HwyETL_vs_TCHC.md` | Differences between this port and the FME workbench |

`run_tchc.py` is the production implementation. The standalone engine currently
differs in PLC, TSM, safety adjustments and period conversion; notebook results
from that copy do not validate the production tool. The focused capacity tests
load the actual production functions without requiring Emme/GDAL, but their
fixtures and assertions currently reference removed fields, as noted above.

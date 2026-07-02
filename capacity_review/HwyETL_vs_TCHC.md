Here's a comparison of the two implementations with identified discrepancies:

## Structural Differences

| Aspect | FME (Markdown) | Python (TCHC) |
|--------|---------------|---------------|
| Time periods | 5 (EA, AM, MD, PM, EV) | 3 (indexed 0, 1, 2) |
| Period factor source | `1 / (StationCountMax / 100)` | Lookup table `station_peak_period_factor[period][dir][station]` |
| Output rounding | `round(..., 3)` | No explicit rounding of final outputs |

## Discrepancies in Per-Lane Capacity (PLC) Logic

**FME:**
```
IF CountStat IN (935, 980, 999) AND Year > 2015: ABPLC = 2100
ELSE IF ABPLC BETWEEN 1600 AND 1900 AND CountStat ≠ 936: ABPLC = 1900
ELSE IF ABPLC BETWEEN 2100 AND 2400: ABPLC = 2100
```

**Python:**
```python
freeway_capacity_per_lane = 2000.0
if 1600 <= link.planned_lane_capacity_by_direction[0] <= 2400:
    freeway_capacity_per_lane = float(link.planned_lane_capacity_by_direction[0])
freeway_capacity_per_lane = min(freeway_capacity_per_lane, 2100.0)
freeway_capacity_per_lane = max(freeway_capacity_per_lane, 1900.0)
```

- Python defaults to **2000** (not in FME)
- Python ignores `CountStat` entirely — no special cases for stations 935/980/999/936
- Python always reads direction `[0]` rather than direction-specific PLC
- Python clamps to [1900, 2100]; FME snaps discretely to 1900 or 2100

## Discrepancies in Capacity Multipliers (Python-only)

The Python applies several multipliers absent from the FME:

| Multiplier | Where Applied | FME Equivalent |
|---|---|---|
| `ctx.freeway_capacity_rate` | FC=1 HOV class 1, FC=8 HOV class 1 | None |
| `ctx.managed_lane_capacity_rate` | HOV class 3, FC=8 HOV>1, projects 613/614 | None |
| `1.10` TSM ramp meter bonus | FC=1 freeways with metering (year>2015) | None |
| `ctx.roadway_safety_adjustment_factor_by_jurisdiction` | Signalized intersections only | None |

## Signalized Intersection Formula (control_type=1) — Major Discrepancy

**FME (Sections 4+6):**
```
_xcap = Lanes × 1800 − 300   (divided, MedType ≥ 2)
_xcap = Lanes × 1800 − 500   (undivided, MedType < 2)
_xlfcap = (LeftLanes + RightLanes) × TLC
HCAP = _xcap + _xlfcap
```

**Python:**
```python
directional_capacity = (
    through_lane_count * 1800.0 * green_cycle_factor
    + (right_turn_lane_count + left_turn_lane_count) * turn_capacity_per_lane
)
```

Differences:
- FME uses total `Lanes` with a median subtraction (−300/−500); Python uses `through_lane_count` with no median adjustment
- FME does **not** apply a green/cycle ratio to the through capacity; Python multiplies by `green_cycle_factor`
- FME turn capacity uses `(Left + Right) × TLC`; Python uses the same form but with `turn_capacity_per_lane` from a lookup

## Two-Way Stop (control_type=3) — Different Base Rate

**FME:** Groups 2-way and 4-way stops under `_abcnt IN (2,3)` with the same formula.

**Python:** Uses **500 veh/hr/lane** base rate for 2-way stops vs **1800 × GC** for 4-way:
```python
directional_capacity = (
    through_lane_count * 500.0 * gc_through
    + right_turn_lane_count * 500.0 * gc_right
    + left_turn_lane_count * 500.0 * gc_left
)
```

## Ramp Meter (control_type 4/5)

**FME:** `_abcnt = 5: HCAP = Lanes × 1200`

**Python:** `directional_capacity = 1000.0 * GC_factor` (and only for `period_index > 0`)

Both the base rate (1200 vs 1000) and the formula structure differ.

## Rail Crossing (control_type=6) — Missing Capacity Override

**FME:** `HCAP = (ThroughLanes + RightLanes + LeftLanes) × 1300`

**Python:** Only sets `intersection_delay = 0.02`; does **not** override the capacity. The link keeps whatever base capacity was computed earlier.

## Toll Booth (control_type=7) — Different HCAP Formula

**FME:**
```
HCAP = (Lanes × ABPLC + AuxLanes × 1200) × PeriodFactor
XCAP = Lanes × 500 × PeriodFactor
```

**Python:**
```python
directional_capacity = through_lane_count * 500.0  # used for BOTH hcap and xcap
```

The Python sets hourly capacity to the same `through_lanes × 500` formula that the FME uses for XCAP only. The FME's HCAP formula (using ABPLC + aux lanes) is not implemented.

## Arterial 950 Override — Missing Rural Check

**FME:** `IF ABPLC=950 AND Lanes < 2 AND _isrural=yes: HCAP = 950`

**Python:** `if planned_lane_capacity == 950 and lane_count < 2: directional_capacity = 950.0`

Python omits the `_isrural` condition.

## Constants Comparison

| Constant | FME | Python | Match? |
|---|---|---|---|
| Freeway aux lane rate | 1200 | 1200 | Yes |
| HOV per-lane rate | 2000 | 2000 | Yes |
| No-control base rate | 1800 | 1800 | Yes |
| Divided median adj | −300 | −300 | Yes |
| Undivided median adj | −500 | −500 (−300−200) | Yes |
| Ramp rate (FC8) | 1800 | 1800 | Yes |
| Connector rate (FC9) | 1200 | 1200 | Yes |
| Signal min floor | 1000 | 1000 | Yes |
| Stop min floor | 500 | 500 | Yes |
| Intersection delay (signal) | 0.17 | 0.17 | Yes |
| Intersection delay (stop) | 0.20 | 0.20 | Yes |
| Intersection delay (ramp meter) | 0.50 | 0.50 | Yes |
| Intersection delay (rail) | 0.02 | 0.02 | Yes |
| TLC FC=2 | 250 | 250 | Yes |
| TLC FC=3 | 150 | 150 | Yes |
| TLC FC=4–9 | 100 | 100 | Yes |
| Ramp meter base | 1200/lane | 1000 flat | **No** |
| Rail crossing rate | 1300/lane | Not implemented | **No** |

## Summary

The Python implements the **FORTRAN TCHC** algorithm, while the Markdown documents the **FME HwyETL** workbench. They share the same conceptual framework but diverge in several key areas: the signalized capacity formula (GC ratio application), toll booth/ramp meter rates, capacity multipliers (safety, TSM, managed-lane), number of time periods, and PLC override logic. The intersection delay constants are consistent between both.
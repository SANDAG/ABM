Here is the complete roadway capacity calculation logic extracted from the FME workbench:

---

# Roadway Capacity Calculation Logic — HwyETL_Final.fmw

## Key Variables

| Variable | Meaning |
|----------|---------|
| `_ABCHA` / `_ABCHP` / `_ABCHMD` / `_ABCHEA` / `_ABCHEV` | Hourly Capacity (HCAP) for AM / PM / Midday / EarlyAM / Evening |
| `_ABCXA` / `_ABCXP` / `_ABCXMD` / `_ABCXEA` / `_ABCXEV` | Intersection Capacity (XCAP) by time period |
| `_ABCPA` / `_ABCPP` / `_ABCPMD` / `_ABCPEA` / `_ABCPEV` | Final Period Capacity (= HCAP × PeriodFactor) |
| `ABPLC` | Per Lane Capacity |
| `_ABTXA` | Turn Excess Adjustment factor |
| `_xcap` | Intermediate intersection capacity |
| `_xlfcap` | Left/Right turn lane capacity adjustment |
| `TLC` | Turn Lane Capacity per lane |
| `_abcnt` | Approach/intersection configuration type (1–6) |
| `AMPeriodFactor` / `TOD__OPPeriodFactor` / `TOD_PMPeriodFactor` | Period conversion factors |

---

## 1. Period Factor Calculation

```
AMPeriodFactor = 1 / (StationCountMax / 100)
```
Derived from time-of-day count station data. AM and PM have dedicated factors; EarlyAM, Midday, and Evening share the Off-Peak factor.

---

## 2. Per Lane Capacity (ABPLC) Overrides

```
IF CountStat IN (935, 980, 999) AND Year > 2015:
    ABPLC = 2100
ELSE IF ABPLC BETWEEN 1600 AND 1900 AND CountStat ≠ 936:
    ABPLC = 1900
ELSE IF ABPLC BETWEEN 2100 AND 2400:
    ABPLC = 2100
ELSE:
    ABPLC = existing value
```

For HOV facilities: if `ABPLC > 2100`, cap to `2100`.

---

## 3. Initial HCAP by Facility Type

### Freeway (FunClass=1, OpType=1)
```
HCAP = (Lanes × ABPLC) + (AuxLanes × 1200)
XCAP = 999999 (unconstrained)
_ABTXA = 0.0
```

### HOV Freeway (OpType 2, 3)
```
HCAP = Lanes × 2000
XCAP = 999999
_ABTXA = 0.0
```

### No Control, Divided (MedType ≥ 2)
```
HCAP = 1800 × Lanes − 300
IF ABPLC=950 AND Lanes < 2 AND _isrural=yes:
    HCAP = 950
XCAP = 999999
_ABTXA = 0.0
```

### No Control, Undivided (MedType < 2)
```
HCAP = 1800 × Lanes − 500
IF ABPLC=950 AND Lanes < 2 AND _isrural=yes:
    HCAP = 950
XCAP = 999999
_ABTXA = 0.0
```

### Ramps (FunClass=8)
```
HCAP = Lanes × 1800
IF StreetName CONTAINS 'ACCESS':
    HCAP = 9999
XCAP = 999999
_ABTXA = 0.0
```

### Centroid Connectors (FunClass=9)
```
HCAP = Lanes × 1200
XCAP = 999999
_ABTXA = 0.0
```

### Toll Booths
```
HCAP = ((Lanes × ABPLC) + (AuxLanes × 1200)) × PeriodFactor
XCAP = round((Lanes × 500) × PeriodFactor, 3)
_ABTXA = 1.0
```

---

## 4. Intersection Capacity (_xcap)

### Initial Intersection Capacity
```
IF MedType ≥ 2: _xcap = Lanes × 1800 − 300
IF MedType < 2:  _xcap = Lanes × 1800 − 500
```

### FC8 Intersection Capacity
```
IF StreetName = 'ACCESS': _xcap = 9999
ELSE IF _abcnt IN (4,5) AND GCRatio ≥ 1: _xcap = 1000 × GCRatio
ELSE IF _abcnt IN (4,5) AND GCRatio < 1: _xcap = 1000
```

---

## 5. Turn Lane Capacity (TLC)

```
IF FunClass = 2: TLC = 250
IF FunClass = 3: TLC = 150
IF FunClass IN (4–9): TLC = 100
```

### Left/Right Lane Adjustment
```
_xlfcap = (LeftLanes + RightLanes) × TLC
IF _abcnt = 3 (all-way stop): _xlfcap = 0
```

---

## 6. Final HCAP Assembly (Signalized / Stop-Controlled)

```
DEFAULT: HCAP = _xcap + _xlfcap

OVERRIDES:
    IF Lanes = 9: HCAP = 0 (closed)
    IF _abcnt = 1 AND (_xcap + _xlfcap) < 1000: HCAP = 1000 (min floor)
    IF _abcnt IN (2,3) AND (_xcap + _xlfcap) < 500: HCAP = 500 (min floor)
    IF _abcnt = 6: HCAP = (ThroughLanes + RightLanes + LeftLanes) × 1300
    IF _abcnt = 5: HCAP = Lanes × 1200
```

---

## 7. Turn Excess Adjustment (_ABTXA)

```
IF _abcnt = 1 (signalized):         0.17
IF _abcnt IN (2, 3) (stop-ctrl):    0.20
IF _abcnt IN (4, 5) (yield/rndbt):  0.50
IF _abcnt = 6:                       0.02
DEFAULT:                             0.0
```

---

## 8. Min/Max Cap Enforcement

```
IF HCAP < 1000 AND _abcnt = 1:
    HCAP = 1000 (minimum for signalized intersections)
```
Applied to all time periods.

---

## 9. Final Period Capacity (Output)

```
ABCPEA = round(HCAP_EA × TOD__OPPeriodFactor, 3)
ABCPA  = round(HCAP_AM × AMPeriodFactor, 3)
ABCPMD = round(HCAP_MD × TOD__OPPeriodFactor, 3)
ABCPP  = round(HCAP_PM × TOD_PMPeriodFactor, 3)
ABCPEV = round(HCAP_EV × TOD__OPPeriodFactor, 3)
```

XCAP period values follow the same pattern:
```
ABCXA  = round(HCAP_AM × AMPeriodFactor, 3)
ABCXEA = round(HCAP_EA × TOD__OPPeriodFactor, 3)
ABCXMD = round(HCAP_MD × TOD__OPPeriodFactor, 3)
ABCXP  = round(HCAP_PM × TOD_PMPeriodFactor, 3)
ABCXEV = round(HCAP_EV × TOD__OPPeriodFactor, 3)
```

---

## 10. Routing Logic (Which Path Applies)

| Condition | Transformer |
|-----------|-------------|
| FunClass=1, OpType=1 | InitialFwyCap / FwyCap |
| FunClass=1, OpType=2,3 | InitailFwyHOVCap / FwyCap |
| No Control + MedType ≥ 2 | InitialNoControlCap_Med2 |
| No Control + MedType < 2 | InitialNoControlCap_Med1 |
| FunClass=8 | InitialFC8Cap → FC8Xcap |
| FunClass=9 | InitialFC9Cap |
| Toll Booth | InitialTollBoothCap |
| All other signalized/stop | InitialOtherControlCap → SetHCap |

Post-processing pipeline: `SetXCap` → `MinMaxHCap` → Final period output.

---

## 11. HCAP Rate Summary

| Facility Type | Per-Lane Rate (veh/hr/ln) | Aux Lane Rate | Median Adj |
|---|---|---|---|
| Freeway (OpType=1) | ABPLC (typ. 2100) | 1200 | — |
| HOV (OpType 2,3) | 2000 | — | — |
| No Control, Divided | 1800 | — | −300 |
| No Control, Undivided | 1800 | — | −500 |
| Ramps (FC8) | 1800 | — | — |
| Connectors (FC9) | 1200 | — | — |
| Toll Booths | 500 (XCAP) / ABPLC (HCAP) | 1200 | — |
| Signalized (_abcnt=6) | 1300 per all lanes | — | — |
| Yield/Roundabout (_abcnt=5) | 1200 | — | — |
| Rural 1-lane no-control | 950 (fixed) | — | — |

---

## 12. BA Direction

The BA (B→A) direction uses identical logic with `BALanes`, `BAControlType`, `BAPLC`, etc. Formulas are symmetric:
```
IF MedType ≥ 2: _xcap = BALanes × 1800 − 300
IF MedType < 2:  _xcap = BALanes × 1800 − 500
```

All output attributes mirror the AB direction: `BACPEA`, `BACPA`, `BACPMD`, `BACPP`, `BACPEV`.
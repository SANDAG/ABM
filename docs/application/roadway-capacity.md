# Recalculating Roadway Capacity

The **Import -> Run TCHC** tool in Emme Modeller recalculates roadway capacity,
free-flow travel time and intersection delay from the scenario's network coding.
TCHC stands for Transportation Coverage Highway Capacity. Use the tool after
changing lanes, speeds, or intersection controls so that the network import can
use refreshed values for traffic assignment.

For an overview of editing a supplied network, see [Network Coding](network-coding.md).
The [Networks](../networks.md) page describes the input geodatabase and its attributes.

## Prepare the inputs

Work with a copy of the scenario's input geodatabase. The tool's normal run
writes results to that geodatabase; preview mode lets you review calculations first.

You will need:

- An Emme project with the SANDAG toolbox installed, and the scenario directory
  containing `conf/sandag_abm.properties`.
- The scenario's network geodatabase, normally `input/EMMEOutputs.gdb`. It must
  contain `TNED_HwyNet`, `TNED_HwyNodes` and `RuralZone`, including the existing
  roadway input and output fields. See the
  [geodatabase overview](../networks.md#emme-outputs-file-geodatabase) and the
  [roadway link attributes](../networks.md#tned-roadway-network-links-feature-class).
- A count-station hourly percentage file, normally `input/sta.hrpct`, and a
  green-to-cycle ratio lookup file, normally `input/gc.csv`. Blank file selectors
  use the paths configured in the scenario properties.

Roadway links need unique `HWYCOV0_ID` values and complete required input coding.
Check functional class, speed, HOV and median coding, period lane counts, and
intersection controls and turn lanes. Reverse-direction inputs are required for
two-way links (`WAY` of 0 or 2). The tool reports missing required inputs in the
Modeller logbook.

## Choose which links to refresh

By default, the tool selects roadway links with missing calculated values.
Freeways ignore missing intersection-delay fields, but are also selected when
an applicable intersection control is nonzero. Zone connectors are excluded
from automatic missing-value selection.

An edit to a link's inputs does not necessarily select it if its calculated
values are already populated. Choose one of these options when refreshing edits:

- **Link ID list (optional):** supply a text or CSV file containing one
  `HWYCOV0_ID` per line. These links are selected in addition to the default
  selection. The link ID is described in the
  [roadway link attribute reference](../networks.md#tned-roadway-network-links-feature-class).
- **Recompute every link:** refresh all links in functional classes 1–10,
  including zone connectors. Use this after changing shared lookup files or
  capacity settings.

Leave **Treat stored zeroes as missing values** unchecked for routine use.
Zero is a legitimate result for some outputs, such as intersection delay;
enabling this option can select many more links.

## Preview and apply the results

1. Open the tool from the Emme Modeller toolbox.
2. Set **Scenario directory** to the scenario root. Select **Source gdb** or
   leave it blank to use the single `*.gdb` in the scenario's `input` directory.
   If that directory contains multiple geodatabases, select the source explicitly.
3. Confirm the count-station and green-to-cycle lookup files, then choose the
   link selection described above.
4. Leave both capacity rates at **1.0** and **Apply extra time period capacity
   adjustments** unchecked unless your scenario requires different settings.
   The rates change freeway and managed-lane capacity calculations. The extra
   adjustments scale period and intersection capacity, and are disabled by default.
5. Check **Do not write results to the geodatabase** and run the tool.
6. Review the selected-link count and messages in the **Run TCHC report** in
   the Modeller logbook. The preview CSV, `tchc_report_YYYY-MM-DD_HHMMSS.csv`,
   is saved in the scenario root. Compare the `original` and `computed` rows for
   each link ID. No CSV is produced when no links are selected.
7. Once the results are satisfactory, clear **Do not write results to the
   geodatabase** and rerun with the same inputs and selection options.
8. Check the refreshed fields in the source geodatabase, then run **Import
   network** to bring those values into Emme before traffic assignment.

## Understand the outputs

The calculation uses roadway class, lanes, speed and intersection coding to
update the following fields in `TNED_HwyNet`:

| Field stem | Result |
|---|---|
| `CH` | Hourly roadway capacity in vehicles per hour |
| `CP` | Capacity expanded using the count-station period factor |
| `CX` | Intersection capacity, or an unconstrained value where no control applies |
| `TM` | Free-flow link travel time in minutes |
| `TX` | Intersection delay in minutes |

Fields use `AB` and `BA` direction prefixes and `EA`, `A`, `MD`, `P` and `EV`
period suffixes. One-way links retain their existing reverse-direction values.
Green-to-cycle (`GC`) and per-lane capacity (`PLC`) fields are also updated where
applicable. See the [roadway attribute definitions](../networks.md#tned-roadway-network-links-feature-class)
for the field naming and network coding conventions.

AM factors use hourly columns 6, 7 and 8; MD factors use 10, 11 and 12; PM
factors use 15, 16 and 17. Each factor uses the largest hourly traffic share
among its selected columns. The MD calculation supplies EA, MD and EV outputs;
optional extra period adjustments can then give their CP/CX values different
scales. Custom hour selections require a scripted call rather than the toolbox page.

## Use with a model run

During model initialization from iteration 1, the master run invokes the
capacity calculation after the land-use measures step and before network import
unless `RunModel.skipTCHC` is enabled. The default selection rules still apply:
use the toolbox's link list or full-recalculation option to refresh edited links
whose outputs are already populated.

If a run stops, inspect the logbook for missing input fields or values and
correct the source network using the [Networks](../networks.md) reference.

# Storage Usage

## Overview

A complete ABM model run requires **450 GB of available disk space** as a minimum. A typical scenario run will generate approximately **280 GB** of data across various output directories, with additional space needed for temporary files during processing.

All model outputs are written to subdirectories within `output` folder of the scenario directory. For detailed information about specific output files and their contents, see the [Outputs documentation](outputs.md).

## Folder Size Breakdown

The following table shows typical storage usage from a recent 2035 scenario run (2035_v15_4_1):

| Folder | Size | Purpose |
|--------|------|---------|
| **emme_project** | 199.40 GB | Emme network databases, matrices, and transit assignments - largest component |
| **output** | 69.02 GB | Primary model outputs including trip tables, person/household files, and results |
| **report** | 9.61 GB | Generated reports, summaries, and visualization outputs |
| **input** | 1.86 GB | Scenario-specific input data |
| **src** | 101.21 MB | Model source code |
| **application** | 97.12 MB | Application binaries and dependencies |
| **logFiles** | 10.89 MB | Run logs and debugging information |
| **python** | 954.88 KB | Python scripts and utilities |
| **uec** | 216.36 KB | Utility expression calculator files |
| **conf** | 238.06 KB | Configuration files |
| **analysis** | 95.20 KB | Analysis scripts and results |
| **bin** | 99.86 KB | Executable binaries |
| **sql** | 0.00 B | SQL scripts (if used) |
| **TOTAL** | **280.10 GB** | |

## Skim File Sizes

Skim files are stored in OMX (Open Matrix) format within the `output\skims` folder and represent a significant portion of storage usage. The model produces skim files for **5 time periods**: Early AM (EA), AM Peak (AM), Midday (MD), PM Peak (PM), and Evening (EV).

### Size by Time Period

Each time period generates approximately **9.2 GB** of skim files:

| File Type | Size per Time Period | Description |
|-----------|---------------------|-------------|
| `traffic_skims_[period].omx` | 3.7 GB | Highway skims for SOV, HOV2, HOV3, and truck travel |
| `transit_skims_[period].omx` | 5.5 GB | Transit skims for walk, PNR, and KNR access modes |
| **Total per period** | **9.2 GB** | |

> **Note:** On-disk sizes reflect HDF5 compression. Uncompressed, each individual matrix occupies ~187 MB (4,947 × 4,947 zones × 8 bytes for float64).

**Total skim storage (all 5 periods): ~46 GB**

---

### Traffic Skim Contents

Traffic skim files (3.7 GB each) contain highway impedance matrices for a **4,947 × 4,947 zone system**. Each file includes:

- **Single Occupancy Vehicle (SOV)**: Transponder (TR) and non-transponder (NT) routes × Low (L), Medium (M), High (H) income groups
- **High Occupancy Vehicle 2+ (HOV2)**: Low (L), Medium (M), and High (H) income groups
- **High Occupancy Vehicle 3+ (HOV3)**: Low (L), Medium (M), and High (H) income groups
- **Trucks (TRK)**: Low (L), Medium (M), and High (H) value-of-time classes

Matrices available vary by mode:

| Attribute | SOV | HOV2/HOV3 | TRK |
|-----------|:---:|:---------:|:---:|
| Distance (DIST) | ✓ | ✓ | ✓ |
| Travel time (TIME) | ✓ | ✓ | ✓ |
| Toll cost (TOLLCOST) | ✓ | ✓ | ✓ |
| Toll distance (TOLLDIST) | ✓ | ✓ | — |
| HOV distance (HOVDIST) | — | ✓ | — |
| Reliability (REL) | ✓ | ✓ | — |

**`traffic_skims_PM.omx` contains 75 matrices** (SOV: 30, HOV2/3: 36, TRK: 9)

---

### Transit Skim Contents

Transit skim files (5.5 GB each) contain transit level-of-service matrices for the same **4,947 × 4,947 zone system**. Each file includes skims for:

- **Walk access (WALK)**: Local (LOC), Premium (PRM), and Mixed (MIX) transit sub-modes
- **Park & Ride (PNR)**: Inbound (PNRIN) and Outbound (PNROUT) × LOC / PRM / MIX
- **Kiss & Ride (KNR)**: Inbound (KNRIN) and Outbound (KNROUT) × LOC / PRM / MIX

Matrices available vary by transit sub-mode:

| Attribute | LOC | PRM | MIX |
|-----------|:---:|:---:|:---:|
| Bus IVTT (BUSIVTT) | ✓ | — | ✓ |
| LRT IVTT (LRTIVTT) | — | ✓ | ✓ |
| Commuter rail IVTT (CMRIVTT) | — | ✓ | ✓ |
| BRT IVTT (BRTIVTT) | — | ✓ | ✓ |
| Express bus IVTT (EXPIVTT) | — | ✓ | ✓ |
| Limited express IVTT (LTDEXPIVTT) | — | ✓ | ✓ |
| Total IVTT (TOTALIVTT) | ✓ | ✓ | ✓ |
| Access time (ACC) | ✓ | ✓ | ✓ |
| Egress time (EGR) | ✓ | ✓ | ✓ |
| First wait (FIRSTWAIT) | ✓ | ✓ | ✓ |
| Transfer wait (XFERWAIT) | ✓ | ✓ | ✓ |
| Transfer walk (XFERWALK) | ✓ | ✓ | ✓ |
| Transfers (XFERS) | ✓ | ✓ | ✓ |
| Fare (FARE) | ✓ | ✓ | ✓ |

**`transit_skims_PM.omx` contains 180 matrices** (5 access groups × (9 LOC + 13 PRM + 14 MIX) = 180)

---

## Disk vs. Memory Footprint

### Why they differ

Skim files on disk are stored with **HDF5 compression**, so the file sizes reported in the folder are significantly smaller than what the data actually occupies when loaded. When a Python program (e.g. ActivitySim) reads a matrix, HDF5 decompresses it fully into a **numpy array in RAM** — the uncompressed size, not the on-disk size, is what consumes memory.

> **Rule of thumb:** disk size = compressed; memory size = always uncompressed (4,947 × 4,947 × bytes per dtype).

### PM period: disk vs. memory

| File | Matrices | On-disk (compressed) | In memory (uncompressed) | Compression ratio |
|------|:--------:|---------------------:|-------------------------:|:-----------------:|
| `traffic_skims_PM.omx` | 75 | 3.7 GB | 13.7 GB | ~3.7× |
| `transit_skims_PM.omx` | 180 | 5.5 GB | 32.9 GB | ~6.0× |
| **Total (PM only)** | **255** | **9.2 GB** | **~46.7 GB** | |

Transit compresses much better than traffic because many transit OD pairs are zero (sparse matrices), which HDF5 handles very efficiently.

### All 5 time periods loaded simultaneously

| | Matrices | Disk (compressed) | Memory (uncompressed) |
|---|:--------:|------------------:|----------------------:|
| Traffic (5 periods) | 375 | ~18.5 GB | ~68.5 GB |
| Transit (5 periods) | 900 | ~27.5 GB | ~164.5 GB |
| **Grand total** | **1,275** | **~46 GB** | **~233 GB** |

### Practical implications

- **dtype matters.** All matrices are currently `float64` (8 bytes/cell). Casting to `float32` (4 bytes/cell) would halve memory usage to ~116 GB across all periods. Many attributes such as XFERS, TOLLCOST, and FARE do not require float64 precision.
- **Removing matrices saves more memory than disk space.** For example, the 15 SOV_NT matrices occupy only ~740 MB on disk but consume ~2.7 GB in RAM when loaded.
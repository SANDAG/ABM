# Storage Usage

## Overview

The measurements on this page are from the **2027 Regional Transportation Improvement Program (RTIP), forecast year 2050, scenario ID 585**. This is the latest scenario analyzed for this documentation update (October 2026). Actual disk usage will vary by scenario, model version, and run configuration.

The completed scenario occupies **286.52 GiB** of storage. For planning purposes, existing guidance recommends allowing at least **450 GB of available disk space** for a full ABM run, including space for temporary files created during processing. This 450 GB figure is a planning guideline, **not a measured peak disk requirement** from scenario 585.

Model files are stored across several directories in the scenario folder, including `emme_project`, `output`, and `report`. For detailed information about specific output files and their contents, see the [Outputs documentation](outputs.md).

> **Units:** Sizes in the tables below use binary units: 1 KiB = 1,024 bytes; 1 MiB = 1,024 KiB; 1 GiB = 1,024 MiB. Thus, 286.52 GiB is approximately 307.65 GB in decimal units. Figures are rounded and may not sum exactly.

## Folder Size Breakdown

The following table summarizes the completed **2050 scenario (ID 585)** used for the 2027 RTIP, measured at the scenario directory root:

| Folder / File | Type | Size | Purpose |
|---------------|------|-----:|---------|
| **emme_project** | Directory | 199.39 GiB | Emme network databases, matrices, and assignment results; largest component |
| **output** | Directory | 75.49 GiB | Model outputs, including skim files, trip tables, and other results |
| **report** | Directory | 9.92 GiB | Generated reports and summaries |
| **input** | Directory | 1.51 GiB | Scenario-specific input data |
| **logFiles** | Directory | 111.77 MiB | Run logs and diagnostic information |
| **src** | Directory | 105.17 MiB | Model source code |
| **python** | Directory | 959.88 KiB | Python scripts included with the scenario |
| **analysis** | Directory | 95.20 KiB | Analysis-related files |
| **bin** | Directory | 68.60 KiB | Executable or supporting files |
| **conf** | Directory | 34.66 KiB | Configuration files |
| **git_info.yaml** | File | 62.00 B | Source/version metadata |
| **TOTAL (11 entries)** | | **286.52 GiB** | |

The report measures **logical file sizes** in the completed scenario. It does not measure peak temporary storage during model execution or physical disk allocation.

Because OMX skim files are compressed, this page also compares their on-disk sizes with their uncompressed matrix sizes. This helps explain the difference between storage requirements and the amount of data represented by these files.

## Skim File Sizes

Skim files are stored in OMX (Open Matrix/HDF5) format within the `output\skims` folder. The model produces highway and transit skim files for **five time periods**: Early AM (EA), AM Peak (AM), Midday (MD), PM Peak (PM), and Evening (EV). The scenario also includes two smaller destination-related OMX files.

### OMX File Summary

The table summarizes highway and transit skim sizes across the five time periods for **2050 scenario 585**. The on-disk sizes reflect HDF5 compression; *raw matrix data* estimates the size of the dense, uncompressed values.

| File type | On disk per file | Matrices per file | Raw matrix data per file |
|-----------|-----------------:|------------------:|-------------------------:|
| `traffic_skims_[period].omx` | 3.57–3.76 GiB | 75 (76 for MD) | 13.68–13.86 GiB |
| `transit_skims_[period].omx` | 5.30–5.62 GiB | 180 | 32.82 GiB |
| **Total per time period** | **9.04–9.19 GiB** | **255 (256 for MD)** | **46.50–46.68 GiB** |

The scenario also includes `dest_pmsa.omx` (4.03 MiB on disk) and `dest_poi.omx` (1.40 MiB on disk), each containing one matrix with an estimated raw size of 93.36 MiB.

**Across all 12 OMX files**, the total is **45.72 GiB on disk**, **1,278 matrices**, and **232.84 GiB of estimated raw matrix data**—approximately **5.09×** the on-disk size. Of these, 1,276 matrices are highway and transit skims; two are destination-related.

### Traffic Skim Contents

Traffic skim files contain highway impedance matrices for a **4,947 × 4,947 zone system**. They include:

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

**`traffic_skims_PM.omx` contains 75 matrices** (SOV: 30, HOV2/3: 36, TRK: 9). The MD file contains one additional matrix, **`TRK_TIME__MD`**, bringing its total to 76. The purpose of this MD-only matrix has not yet been confirmed; it can be inspected using the OMX utility (see [Reproducing the Results](#reproducing-the-results)).

---

### Transit Skim Contents

Transit skim files contain transit level-of-service matrices for the same **4,947 × 4,947 zone system**. They include skims for:

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

**`transit_skims_PM.omx` contains 180 matrices** (5 access groups × (9 LOC + 13 PRM + 14 MIX) = 180).

---

## Understanding OMX Compression and Uncompressed Size

OMX files use HDF5 compression to reduce disk storage. The **on-disk size** includes compressed matrix datasets and other file contents; the **raw matrix data size** is calculated from each matrix's dimensions and data type (`number of elements × bytes per value`), without loading the values.

For scenario 585, the 12 OMX files occupy **45.72 GiB on disk**, while their matrices represent approximately **232.84 GiB of uncompressed data** (about **5.09×** the on-disk size). For scale, a 4,947 × 4,947 `float64` matrix represents approximately 186.7 MiB of raw data, compared with 93.4 MiB for `float32`.

> **Note:** The raw matrix total is an estimate of dense, uncompressed data—not measured peak RAM usage during model execution. Actual memory use depends on which matrices are loaded, as well as caching, temporary arrays, and multiprocessing behavior.

### PM Period Example

| File | Matrices | On disk | Raw matrix data | Raw/disk |
|------|---------:|--------:|----------------:|---------:|
| `traffic_skims_PM.omx` | 75 | 3.72 GiB | 13.68 GiB | 3.68× |
| `transit_skims_PM.omx` | 180 | 5.42 GiB | 32.82 GiB | 6.05× |
| **PM total** | **255** | **9.14 GiB** | **46.50 GiB** | |

Transit skims tend to compress well because many origin–destination combinations contain zero or repeated values.

### Across All Five Time Periods

| Type | Matrices | On disk (approx.) | Raw matrix data (approx.) |
|------|---------:|------------------:|--------------------------:|
| Traffic skims | 376 | 18.4 GiB | 68.6 GiB |
| Transit skims | 900 | 27.4 GiB | 164.1 GiB |
| **Traffic + transit** | **1,276** | **45.7 GiB** | **232.7 GiB** |

These are **hypothetical dense-array totals**, not evidence that all 1,276 traffic and transit matrices are simultaneously resident in memory. Including the two destination-related OMX files yields the **1,278 matrices and 232.84 GiB** reported in the complete OMX summary.

### Practical Implications

- **Data type matters.** For the same matrix dimensions, a `float32` array uses half the raw space of a `float64` array. Any conversion would require accuracy and compatibility testing; a smaller payload does not guarantee a proportional reduction in peak model memory.
- **Reducing loaded or duplicated matrices may reduce memory demands.** The benefit depends on which matrices are used concurrently and how the model manages its arrays, particularly during multiprocessing.
- **Disk size alone is not a reliable RAM predictor.** Compressed OMX files can expand several-fold when read into dense arrays, but observed peak RAM must be measured separately.

## Reproducing the Results

The measurements above were generated with standalone tools in the ABM repository's [`scripts/utilities/`](https://github.com/SANDAG/ABM/tree/main/scripts/utilities) directory:

- [`report_folder_sizes.py`](https://github.com/SANDAG/ABM/blob/main/scripts/utilities/report_folder_sizes.py) — scans a scenario root and summarizes immediate files and subdirectories, including the nested contents of each directory.
- [`report_omx_sizes.py`](https://github.com/SANDAG/ABM/blob/main/scripts/utilities/report_omx_sizes.py) — scans OMX files within a directory and reports disk size, matrix counts, and theoretical dense-array sizes.

From the ABM repository root, run the following commands, replacing the example path with the local location of scenario 585:

```powershell
python scripts/utilities/report_folder_sizes.py "C:\path\to\scenario_585"
python scripts/utilities/report_omx_sizes.py "C:\path\to\scenario_585\output\skims"
```

To inspect individual PM-period matrices:

```powershell
python scripts/utilities/report_omx_sizes.py "C:\path\to\scenario_585\output\skims" --pattern "*_PM.omx" --details
```

To list the matrices in the midday highway skim—including the additional `TRK_TIME__MD`—use the following command if `uv` is installed:

```powershell
uv run --with openmatrix python scripts/utilities/report_omx_sizes.py "T:\path\to\scenario_585\output\skims" --pattern "traffic_skims_MD.omx" --details
```

This lists all matrices in `traffic_skims_MD.omx`; it does not determine whether the extra matrix is needed by the model.

The folder-size utility uses Python's standard library. Matrix inspection in the OMX utility requires `openmatrix` (`python -m pip install openmatrix`). Consult the [utilities README](https://github.com/SANDAG/ABM/blob/main/scripts/utilities/README.md) for further options, including CSV export and running OMX file-size analysis without matrix inspection.

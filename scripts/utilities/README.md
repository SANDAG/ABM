# ABM Utilities

This directory contains standalone Python utilities for inspecting,
analyzing, and troubleshooting SANDAG ABM inputs, outputs, and
scenario files.

These utilities support model development, validation, performance
analysis, and maintenance. They are not part of the standard ABM
execution workflow and can generally be run independently.

Each utility includes command-line usage instructions and any
required dependencies below.

## Available Utilities

| Script | Purpose |
|--------|---------|
| `report_folder_sizes.py` | Analyze disk storage usage across ABM scenario folders. |
| `report_omx_sizes.py` | Inspect OMX skim files and estimate uncompressed matrix sizes. |

## 1. Scenario folder sizes

`report_folder_sizes.py` reports the size of each file and immediate subfolder
under a scenario directory. It includes nested files in each top-level folder's
size and sorts the report from largest to smallest, matching the presentation in
the [ABM storage-usage documentation](https://sandag.github.io/ABM/storage-usage.html).

```powershell
python scripts/utilities/report_folder_sizes.py "C:\ABM\scenarios\my_scenario"
python scripts/utilities/report_folder_sizes.py "C:\ABM\scenarios\my_scenario" --workers 4 --csv folder_sizes.csv
```

Only Python's standard library is required. The default is 8 threads; for
network drives, adjust `--workers` if needed. Symbolic links are not followed.
An explicit warning and nonzero exit code (2) indicate inaccessible entries
and possibly incomplete totals. The scan reports **logical file lengths**,
not disk blocks allocated on a filesystem.

## 2. OMX skim file sizes and uncompressed matrix payloads

`report_omx_sizes.py` scans all `.omx` files directly within a skim directory.
Its summary includes on-disk file sizes, matrix counts, and the **theoretical
size of dense, uncompressed matrix values** (computed from the per-matrix shape
and datatype). To see each matrix individually, add `--details`.

```powershell
python -m pip install openmatrix
python scripts/utilities/report_omx_sizes.py "C:\ABM\scenarios\my_scenario\output\skims"
python scripts/utilities/report_omx_sizes.py "C:\ABM\scenarios\my_scenario\output\skims" --pattern "*_PM.omx" --details
python scripts/utilities/report_omx_sizes.py "C:\ABM\scenarios\my_scenario\output\skims" --csv skim_sizes.csv
```

For *disk sizes only*, `openmatrix` is not needed:

```powershell
python scripts/utilities/report_omx_sizes.py "C:\ABM\scenarios\my_scenario\output\skims" --no-matrices
```

Matrix inspection reads dataset **metadata**, not the full matrix values.
The reported uncompressed bytes are `product(shape) * dtype.itemsize`, summed
across datasets. They do **not** measure peak RAM in ActivitySim, including
copies, temporary arrays, memory mappings, shared memory, or multiple workers.
The Raw/disk column compares this matrix total with the whole file's disk size,
including HDF5 metadata and mappings; it is an approximate comparison rather
than a pure compression benchmark.

## Units and reproducing published tables

Both utilities use binary units: **1 GiB = 1,024 MiB = 1,073,741,824 bytes**.
The previously published [storage-usage table](https://sandag.github.io/ABM/storage-usage.html)
labels its figures "GB" but uses base-1,024 conversion. Numeric results will
therefore be comparable, with more precise labels here.

To reproduce the folder table, scan the scenario root. To reproduce the PM
traffic and transit skim figures, scan the `output\skims` folder using
`--pattern "*_PM.omx"`. To summarize all periods, omit `--pattern`.
These commands report the data **in the scenario supplied**; outputs can differ
by ABM version, scenario, skim contents, and file compression.

To see all options, run either utility with `--help`.

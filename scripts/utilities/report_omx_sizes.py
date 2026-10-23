"""Report OMX skim file sizes and estimated uncompressed matrix payloads.

This reads matrix metadata only; it does not load matrix values into memory.

Examples:
    python scripts/utilities/report_omx_sizes.py "C:\\ABM\\scenario\\output\\skims"
    python scripts/utilities/report_omx_sizes.py "C:\\ABM\\scenario\\output\\skims" --pattern "*_PM.omx" --details
    python scripts/utilities/report_omx_sizes.py "C:\\ABM\\scenario\\output\\skims" --csv skim_sizes.csv

Requires ``openmatrix`` for matrix metadata (``pip install openmatrix``).
Use ``--no-matrices`` for disk-only reporting without that dependency.
"""

import argparse
import csv
from pathlib import Path


def format_size(num_bytes: int) -> str:
    """Format a byte count in binary units (GiB, MiB, etc.)."""
    value = float(num_bytes)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if value < 1024 or unit == "TiB":
            return f"{value:,.2f} {unit}"
        value /= 1024
    raise AssertionError("unreachable")


def inspect_file(path: Path, include_matrices: bool = True) -> dict:
    """Return on-disk size and (optionally) matrix metadata for one OMX file.

    The uncompressed estimate is sum(shape dimensions * dtype.itemsize) over
    all matrices. It is NOT a measurement of a model process's peak RAM.
    """
    result = {
        "name": path.name,
        "disk_bytes": path.stat().st_size,
        "matrix_count": None,
        "raw_bytes": None,
        "matrices": [],
        "error": None,
    }
    if not include_matrices:
        return result

    # Import only when needed, so disk-only reporting requires no extra packages.
    import openmatrix as omx

    try:
        with omx.open_file(str(path), "r") as f:
            for name in sorted(f.list_matrices()):
                matrix = f[name]  # h5py Dataset metadata; does not read its values
                shape = tuple(int(dim) for dim in matrix.shape)
                cell_count = 1
                for dim in shape:
                    cell_count *= dim
                raw_bytes = cell_count * matrix.dtype.itemsize
                result["matrices"].append({
                    "name": name,
                    "shape": shape,
                    "dtype": str(matrix.dtype),
                    "raw_bytes": raw_bytes,
                })
        result["matrix_count"] = len(result["matrices"])
        result["raw_bytes"] = sum(m["raw_bytes"] for m in result["matrices"])
    except (OSError, ValueError, KeyError) as exc:
        # Report partial results as errors rather than silently implying zero size.
        result["error"] = f"{type(exc).__name__}: {exc}"
        result["matrices"] = []
    return result


def scan_folder(folder: str | Path, pattern: str = "*.omx", include_matrices: bool = True) -> list[dict]:
    """Inspect matching OMX files directly within a folder (not recursive)."""
    folder = Path(folder)
    if not folder.is_dir():
        raise NotADirectoryError(f"Not a directory: {folder}")
    files = sorted(p for p in folder.glob(pattern) if p.is_file())
    if include_matrices:
        try:
            import openmatrix  # noqa: F401
        except ImportError as exc:
            raise RuntimeError(
                "Matrix inspection requires openmatrix: python -m pip install openmatrix\n"
                "Alternatively, use --no-matrices to report file sizes only."
            ) from exc
    return [inspect_file(p, include_matrices) for p in files]


def report(results: list[dict], show_details: bool = False) -> None:
    """Print a file-level summary and optional per-matrix details."""
    if not results:
        print("No matching OMX files found.")
        return

    print("\nOMX SKIM SIZE SUMMARY")
    print(f"{'File':<34} {'Disk size':>14} {'Matrices':>9} {'Raw matrices':>15} {'Raw/disk':>9}")
    print("-" * 86)
    for item in results:
        count = str(item["matrix_count"]) if item["matrix_count"] is not None else "-"
        raw = format_size(item["raw_bytes"]) if item["raw_bytes"] is not None else "-"
        ratio = (f'{item["raw_bytes"] / item["disk_bytes"]:.2f}x'
                 if item["raw_bytes"] is not None and item["disk_bytes"] else "-")
        print(f'{item["name"]:<34} {format_size(item["disk_bytes"]):>14} '
              f'{count:>9} {raw:>15} {ratio:>9}')
        if item["error"]:
            print(f'  ERROR reading matrix metadata: {item["error"]}')

    total_disk = sum(item["disk_bytes"] for item in results)
    valid = [item for item in results if item["raw_bytes"] is not None]
    complete = len(valid) == len(results)
    total_raw = sum(item["raw_bytes"] for item in valid)
    total_matrices = sum(item["matrix_count"] for item in valid)
    matrix_count_display = str(total_matrices) if complete else "partial"
    total_raw_display = format_size(total_raw) if complete else "partial"
    ratio_display = f"{total_raw / total_disk:.2f}x" if complete and total_disk else "-"
    print("-" * 86)
    print(f'{"TOTAL":<34} {format_size(total_disk):>14} '
          f'{matrix_count_display:>9} {total_raw_display:>15} {ratio_display:>9}')

    if show_details:
        for item in results:
            if not item["matrices"]:
                continue
            print(f'\n{item["name"]}:')
            print(f'  {"Matrix":<44} {"Shape":<16} {"dtype":<10} {"Raw size":>14}')
            for matrix in item["matrices"]:
                shape_str = "x".join(str(dim) for dim in matrix["shape"])
                print(f'  {matrix["name"]:<44} {shape_str:<16} '
                      f'{matrix["dtype"]:<10} {format_size(matrix["raw_bytes"]):>14}')

    print("\nNote: Raw matrices = sum of dense, uncompressed matrix payload sizes.")
    print("This is not measured RAM use or peak ActivitySim memory consumption.")
    print("Raw/disk compares matrix payload bytes with the whole OMX file size.\n")


def write_csv(results: list[dict], destination: str | Path) -> None:
    """Write a file-level summary with exact byte counts for reproducibility."""
    with Path(destination).open("w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["file", "disk_bytes", "matrix_count", "raw_matrix_bytes", "error"])
        for item in results:
            writer.writerow([item["name"], item["disk_bytes"], item["matrix_count"],
                             item["raw_bytes"], item["error"] or ""])


def main() -> int:
    parser = argparse.ArgumentParser(description="Summarize OMX disk sizes and uncompressed matrix payloads.")
    parser.add_argument("folder", type=Path, help="Directory containing OMX files, e.g. scenario/output/skims")
    parser.add_argument("--pattern", default="*.omx", help='Filename glob (default: "*.omx")')
    parser.add_argument("--details", action="store_true", help="Show every matrix name, shape, dtype, and size")
    parser.add_argument("--no-matrices", action="store_true", help="Only report disk sizes (no openmatrix required)")
    parser.add_argument("--csv", type=Path, help="Write file-level summary to CSV")
    args = parser.parse_args()

    if args.details and args.no_matrices:
        parser.error("--details and --no-matrices cannot be combined")
    try:
        results = scan_folder(args.folder, args.pattern, not args.no_matrices)
        report(results, show_details=args.details)
        if args.csv:
            write_csv(results, args.csv)
            print(f"CSV written: {args.csv}")
    except (OSError, RuntimeError, ValueError) as exc:
        parser.exit(1, f"Error: {exc}\n")
    return 2 if any(item["error"] for item in results) else 0


if __name__ == "__main__":
    raise SystemExit(main())

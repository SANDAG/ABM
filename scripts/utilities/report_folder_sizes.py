"""Report the sizes of files and subfolders directly inside a directory.

Examples:
    python scripts/utilities/report_folder_sizes.py "C:\\ABM\\scenario"
    python scripts/utilities/report_folder_sizes.py "C:\\ABM\\scenario" --workers 4 --csv folder_sizes.csv

Sizes are logical file lengths (not allocated disk blocks). Nested files are
included in their top-level directory total. Symbolic links are not followed.
"""

import argparse
import csv
import os
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


def format_size(num_bytes: int) -> str:
    """Format bytes using binary (base-1024) units."""
    size = float(num_bytes)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if size < 1024 or unit == "TiB":
            return f"{size:,.2f} {unit}"
        size /= 1024
    raise AssertionError("unreachable")


def directory_size(path: str) -> tuple[int, int, int]:
    """Return (file_bytes, unreadable_count, skipped_symlinks) under a directory.

    Iterative traversal avoids recursion errors in deeply nested directories.
    """
    total, errors, links = 0, 0, 0
    pending = [path]
    while pending:
        current = pending.pop()
        try:
            with os.scandir(current) as entries:
                for entry in entries:
                    try:
                        if entry.is_symlink():
                            links += 1
                        elif entry.is_file(follow_symlinks=False):
                            total += entry.stat(follow_symlinks=False).st_size
                        elif entry.is_dir(follow_symlinks=False):
                            pending.append(entry.path)
                    except OSError:
                        errors += 1
        except OSError:
            errors += 1
    return total, errors, links


def inspect_entry(entry: os.DirEntry) -> dict:
    """Summarize one immediate entry in the requested folder."""
    info = {"name": entry.name, "type": "OTHER", "bytes": 0, "errors": 0, "links": 0}
    try:
        if entry.is_symlink():
            info["type"] = "LINK"
            info["links"] = 1
        elif entry.is_file(follow_symlinks=False):
            info["type"] = "FILE"
            info["bytes"] = entry.stat(follow_symlinks=False).st_size
        elif entry.is_dir(follow_symlinks=False):
            info["type"] = "DIR"
            info["bytes"], info["errors"], info["links"] = directory_size(entry.path)
    except OSError:
        info["type"] = "ERR"
        info["errors"] += 1
    return info


def scan_folder(folder: str | Path, workers: int = 8, sort_by: str = "size") -> list[dict]:
    """Scan a folder's immediate entries; subfolder sizes include descendants."""
    folder = Path(folder)
    if not folder.is_dir():
        raise NotADirectoryError(f"Not a directory: {folder}")
    if workers < 1:
        raise ValueError("workers must be at least 1")
    if sort_by not in ("name", "size"):
        raise ValueError("sort_by must be 'name' or 'size'")

    with os.scandir(folder) as iterator:
        entries = list(iterator)
    with ThreadPoolExecutor(max_workers=workers) as executor:
        results = list(executor.map(inspect_entry, entries))
    if sort_by == "name":
        results.sort(key=lambda item: item["name"].casefold())
    else:
        results.sort(key=lambda item: (-item["bytes"], item["name"].casefold()))
    return results


def report(results: list[dict], folder: str | Path) -> None:
    """Print a readable summary; warn when totals may be incomplete."""
    print(f"\nScanning: {Path(folder).resolve()}")
    if not results:
        print("Directory is empty.\n")
        return
    print(f"{'Name':<45} {'Type':<6} {'Size':>15}")
    print("-" * 70)
    for item in results:
        name = item["name"] if len(item["name"]) <= 44 else item["name"][:41] + "..."
        print(f'{name:<45} {item["type"]:<6} {format_size(item["bytes"]):>15}')
    total = sum(item["bytes"] for item in results)
    print("-" * 70)
    print(f'{"TOTAL (" + str(len(results)) + " entries)":<45} {"":<6} {format_size(total):>15}')
    errors = sum(item["errors"] for item in results)
    links = sum(item["links"] for item in results)
    if errors:
        print(f"WARNING: {errors} inaccessible entries/directories; total may be understated.")
    if links:
        print(f"Note: {links} symbolic links excluded (not followed).")
    print("Sizes are based on file lengths (1 GiB = 1,073,741,824 bytes).\n")


def write_csv(results: list[dict], destination: str | Path) -> None:
    """Export the report's raw byte counts and scan warnings."""
    with Path(destination).open("w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["name", "type", "bytes", "unreadable_count", "skipped_symlinks"])
        for item in results:
            writer.writerow([item["name"], item["type"], item["bytes"], item["errors"], item["links"]])


def main() -> int:
    parser = argparse.ArgumentParser(description="Report top-level folder and file sizes (including nested files).")
    parser.add_argument("folder", type=Path, help="Scenario or other directory to scan")
    parser.add_argument("--workers", type=int, default=8,
                        help="Concurrent top-level directory scans (default: 8)")
    parser.add_argument("--sort", choices=("size", "name"), default="size",
                        help="Report order (default: size, descending)")
    parser.add_argument("--csv", type=Path, help="Write file/folder sizes to CSV")
    args = parser.parse_args()
    try:
        results = scan_folder(args.folder, workers=args.workers, sort_by=args.sort)
        report(results, args.folder)
        if args.csv:
            write_csv(results, args.csv)
            print(f"CSV written: {args.csv}")
    except (OSError, ValueError) as exc:
        parser.exit(1, f"Error: {exc}\n")
    return 2 if any(item["errors"] for item in results) else 0


if __name__ == "__main__":
    raise SystemExit(main())

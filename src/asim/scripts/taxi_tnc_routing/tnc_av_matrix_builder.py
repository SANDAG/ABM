"""
TNC/AV Demand Matrix Builder

Reads vehicle trip outputs from taxi_tnc_routing and av_routing models,
aggregates to O-D matrices by time period and occupancy, and writes
OMX files for import into Emme traffic assignment.

Output Files:
    - TNCVehicleTrips_pp.omx: TNC vehicle trips by occupancy (0, 1, 2, 3+)
    - EmptyAVTrips.omx: AV deadhead/repositioning trips only

Usage
-----
    python tnc_av_matrix_builder.py <project_dir> [--settings <yaml_file>]

Arguments
---------
    project_dir : str
        Path to the project directory. All relative paths in the settings
        YAML file (asim_output_dir, output_dir, skim_dir, matrix_output_dir)
        are resolved relative to this directory.

    --settings : str, optional
        Path to the settings YAML file (default: taxi_tnc_routing_settings.yaml)

Examples
--------
    python tnc_av_matrix_builder.py /path/to/project
    python tnc_av_matrix_builder.py C:/models/scenario1 --settings custom_settings.yaml
"""

import os
import logging
import argparse
import time
from typing import Optional, Dict

import numpy as np
import pandas as pd
import openmatrix as omx
import yaml
from pydantic import BaseModel, ValidationError, model_validator

logger = logging.getLogger(__name__)


class MatrixBuilderSettings(BaseModel):
    """
    Settings for the TNC/AV Matrix Builder.

    Extends taxi_tnc_routing settings with matrix output configuration.
    """

    # Input paths - from existing taxi_tnc_routing settings
    asim_output_dir: str
    output_dir: str  # taxi_tnc_routing output dir (contains tnc vehicle trips)

    # Skim configuration - reused for zone mapping
    skim_dir: str
    skim_files: list
    skim_periods: list = ["EA", "AM", "MD", "PM", "EV"]
    periods: list = [0, 6, 12, 25, 32, 48]

    # Matrix builder specific settings
    av_vehicle_trips_file: str = "final_av_vehicle_trips"
    tnc_vehicle_trips_file: str = "output_tnc_vehicle_trips.csv"
    matrix_output_dir: str = "./output"
    max_occupancy_bin: int = 3

    @model_validator(mode="after")
    def check_periods_match(self):
        """Validate that periods and skim_periods have compatible lengths."""
        if len(self.periods) != len(self.skim_periods) + 1:
            raise ValueError(
                f"periods ({len(self.periods)}) must have one more element than "
                f"skim_periods ({len(self.skim_periods)})"
            )
        return self


def load_settings(project_dir: str, yaml_path: str) -> MatrixBuilderSettings:
    """
    Load and validate settings from YAML file.

    All relative paths in the settings are resolved relative to the project_dir.

    Parameters
    ----------
    project_dir : str
        Base project directory. All relative paths in settings will be resolved
        relative to this directory.
    yaml_path : str
        Path to the YAML settings file.

    Returns
    -------
    MatrixBuilderSettings
        Validated settings object.
    """
    # Try yaml_path as-is, then relative to project_dir, then relative to script dir
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        yaml_path,
        os.path.join(project_dir, yaml_path),
        os.path.join(script_dir, yaml_path),
    ]
    settings_file = next((p for p in candidates if os.path.exists(p)), None)
    if settings_file is None:
        raise FileNotFoundError(
            f"Settings file not found: tried {candidates}"
        )

    with open(settings_file, "r") as f:
        data = yaml.safe_load(f)

    # Resolve relative paths in settings relative to project_dir
    path_fields = ["asim_output_dir", "output_dir", "skim_dir", "matrix_output_dir"]
    for field in path_fields:
        if field in data and data[field]:
            path_value = os.path.expanduser(data[field])
            if not os.path.isabs(path_value):
                data[field] = os.path.join(project_dir, path_value)

    try:
        settings = MatrixBuilderSettings(**data)
    except ValidationError as e:
        logger.error(f"Settings validation error: {e}")
        raise

    # Ensure output directory exists
    matrix_output_dir = os.path.expanduser(settings.matrix_output_dir)
    if not os.path.exists(matrix_output_dir):
        os.makedirs(matrix_output_dir)
        logger.info(f"Created matrix output directory: {matrix_output_dir}")

    return settings


class TNCVehicleMatrixBuilder:
    """
    Builds OMX demand matrices from TNC and AV vehicle trip outputs.

    Reads vehicle trip tables from taxi_tnc_routing and av_routing models,
    aggregates trips by O-D, period, and occupancy, and writes OMX matrices
    for import into traffic assignment.

    Attributes
    ----------
    settings : MatrixBuilderSettings
        Configuration settings from YAML file
    skim_zone_mapping : dict
        Mapping from TAZ ID to skim matrix index
    zone_ids : np.ndarray
        Ordered array of zone IDs matching skim matrix dimensions
    num_zones : int
        Number of zones in the skim/output matrices
    period_labels : list
        Unique period labels (EA, AM, MD, PM, EV)
    """

    def __init__(self, settings: MatrixBuilderSettings, sample_rate: float = 1.0):
        """
        Initialize the matrix builder with settings.

        Parameters
        ----------
        settings : MatrixBuilderSettings
            Configuration settings from YAML file.
        sample_rate : float, optional
            Sample rate used in the simulation (0-1). Output matrices will be
            scaled by 1/sample_rate to expand to full population. Default is 1.0.
        """
        self.settings = settings
        self.sample_rate = sample_rate
        self.expansion_factor = 1.0 / sample_rate
        self.skim_zone_mapping: Dict[int, int] = {}
        self.maz_to_taz: Dict[int, int] = {}
        self.zone_ids: Optional[np.ndarray] = None
        self.zone_mapping_name: str = "zone_id"  # Default mapping name
        self.num_zones: int = 0
        self.period_labels: list = []
        self.depart_bin_to_period: Dict[int, str] = {}

        self._load_zone_mapping()
        self._load_maz_to_taz_mapping()
        self._setup_period_mapping()

    def _load_zone_mapping(self) -> None:
        """
        Load zone mapping from skim file.

        Reads the first skim file to get zone ordering and creates
        a mapping from TAZ ID to matrix index. If no explicit mapping
        exists in the OMX file, assumes standard 1-indexed zone IDs
        based on matrix dimensions.
        """
        skim_dir = os.path.expanduser(self.settings.skim_dir)
        skim_file = self.settings.skim_files[0]
        skim_path = os.path.join(skim_dir, skim_file)

        if not os.path.exists(skim_path):
            raise FileNotFoundError(
                f"Do not know how to map zones -- skim file not found: {skim_path}"
            )

        with omx.open_file(skim_path, "r") as f:
            # Get list of available mappings
            available_mappings = f.list_mappings()

            if available_mappings:
                # Use the first available mapping
                mapping_name = available_mappings[0]
                self.zone_mapping_name = mapping_name  # Store for output files
                mapping = f.mapping(mapping_name)
                self.zone_ids = np.array(list(mapping.keys()))
                logger.info(f"Using zone mapping '{mapping_name}' from {skim_file}")
            else:
                # No mapping exists - assume standard zone IDs based on matrix shape
                # Get shape from first matrix core
                matrix_names = f.list_matrices()
                if not matrix_names:
                    raise ValueError(f"No matrices found in skim file: {skim_path}")

                first_matrix = f[matrix_names[0]]
                self.num_zones = first_matrix.shape[0]
                # Assume 1-indexed zone IDs
                self.zone_ids = np.arange(1, self.num_zones + 1)
                logger.warning(
                    f"No zone mapping found in {skim_file}. "
                    f"Assuming standard 1-indexed zone IDs (1 to {self.num_zones})"
                )

            self.num_zones = len(self.zone_ids)
            self.skim_zone_mapping = {
                zone_id: idx for idx, zone_id in enumerate(self.zone_ids)
            }

        logger.info(f"Loaded zone mapping with {self.num_zones} zones from {skim_file}")

    def _load_maz_to_taz_mapping(self) -> None:
        """
        Load MAZ to TAZ mapping from final_land_use table.

        Reads the land use table from ActivitySim output directory to create
        a mapping from MAZ (Micro Analysis Zone) to TAZ (Traffic Analysis Zone).
        This is needed because AV vehicle trips use MAZ for origin/destination
        but output matrices need to be at TAZ level.
        """
        asim_dir = os.path.expanduser(self.settings.asim_output_dir)

        # Try parquet first, then csv
        parquet_path = os.path.join(asim_dir, "final_land_use.parquet")
        csv_path = os.path.join(asim_dir, "final_land_use.csv")

        land_use = None
        file_used = None

        if os.path.exists(parquet_path):
            try:
                land_use = pd.read_parquet(parquet_path)
                file_used = parquet_path
            except Exception as e:
                logger.warning(f"Error reading land_use parquet: {e}")

        if land_use is None and os.path.exists(csv_path):
            try:
                land_use = pd.read_csv(csv_path)
                file_used = csv_path
            except Exception as e:
                logger.warning(f"Error reading land_use csv: {e}")

        if land_use is None:
            logger.warning(
                f"Land use file not found: tried {parquet_path} and {csv_path}. "
                f"MAZ to TAZ mapping will not be available for AV trips."
            )
            return

        # Determine MAZ and TAZ column names
        # Common column name patterns
        maz_col = None
        taz_col = None

        # Check for MAZ column
        for col in ["MAZ", "maz", "zone_id", "ZONE_ID", "mgra", "MGRA"]:
            if col in land_use.columns:
                maz_col = col
                break

        # Check for TAZ column
        for col in ["TAZ", "taz", "TAZ_ORIGINAL", "taz_original"]:
            if col in land_use.columns:
                taz_col = col
                break

        # If MAZ column is the index, use that
        if maz_col is None and land_use.index.name in [
            "MAZ",
            "maz",
            "zone_id",
            "ZONE_ID",
            "mgra",
            "MGRA",
        ]:
            land_use = land_use.reset_index()
            maz_col = land_use.columns[0]

        if maz_col is None or taz_col is None:
            logger.warning(
                f"Could not identify MAZ and/or TAZ columns in land use file. "
                f"Available columns: {list(land_use.columns)}. "
                f"MAZ to TAZ mapping will not be available for AV trips."
            )
            return

        # Create the mapping
        self.maz_to_taz = land_use.set_index(maz_col)[taz_col].to_dict()

        logger.info(
            f"Loaded MAZ to TAZ mapping with {len(self.maz_to_taz)} zones from {file_used} "
            f"(MAZ col: {maz_col}, TAZ col: {taz_col})"
        )

    def _setup_period_mapping(self) -> None:
        """
        Setup time period mapping from settings.

        Uses periods boundaries and skim_periods labels to create
        a mapping from depart_bin (1-48) to period label.
        """
        # Get unique period labels
        self.period_labels = list(dict.fromkeys(self.settings.skim_periods))

        # Create depart_bin -> period mapping
        # periods defines boundaries: [0, 6, 12, 25, 32, 48]
        # skim_periods defines labels for each interval
        edges = np.array(self.settings.periods, dtype=int)
        labels = np.array(self.settings.skim_periods, dtype=object)

        # Map each depart_bin (1-48) to a period
        for depart_bin in range(1, 49):
            # Find which interval this bin falls into
            idx = np.searchsorted(edges, depart_bin, side="right") - 1
            idx = np.clip(idx, 0, len(labels) - 1)
            self.depart_bin_to_period[depart_bin] = labels[idx]

        logger.info(f"Setup period mapping for periods: {self.period_labels}")

    def _read_tnc_vehicle_trips(self) -> Optional[pd.DataFrame]:
        """
        Read TNC vehicle trips from taxi_tnc_routing output.

        Returns
        -------
        pd.DataFrame or None
            TNC vehicle trips with columns: origin_taz, destination_taz,
            depart_bin, occupancy. Returns None if file not found or empty.
        """
        output_dir = os.path.expanduser(self.settings.output_dir)
        tnc_file = os.path.join(output_dir, self.settings.tnc_vehicle_trips_file)

        if not os.path.exists(tnc_file):
            logger.warning(f"TNC vehicle trips file not found: {tnc_file}")
            return None

        try:
            trips = pd.read_csv(tnc_file)
        except Exception as e:
            logger.warning(f"Error reading TNC vehicle trips: {e}")
            return None

        if trips.empty:
            logger.warning("TNC vehicle trips file is empty")
            return None

        # Validate required columns
        required_cols = ["origin_taz", "destination_taz", "depart_bin", "occupancy"]
        missing_cols = [col for col in required_cols if col not in trips.columns]
        if missing_cols:
            logger.warning(f"TNC trips missing required columns: {missing_cols}")
            return None

        logger.info(f"Read {len(trips)} TNC vehicle trips from {tnc_file}")
        return trips[required_cols].copy()

    def _read_av_vehicle_trips(self) -> Optional[pd.DataFrame]:
        """
        Read AV vehicle trips from ActivitySim output.

        Tries .parquet first, then .csv. Returns only deadhead trips.

        Returns
        -------
        pd.DataFrame or None
            AV deadhead trips with columns: origin, destination, depart,
            is_deadhead. Returns None if not found or empty.
        """
        asim_dir = os.path.expanduser(self.settings.asim_output_dir)
        base_name = self.settings.av_vehicle_trips_file

        # Try parquet first, then csv
        parquet_path = os.path.join(asim_dir, f"{base_name}.parquet")
        csv_path = os.path.join(asim_dir, f"{base_name}.csv")

        trips = None
        file_used = None

        if os.path.exists(parquet_path):
            try:
                trips = pd.read_parquet(parquet_path)
                file_used = parquet_path
            except Exception as e:
                logger.warning(f"Error reading AV trips parquet: {e}")

        if trips is None and os.path.exists(csv_path):
            try:
                trips = pd.read_csv(csv_path)
                file_used = csv_path
            except Exception as e:
                logger.warning(f"Error reading AV trips csv: {e}")

        if trips is None:
            logger.warning(
                f"AV vehicle trips file not found: tried {parquet_path} and {csv_path}"
            )
            return None

        if trips.empty:
            logger.warning("AV vehicle trips file is empty")
            return None

        # Filter to deadhead trips only
        deadhead_trips = trips[
            (trips["is_deadhead"] == True)
            & (trips.origin > 0)
            & (trips.destination > 0)
        ].copy()

        if deadhead_trips.empty:
            logger.warning("No deadhead trips found in AV vehicle trips")
            return None

        # Validate required columns for deadhead trips
        required_cols = ["origin", "destination", "depart"]
        missing_cols = [
            col for col in required_cols if col not in deadhead_trips.columns
        ]
        if missing_cols:
            logger.warning(
                f"AV deadhead trips missing required columns: {missing_cols}"
            )
            return None

        logger.info(
            f"Read {len(deadhead_trips)} AV deadhead trips from {file_used} "
            f"(out of {len(trips)} total AV trips)"
        )

        # Convert MAZ to TAZ for origin and destination
        if not self.maz_to_taz:
            logger.warning("No MAZ to TAZ mapping available. AV trips will be skipped.")
            return None

        deadhead_trips = deadhead_trips.copy()
        deadhead_trips["origin_taz"] = deadhead_trips["origin"].map(self.maz_to_taz)
        deadhead_trips["destination_taz"] = deadhead_trips["destination"].map(
            self.maz_to_taz
        )

        # Check for unmapped MAZs
        unmapped_origins = deadhead_trips["origin_taz"].isna().sum()
        unmapped_dests = deadhead_trips["destination_taz"].isna().sum()

        if unmapped_origins > 0 or unmapped_dests > 0:
            logger.error(
                f"Found {unmapped_origins} unmapped origin MAZs and "
                f"{unmapped_dests} unmapped destination MAZs in AV trips"
            )
            raise ValueError(
                "Unmapped MAZs found in AV deadhead trips -- check MAZ to TAZ mapping"
            )

        deadhead_trips["origin_taz"] = deadhead_trips["origin_taz"].astype(int)
        deadhead_trips["destination_taz"] = deadhead_trips["destination_taz"].astype(
            int
        )

        return deadhead_trips[
            ["origin_taz", "destination_taz", "depart", "is_deadhead"]
        ].copy()

    def _map_zones_to_indices(
        self, trips: pd.DataFrame, origin_col: str, dest_col: str
    ) -> pd.DataFrame:
        """
        Map zone IDs to matrix indices.

        Parameters
        ----------
        trips : pd.DataFrame
            Trip records with origin/destination columns
        origin_col : str
            Column name for origin zone
        dest_col : str
            Column name for destination zone

        Returns
        -------
        pd.DataFrame
            Trips with added origin_idx and dest_idx columns
        """
        trips = trips.copy()
        trips["origin_idx"] = trips[origin_col].map(self.skim_zone_mapping)
        trips["dest_idx"] = trips[dest_col].map(self.skim_zone_mapping)

        # Filter out trips with unmapped zones
        valid_mask = trips["origin_idx"].notna() & trips["dest_idx"].notna()
        n_invalid = (~valid_mask).sum()
        if n_invalid > 0:
            logger.warning(f"Dropping {n_invalid} trips with unmapped zones")

        trips = trips[valid_mask].copy()
        trips["origin_idx"] = trips["origin_idx"].astype(int)
        trips["dest_idx"] = trips["dest_idx"].astype(int)

        return trips

    def _map_depart_to_period(
        self, trips: pd.DataFrame, depart_col: str
    ) -> pd.DataFrame:
        """
        Map departure time to period labels.

        Parameters
        ----------
        trips : pd.DataFrame
            Trip records with departure time column
        depart_col : str
            Column name for departure time (1-48 half-hour bins)

        Returns
        -------
        pd.DataFrame
            Trips with added 'period' column
        """
        trips = trips.copy()
        trips["period"] = trips[depart_col].map(self.depart_bin_to_period)

        # Handle any unmapped values
        unmapped = trips["period"].isna()
        if unmapped.any():
            logger.warning(
                f"Found {unmapped.sum()} trips with unmapped depart times, "
                f"assigning to first period"
            )
            trips.loc[unmapped, "period"] = self.period_labels[0]

        return trips

    def _aggregate_to_matrix(self, trips: pd.DataFrame, period: str) -> np.ndarray:
        """
        Aggregate trips to O-D matrix for a given period.

        Parameters
        ----------
        trips : pd.DataFrame
            Trip records with origin_idx, dest_idx, and period columns
        period : str
            Period label to filter by

        Returns
        -------
        np.ndarray
            2D array of shape (num_zones, num_zones) with trip counts
        """
        matrix = np.zeros((self.num_zones, self.num_zones), dtype=np.float32)

        period_trips = trips[trips["period"] == period]

        if period_trips.empty:
            return matrix

        # Use numpy bincount for fast aggregation
        o_idx = period_trips["origin_idx"].values
        d_idx = period_trips["dest_idx"].values

        # Create flat index for O-D pairs
        flat_idx = o_idx * self.num_zones + d_idx

        # Count trips per O-D pair
        counts = np.bincount(flat_idx, minlength=self.num_zones * self.num_zones)
        matrix = counts.reshape(self.num_zones, self.num_zones).astype(np.float32)

        return matrix

    def _create_empty_matrix(self) -> np.ndarray:
        """Create a zero-filled matrix of the correct dimensions."""
        return np.zeros((self.num_zones, self.num_zones), dtype=np.float32)

    def _write_tnc_matrices(
        self, tnc_trips: Optional[pd.DataFrame], input_trip_count: int
    ) -> int:
        """
        Write TNC vehicle trip matrices to OMX files.

        Creates one OMX file per period with cores for each occupancy bin
        (0, 1, 2, 3+). Occupancy 0 includes deadhead/repositioning trips.

        If tnc_trips is None or empty, writes empty matrices with a warning.

        Parameters
        ----------
        tnc_trips : pd.DataFrame or None
            ALL TNC vehicle trips (both serving and deadhead)
        input_trip_count : int
            Original number of trips from input file for validation

        Returns
        -------
        int
            Total number of trips written to matrices
        """
        output_dir = os.path.expanduser(self.settings.matrix_output_dir)
        total_written = 0

        if tnc_trips is None or tnc_trips.empty:
            logger.warning("No TNC trips available, writing empty TNC matrices")
            tnc_trips = pd.DataFrame(
                columns=["origin_taz", "destination_taz", "depart_bin", "occupancy"]
            )

        # Track trips after zone/period mapping for validation
        trips_after_mapping = len(tnc_trips)

        # Map zones and periods
        tnc_trips = self._map_zones_to_indices(
            tnc_trips, "origin_taz", "destination_taz"
        )
        tnc_trips = self._map_depart_to_period(tnc_trips, "depart_bin")

        trips_after_zone_mapping = len(tnc_trips)

        # Bin occupancy (0, 1, 2, 3+)
        max_bin = self.settings.max_occupancy_bin
        tnc_trips["occ_bin"] = tnc_trips["occupancy"].clip(upper=max_bin).astype(int)

        # Write one OMX file per period
        for period in self.period_labels:
            omx_filename = f"TNCVehicleTrips_{period}.omx"
            omx_path = os.path.join(output_dir, omx_filename)

            with omx.open_file(omx_path, "w") as f:
                # Create zone mapping using same name as input skim
                f.create_mapping(self.zone_mapping_name, self.zone_ids)

                # Write matrix for each occupancy bin
                for occ_bin in range(max_bin + 1):
                    core_name = f"TNC_{period}_{occ_bin}"

                    occ_trips = tnc_trips[tnc_trips["occ_bin"] == occ_bin]
                    matrix = self._aggregate_to_matrix(occ_trips, period)

                    # Scale matrix by expansion factor (1/sample_rate)
                    matrix = matrix * self.expansion_factor

                    f[core_name] = matrix

                    matrix_trips = matrix.sum()
                    total_written += matrix_trips
                    logger.info(f"  {core_name}: {matrix_trips:.0f} trips")

            logger.info(f"Wrote TNC matrices to {omx_path}")

        # Assert that all trips after zone mapping are written (accounting for expansion)
        expected_written = trips_after_zone_mapping * self.expansion_factor
        assert abs(total_written - expected_written) < 1e-6, (
            f"TNC trip count mismatch: {trips_after_zone_mapping} trips after zone mapping "
            f"* {self.expansion_factor} expansion = {expected_written:.0f} expected, "
            f"but {total_written:.0f} trips written to matrices"
        )

        logger.info(
            f"TNC validation: {input_trip_count} input trips -> "
            f"{trips_after_zone_mapping} after zone mapping -> "
            f"{total_written:.0f} written to matrices"
        )

        return int(total_written)

    def _write_empty_av_matrices(
        self, av_deadhead: Optional[pd.DataFrame], input_trip_count: int
    ) -> int:
        """
        Write AV deadhead trip matrices to OMX file.

        Writes EmptyAVTrips.omx with one core per period containing ONLY
        deadhead/repositioning trips from the AV routing model.

        If av_deadhead is None or empty, writes empty matrices with warning.

        Parameters
        ----------
        av_deadhead : pd.DataFrame or None
            AV deadhead/repositioning trips only
        input_trip_count : int
            Original number of trips from input file for validation

        Returns
        -------
        int
            Total number of trips written to matrices
        """
        output_dir = os.path.expanduser(self.settings.matrix_output_dir)
        omx_path = os.path.join(output_dir, "EmptyAVTrips.omx")
        total_written = 0

        if av_deadhead is None or av_deadhead.empty:
            logger.warning("No AV deadhead trips available, writing empty AV matrices")
            av_deadhead = pd.DataFrame(
                columns=["origin_taz", "destination_taz", "depart"]
            )

        trips_after_taz_mapping = len(av_deadhead)

        # Map zones and periods
        if not av_deadhead.empty:
            av_deadhead = self._map_zones_to_indices(
                av_deadhead, "origin_taz", "destination_taz"
            )
            av_deadhead = self._map_depart_to_period(av_deadhead, "depart")

        trips_after_zone_mapping = len(av_deadhead)

        with omx.open_file(omx_path, "w") as f:
            # Create zone mapping using same name as input skim
            f.create_mapping(self.zone_mapping_name, self.zone_ids)

            # Write matrix for each period
            for period in self.period_labels:
                core_name = f"EmptyAV_{period}"

                if av_deadhead.empty:
                    matrix = self._create_empty_matrix()
                else:
                    matrix = self._aggregate_to_matrix(av_deadhead, period)

                # Scale matrix by expansion factor (1/sample_rate)
                matrix = matrix * self.expansion_factor

                f[core_name] = matrix

                matrix_trips = matrix.sum()
                total_written += matrix_trips
                logger.info(f"  {core_name}: {matrix_trips:.0f} trips")

        logger.info(f"Wrote EmptyAV matrices to {omx_path}")

        # Assert that all trips after zone mapping are written (accounting for expansion)
        expected_written = trips_after_zone_mapping * self.expansion_factor
        assert abs(total_written - expected_written) < 1e-6, (
            f"AV trip count mismatch: {trips_after_zone_mapping} trips after zone mapping "
            f"* {self.expansion_factor} expansion = {expected_written:.0f} expected, "
            f"but {total_written:.0f} trips written to matrices"
        )

        logger.info(
            f"AV validation: {input_trip_count} deadhead trips -> "
            f"{trips_after_taz_mapping} after MAZ->TAZ mapping -> "
            f"{trips_after_zone_mapping} after zone mapping -> "
            f"{total_written:.0f} written to matrices"
        )

        return int(total_written)

    def build_matrices(self) -> None:
        """
        Main entry point: read inputs, aggregate, and write OMX files.

        Workflow:
        1. Read TNC vehicle trips (warn if missing/empty)
        2. Read AV vehicle trips and filter to deadhead (warn if missing/empty)
        3. Map zones and periods
        4. Write TNCVehicleTrips_pp.omx files (all TNC trips by occupancy)
        5. Write EmptyAVTrips.omx file (AV deadhead only)
        """
        # Read TNC vehicle trips
        logger.info("Reading TNC vehicle trips...")
        tnc_trips = self._read_tnc_vehicle_trips()
        tnc_input_count = len(tnc_trips) if tnc_trips is not None else 0

        # Read AV vehicle trips (deadhead only)
        logger.info("Reading AV vehicle trips...")
        av_deadhead = self._read_av_vehicle_trips()
        av_input_count = len(av_deadhead) if av_deadhead is not None else 0

        # Check if we have any data at all
        if tnc_trips is None and av_deadhead is None:
            logger.warning(
                "No TNC or AV trips found. Writing empty matrices for all outputs."
            )

        # Write TNC matrices (all TNC trips by occupancy)
        logger.info("Writing TNC vehicle trip matrices...")
        tnc_written = self._write_tnc_matrices(tnc_trips, tnc_input_count)

        # Write EmptyAV matrices (AV deadhead only)
        logger.info("Writing EmptyAV matrices...")
        av_written = self._write_empty_av_matrices(av_deadhead, av_input_count)

        # Summary
        logger.info("=" * 60)
        logger.info("Matrix building complete!")
        logger.info("=" * 60)

        logger.info(
            f"  TNC trips: {tnc_input_count:,} input -> {tnc_written:,} written"
        )
        logger.info(
            f"  AV deadhead trips: {av_input_count:,} input -> {av_written:,} written"
        )
        logger.info(f"  Output directory: {self.settings.matrix_output_dir}")


def main():
    """CLI entry point."""
    start_time = time.time()
    parser = argparse.ArgumentParser(
        description="Build TNC/AV demand matrices from routing model outputs"
    )
    parser.add_argument(
        "project_dir",
        type=str,
        help="Path to the project directory. All relative paths in the settings "
        "YAML file will be resolved relative to this directory.",
    )
    parser.add_argument(
        "--settings",
        default="taxi_tnc_routing_settings.yaml",
        help="Path to settings YAML file (default: taxi_tnc_routing_settings.yaml)",
    )
    parser.add_argument(
        "--sample-rate",
        type=float,
        default=1.0,
        help="Sample rate (0-1) used in the simulation. Output matrices will be scaled "
             "by 1/sample_rate to expand to full population. (default: 1.0)",
    )
    args = parser.parse_args()

    # Validate sample rate
    if not 0 < args.sample_rate <= 1:
        raise ValueError(f"Sample rate must be between 0 and 1 (exclusive of 0), got: {args.sample_rate}")

    # Resolve project directory to absolute path
    project_dir = os.path.abspath(os.path.expanduser(args.project_dir))
    if not os.path.isdir(project_dir):
        raise ValueError(f"Project directory does not exist: {project_dir}")

    settings = load_settings(project_dir=project_dir, yaml_path=args.settings)

    # Setup logging
    log_dir = settings.matrix_output_dir
    log_file = os.path.join(log_dir, "tnc_av_matrix_builder.log")

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
        handlers=[logging.FileHandler(log_file), logging.StreamHandler()],
    )

    logger.info("=" * 60)
    logger.info("TNC/AV Demand Matrix Builder")
    logger.info("=" * 60)
    logger.info(f"Sample rate: {args.sample_rate}")

    try:
        builder = TNCVehicleMatrixBuilder(settings, sample_rate=args.sample_rate)
        builder.build_matrices()
    except Exception as e:
        logger.error(f"Error building matrices: {e}", exc_info=True)
        raise

    elapsed_time = time.time() - start_time
    logger.info(
        f"\nTime to complete: {elapsed_time:.2f} seconds = {elapsed_time/60:.2f} minutes"
    )


if __name__ == "__main__":
    main()

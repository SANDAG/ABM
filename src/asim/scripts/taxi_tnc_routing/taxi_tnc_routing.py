import numpy as np
import pandas as pd
import openmatrix as omx
import os
import time
from pydantic import BaseModel, ValidationError, model_validator
import logging
import yaml

logger = logging.getLogger(__name__)

# input_folder = r"C:\Users\david.hensle\OneDrive - Resource Systems Group, Inc\Documents\projects\sandag\AV_TNC_models\tnc_data\full"
# input_folder = r"C:\Users\david.hensle\OneDrive - Resource Systems Group, Inc\Documents\projects\sandag\AV_TNC_models\tnc_data\test"


class TaxiTNCSettings(BaseModel):
    """
    Taxi / TNC route choice settings
    """

    # path to activitysim output folder with trip data
    asim_output_dir: str
    asim_demand_files: list

    # path to folder with skim data
    skim_dir: str
    # name of skim matrix in OMX file minus time period suffix
    skim_time_core: str = "HOV2_TR_H_TIME"
    skim_dist_core: str = "HOV2_TR_H_DIST"
    skim_periods: list = ["EA", "AM", "MD", "PM", "EV"]  # time periods to process
    periods: list = [0, 6, 12, 25, 32, 48]
    skim_files: list = [
        "traffic_skims_EA.omx",
        "traffic_skims_AM.omx",
        "traffic_skims_MD.omx",
        "traffic_skims_PM.omx",
        "traffic_skims_EV.omx",
    ]

    # output folder for results
    output_dir: str = "./output"

    # modes for shared and single occupancy TNC / taxi trips
    shared_tnc_modes: list = ["TNC_SHARED"]
    single_tnc_modes: list = ["TNC_SINGLE", "TAXI"]

    # time bin size (in mins)
    time_bin_size: int = 10

    # buffer size comparing origins and destinations for potential pooling (in mins)
    pooling_buffer: int = 10
    # batch size for matching pooled trips together and matching trips to vehicles
    pair_batch_size: int = 6000

    # maximum allowed detour time for pooled trips (in mins)
    max_detour: int = 15

    # column in the landuse table that indicates the presence of refueling stations
    landuse_refuel_col: str = "refueling_stations"
    # maximum distance (in miles) a vehicle can operate before refueling
    max_refuel_dist: int = 300

    # maximum initial wait time (in minutes) for a vehicle before a new vehicle is created
    max_wait_time: int = 15

    # numpy random seed for reproducibility
    random_seed: int = 42

    @property
    def period_time_bin_minutes(self) -> int:
        # minutes per (max period index); unchanged if periods list changes
        return (24 * 60) // max(self.periods)

    @model_validator(mode="after")
    def check_skim_periods(self):
        if len(self.skim_periods) != len(self.periods) - 1:
            raise ValueError(
                "Length of skim_periods must be one less than length of periods"
            )
        if len(self.skim_files) != len(self.skim_periods):
            raise ValueError("Length of skim_files must match length of skim_periods")
        return self


def load_settings(
    yaml_path: str = "taxi_tnc_routing_settings.yaml",
) -> TaxiTNCSettings:
    with open(os.path.join(os.path.dirname(__file__), yaml_path), "r") as f:
        data = yaml.safe_load(f)
    try:
        settings = TaxiTNCSettings(**data)
    except ValidationError as e:
        logger.error("Settings validation error:", e)
        raise

    # ensure output path exists
    if not os.path.exists(os.path.expanduser(settings.output_dir)):
        os.makedirs(os.path.expanduser(settings.output_dir))
        logger.info(f"Created output directory: {settings.output_dir}")

    # setup logger
    log_file_location = os.path.join(
        os.path.expanduser(settings.output_dir), "taxi_tnc_routing.log"
    )
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
        handlers=[logging.FileHandler(log_file_location), logging.StreamHandler()],
    )

    # set random seed for reproducibility
    np.random.seed(settings.random_seed)

    return settings


class TaxiTNCRouter:
    def __init__(self, settings: TaxiTNCSettings):
        self.settings = settings
        self.skim_time = None
        self.skim_dist = None
        self.skim_zone_mapping = None
        self.current_skim_time_period = None
        self.generate_time_table()

    def generate_time_table(self):
        # Build and cache a time-bin lookup table on self:
        # index: time_bin; cols: hour, minute, period (EA/AM/MD/PM/EV), depart_bin (1..48 starting at 3 AM)
        minutes_per_bin = int(self.settings.time_bin_size)
        bins_per_day = (24 * 60) // minutes_per_bin
        time_bins = np.arange(bins_per_day, dtype=int)

        # Offset bin starts by 3:00 (180 min), wrap at 24h
        bin_start_min = (180 + time_bins * minutes_per_bin) % 1440  # minutes from 00:00

        # Clock time
        hours = (bin_start_min // 60) % 24
        minutes = bin_start_min % 60

        # ActivitySim depart bin: half-hour bins starting at 3:00 (bin 1)
        # Wrap with modulo 1440 to handle minutes before 3:00 correctly.
        depart_bin = ((bin_start_min - 180) % 1440) // 30 + 1
        depart_bin = depart_bin.astype(int)  # 1..48

        # Map depart_bin -> skim period using configured breakpoints
        # periods are boundaries like [0, 6, 12, 25, 32, 48] meaning (0,6], (6,12], ...
        edges = np.asarray(self.settings.periods, dtype=int)
        labels = np.asarray(self.settings.skim_periods, dtype=object)
        period_idx = np.digitize(depart_bin, edges, right=True) - 1  # 0..len(labels)-1
        period_idx = np.clip(period_idx, 0, len(labels) - 1)
        period = labels[period_idx]

        self.time_bin_table = pd.DataFrame(
            {
                "hour": hours,
                "minute": minutes,
                "period": period,
                "depart_bin": depart_bin,
            },
            index=pd.Index(time_bins, name="time_bin"),
        )
        return

    def read_skim_data(self, time_bin):
        # current period of the time_bin
        time_period = self.time_bin_table.loc[time_bin, "period"]
        time_mismatch = self.current_skim_time_period != time_period

        def load_skim(omx_file, core):
            with omx.open_file(
                os.path.join(self.settings.skim_dir, omx_file), "r"
            ) as f:
                skim = np.array(f[core])
                mapping = f.mapping(f.list_mappings()[0])
            return skim, mapping

        # load new skim if no skim data currently exists or the skim is not for the current period
        if self.skim_time is None or time_mismatch:
            self.current_skim_time_period = time_period
            skim_file = self.settings.skim_files[
                self.settings.skim_periods.index(time_period)
            ]
            self.skim_time, mapping = load_skim(
                skim_file, self.settings.skim_time_core + "__" + time_period
            )
            self.skim_dist, mapping = load_skim(
                skim_file, self.settings.skim_dist_core + "__" + time_period
            )
            assert (
                self.skim_zone_mapping == mapping
            ) or self.skim_zone_mapping is None, "Zone mapping changed between skims"
            self.skim_zone_mapping = mapping
        # otherwise keep using existing skim data
        else:
            pass

        return

    def read_input_data(self):
        # Read the data from the OpenMatrix file
        # Convert the data to a pandas DataFrame
        base_cols = ["trip_id", "trip_mode", "depart", "origin", "destination"]
        opt_col = "arrival_mode"
        frames = []
        for fname in self.settings.asim_demand_files:
            fpath = os.path.join(self.settings.asim_output_dir, fname)
            if not os.path.exists(fpath):
                raise ValueError(f"Demand file not found: {fpath}")
            lower = fpath.lower()
            if lower.endswith((".parquet", ".parq", ".pq")):
                try:
                    df = pd.read_parquet(fpath, columns=base_cols + [opt_col])
                except (ValueError, KeyError):
                    df = pd.read_parquet(fpath, columns=base_cols)
            elif lower.endswith((".csv", ".csv.gz", ".txt", ".txt.gz")):
                try:
                    df = pd.read_csv(fpath, usecols=base_cols + [opt_col])
                except (ValueError, KeyError):
                    df = pd.read_csv(fpath, usecols=base_cols)
            else:
                raise ValueError(f"Unsupported demand file type: {fpath}")
            df["source"] = (
                fname.strip(".parquet").strip(".csv").strip(".csv.gz").strip("final_")
            )
            if opt_col in df.columns:
                df["trip_mode"] = df[opt_col]
                df.drop(columns=[opt_col], inplace=True)
            frames.append(df)

        trips = pd.concat(frames, ignore_index=True)

        trips = trips[
            trips.trip_mode.isin(
                self.settings.single_tnc_modes + self.settings.shared_tnc_modes
            )
        ]

        self.landuse = pd.read_csv(
            os.path.join(self.settings.asim_output_dir, "final_land_use.csv")
        )

        if "MAZ" in self.landuse.columns:
            maz_to_taz_map = self.landuse.set_index("MAZ")["taz"].to_dict()
        elif "zone_id" in self.landuse.columns:
            maz_to_taz_map = self.landuse.set_index("zone_id")["TAZ"].to_dict()
        else:
            raise KeyError(
                "Land use data must contain either 'MAZ' or 'zone_id' column for mapping to TAZ."
            )

        # mapping origin and destination maz to skim taz index for fast lookups
        trips["oskim_idx"] = (
            trips["origin"].map(maz_to_taz_map).map(self.skim_zone_mapping)
        )
        trips["dskim_idx"] = (
            trips["destination"].map(maz_to_taz_map).map(self.skim_zone_mapping)
        )
        if trips.trip_id.is_unique is False:
            trips["original_trip_id"] = trips["trip_id"]
            trips.reset_index(inplace=True, drop=True)
            trips["trip_id"] = trips.index

        id_mapping_df = trips[["trip_id", "original_trip_id", "source"]].copy()

        return trips, id_mapping_df

    def sample_tnc_trip_simulation_time_bin(self, trips):
        # Create a new column for the time bin
        # Convert departure time to mins, sampling within the half hour
        # Then bin into the bin size (supplied in mins)

        trips["time_in_mins"] = (trips["depart"] - 1) * 30 + np.random.randint(
            0, self.settings.period_time_bin_minutes, size=len(trips)
        )
        trips["time_bin"] = trips["time_in_mins"] // self.settings.time_bin_size
        assert trips.time_bin.isin(
            self.time_bin_table.index
        ).all(), "Some time bins are out of range"

        return trips

    def _determine_potential_trip_pairs(self, trips):
        # pre-filter trips for pooling
        # find pairs of trips that are within the same origin and destination buffer
        # Ensure indices are integer arrays and no NA
        o_idx = trips["oskim_idx"].to_numpy(dtype=int)
        d_idx = trips["dskim_idx"].to_numpy(dtype=int)
        trip_ids = trips["trip_id"].to_numpy()

        origin_proximity_mask = self.skim_time[o_idx, :] <= self.settings.pooling_buffer
        destination_proximity_mask = (
            self.skim_time[d_idx, :] <= self.settings.pooling_buffer
        )

        # From your masks (shape: n_trips x n_zones), select only the columns for the trips’ zones
        # This yields trip-to-trip boolean matrices
        origin_pair_ok = origin_proximity_mask[:, o_idx]  # shape: n_trips x n_trips
        dest_pair_ok = destination_proximity_mask[:, d_idx]  # shape: n_trips x n_trips

        # Both origin and destination must be within the buffer
        pair_ok = origin_pair_ok & dest_pair_ok

        # Drop self-pairs and keep i<j to avoid duplicates
        np.fill_diagonal(pair_ok, False)
        ii, jj = np.where(np.triu(pair_ok, k=1))
        pct = 0 if len(trips) == 0 else (len(ii) / (len(trips) ** 2)) * 100
        logger.info(
            f"\tFound {len(ii)} potential trip pairs from {len(trips)} trips or {pct:.2f}% of n**2"
        )

        trip_pairs = pd.DataFrame(
            {"trip_i": trip_ids[ii], "trip_j": trip_ids[jj]}, index=None
        )

        # include skim times for origins/destinations
        o2o_time = self.skim_time[o_idx][:, o_idx]
        d2d_time = self.skim_time[d_idx][:, d_idx]
        trip_pairs["origin_time"] = o2o_time[ii, jj]
        trip_pairs["dest_time"] = d2d_time[ii, jj]
        # also include the origin and destination skim idxs
        # create maps from trip id to origin and to destination
        lookup = trips.loc[
            :, ["trip_id", "oskim_idx", "dskim_idx", "origin", "destination"]
        ].set_index("trip_id")

        trip_pairs = trip_pairs.join(
            lookup.rename(
                columns={
                    "oskim_idx": "o_i_skim_idx",
                    "dskim_idx": "d_i_skim_idx",
                    "origin": "origin_i",
                    "destination": "destination_i",
                }
            ),
            on="trip_i",
        ).join(
            lookup.rename(
                columns={
                    "oskim_idx": "o_j_skim_idx",
                    "dskim_idx": "d_j_skim_idx",
                    "origin": "origin_j",
                    "destination": "destination_j",
                }
            ),
            on="trip_j",
        )

        return trip_pairs

    def determine_potential_trip_pairs(self, trips):
        """Wrapper around determining potential trip pairs to implement batching"""

        # break trips up into batches
        trip_pairs_list = []
        n_trips = len(trips)
        if n_trips == 0:
            return self._determine_potential_trip_pairs(trips)

        batch_size = self.settings.pair_batch_size
        for start_idx in range(0, n_trips, batch_size):
            end_idx = min(start_idx + batch_size, n_trips)
            trip_batch = trips.iloc[start_idx:end_idx]
            logger.info(
                f"\tProcessing trip batch {start_idx} to {end_idx} of {n_trips} trips"
            )
            trip_pairs_batch = self._determine_potential_trip_pairs(trip_batch)
            trip_pairs_list.append(trip_pairs_batch)
        trip_pairs = pd.concat(trip_pairs_list, ignore_index=True)

        return trip_pairs

    def determine_detour_times(self, trip_pairs):
        # For each trip pair, calculate the four possible routing scenarios
        # and determine if they are valid (within detour limits)
        # and select the best valid scenario (minimum total time)
        # WARNING if these scenarios are changed, they also need to be changed downstream in the routing

        oi = trip_pairs["o_i_skim_idx"].to_numpy()
        oj = trip_pairs["o_j_skim_idx"].to_numpy()
        di = trip_pairs["d_i_skim_idx"].to_numpy()
        dj = trip_pairs["d_j_skim_idx"].to_numpy()

        # direct times
        i_direct = self.skim_time[oi, di]
        j_direct = self.skim_time[oj, dj]

        # scenario totals
        t1 = (
            self.skim_time[oi, oj] + self.skim_time[oj, di] + self.skim_time[di, dj]
        )  # oi->oj->di->dj
        t2 = (
            self.skim_time[oj, oi] + self.skim_time[oi, dj] + self.skim_time[dj, di]
        )  # oj->oi->dj->di
        t3 = (
            self.skim_time[oi, oj] + self.skim_time[oj, dj] + self.skim_time[dj, di]
        )  # oi->oj->dj->di
        t4 = (
            self.skim_time[oj, oi] + self.skim_time[oi, di] + self.skim_time[di, dj]
        )  # oj->oi->di->dj

        # per-trip detours
        d1_i = t1 - i_direct
        d1_j = t1 - j_direct
        d2_i = t2 - i_direct
        d2_j = t2 - j_direct
        d3_i = t3 - i_direct
        d3_j = t3 - j_direct
        d4_i = t4 - i_direct
        d4_j = t4 - j_direct

        # checking detour times against max detour time
        max_detour = self.settings.max_detour
        v1 = (d1_i <= max_detour) & (d1_j <= max_detour)
        v2 = (d2_i <= max_detour) & (d2_j <= max_detour)
        v3 = (d3_i <= max_detour) & (d3_j <= max_detour)
        v4 = (d4_i <= max_detour) & (d4_j <= max_detour)

        totals = np.column_stack([t1, t2, t3, t4])
        valids = np.column_stack([v1, v2, v3, v4])
        det_i_m = np.column_stack([d1_i, d2_i, d3_i, d4_i])
        det_j_m = np.column_stack([d1_j, d2_j, d3_j, d4_j])

        # mask invalid scenarios
        totals_masked = np.where(valids, totals, np.inf)

        # choose best valid scenario
        min_idx = np.argmin(totals_masked, axis=1)  # 0..3
        row = np.arange(len(min_idx))
        min_total = totals_masked[row, min_idx]
        valid = np.isfinite(min_total)

        route_scenario = np.where(valid, min_idx + 1, 0)  # 1..4, or 0 if none valid
        total_ivt = np.where(valid, min_total, np.nan)
        detour_i = np.where(valid, det_i_m[row, min_idx], np.nan)
        detour_j = np.where(valid, det_j_m[row, min_idx], np.nan)

        trip_pairs["route_scenario"] = route_scenario
        trip_pairs["total_ivt"] = total_ivt
        trip_pairs["valid"] = valid
        trip_pairs["detour_i"] = detour_i
        trip_pairs["detour_j"] = detour_j
        trip_pairs["total_detour"] = detour_i + detour_j

        eps = 1e-9  # avoiding case where detour is exactly equal to max_detour
        assert (
            trip_pairs[trip_pairs.valid]["detour_i"] <= (max_detour + eps)
        ).all(), "Detour times exceed maximum allowed"
        assert (
            trip_pairs[trip_pairs.valid]["detour_j"] <= (max_detour + eps)
        ).all(), "Detour times exceed maximum allowed"
        assert (
            trip_pairs[trip_pairs.valid]["route_scenario"] > 0
        ).all(), "Valid trips must have a scenario"

        logger.info(f"\tTotal trip pairs within original OD buffer: {len(trip_pairs)}")
        trip_pairs = trip_pairs[trip_pairs.valid]
        logger.info(
            f"\tFound {len(trip_pairs)} valid trip pairs across {trip_pairs['trip_i'].nunique()} unique trips"
        )
        logger.info(
            f"\twith an average detour times for trip i {trip_pairs['detour_i'].mean():.2f} and trip j {trip_pairs['detour_j'].mean():.2f}"
        )

        return trip_pairs

    def select_mutual_best_recursive(self, trip_pairs: pd.DataFrame) -> pd.DataFrame:
        # start from valid pairs only, keep only needed cols for speed
        pairs = trip_pairs[["trip_i", "trip_j", "total_detour"]].copy()
        if pairs.empty:
            return trip_pairs.iloc[0:0].copy()

        # deterministic tie-breaking
        pairs = pairs.sort_values(["total_detour", "trip_i", "trip_j"]).reset_index(
            drop=True
        )

        batches = []
        used = set()
        i = 0
        while not pairs.empty:
            # best partner for each i, and best partner for each j
            best_j_for_i_idx = pairs.groupby("trip_i", sort=False)[
                "total_detour"
            ].idxmin()
            best_j_for_i = pairs.loc[
                best_j_for_i_idx, ["trip_i", "trip_j", "total_detour"]
            ]

            best_i_for_j_idx = pairs.groupby("trip_j", sort=False)[
                "total_detour"
            ].idxmin()
            best_i_for_j = pairs.loc[
                best_i_for_j_idx, ["trip_j", "trip_i", "total_detour"]
            ]
            best_i_for_j.columns = [
                "trip_j",
                "trip_i",
                "detour_time_j",
            ]  # rename for merge

            # mutual-best intersection
            mutual = best_j_for_i.merge(
                best_i_for_j[["trip_i", "trip_j"]], on=["trip_i", "trip_j"], how="inner"
            )
            # handle instances where trip_i is also in trip_j
            mutual = mutual[~mutual["trip_j"].isin(mutual["trip_i"])]

            if mutual.empty:
                # no more matches
                break

            batches.append(mutual[["trip_i", "trip_j"]].copy())

            # remove all trips that just got paired
            used.update(mutual["trip_i"].to_numpy().tolist())
            used.update(mutual["trip_j"].to_numpy().tolist())
            pairs = pairs[
                ~pairs["trip_i"].isin(used) & ~pairs["trip_j"].isin(used)
            ].reset_index(drop=True)

            i += 1
            if i > 10000:
                raise RuntimeError("Exceeded maximum iterations for trip pooling")

        if not batches:
            return trip_pairs.iloc[0:0].copy()

        disjoint_keys = pd.concat(batches, ignore_index=True)

        # bring all attributes back from original trip_pairs
        trip_matches = disjoint_keys.merge(
            trip_pairs, on=["trip_i", "trip_j"], how="left"
        ).reset_index(drop=True)

        # sanity checks
        assert trip_matches["trip_i"].is_unique
        assert trip_matches["trip_j"].is_unique
        assert (~trip_matches["trip_i"].isin(trip_matches["trip_j"])).all()

        logger.info(
            f"\tSelected {len(trip_matches)} disjoint pairs via recursive mutual-best"
        )
        logger.info(
            f"\twith an average detour times for trip i {trip_matches['detour_i'].mean():.2f} and trip j {trip_matches['detour_j'].mean():.2f}"
        )
        logger.info(
            f"\tAverage total detour time {trip_matches['total_detour'].mean():.2f}"
        )
        logger.info(
            f"\tAverage total in vehicle time {trip_matches['total_ivt'].mean():.2f}"
        )

        return trip_matches

    def create_trip_routes(self, trip_matches):
        trips_routed = trip_matches[
            ["trip_i", "trip_j", "total_ivt", "route_scenario", "detour_i", "detour_j"]
        ].copy()

        s1_idx = [0, 1, 2, 3]  # oi->oj->di->dj
        s2_idx = [1, 0, 3, 2]  # oj->oi->dj->di
        s3_idx = [0, 1, 3, 2]  # oi->oj->dj->di
        s4_idx = [1, 0, 2, 3]  # oj->oi->di->dj

        ods_idx = trip_matches[
            ["o_i_skim_idx", "o_j_skim_idx", "d_i_skim_idx", "d_j_skim_idx"]
        ].to_numpy()
        ods = trip_matches[
            ["origin_i", "origin_j", "destination_i", "destination_j"]
        ].to_numpy()

        scenario_orders = np.array(
            [s1_idx, s2_idx, s3_idx, s4_idx]
        )  # index with (route_scenario-1)
        rs = trip_matches["route_scenario"].to_numpy()

        valid_mask = rs > 0
        order_idx = (
            scenario_orders[rs[valid_mask] - 1]
            if valid_mask.any()
            else np.empty((0, 4), dtype=int)
        )

        ordered_nodes = np.full((len(trips_routed), 4), -1, dtype=int)
        ordered_nodes_idx = np.full((len(trips_routed), 4), -1, dtype=int)
        if valid_mask.any():
            row_idx = np.arange(len(trips_routed))[valid_mask]
            ordered_nodes_idx[valid_mask] = ods_idx[row_idx[:, None], order_idx]
            ordered_nodes[valid_mask] = ods[row_idx[:, None], order_idx]

        trips_routed[
            [
                "origin_skim_idx",
                "stop1_skim_idx",
                "stop2_skim_idx",
                "destination_skim_idx",
            ]
        ] = ordered_nodes_idx
        trips_routed[
            [
                "origin",
                "stop1",
                "stop2",
                "destination",
            ]
        ] = ordered_nodes
        return trips_routed

    def create_full_trip_route_table(self, tnc_trips, trips_routed):
        # need to append any additional tnc trips during this time period that were not grouped
        mask = ~(
            tnc_trips.trip_id.isin(trips_routed.trip_i)
            | tnc_trips.trip_id.isin(trips_routed.trip_j)
        )
        cols = {
            "trip_id": "trip_i",
            "oskim_idx": "origin_skim_idx",
            "dskim_idx": "destination_skim_idx",
            "origin": "origin",
            "destination": "destination",
        }
        single_trips = tnc_trips[mask][cols.keys()].rename(columns=cols).copy()
        single_trips["total_ivt"] = self.skim_time[
            single_trips.origin_skim_idx, single_trips.destination_skim_idx
        ]
        single_trips["detour_i"] = 0

        full_trip_routes = pd.concat([trips_routed, single_trips], ignore_index=True)

        logger.info(
            f"\tAdded {single_trips.shape[0]} single trips for a total of {full_trip_routes.shape[0]} episodes."
        )

        assert full_trip_routes.trip_i.is_unique, "Trip IDs are not unique"

        return full_trip_routes

    def create_new_vehicles(self, vehicles, trips_to_service, trip_to_veh_map):
        """
        Create new vehicles for trips that could not be matched to existing free vehicles
        """
        idx_start_num = 0 if vehicles is None else vehicles.index.max()

        new_vehs = pd.DataFrame(
            data={
                "location_skim_idx": trips_to_service.origin_skim_idx.values,
                "location": trips_to_service.origin.values,
                "is_free": False,
                "last_refuel_time": -1,
                "next_time_free": np.nan,
                "drive_dist_since_refuel": 0,
            },
            index=idx_start_num + np.arange(1, len(trips_to_service) + 1),
        )
        new_vehs.index.name = "vehicle_id"

        # updating the trip to vehicle mapping with the newly created vehicles
        trip_to_veh_map.loc[trips_to_service.trip_i] = new_vehs.index.values

        if vehicles is not None:
            vehicles = pd.concat([vehicles, new_vehs], ignore_index=False)
        else:
            vehicles = new_vehs

        return vehicles, trip_to_veh_map

    def _match_vehicles_to_trips(
        self, free_vehicles, unserviced_trips, trip_to_veh_map
    ):

        i = 0
        while not (free_vehicles.empty or unserviced_trips.empty):
            trip_origins = unserviced_trips.origin_skim_idx.to_numpy().astype(int)
            veh_locations = free_vehicles.location_skim_idx.to_numpy().astype(int)

            # find skim times between trip origins and all veh locations
            skim_times = self.skim_time[trip_origins[:, None], veh_locations]

            # grab the minimum time for each trip and vehicle
            min_times = skim_times.min(axis=1)

            # if multiple trips match to the same vehicle, take the one with the smallest time
            closest_vehicles = free_vehicles.index[np.argmin(skim_times, axis=1)]

            # map trips to their closest vehicles
            trip_to_veh_map.loc[unserviced_trips.trip_i] = closest_vehicles

            # trips with a min wait time larger than the maximum allowed are removed from pool
            # ties are given to the first trip as given by trip_id
            time_gt_max_wait = min_times > self.settings.max_wait_time
            trip_to_veh_map.loc[unserviced_trips.trip_i[time_gt_max_wait]] = np.nan
            ties_mask = (
                trip_to_veh_map.duplicated(keep="first") & trip_to_veh_map.notna()
            )
            trip_to_veh_map.loc[ties_mask] = np.nan

            # remove matched trips and vehicles from the pools or trips that have no vehs within the max wait time
            serviced_trips = trip_to_veh_map[trip_to_veh_map.notna()]
            unserviced_trips = unserviced_trips[
                ~(unserviced_trips.trip_i.isin(serviced_trips.index) | time_gt_max_wait)
            ]
            free_vehicles = free_vehicles[
                ~free_vehicles.index.isin(serviced_trips.values)
            ]

            if i > 10000:
                # making sure we never get stuck in an infinite loop
                raise RuntimeError("Exceeded maximum iterations for vehicle matching")

        return free_vehicles, unserviced_trips, trip_to_veh_map

    def match_vehicles_to_trips(self, vehicles, full_trip_routes):
        if vehicles is not None:
            free_vehicles = vehicles[vehicles.is_free].copy()
        else:
            free_vehicles = pd.DataFrame()
        unserviced_trips = full_trip_routes.copy()

        trip_to_veh_map = pd.Series(
            name="vehicle_id",
            index=unserviced_trips.trip_i,
            dtype=vehicles.index.dtype if vehicles is not None else "int64",
        )

        # loop until all available vehicles are assigned or there are no more unserviced trips
        batch_size = self.settings.pair_batch_size
        matched_trip_count = 0
        logger.info(
            f"\tStarting vehicle to trip matching with {len(free_vehicles)} free vehicles and {len(unserviced_trips)} unserviced trips in trip batches of {batch_size}"
        )
        for i in range(0, len(unserviced_trips), batch_size):
            if free_vehicles.empty:
                break
            unserviced_trips_block = unserviced_trips.iloc[i : i + batch_size]
            (
                free_vehicles,
                unserviced_trips_block,
                trip_to_veh_map,
            ) = self._match_vehicles_to_trips(
                free_vehicles, unserviced_trips_block, trip_to_veh_map
            )
            logger.info(
                f"\t Iteration {i}: Matched {trip_to_veh_map.notna().sum() - matched_trip_count} trips to vehicles"
            )
            matched_trip_count = trip_to_veh_map.notna().sum()

        if trip_to_veh_map.isna().any():
            # create new vehicles for those still unmatched
            # trips without match
            unserviced_mask = full_trip_routes.trip_i.isin(
                trip_to_veh_map.index[trip_to_veh_map.isna()]
            )
            trips_still_needing_service = full_trip_routes[unserviced_mask]
            vehicles, trip_to_veh_map = self.create_new_vehicles(
                vehicles, trips_still_needing_service, trip_to_veh_map
            )

        assert (
            not trip_to_veh_map.isna().any()
        ), f"Some trips were not matched to vehicles {trip_to_veh_map[trip_to_veh_map.isna()]}"

        # return a mapping of trips.trip_i to vehicles.vehicle_id
        # trips that were not matched will have an NaN vehicle_id
        return vehicles, trip_to_veh_map

    def create_vehicle_trips(self, full_trip_routes, vehicles, trip_to_veh_map):
        # Create vehicle trips from the full trip routes

        # first turning full_trip_routes into a list of stops
        pickup = full_trip_routes[
            ["trip_i", "trip_j", "origin_skim_idx", "origin"]
        ].rename(
            columns={"origin_skim_idx": "destination_skim_idx", "origin": "destination"}
        )
        # creating a temporary trip num because not all vehicles will have stops
        pickup["srv_trip_num"] = 1
        pickup["trip_type"] = "pickup"

        stop_1 = full_trip_routes.loc[
            full_trip_routes.stop1_skim_idx > -1,
            ["trip_i", "trip_j", "stop1_skim_idx", "stop1"],
        ].rename(
            columns={"stop1_skim_idx": "destination_skim_idx", "stop1": "destination"}
        )
        stop_1["srv_trip_num"] = 2
        stop_1["trip_type"] = "pickup"

        stop_2 = full_trip_routes.loc[
            full_trip_routes.stop2_skim_idx > -1,
            ["trip_i", "trip_j", "stop2_skim_idx", "stop2"],
        ].rename(
            columns={"stop2_skim_idx": "destination_skim_idx", "stop2": "destination"}
        )
        stop_2["srv_trip_num"] = 3
        stop_2["trip_type"] = "dropoff"

        drop_off = full_trip_routes[
            ["trip_i", "trip_j", "destination_skim_idx", "destination"]
        ].copy()
        drop_off["srv_trip_num"] = 4
        drop_off["trip_type"] = "dropoff"

        new_v_trips = pd.concat([pickup, stop_1, stop_2, drop_off])

        new_v_trips["vehicle_id"] = (
            new_v_trips["trip_i"].map(trip_to_veh_map).astype(int)
        )

        # ensure ordering before deriving origins
        new_v_trips.sort_values(
            ["vehicle_id", "srv_trip_num"], inplace=True, ignore_index=True
        )

        new_v_trips["origin_skim_idx"] = pd.NA
        new_v_trips["origin"] = pd.NA

        # first leg origin = vehicle current location
        first_leg_mask = new_v_trips.srv_trip_num == 1
        new_v_trips.loc[first_leg_mask, "origin_skim_idx"] = new_v_trips.loc[
            first_leg_mask, "vehicle_id"
        ].map(vehicles.location_skim_idx)
        new_v_trips.loc[first_leg_mask, "origin"] = new_v_trips.loc[
            first_leg_mask, "vehicle_id"
        ].map(vehicles.location)

        # subsequent leg origin = previous leg destination
        new_v_trips.loc[~first_leg_mask, "origin_skim_idx"] = new_v_trips.groupby(
            "vehicle_id"
        )["destination_skim_idx"].shift(1)[~first_leg_mask]
        new_v_trips.loc[~first_leg_mask, "origin"] = new_v_trips.groupby("vehicle_id")[
            "destination"
        ].shift(1)[~first_leg_mask]

        o_idx_na = new_v_trips.origin_skim_idx.notna()
        d_idx_na = new_v_trips.destination_skim_idx.notna()
        o_na = new_v_trips.origin.notna()
        d_na = new_v_trips.destination.notna()
        assert (
            o_idx_na & d_idx_na
        ).all(), f"Some vehicle trips do not have an origin or destination skim index:\n {new_v_trips[~(o_idx_na & d_idx_na)]}"
        assert (
            o_na & d_na
        ).all(), f"Some vehicle trips do not have an origin or destination:\n {new_v_trips[~(o_na & d_na)]}"

        cols = ["origin_skim_idx", "destination_skim_idx", "origin", "destination"]
        new_v_trips[cols] = new_v_trips[cols].astype({col: int for col in cols})

        # update which trips are being served during the specific vehicle leg
        # right now, we have both trip_i and trip_j (if pooled) for all legs
        new_v_trips["servicing_trip_id"] = new_v_trips[
            "trip_i"
        ]  # leave a trail to know where the vehicle is going
        # first trip from vehicle location to pickup has no occupants.
        new_v_trips.loc[new_v_trips.srv_trip_num == 1, ["trip_i", "trip_j"]] = np.nan
        # who gets picked up and goes to the stops is dependent on the scenario
        scenario = (
            full_trip_routes.set_index("trip_i")
            .route_scenario.reindex(new_v_trips.servicing_trip_id)
            .values
        )
        new_v_trips["scenario"] = scenario
        # scenario 1: oi->oj->di->dj
        new_v_trips.loc[
            (scenario == 1) & (new_v_trips.srv_trip_num == 2), "trip_j"
        ] = np.nan
        new_v_trips.loc[
            (scenario == 1) & (new_v_trips.srv_trip_num == 4), "trip_i"
        ] = np.nan
        # scenario 2: oj->oi->dj->di
        new_v_trips.loc[
            (scenario == 2) & (new_v_trips.srv_trip_num == 2), "trip_i"
        ] = np.nan
        new_v_trips.loc[
            (scenario == 2) & (new_v_trips.srv_trip_num == 4), "trip_j"
        ] = np.nan
        # scenario 3: oi->oj->dj->di
        new_v_trips.loc[
            (scenario == 3) & (new_v_trips.srv_trip_num == 2), "trip_j"
        ] = np.nan
        new_v_trips.loc[
            (scenario == 3) & (new_v_trips.srv_trip_num == 4), "trip_j"
        ] = np.nan
        # scenario 4: oj->oi->di->dj
        new_v_trips.loc[
            (scenario == 4) & (new_v_trips.srv_trip_num == 2), "trip_i"
        ] = np.nan
        new_v_trips.loc[
            (scenario == 4) & (new_v_trips.srv_trip_num == 4), "trip_i"
        ] = np.nan

        # determine time bin of each trip
        new_v_trips["OD_time"] = self.skim_time[
            new_v_trips.origin_skim_idx.to_numpy(),
            new_v_trips.destination_skim_idx.to_numpy(),
        ]
        cumulative_trip_time = new_v_trips.groupby("vehicle_id")["OD_time"].cumsum()
        new_v_trips["depart_bin"] = (
            full_trip_routes.time_bin.mode()[0]
            + (cumulative_trip_time - new_v_trips["OD_time"]) // 15
        )
        new_v_trips["arrival_bin"] = full_trip_routes.time_bin.mode()[0] + (
            cumulative_trip_time // 15
        )

        new_v_trips["OD_dist"] = self.skim_dist[
            new_v_trips.origin_skim_idx.to_numpy(),
            new_v_trips.destination_skim_idx.to_numpy(),
        ]

        assert (
            new_v_trips.depart_bin <= new_v_trips.arrival_bin
        ).all(), f"Departure bin must be less than or equal to arrival bin {new_v_trips[new_v_trips.depart_bin > new_v_trips.arrival_bin]}"

        # merge initial wait time from repositioning trip to full_trip_routes
        initial_wait = new_v_trips[new_v_trips.srv_trip_num == 1][
            ["servicing_trip_id", "OD_time"]
        ].set_index("servicing_trip_id")
        full_trip_routes["initial_wait"] = full_trip_routes.trip_i.map(
            initial_wait.OD_time
        )

        return new_v_trips, full_trip_routes

    def update_vehicle_fleet(
        self, vehicles, vehicle_trips_i, time_bin, refuel_veh_trips_i
    ):
        # update the next_time_free based on the vehicle trips
        next_time_free_update = (
            vehicle_trips_i.groupby("vehicle_id").arrival_bin.max() + 1
        )  # +1 rouding up to account for dropoff time
        vehicles.loc[
            next_time_free_update.index, "next_time_free"
        ] = next_time_free_update
        # +1 on time bin since we are calculating the availaibility for the next time bin
        vehicles["is_free"] = np.where(
            (time_bin + 1 >= vehicles.next_time_free), True, False
        )

        # also want to update the vehicle location
        final_veh_location_idx = vehicle_trips_i.groupby(
            "vehicle_id"
        ).destination_skim_idx.last()
        vehicles.loc[
            final_veh_location_idx.index, "location_skim_idx"
        ] = final_veh_location_idx
        final_veh_location = vehicle_trips_i.groupby("vehicle_id").destination.last()
        vehicles.loc[final_veh_location.index, "location"] = final_veh_location

        tot_drive_time = vehicle_trips_i.groupby("vehicle_id").OD_time.sum()
        vehicles.loc[tot_drive_time.index, "drive_dist_since_refuel"] += tot_drive_time

        # updating fleet with refueled vehicles
        if refuel_veh_trips_i is not None:
            vehicles.loc[
                refuel_veh_trips_i.vehicle_id, "last_refuel_time"
            ] = refuel_veh_trips_i.arrival_bin
            vehicles.loc[refuel_veh_trips_i.vehicle_id, "drive_dist_since_refuel"] = 0
            vehicles.loc[refuel_veh_trips_i.vehicle_id, "is_free"] = False
            vehicles.loc[refuel_veh_trips_i.vehicle_id, "next_time_free"] = (
                refuel_veh_trips_i.arrival_bin + 1
            )
            vehicles.loc[
                refuel_veh_trips_i.vehicle_id, "location_skim_idx"
            ] = refuel_veh_trips_i.destination_skim_idx
            vehicles.loc[
                refuel_veh_trips_i.vehicle_id, "location"
            ] = refuel_veh_trips_i.destination

        return vehicles

    def summarize_tnc_trips(self, tnc_veh_trips, tnc_trips, vehicles, pooled_trips):

        # Summary statistics for TNC vehicle trips
        logger.info("TNC Vehicle Trips Summary:")
        logger.info(f"Total TNC Vehicle Trips: {len(tnc_veh_trips)}")
        logger.info(f"Total TNC Trips: {len(tnc_trips)}")
        logger.info(f"Total Vehicles: {len(vehicles)}")
        logger.info(
            f"TNC Trips to Vehicles Ratio: {(len(tnc_trips) / len(vehicles)):.2f}"
        )
        logger.info(
            f"Average trips per vehicle: {(tnc_veh_trips.groupby('vehicle_id').size().mean()):.2f}"
        )
        logger.info(
            f"Average number of trips per vehicle: {(tnc_veh_trips.groupby('vehicle_id').size().mean()):.2f}"
        )
        logger.info(
            f"Average initial wait time: {pooled_trips.initial_wait.mean():.2f} mins"
        )
        avg_detour = (
            pooled_trips[pooled_trips.trip_j.notna()][["detour_i", "detour_j"]]
            .melt()
            .dropna()
            .value.mean()
        )
        logger.info(
            f"Average detour time per passenger on pooled trips: {avg_detour:.2f} mins"
        )
        tnc_veh_trips["is_deadhead"] = (
            tnc_veh_trips.trip_i.isna() & tnc_veh_trips.trip_j.isna()
        )
        logger.info(
            f"Percentage of deadhead vehicle trips: {tnc_veh_trips.is_deadhead.mean() * 100:.2f}%"
        )
        tnc_veh_trips["occupancy"] = (
            tnc_veh_trips[["trip_i", "trip_j"]].notna().sum(axis=1)
        )
        logger.info(
            f"Average occupancy of all vehicle trips: {tnc_veh_trips.occupancy.mean():.2f}"
        )
        num_refuel_trips = tnc_veh_trips[tnc_veh_trips.trip_type == "refuel"].shape[0]
        logger.info(
            f"Total number of refuel trips: {num_refuel_trips} = {(num_refuel_trips / len(tnc_veh_trips) * 100):.2f}% of all vehicle trips"
        )

        # also performing consistency checks on the outputs
        assert (
            tnc_trips.trip_id.isin(tnc_veh_trips.trip_i)
            | tnc_trips.trip_id.isin(tnc_veh_trips.trip_j)
        ).all(), "Some trip IDs were not serviced by any vehicle trips"
        assert vehicles.index.isin(
            tnc_veh_trips.vehicle_id
        ).all(), "Some vehicle were created but do not serve any trips"
        assert (
            tnc_veh_trips.groupby("vehicle_id").size() >= 2
        ).all(), "Some vehicles do not have at least 2 trips"

        pass

    def check_refuel_needs(self, vehicle_trips_i, vehicles, time_bin):
        """Check to see if the vehicle needs to refuel.

        If the vehicle has been operating for more than the refuel interval,
        it needs be routed to the nearest refuel station before it becomes available again.
        """
        # updated drive time
        drive_time_from_trips_in_bin = vehicle_trips_i.groupby(
            "vehicle_id"
        ).OD_dist.sum()
        tot_drive_time = (
            drive_time_from_trips_in_bin
            + vehicles.loc[
                drive_time_from_trips_in_bin.index, "drive_dist_since_refuel"
            ]
        )
        needs_refuel = tot_drive_time > self.settings.max_refuel_dist

        if not needs_refuel.any():
            return None

        # find the nearest refuel station for each vehicle that needs to refuel
        vehs_to_refuel = (
            vehicle_trips_i[
                vehicle_trips_i.vehicle_id.isin(needs_refuel[needs_refuel].index)
            ]
            .drop_duplicates("vehicle_id", keep="last")
            .copy()
        )
        refuel_stations = self.landuse[
            self.landuse[self.settings.landuse_refuel_col] > 0
        ]
        refuel_stations_taz = (
            refuel_stations.TAZ
            if "TAZ" in refuel_stations.columns
            else refuel_stations.taz
        )
        refuel_stations_idx = refuel_stations_taz.map(self.skim_zone_mapping).to_numpy()

        # find skim times between trip origins and all veh locations
        skim_times = self.skim_time[
            np.ix_(vehs_to_refuel.destination_skim_idx.to_numpy(), refuel_stations_idx)
        ]

        # best station per vehicle
        station_choice_idx = np.argmin(skim_times, axis=1)
        min_times = skim_times[np.arange(len(station_choice_idx)), station_choice_idx]
        nearest_station_skim_idx = refuel_stations_idx[station_choice_idx]
        refuel_stations_maz = (
            refuel_stations.MAZ
            if "MAZ" in refuel_stations.columns
            else refuel_stations.mgra
        ).to_numpy()
        nearest_station_maz = refuel_stations_maz[station_choice_idx]

        # create refuel vehicle trips
        refuel_veh_trips = pd.DataFrame(
            {
                "vehicle_id": vehs_to_refuel.vehicle_id,
                "trip_i": np.nan,
                "trip_j": np.nan,
                "scenario": -1,
                "srv_trip_num": -1,
                "origin_skim_idx": vehs_to_refuel.destination_skim_idx,  # start refuel trip from last dropoff
                "destination_skim_idx": nearest_station_skim_idx,
                "origin": vehs_to_refuel.destination,  # start refuel trip from last dropoff
                "destination": nearest_station_maz,
                "servicing_trip_id": np.nan,
                "OD_time": min_times,
                "OD_dist": self.skim_dist[
                    vehs_to_refuel.destination_skim_idx.to_numpy(),
                    nearest_station_skim_idx,
                ],
                "depart_bin": vehs_to_refuel.arrival_bin,  # start refuel trip right after last trip
                "arrival_bin": vehs_to_refuel.arrival_bin
                + np.ceil(min_times / self.settings.time_bin_size).astype(
                    int
                ),  # assuming refuel trip takes the time to get to the station
                "trip_type": "refuel",
            }
        )
        logger.info(f"\tRouting {len(refuel_veh_trips)} vehicles to refuel stations")

        return refuel_veh_trips

    def remap_skim_idx_to_zone(self, tnc_veh_trips, pooled_trips, tnc_trips):
        # remap the skim idx back to zone ids for output
        skim_idx_to_taz_map = {v: k for k, v in self.skim_zone_mapping.items()}

        # map skim index back to taz
        tnc_veh_trips["origin_taz"] = tnc_veh_trips.origin_skim_idx.map(
            skim_idx_to_taz_map
        )
        tnc_veh_trips["destination_taz"] = tnc_veh_trips.destination_skim_idx.map(
            skim_idx_to_taz_map
        )

        pooled_trips["origin_taz"] = pooled_trips.origin_skim_idx.map(
            skim_idx_to_taz_map
        )
        pooled_trips["stop1_taz"] = pooled_trips.stop1_skim_idx.map(skim_idx_to_taz_map)
        pooled_trips["stop2_taz"] = pooled_trips.stop2_skim_idx.map(skim_idx_to_taz_map)
        pooled_trips["destination_taz"] = pooled_trips.destination_skim_idx.map(
            skim_idx_to_taz_map
        )

        missing_o = tnc_veh_trips["origin"].isna()
        missing_d = tnc_veh_trips["destination"].isna()
        assert (
            not missing_o.any()
        ), f"Some vehicle trips do not have an origin MAZ {tnc_veh_trips[missing_o]}"
        assert (
            not missing_d.any()
        ), f"Some vehicle trips do not have a destination MAZ {tnc_veh_trips[missing_d]}"

        # Drop skim indices after mapping back to zones/MAZ
        tnc_veh_trips.drop(
            columns=["origin_skim_idx", "destination_skim_idx"],
            inplace=True,
            errors="ignore",
        )
        pooled_trips.drop(
            columns=[
                "origin_skim_idx",
                "destination_skim_idx",
                "stop1_skim_idx",
                "stop2_skim_idx",
            ],
            inplace=True,
            errors="ignore",
        )

        return tnc_veh_trips, pooled_trips

    def write_outputs(self, tnc_veh_trips, pooled_trips, id_mapping_df):
        """Write final tables to output folder"""
        tnc_veh_trips.to_csv(
            os.path.join(self.settings.output_dir, "output_tnc_vehicle_trips.csv"),
            index=True,
        )
        pooled_trips.to_csv(
            os.path.join(self.settings.output_dir, "output_tnc_pooled_trips.csv"),
            index=False,
        )

        id_mapping_df.to_csv(
            os.path.join(self.settings.output_dir, "tnc_trip_id_mapping.csv"),
            index=False,
        )

    def route_taxi_tncs(self):
        """Main function to route taxi and TNC trips, including pooling logic."""
        # read in initial skim data
        self.read_skim_data(0)

        # read in taxi / tnc trip data
        tnc_trips, id_mapping_df = self.read_input_data()
        tnc_trips = self.sample_tnc_trip_simulation_time_bin(tnc_trips)

        # initialize vehicle fleet
        vehicles = None
        veh_trips = []
        pooling_trips = []

        # loop through each time bin and process trips
        for time_bin, tnc_trips_i in tnc_trips.groupby("time_bin"):
            logger.info(
                f"Processing time bin {time_bin} with {len(tnc_trips_i)} taxi / tnc trips:"
            )
            shared_tnc_trips = tnc_trips_i[
                tnc_trips_i.trip_mode.isin(self.settings.shared_tnc_modes)
            ]
            trip_pairs = self.determine_potential_trip_pairs(shared_tnc_trips.copy())

            trip_pairs = self.determine_detour_times(trip_pairs)

            trip_matches = self.select_mutual_best_recursive(trip_pairs)

            shared_trips_routed = self.create_trip_routes(trip_matches)

            full_trip_routes = self.create_full_trip_route_table(
                tnc_trips_i, shared_trips_routed
            )
            full_trip_routes["time_bin"] = time_bin

            vehicles, trip_to_veh_map = self.match_vehicles_to_trips(
                vehicles, full_trip_routes
            )

            vehicle_trips_i, full_trip_routes = self.create_vehicle_trips(
                full_trip_routes, vehicles, trip_to_veh_map
            )
            veh_trips.append(vehicle_trips_i)
            pooling_trips.append(full_trip_routes)

            refuel_veh_trips_i = self.check_refuel_needs(
                vehicle_trips_i, vehicles, time_bin
            )
            if refuel_veh_trips_i is not None:
                veh_trips.append(refuel_veh_trips_i)

            vehicles = self.update_vehicle_fleet(
                vehicles, vehicle_trips_i, time_bin, refuel_veh_trips_i
            )

        tnc_veh_trips = pd.concat(veh_trips)
        tnc_veh_trips["vehicle_trip_id"] = (
            tnc_veh_trips.vehicle_id * 1000
            + tnc_veh_trips.groupby("vehicle_id").cumcount()
            + 1
        )
        tnc_veh_trips.set_index("vehicle_trip_id", inplace=True)
        pooled_trips = pd.concat(pooling_trips)

        self.summarize_tnc_trips(tnc_veh_trips, tnc_trips, vehicles, pooled_trips)

        tnc_veh_trips, pooled_trips = self.remap_skim_idx_to_zone(
            tnc_veh_trips, pooled_trips, tnc_trips
        )

        self.write_outputs(tnc_veh_trips, pooled_trips, id_mapping_df)

        return tnc_veh_trips


if __name__ == "__main__":
    start_time = time.time()

    settings = load_settings(yaml_path="taxi_tnc_routing_settings.yaml")

    router = TaxiTNCRouter(settings)
    router.route_taxi_tncs()

    elapsed_time = time.time() - start_time
    logger.info(
        f"Time to complete: {elapsed_time:.2f} seconds = {elapsed_time/60:.2f} minutes"
    )

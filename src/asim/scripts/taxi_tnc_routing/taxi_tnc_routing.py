import numpy as np
import pandas as pd
import openmatrix as omx
import os
import time

input_folder = r"C:\Users\david.hensle\OneDrive - Resource Systems Group, Inc\Documents\projects\sandag\AV_TNC_models\tnc_data\full"


def read_input_data(input_folder, skim_core):
    # Read the data from the OpenMatrix file
    with omx.open_file(os.path.join(input_folder, "traffic_skims_AM.omx"), "r") as f:
        skim = np.array(f[skim_core])
        mapping = f.mapping(f.list_mappings()[0])

    # Convert the data to a pandas DataFrame
    cols = ["trip_id", "trip_mode", "depart", "origin", "destination"]
    # trips = pd.read_csv(os.path.join(input_folder, "final_trips.csv"), usecols=cols)
    # reading subset for faster I/O
    trips = pd.read_csv(os.path.join(input_folder, "final_tnc_trips.csv"), usecols=cols)
    trips = trips[trips.trip_mode.isin(["TAXI", "TNC_SINGLE", "TNC_SHARED"])]

    landuse = pd.read_csv(os.path.join(input_folder, "land_use.csv"))
    maz_to_taz_map = landuse.set_index("MAZ")["taz"].to_dict()

    trips["oskim_idx"] = trips["origin"].map(maz_to_taz_map).map(mapping)
    trips["dskim_idx"] = trips["destination"].map(maz_to_taz_map).map(mapping)

    return trips, skim, mapping


def determine_time_bin(trips, bin_size):
    # Create a new column for the time bin
    # Convert departure time to mins, sampling within the half hour
    # Then bin into the bin size (supplied in mins)
    trips["time_in_mins"] = trips["depart"] * 30 + np.random.randint(
        0, 30, size=len(trips)
    )
    trips["time_bin"] = trips["time_in_mins"] // bin_size

    return trips


def determine_potential_trip_pairs(trips, skim_data, pooling_buffer=10):
    # pre-filter trips for pooling
    # find pairs of trips that are within the same origin and destination buffer
    # Ensure indices are integer arrays and no NA
    o_idx = trips["oskim_idx"].to_numpy(dtype=int)
    d_idx = trips["dskim_idx"].to_numpy(dtype=int)
    trip_ids = trips["trip_id"].to_numpy()

    origin_proximity_mask = skim_data[o_idx, :] <= pooling_buffer
    destination_proximity_mask = skim_data[d_idx, :] <= pooling_buffer

    # From your masks (shape: n_trips x n_zones), select only the columns for the trips’ zones
    # This yields trip-to-trip boolean matrices
    origin_pair_ok = origin_proximity_mask[:, o_idx]  # shape: n_trips x n_trips
    dest_pair_ok = destination_proximity_mask[:, d_idx]  # shape: n_trips x n_trips

    # Both origin and destination must be within the buffer
    pair_ok = origin_pair_ok & dest_pair_ok

    # Drop self-pairs and keep i<j to avoid duplicates
    np.fill_diagonal(pair_ok, False)
    ii, jj = np.where(np.triu(pair_ok, k=1))
    print(
        f"\tFound {len(ii)} potential trip pairs from {len(trips)} trips or {(len(trips) **2)/ len(ii)}% of n**2"
    )

    trip_pairs = pd.DataFrame(
        {"trip_i": trip_ids[ii], "trip_j": trip_ids[jj]}, index=None
    )

    # include skim times for origins/destinations
    o2o_time = skim_data[o_idx][:, o_idx]
    d2d_time = skim_data[d_idx][:, d_idx]
    trip_pairs["origin_time"] = o2o_time[ii, jj]
    trip_pairs["dest_time"] = d2d_time[ii, jj]
    # also include the origin and destination skim idxs
    # create maps from trip id to origin and to destination
    lookup = trips.loc[:, ["trip_id", "oskim_idx", "dskim_idx"]].set_index("trip_id")

    trip_pairs = trip_pairs.join(
        lookup.rename(
            columns={"oskim_idx": "o_i_skim_idx", "dskim_idx": "d_i_skim_idx"}
        ),
        on="trip_i",
    ).join(
        lookup.rename(
            columns={"oskim_idx": "o_j_skim_idx", "dskim_idx": "d_j_skim_idx"}
        ),
        on="trip_j",
    )
    return trip_pairs


def determine_detour_times(trip_pairs, skim_data, max_detour=15):
    # For each trip pair, calculate the four possible routing scenarios
    # and determine if they are valid (within detour limits)
    # and select the best valid scenario (minimum total time)

    oi = trip_pairs["o_i_skim_idx"].to_numpy()
    oj = trip_pairs["o_j_skim_idx"].to_numpy()
    di = trip_pairs["d_i_skim_idx"].to_numpy()
    dj = trip_pairs["d_j_skim_idx"].to_numpy()

    # direct times
    i_direct = skim_data[oi, di]
    j_direct = skim_data[oj, dj]

    # scenario totals
    # WARNING if these scenarios are changed, they also need to be changed downstream in the routing
    t1 = skim_data[oi, oj] + skim_data[oj, di] + skim_data[di, dj]  # oi->oj->di->dj
    t2 = skim_data[oj, oi] + skim_data[oi, dj] + skim_data[dj, di]  # oj->oi->dj->di
    t3 = skim_data[oi, oj] + skim_data[oj, dj] + skim_data[dj, di]  # oi->oj->dj->di
    t4 = skim_data[oj, oi] + skim_data[oi, di] + skim_data[di, dj]  # oj->oi->di->dj

    # per-trip detours
    d1_i = t1 - i_direct
    d1_j = t1 - j_direct
    d2_i = t2 - i_direct
    d2_j = t2 - j_direct
    d3_i = t3 - i_direct
    d3_j = t3 - j_direct
    d4_i = t4 - i_direct
    d4_j = t4 - j_direct

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

    assert (
        trip_pairs[trip_pairs.valid]["detour_i"] < max_detour
    ).all(), "Detour times exceed maximum allowed"
    assert (
        trip_pairs[trip_pairs.valid]["detour_j"] < max_detour
    ).all(), "Detour times exceed maximum allowed"
    assert (
        trip_pairs[trip_pairs.valid]["route_scenario"] > 0
    ).all(), "Valid trips must have a scenario"

    print(f"\tTotal trip pairs within original OD buffer: {len(trip_pairs)}")
    trip_pairs = trip_pairs[trip_pairs.valid]
    print(
        f"\tFound {len(trip_pairs)} valid trip pairs across {trip_pairs['trip_i'].nunique()} unique trips"
    )
    print(
        f"\twith an average detour times for trip i {trip_pairs['detour_i'].mean()} and trip j {trip_pairs['detour_j'].mean()}"
    )

    return trip_pairs


def select_mutual_best_recursive(trip_pairs: pd.DataFrame) -> pd.DataFrame:
    # start from valid pairs only, keep only needed cols for speed
    pairs = trip_pairs.loc[
        trip_pairs["valid"], ["trip_i", "trip_j", "total_detour"]
    ].copy()
    if pairs.empty:
        return trip_pairs.iloc[0:0].copy()

    # deterministic tie-breaking
    pairs = pairs.sort_values(["total_detour", "trip_i", "trip_j"]).reset_index(
        drop=True
    )

    batches = []
    used = set()
    while not pairs.empty:
        # best partner for each i, and best partner for each j
        best_j_for_i_idx = pairs.groupby("trip_i", sort=False)["total_detour"].idxmin()
        best_j_for_i = pairs.loc[best_j_for_i_idx, ["trip_i", "trip_j", "total_detour"]]

        best_i_for_j_idx = pairs.groupby("trip_j", sort=False)["total_detour"].idxmin()
        best_i_for_j = pairs.loc[best_i_for_j_idx, ["trip_j", "trip_i", "total_detour"]]
        best_i_for_j.columns = ["trip_j", "trip_i", "detour_time_j"]  # rename for merge

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

    print(f"\tSelected {len(trip_matches)} disjoint pairs via recursive mutual-best")
    print(
        f"\twith an average detour times for trip i {trip_matches['detour_i'].mean()} and trip j {trip_matches['detour_j'].mean()}"
    )
    print(f"\tAverage total detour time {trip_matches['total_detour'].mean()}")
    print(f"\tAverage total in vehicle time {trip_matches['total_ivt'].mean()}")

    return trip_matches


def create_trip_routes(trip_matches):
    trips_routed = trip_matches[["trip_i", "trip_j", "total_ivt"]].copy()

    s1_idx = [0, 1, 2, 3]  # oi->oj->di->dj
    s2_idx = [1, 0, 3, 2]  # oj->oi->dj->di
    s3_idx = [0, 1, 3, 2]  # oi->oj->dj->di
    s4_idx = [1, 0, 2, 3]  # oj->oi->di->dj

    ods = trip_matches[
        ["o_i_skim_idx", "o_j_skim_idx", "d_i_skim_idx", "d_j_skim_idx"]
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
    if valid_mask.any():
        row_idx = np.arange(len(trips_routed))[valid_mask]
        ordered_nodes[valid_mask] = ods[row_idx[:, None], order_idx]

    trips_routed[
        ["origin_skim_idx", "stop1_skim_idx", "stop2_skim_idx", "destination_skim_idx"]
    ] = ordered_nodes
    return trips_routed


def create_full_trip_route_table(tnc_trips, trips_routed, skim_data):
    # need to append any additional tnc trips during this time period that were not grouped
    mask = ~(
        tnc_trips.trip_id.isin(trips_routed.trip_i)
        | tnc_trips.trip_id.isin(trips_routed.trip_j)
    )
    cols = {
        "trip_id": "trip_i",
        "oskim_idx": "origin_skim_idx",
        "dskim_idx": "destination_skim_idx",
    }
    single_trips = tnc_trips[mask][cols.keys()].rename(columns=cols).copy()
    single_trips["total_ivt"] = skim_data[
        single_trips.origin_skim_idx, single_trips.destination_skim_idx
    ]

    full_trip_routes = pd.concat([trips_routed, single_trips], ignore_index=True)

    print(
        f"\tAdded {single_trips.shape[0]} single trips for a total of {full_trip_routes.shape[0]} episodes."
    )

    return full_trip_routes


def create_vehicle_trips(vehicle_trips, full_trip_routes):
    # Create vehicle trips from the full trip routes
    pass
    return vehicle_trips


def route_taxi_tncs():
    tnc_trips, skim, mapping = read_input_data(input_folder, "SOV_TR_H_TIME__AM")

    skim_data = np.array(skim)

    tnc_trips = determine_time_bin(tnc_trips, bin_size=15)

    vehicle_trips = pd.DataFrame(
        columns=["vehicle_id", "origin", "destination", "time_bin", "trip_i", "trip_j"]
    )

    for _, tnc_trips_i in tnc_trips.groupby("time_bin"):
        print(
            f"Processing time bin {tnc_trips_i['time_bin'].iloc[0]} with {len(tnc_trips_i)} taxi / tnc trips:"
        )
        trip_pairs = determine_potential_trip_pairs(
            tnc_trips_i.copy(), skim_data, pooling_buffer=10
        )

        trip_pairs = determine_detour_times(trip_pairs, skim_data)

        trip_matches = select_mutual_best_recursive(trip_pairs)

        shared_trips_routed = create_trip_routes(trip_matches)

        full_trip_routes = create_full_trip_route_table(
            tnc_trips_i, shared_trips_routed, skim_data
        )

        vehicle_trips = create_vehicle_trips(vehicle_trips, full_trip_routes)

    return vehicle_trips


if __name__ == "__main__":
    start_time = time.time()

    vehicle_trips = route_taxi_tncs()
    print(f"Created {len(vehicle_trips)} vehicle trips.")
    
    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"Time to complete: {elapsed_time:.2f} seconds")
